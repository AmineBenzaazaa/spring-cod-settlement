package com.benzaazaa.codsettlement.common.error;

/** The request clashes with the current state of the data (duplicates, locks). Maps to 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
