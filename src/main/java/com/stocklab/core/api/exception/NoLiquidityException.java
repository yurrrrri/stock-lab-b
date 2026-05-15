package com.stocklab.core.api.exception;

public class NoLiquidityException extends RuntimeException {

    public NoLiquidityException(String message) {
        super(message);
    }
}
