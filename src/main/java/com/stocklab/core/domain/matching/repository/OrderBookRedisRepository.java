package com.stocklab.core.domain.matching.repository;

import com.stocklab.core.domain.matching.orderbook.OrderBookLevel;
import com.stocklab.core.domain.matching.orderbook.OrderBookMatchCandidate;
import com.stocklab.core.domain.matching.orderbook.OrderBookPriceCodec;
import com.stocklab.core.domain.matching.orderbook.OrderBookSnapshot;
import com.stocklab.core.domain.order.Order;
import com.stocklab.core.domain.order.OrderSide;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    public Optional<BigDecimal> getBestOppositePrice(String stockCode, OrderSide orderSide) {
        if (orderSide == OrderSide.BUY) {
            return getBestPrice(sellKey(stockCode), false);
        }
        return getBestPrice(buyKey(stockCode), true);
    }

    public OrderBookSnapshot getSnapshot(String stockCode, int depth) {
        return new OrderBookSnapshot(
                stockCode,
                getLevels(buyKey(stockCode), true, depth),
                getLevels(sellKey(stockCode), false, depth)
        );
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

    private Optional<BigDecimal> getBestPrice(String bookKey, boolean highestFirst) {
        Set<ZSetOperations.TypedTuple<String>> entries = highestFirst
                ? redisTemplate.opsForZSet().reverseRangeWithScores(bookKey, 0, 0)
                : redisTemplate.opsForZSet().rangeWithScores(bookKey, 0, 0);

        if (entries == null || entries.isEmpty()) {
            return Optional.empty();
        }

        ZSetOperations.TypedTuple<String> entry = entries.iterator().next();
        if (entry.getScore() == null) {
            return Optional.empty();
        }
        return Optional.of(OrderBookPriceCodec.decode(entry.getScore()));
    }

    private List<OrderBookLevel> getLevels(String bookKey, boolean highestFirst, int depth) {
        Set<ZSetOperations.TypedTuple<String>> entries = highestFirst
                ? redisTemplate.opsForZSet().reverseRangeWithScores(bookKey, 0, depth - 1L)
                : redisTemplate.opsForZSet().rangeWithScores(bookKey, 0, depth - 1L);

        if (entries == null || entries.isEmpty()) {
            return List.of();
        }

        String stockCode = extractStockCode(bookKey);
        List<OrderBookLevel> levels = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> entry : entries) {
            if (entry.getValue() == null || entry.getScore() == null) {
                continue;
            }
            Long orderId = Long.valueOf(entry.getValue());
            BigDecimal price = OrderBookPriceCodec.decode(entry.getScore());
            BigDecimal quantity = getRemainingQuantity(stockCode, orderId).orElse(BigDecimal.ZERO);
            levels.add(new OrderBookLevel(orderId, price, quantity));
        }
        return levels;
    }

    private String extractStockCode(String bookKey) {
        int lastColon = bookKey.lastIndexOf(':');
        return bookKey.substring(lastColon + 1);
    }
}
