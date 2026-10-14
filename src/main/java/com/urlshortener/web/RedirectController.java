package com.urlshortener.web;

import com.urlshortener.domain.ShortLink;
import com.urlshortener.error.ApiException;
import com.urlshortener.error.ErrorCode;
import com.urlshortener.service.AnalyticsService;
import com.urlshortener.service.LinkService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {

    private static final String CODE_PATH =
            "/{code:[23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]{7}}";

    private final LinkService linkService;
    private final AnalyticsService analyticsService;

    public RedirectController(LinkService linkService, AnalyticsService analyticsService) {
        this.linkService = linkService;
        this.analyticsService = analyticsService;
    }

    @GetMapping(CODE_PATH)
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        ShortLink link = linkService.resolveForRedirect(code);
        if (!link.isActive()) {
            throw new ApiException(ErrorCode.LINK_INACTIVE, HttpStatus.GONE, "Link " + code + " is inactive");
        }
        try {
            analyticsService.incrementCurrentMinute(code);
        } catch (Exception ex) {
            RequestLoggingFilter.logEvent("redirect", code, "503");
            throw new ApiException(
                    ErrorCode.STORE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE, "Data store is unavailable");
        }
        RequestLoggingFilter.logEvent("redirect", code, "302");
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, link.getDestination())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }
}
