package com.urlshortener.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.urlshortener.domain.ShortLink;
import com.urlshortener.error.ApiException;
import com.urlshortener.error.ErrorCode;
import com.urlshortener.service.AnalyticsService;
import com.urlshortener.service.LinkService;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RedirectControllerTest {

    @Test
    void analyticsFailurePreventsRedirectAndReturnsStoreUnavailable() {
        String code = "Ab3CdEf";
    LinkService linkService = serviceReturning(
        new ShortLink(code, "https://example.com/", true, Instant.now(), "https://example.com/"));
    AnalyticsService analyticsService = new AnalyticsService(null, null) {
        @Override
        public void incrementCurrentMinute(String ignoredCode) {
        throw new IllegalStateException("store unavailable");
        }
    };

    ApiException exception = assertThrows(
        ApiException.class, () -> new RedirectController(linkService, analyticsService).redirect(code));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.status());
        assertEquals(ErrorCode.STORE_UNAVAILABLE, exception.code());
    }

    @Test
    void inactiveLinkDoesNotIncrementAnalytics() {
        String code = "Ab3CdEf";
        LinkService linkService = serviceReturning(
                new ShortLink(code, "https://example.com/", false, Instant.now(), null));
        AtomicBoolean incrementCalled = new AtomicBoolean();
        AnalyticsService analyticsService = new AnalyticsService(null, null) {
            @Override
            public void incrementCurrentMinute(String ignoredCode) {
                incrementCalled.set(true);
            }
        };

        ApiException exception = assertThrows(
                ApiException.class, () -> new RedirectController(linkService, analyticsService).redirect(code));

        assertEquals(HttpStatus.GONE, exception.status());
        assertEquals(ErrorCode.LINK_INACTIVE, exception.code());
        assertFalse(incrementCalled.get());
    }

    private LinkService serviceReturning(ShortLink link) {
        return new LinkService(null, null, null, null, null) {
            @Override
            public ShortLink resolveForRedirect(String ignoredCode) {
                return link;
            }
        };
    }
}