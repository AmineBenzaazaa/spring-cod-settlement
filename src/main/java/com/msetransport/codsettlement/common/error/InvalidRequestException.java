package com.msetransport.codsettlement.common.error;

import java.util.List;

/**
 * Input that passed Bean Validation but is still invalid (e.g. an unknown courier ID, or a
 * CSV file with bad rows). Maps to 400 with every message listed under {@code errors.<field>}.
 */
public class InvalidRequestException extends RuntimeException {

    private final String field;
    private final List<String> messages;

    public InvalidRequestException(String field, String message) {
        this(field, List.of(message));
    }

    public InvalidRequestException(String field, List<String> messages) {
        super(String.join("; ", messages));
        this.field = field;
        this.messages = List.copyOf(messages);
    }

    public String getField() {
        return field;
    }

    public List<String> getMessages() {
        return messages;
    }
}
