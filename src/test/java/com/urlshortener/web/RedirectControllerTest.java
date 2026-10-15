package com.urlshortener.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.urlshortener.error.ApiException;
import com.urlshortener.error.ErrorCode;
import com.urlshortener.service.AnalyticsService;
import com.urlshortener.service.LinkService;
import com.urlshortener.service.RedirectTarget;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RedirectControllerTest {

    @Test
    void analyticsFailurePreventsRedirectAndReturnsStoreUnavailable() {
        String code = "Ab3CdEf";
    LinkService linkService = serviceReturning(
        new RedirectTarget(code, "https://example.com/", true));
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
                new RedirectTarget(code, "https://example.com/", false));
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

    private LinkService serviceReturning(RedirectTarget link) {
        return new LinkService(null, null, null, null, null, null) {
            @Override
            public RedirectTarget resolveForRedirect(String ignoredCode) {
                return link;
            }
        };
    }
}
