package com.urlshortener.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cache.redirect")
public record RedirectCacheProperties(long maxSize, Duration ttl) {

    public RedirectCacheProperties {
        if (maxSize < 1) {
            throw new IllegalArgumentException("app.cache.redirect.max-size must be positive");
        }
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("app.cache.redirect.ttl must be positive");
        }
    }
}
