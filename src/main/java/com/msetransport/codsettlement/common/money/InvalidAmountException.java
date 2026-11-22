package com.benzaazaa.codsettlement.common.money;

/** Thrown when a value cannot be interpreted as a valid money amount. */
public class InvalidAmountException extends IllegalArgumentException {

    public InvalidAmountException(String message) {
        super(message);
    }
}
