package com.urlshortener.error;

import jakarta.servlet.http.HttpServletRequest;
import com.urlshortener.web.RequestLoggingFilter;
import java.net.URI;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new ApiException.ValidationError(
                        f.getField(), f.getDefaultMessage()))
                .toList();

        ApiException api = new ApiException(
                ErrorCode.INVALID_URL,
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                errors);
        return handleApi(api, request);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApi(
            ApiException ex, HttpServletRequest request) {
        if (ex.code() == ErrorCode.STORE_UNAVAILABLE) {
            RequestLoggingFilter.logEvent("store-error", null, ex.code().name());
        } else if (ex.status().is4xxClientError()) {
            RequestLoggingFilter.logEvent("reject", null, ex.code().name());
        }
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                ex.status(), ex.getMessage());
        pd.setTitle(titleFor(ex.code()));
        pd.setType(URI.create("urn:url-shortener:problem:" + ex.code().name()));
        pd.setProperty("code", ex.code().name());
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setInstance(URI.create(request.getRequestURI()));
        if (!ex.validationErrors().isEmpty()) {
            pd.setProperty("validationErrors", ex.validationErrors());
        }
        return ResponseEntity.status(ex.status()).body(pd);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleOther(
            Exception ex, HttpServletRequest request) {
        RequestLoggingFilter.logEvent("internal-error", null, ErrorCode.INTERNAL_ERROR.name());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "The request could not be completed due to an internal error.");
        pd.setTitle("Internal error");
        pd.setType(URI.create("urn:url-shortener:problem:INTERNAL_ERROR"));
        pd.setProperty("code", ErrorCode.INTERNAL_ERROR.name());
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
    }

    private static String titleFor(ErrorCode code) {
        return switch (code) {
            case INVALID_URL -> "Invalid URL";
            case INVALID_CURSOR -> "Invalid cursor";
            case LINK_NOT_FOUND -> "Link not found";
            case LINK_INACTIVE -> "Link inactive";
            case SHORT_CODE_COLLISION_EXHAUSTED -> "Short code collision exhausted";
            case INTERNAL_ERROR -> "Internal error";
            case STORE_UNAVAILABLE -> "Store unavailable";
        };
    }
}
