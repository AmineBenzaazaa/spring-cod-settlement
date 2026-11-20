package com.benzaazaa.codsettlement.common.error;

/**
 * A well-formed request that breaks a domain rule, e.g. "a delivered order cannot be
 * cancelled" or "matched results cannot be disputed". Maps to 422 Unprocessable Entity.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
