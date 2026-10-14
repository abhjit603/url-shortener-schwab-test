package com.urlshortener.web.dto;

import java.time.Instant;
import java.util.List;

public record ClickAnalyticsResponse(
        String code,
        long total,
        String bucketUnit,
        List<BucketCount> buckets
) {
    public record BucketCount(Instant bucketStart, long count) {}
}
