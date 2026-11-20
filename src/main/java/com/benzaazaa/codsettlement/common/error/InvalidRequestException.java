package com.benzaazaa.codsettlement.common.error;

/** Input that passed Bean Validation but is still invalid (e.g. an unknown courier ID). Maps to 400. */
public class InvalidRequestException extends RuntimeException {

    private final String field;

    public InvalidRequestException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
