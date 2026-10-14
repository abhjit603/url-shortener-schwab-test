package com.urlshortener.web;

import com.urlshortener.domain.ShortLink;
import com.urlshortener.service.AnalyticsService;
import com.urlshortener.service.LinkService;
import com.urlshortener.web.dto.ClickAnalyticsResponse;
import com.urlshortener.web.dto.CreateLinkRequest;
import com.urlshortener.web.dto.LinkResponse;
import com.urlshortener.web.dto.LinkPageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/links")
public class AdminLinkController {

    private final LinkService linkService;
    private final AnalyticsService analyticsService;
    private final LinkMapper mapper;

    public AdminLinkController(LinkService linkService, AnalyticsService analyticsService, LinkMapper mapper) {
        this.linkService = linkService;
        this.analyticsService = analyticsService;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<LinkResponse> create(@Valid @RequestBody CreateLinkRequest request) {
        LinkService.CreateOutcome outcome = linkService.create(request.destination());
        HttpStatus status = outcome.newlyCreated() ? HttpStatus.CREATED : HttpStatus.OK;
        RequestLoggingFilter.logEvent("create", outcome.link().getCode(), status.value() + "");
        return ResponseEntity.status(status).body(mapper.toResponse(outcome.link()));
    }

    @GetMapping
    public LinkPageResponse list(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Boolean active) {
        LinkService.LinkPage page = linkService.list(limit, cursor, active);
        return new LinkPageResponse(
                page.links().stream().map(mapper::toResponse).toList(), page.nextCursor());
    }

    @GetMapping("/{code}")
    public LinkResponse get(@PathVariable String code) {
        return mapper.toResponse(linkService.require(code));
    }

    @PostMapping("/{code}/deactivate")
    public LinkResponse deactivate(@PathVariable String code) {
        ShortLink link = linkService.deactivate(code);
        RequestLoggingFilter.logEvent("deactivate", code, "200");
        return mapper.toResponse(link);
    }

    @GetMapping("/{code}/clicks")
    public ClickAnalyticsResponse clicks(@PathVariable String code) {
        return mapper.toClicks(analyticsService.stats(code));
    }
}
