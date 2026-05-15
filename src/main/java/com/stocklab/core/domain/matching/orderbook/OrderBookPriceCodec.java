package com.stocklab.core.domain.matching.orderbook;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class OrderBookPriceCodec {

    public static final int SCALE = 4;

    private static final BigDecimal MULTIPLIER = BigDecimal.TEN.pow(SCALE);

    private OrderBookPriceCodec() {
    }

    public static long encode(BigDecimal price) {
        return price.setScale(SCALE, RoundingMode.UNNECESSARY)
                .multiply(MULTIPLIER)
                .longValueExact();
    }

    public static BigDecimal decode(long priceTicks) {
        return BigDecimal.valueOf(priceTicks, SCALE);
    }

    public static BigDecimal decode(double score) {
        return decode((long) score);
    }
}
