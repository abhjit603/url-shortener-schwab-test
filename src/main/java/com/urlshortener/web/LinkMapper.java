package com.urlshortener.web;

import com.urlshortener.domain.ClickBucket;
import com.urlshortener.domain.ShortLink;
import com.urlshortener.service.AnalyticsService;
import com.urlshortener.service.LinkService;
import com.urlshortener.web.dto.ClickAnalyticsResponse;
import com.urlshortener.web.dto.LinkResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class LinkMapper {

    private final LinkService linkService;

    public LinkMapper(LinkService linkService) {
        this.linkService = linkService;
    }

    public LinkResponse toResponse(ShortLink link) {
        return new LinkResponse(
                link.getCode(),
                linkService.shortUrl(link.getCode()),
                link.getDestination(),
                link.isActive(),
                link.getCreatedAt()
        );
    }

    public ClickAnalyticsResponse toClicks(AnalyticsService.ClickStats stats) {
        List<ClickAnalyticsResponse.BucketCount> buckets = stats.buckets().stream()
                .map(b -> new ClickAnalyticsResponse.BucketCount(b.getBucketStart(), b.getCount()))
                .toList();
        return new ClickAnalyticsResponse(stats.code(), stats.total(), "minute", buckets);
    }
}
