package com.urlshortener.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.urlshortener.config.RedirectCacheProperties;
import com.urlshortener.domain.ShortLink;
import com.urlshortener.error.ApiException;
import com.urlshortener.repository.ShortLinkRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LinkServiceCacheTest {

    @Test
    void repeatedRedirectResolutionReadsTheRepositoryOnce() {
        String code = "Ab3CdEf";
        ShortLinkRepository repository = mock(ShortLinkRepository.class);
        when(repository.findById(code)).thenReturn(Optional.of(new ShortLink(
                code, "https://example.com/", true, Instant.now(), "https://example.com/")));
        LinkService service = new LinkService(
                repository,
                null,
                null,
                null,
                null,
                new RedirectMetadataCache(
                        new RedirectCacheProperties(10, Duration.ofMinutes(5)), new SimpleMeterRegistry()));

        RedirectTarget first = service.resolveForRedirect(code);
        RedirectTarget second = service.resolveForRedirect(code);

        assertEquals(first, second);
        verify(repository, times(1)).findById(code);
    }

    @Test
    void unknownCodesAreNotCached() {
        String code = "Ab3CdEf";
        ShortLinkRepository repository = mock(ShortLinkRepository.class);
        when(repository.findById(code)).thenReturn(Optional.empty());
        LinkService service = new LinkService(
                repository,
                null,
                null,
                null,
                null,
                new RedirectMetadataCache(
                        new RedirectCacheProperties(10, Duration.ofMinutes(5)), new SimpleMeterRegistry()));

        assertThrows(ApiException.class, () -> service.resolveForRedirect(code));
        assertThrows(ApiException.class, () -> service.resolveForRedirect(code));

        verify(repository, times(2)).findById(code);
    }
}
