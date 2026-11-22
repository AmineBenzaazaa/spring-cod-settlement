package com.benzaazaa.codsettlement.settlement.service;

import com.benzaazaa.codsettlement.common.error.InvalidRequestException;
import java.util.List;

/** The uploaded file is rejected as a whole; {@code getMessages()} lists every problem found. */
public class CsvValidationException extends InvalidRequestException {

    public CsvValidationException(List<String> errors) {
        super("file", errors);
    }

    public CsvValidationException(String error) {
        this(List.of(error));
    }
}
