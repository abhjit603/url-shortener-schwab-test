package com.urlshortener.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.urlshortener.config.RedirectCacheProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class RedirectMetadataCacheTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    @Test
    void invalidationPreventsAnOlderLoadFromRepopulatingStaleState() throws Exception {
        RedirectMetadataCache cache = cache(10, Duration.ofMinutes(5));
        String code = "Ab3CdEf";
        RedirectTarget active = new RedirectTarget(code, "https://example.com/", true);
        RedirectTarget inactive = new RedirectTarget(code, "https://example.com/", false);
        CountDownLatch loaderStarted = new CountDownLatch(1);
        CountDownLatch finishLoader = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            Future<RedirectTarget> oldLoad = executor.submit(() -> cache.getOrLoad(code, ignored -> {
                loaderStarted.countDown();
                try {
                    if (!finishLoader.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to finish cache load");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting to finish cache load", ex);
                }
                return active;
            }));

            assertTrue(loaderStarted.await(5, TimeUnit.SECONDS));
            cache.invalidate(code);
            finishLoader.countDown();
            assertEquals(active, oldLoad.get(5, TimeUnit.SECONDS));
            assertEquals(0, cache.estimatedSize());

            assertEquals(inactive, cache.getOrLoad(code, ignored -> inactive));
            assertEquals(1, cache.estimatedSize());
        } finally {
            finishLoader.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void cacheEnforcesSizeAndExpireAfterWrite() throws Exception {
        RedirectMetadataCache cache = cache(1, Duration.ofMillis(40));
        RedirectTarget first = new RedirectTarget("Ab3CdEf", "https://one.example/", true);
        RedirectTarget second = new RedirectTarget("Fg4HiJk", "https://two.example/", true);
        AtomicBoolean reloaded = new AtomicBoolean();

        cache.getOrLoad(first.code(), ignored -> first);
        cache.getOrLoad(second.code(), ignored -> second);
        assertTrue(cache.estimatedSize() <= 1);

        Thread.sleep(100);
        RedirectTarget refreshed = cache.getOrLoad(second.code(), ignored -> {
            reloaded.set(true);
            return second;
        });

        assertEquals(second, refreshed);
        assertTrue(reloaded.get());
    }

    @Test
    void cacheStatisticsAreRegisteredWithoutLinkCodeTags() {
        RedirectMetadataCache cache = new RedirectMetadataCache(
                new RedirectCacheProperties(10, Duration.ofMinutes(5)), meterRegistry);
        RedirectTarget target = new RedirectTarget("Ab3CdEf", "https://example.com/", true);
        cache.getOrLoad(target.code(), ignored -> target);
        cache.getOrLoad(target.code(), ignored -> target);

        var cacheMeters = meterRegistry.getMeters().stream()
                .filter(meter -> meter.getId().getName().startsWith("cache."))
                .toList();
        assertTrue(cacheMeters.stream().anyMatch(meter -> meter.getId().getName().equals("cache.gets")));
        assertTrue(cacheMeters.stream().anyMatch(meter -> meter.getId().getName().equals("cache.size")));
        assertTrue(cacheMeters.stream().anyMatch(meter -> meter.getId().getName().equals("cache.evictions")));
        assertTrue(cacheMeters.stream()
                .flatMap(meter -> meter.getId().getTags().stream())
                .noneMatch(tag -> tag.getKey().equals("code")));
    }

    @Test
    void cacheBoundsMustBePositive() {
        assertThrows(IllegalArgumentException.class,
                () -> new RedirectCacheProperties(0, Duration.ofMinutes(1)));
        assertThrows(IllegalArgumentException.class,
                () -> new RedirectCacheProperties(1, Duration.ZERO));
    }

    private RedirectMetadataCache cache(long size, Duration ttl) {
        return new RedirectMetadataCache(new RedirectCacheProperties(size, ttl), meterRegistry);
    }
}
