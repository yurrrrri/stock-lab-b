package com.stocklab.core.domain.matching.repository;

import com.stocklab.core.domain.matching.orderbook.OrderBookMatchCandidate;
import com.stocklab.core.domain.matching.orderbook.OrderBookPriceCodec;
import com.stocklab.core.domain.order.Order;
import com.stocklab.core.domain.order.OrderSide;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderBookRedisRepository {

    private static final String BUY_KEY_PREFIX = "ORDERBOOK:BUY:";
    private static final String SELL_KEY_PREFIX = "ORDERBOOK:SELL:";
    private static final String QTY_KEY_PREFIX = "ORDERBOOK:QTY:";

    private final StringRedisTemplate redisTemplate;

    private DefaultRedisScript<List> findCrossingMatchScript;

    @PostConstruct
    void initScripts() {
        findCrossingMatchScript = new DefaultRedisScript<>();
        findCrossingMatchScript.setResultType(List.class);
        findCrossingMatchScript.setScriptSource(
                new ResourceScriptSource(new ClassPathResource("redis/find-crossing-match.lua"))
        );
    }

    public void addOrder(
            String stockCode,
            String orderSide,
            Long orderId,
            BigDecimal price,
            BigDecimal quantity
    ) {
        String bookKey = getBookKey(stockCode, orderSide);
        String member = orderId.toString();
        long priceTicks = OrderBookPriceCodec.encode(price);

        Boolean added = redisTemplate.opsForZSet().add(bookKey, member, priceTicks);
        if (Boolean.TRUE.equals(added)) {
            redisTemplate.opsForHash().put(qtyKey(stockCode), member, quantity.toPlainString());
        }
    }

    public Optional<OrderBookMatchCandidate> findCrossingMatch(String stockCode) {
        List<String> result = redisTemplate.execute(
                findCrossingMatchScript,
                List.of(buyKey(stockCode), sellKey(stockCode))
        );

        if (result == null || result.isEmpty()) {
            return Optional.empty();
        }

        Long buyOrderId = Long.valueOf(result.get(0));
        Long sellOrderId = Long.valueOf(result.get(1));
        BigDecimal matchPrice = OrderBookPriceCodec.decode(Long.parseLong(result.get(2)));

        return Optional.of(new OrderBookMatchCandidate(buyOrderId, sellOrderId, matchPrice));
    }

    public void syncOrderInBook(String stockCode, Order order) {
        if (!order.isCancelable() || order.getRemainingQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            removeOrder(stockCode, order.getOrderSide().name(), order.getOrderId());
            return;
        }

        String member = order.getOrderId().toString();
        redisTemplate.opsForHash().put(
                qtyKey(stockCode),
                member,
                order.getRemainingQuantity().toPlainString()
        );

        String bookKey = order.getOrderSide() == OrderSide.BUY
                ? buyKey(stockCode)
                : sellKey(stockCode);
        long priceTicks = OrderBookPriceCodec.encode(order.getPrice());
        redisTemplate.opsForZSet().add(bookKey, member, priceTicks);
    }

    public Optional<BigDecimal> getRemainingQuantity(String stockCode, Long orderId) {
        Object value = redisTemplate.opsForHash().get(qtyKey(stockCode), orderId.toString());
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(new BigDecimal(value.toString()));
    }

    public void removeOrder(String stockCode, String orderSide, Long orderId) {
        String member = orderId.toString();
        redisTemplate.opsForZSet().remove(getBookKey(stockCode, orderSide), member);
        redisTemplate.opsForHash().delete(qtyKey(stockCode), member);
    }

    private String buyKey(String stockCode) {
        return BUY_KEY_PREFIX + stockCode;
    }

    private String sellKey(String stockCode) {
        return SELL_KEY_PREFIX + stockCode;
    }

    private String qtyKey(String stockCode) {
        return QTY_KEY_PREFIX + stockCode;
    }

    private String getBookKey(String stockCode, String orderSide) {
        return orderSide.equalsIgnoreCase("BUY") ? buyKey(stockCode) : sellKey(stockCode);
    }
}
