package com.urlshortener.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.urlshortener.domain.ShortLink;
import com.urlshortener.error.ApiException;
import com.urlshortener.error.ErrorCode;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class LinkCursorCodecTest {

    private final LinkCursorCodec codec = new LinkCursorCodec();

    @Test
    void roundTripsTimestampAndCode() {
        Instant createdAt = Instant.parse("2026-10-14T17:05:00.123456Z");
        ShortLink link = new ShortLink("Ab3CdEf", "https://example.com/", true, createdAt, "https://example.com/");

        String cursor = codec.encode(link);

        assertEquals(new LinkCursorCodec.Position(createdAt, "Ab3CdEf"), codec.decode(cursor));
    }

    @Test
    void rejectsMalformedAndOversizedCursors() {
        assertInvalid("");
        assertInvalid("not-a-cursor");
        assertInvalid("a".repeat(129));
    }

    private void assertInvalid(String cursor) {
        ApiException exception = assertThrows(ApiException.class, () -> codec.decode(cursor));
        assertEquals(ErrorCode.INVALID_CURSOR, exception.code());
    }
}