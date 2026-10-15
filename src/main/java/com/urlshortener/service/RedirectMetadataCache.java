package com.urlshortener.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.urlshortener.config.RedirectCacheProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class RedirectMetadataCache {

    private static final int GENERATION_STRIPES = 64;

    private final Cache<String, RedirectTarget> cache;
    private final AtomicLongArray invalidationGenerations = new AtomicLongArray(GENERATION_STRIPES);
    private final Object invalidationLock = new Object();

    public RedirectMetadataCache(RedirectCacheProperties properties, MeterRegistry meterRegistry) {
        cache = Caffeine.newBuilder()
                .maximumSize(properties.maxSize())
                .expireAfterWrite(properties.ttl())
                .recordStats()
                .build();
        CaffeineCacheMetrics.monitor(meterRegistry, cache, "redirect");
    }

    public RedirectTarget getOrLoad(String code, Function<String, RedirectTarget> loader) {
        RedirectTarget cached = cache.getIfPresent(code);
        if (cached != null) {
            return cached;
        }

        int stripe = stripeIndex(code);
        long generation = invalidationGenerations.get(stripe);
        RedirectTarget loaded = loader.apply(code);
        synchronized (invalidationLock) {
            if (generation == invalidationGenerations.get(stripe)) {
                cache.put(code, loaded);
            }
        }
        return loaded;
    }

    public void invalidate(String code) {
        synchronized (invalidationLock) {
            invalidationGenerations.incrementAndGet(stripeIndex(code));
            cache.invalidate(code);
        }
    }

    private static int stripeIndex(String code) {
        return code.hashCode() & (GENERATION_STRIPES - 1);
    }

    long estimatedSize() {
        cache.cleanUp();
        return cache.estimatedSize();
    }
}
