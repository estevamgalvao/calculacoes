package com.estevam.calculacoes.core.exception;

import com.estevam.calculacoes.core.response.Response;
import com.estevam.calculacoes.parser.exception.CsvParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Global exception handler for the application.
 * Converts exceptions into appropriate HTTP responses.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles CSV parsing errors.
     */
    @ExceptionHandler(CsvParseException.class)
    public ResponseEntity<Response<Void>> handleCsvParseException(CsvParseException ex) {
        Response<Void> error = new Response<>(
            false,
            HttpStatus.BAD_REQUEST.value(),
            "CSV_PARSE_ERROR",
            ex.getMessage(),
            null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles file upload size exceeded.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Response<Void>> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        Response<Void> error = new Response<>(
            false,
            HttpStatus.PAYLOAD_TOO_LARGE.value(),
            "FILE_TOO_LARGE",
            "The uploaded file exceeds the maximum allowed size",
            null
        );
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }

    /**
     * Handles all other unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response<Void>> handleGenericException(Exception ex) {
        Response<Void> error = new Response<>(
            false,
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "INTERNAL_ERROR",
            "An unexpected error occurred: " + ex.getMessage(),
            null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}