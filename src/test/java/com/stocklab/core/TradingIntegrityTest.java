package com.stocklab.core;

import com.stocklab.core.api.dto.OrderRequest;
import com.stocklab.core.domain.auth.User;
import com.stocklab.core.domain.auth.repository.UserRepository;
import com.stocklab.core.domain.matching.repository.OrderBookRedisRepository;
import com.stocklab.core.domain.order.Order;
import com.stocklab.core.domain.order.OrderSide;
import com.stocklab.core.domain.order.OrderType;
import com.stocklab.core.domain.order.event.OrderEvent;
import com.stocklab.core.domain.order.repository.OrderRepository;
import com.stocklab.core.domain.order.service.OrderEventProcessor;
import com.stocklab.core.domain.order.service.OrderService;
import com.stocklab.core.domain.portfolio.Portfolio;
import com.stocklab.core.domain.portfolio.UserStock;
import com.stocklab.core.domain.portfolio.repository.PortfolioRepository;
import com.stocklab.core.domain.portfolio.repository.UserStockRepository;
import com.stocklab.core.domain.stock.Stock;
import com.stocklab.core.domain.stock.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class TradingIntegrityTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderEventProcessor orderEventProcessor;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private UserStockRepository userStockRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderBookRedisRepository orderBookRedisRepository;

    private User buyer;
    private User seller;
    private Stock stock;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        userStockRepository.deleteAll();
        portfolioRepository.deleteAll();
        userRepository.deleteAll();
        stockRepository.deleteAll();

        buyer = userRepository.save(User.builder()
                .email("buyer@test.com")
                .passwordHash("hash")
                .nickname("Buyer")
                .build());
        
        seller = userRepository.save(User.builder()
                .email("seller@test.com")
                .passwordHash("hash")
                .nickname("Seller")
                .build());

        stock = stockRepository.save(Stock.builder()
                .stockCode("AAPL")
                .stockName("Apple")
                .marketType("NASDAQ")
                .isTrading(true)
                .build());

        portfolioRepository.save(Portfolio.builder().user(buyer).cashBalance(BigDecimal.valueOf(1000000)).build());
        portfolioRepository.save(Portfolio.builder().user(seller).cashBalance(BigDecimal.valueOf(0)).build());
        
        userStockRepository.save(UserStock.builder()
                .user(seller)
                .stock(stock)
                .quantity(BigDecimal.valueOf(100))
                .averagePrice(BigDecimal.valueOf(150))
                .build());
    }

    @Test
    @DisplayName("사용자당 분산 락이 트랜잭션을 감싸고 있어 동시 주문 시 잔고 정합성이 유지되어야 한다")
    void concurrentUserOrderTest() throws InterruptedException {
        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        OrderRequest request = new OrderRequest();
        request.setUserId(buyer.getId());
        request.setStockCode(stock.getStockCode());
        request.setOrderType(OrderType.LIMIT);
        request.setOrderSide(OrderSide.BUY);
        request.setPrice(BigDecimal.valueOf(200000)); // 20만 원
        request.setQuantity(BigDecimal.ONE);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            executorService.execute(() -> {
                try {
                    orderService.placeOrder(buyer.getId(), request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        Portfolio portfolio = portfolioRepository.findById(buyer.getId()).get();
        // 5개 주문 모두 성공 시 100만 원(20만 * 5)이 FrozenBalance에 있어야 함
        assertThat(portfolio.getFrozenBalance()).isEqualByComparingTo(BigDecimal.valueOf(1000000));
        assertThat(portfolio.getCashBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(successCount.get()).isEqualTo(5);
    }

    @Test
    @DisplayName("시장가 매수 주문 시 5% 버퍼가 적용되어 예약되어야 하며, 체결 시 차액이 환불되어야 한다")
    void marketOrderSlippageBufferTest() {
        // 1. 매도 호가 생성 (100,000원)
        OrderRequest sellRequest = new OrderRequest();
        sellRequest.setUserId(seller.getId());
        sellRequest.setStockCode(stock.getStockCode());
        sellRequest.setOrderType(OrderType.LIMIT);
        sellRequest.setOrderSide(OrderSide.SELL);
        sellRequest.setPrice(BigDecimal.valueOf(100000));
        sellRequest.setQuantity(BigDecimal.ONE);
        
        Long sellOrderId = orderService.placeOrder(seller.getId(), sellRequest);
        orderEventProcessor.process(createEvent(sellOrderId, sellRequest));

        // 2. 매수 시장가 주문
        OrderRequest buyRequest = new OrderRequest();
        buyRequest.setUserId(buyer.getId());
        buyRequest.setStockCode(stock.getStockCode());
        buyRequest.setOrderType(OrderType.MARKET);
        buyRequest.setOrderSide(OrderSide.BUY);
        buyRequest.setQuantity(BigDecimal.ONE);

        Long buyOrderId = orderService.placeOrder(buyer.getId(), buyRequest);
        
        // 검증: 예약금은 100,000원 + 5% = 105,000원이어야 함
        Portfolio buyerPortfolio = portfolioRepository.findById(buyer.getId()).get();
        assertThat(buyerPortfolio.getFrozenBalance()).isEqualByComparingTo(BigDecimal.valueOf(105000));

        // 3. 체결 처리
        orderEventProcessor.process(createEvent(buyOrderId, buyRequest));

        // 4. 최종 결과 검증
        // 체결가는 100,000원. 예약금 105,000원 중 5,000원은 환불되어야 함.
        buyerPortfolio = portfolioRepository.findById(buyer.getId()).get();
        assertThat(buyerPortfolio.getCashBalance()).isEqualByComparingTo(BigDecimal.valueOf(900000)); // 100만 - 10만
        assertThat(buyerPortfolio.getFrozenBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        
        Portfolio sellerPortfolio = portfolioRepository.findById(seller.getId()).get();
        assertThat(sellerPortfolio.getCashBalance()).isEqualByComparingTo(BigDecimal.valueOf(100000));
    }

    private OrderEvent createEvent(Long orderId, OrderRequest request) {
        Order order = orderRepository.findById(orderId).get();
        return OrderEvent.builder()
                .orderId(orderId)
                .userId(request.getUserId())
                .stockCode(request.getStockCode())
                .orderType(order.getOrderType())
                .orderSide(order.getOrderSide())
                .price(order.getPrice())
                .quantity(order.getQuantity())
                .build();
    }
}
