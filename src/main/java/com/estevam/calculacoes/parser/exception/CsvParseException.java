package com.estevam.calculacoes.parser.exception;

/**
 * Exception thrown when CSV parsing fails.
 */
public class CsvParseException extends Exception {
    public CsvParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
