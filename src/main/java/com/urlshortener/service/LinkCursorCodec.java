package com.urlshortener.service;

import com.urlshortener.domain.ShortLink;
import com.urlshortener.error.ApiException;
import com.urlshortener.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class LinkCursorCodec {

    private static final int MAX_CURSOR_LENGTH = 128;
    private static final Pattern CODE_PATTERN =
            Pattern.compile("[23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]{7}");

    public String encode(ShortLink link) {
        String payload = link.getCreatedAt() + "\n" + link.getCode();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public Position decode(String cursor) {
        if (cursor.isBlank() || cursor.length() > MAX_CURSOR_LENGTH) {
            throw invalidCursor();
        }
        try {
            String payload = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int separator = payload.indexOf('\n');
            if (separator <= 0 || separator != payload.lastIndexOf('\n') || separator == payload.length() - 1) {
                throw invalidCursor();
            }
            Instant createdAt = Instant.parse(payload.substring(0, separator));
            String code = payload.substring(separator + 1);
            if (!CODE_PATTERN.matcher(code).matches()) {
                throw invalidCursor();
            }
            return new Position(createdAt, code);
        } catch (IllegalArgumentException | DateTimeParseException ex) {
            throw invalidCursor();
        }
    }

    private static ApiException invalidCursor() {
        return new ApiException(ErrorCode.INVALID_CURSOR, HttpStatus.BAD_REQUEST, "Cursor is malformed");
    }

    public record Position(Instant createdAt, String code) {
    }
}