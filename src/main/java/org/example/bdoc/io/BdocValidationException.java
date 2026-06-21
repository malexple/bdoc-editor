package org.example.bdoc.io;

import java.util.List;

public class BdocValidationException extends RuntimeException {

    private final List<String> errors;

    public BdocValidationException(String message, List<String> errors) {
        super(message + System.lineSeparator() + String.join(System.lineSeparator(), errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}