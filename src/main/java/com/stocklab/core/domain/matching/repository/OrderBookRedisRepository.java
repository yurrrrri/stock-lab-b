package com.stocklab.core.domain.matching.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class OrderBookRedisRepository {

    private final StringRedisTemplate redisTemplate;
    private static final String BUY_KEY_PREFIX = "ORDERBOOK:BUY:";
    private static final String SELL_KEY_PREFIX = "ORDERBOOK:SELL:";

    /**
     * Add an order to the order book.
     * Score is the price.
     * Member is a unique string (e.g., orderId).
     */
    public void addOrder(String stockCode, String orderSide, Long orderId, BigDecimal price) {
        String key = getKey(stockCode, orderSide);
        redisTemplate.opsForZSet().add(key, orderId.toString(), price.doubleValue());
    }

    /**
     * Remove an order from the order book.
     */
    public void removeOrder(String stockCode, String orderSide, Long orderId) {
        String key = getKey(stockCode, orderSide);
        redisTemplate.opsForZSet().remove(key, orderId.toString());
    }

    /**
     * Get the best (highest) buy price.
     */
    public Set<ZSetOperations.TypedTuple<String>> getBestBuyOrders(String stockCode, int count) {
        String key = BUY_KEY_PREFIX + stockCode;
        return redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, count - 1);
    }

    /**
     * Get the best (lowest) sell price.
     */
    public Set<ZSetOperations.TypedTuple<String>> getBestSellOrders(String stockCode, int count) {
        String key = SELL_KEY_PREFIX + stockCode;
        return redisTemplate.opsForZSet().rangeWithScores(key, 0, count - 1);
    }

    private String getKey(String stockCode, String orderSide) {
        return (orderSide.equalsIgnoreCase("BUY") ? BUY_KEY_PREFIX : SELL_KEY_PREFIX) + stockCode;
    }
}
