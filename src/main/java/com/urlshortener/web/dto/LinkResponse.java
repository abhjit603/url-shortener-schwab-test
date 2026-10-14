package com.urlshortener.web.dto;

import java.time.Instant;

public record LinkResponse(
        String code,
        String shortUrl,
        String destination,
        boolean active,
        Instant createdAt
) {
}
