package com.urlshortener.error;

import java.util.List;
import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final HttpStatus status;
    private final List<ValidationError> validationErrors;

    public ApiException(ErrorCode code, HttpStatus status, String detail) {
        this(code, status, detail, List.of());
    }

    public ApiException(
            ErrorCode code,
            HttpStatus status,
            String detail,
            List<ValidationError> validationErrors
    ) {
        super(detail);
        this.code = code;
        this.status = status;
        this.validationErrors = List.copyOf(validationErrors);
    }

    public ErrorCode code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }

    public List<ValidationError> validationErrors() {
        return validationErrors;
    }

    public record ValidationError(String field, String message) {
    }
}
