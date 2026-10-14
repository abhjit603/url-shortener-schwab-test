package com.urlshortener.service;

import com.urlshortener.domain.ClickBucket;
import com.urlshortener.repository.ClickBucketRepository;
import com.urlshortener.repository.ShortLinkRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {

    private final ClickBucketRepository clickBucketRepository;
    private final ShortLinkRepository shortLinkRepository;

    public AnalyticsService(
            ClickBucketRepository clickBucketRepository,
            ShortLinkRepository shortLinkRepository) {
        this.clickBucketRepository = clickBucketRepository;
        this.shortLinkRepository = shortLinkRepository;
    }

    public void incrementCurrentMinute(String code) {
        Instant bucketStart = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        int updated = clickBucketRepository.incrementCount(code, bucketStart);

        if (updated == 0) {
            try {
                clickBucketRepository.saveAndFlush(new ClickBucket(code, bucketStart, 1L));
            } catch (DataIntegrityViolationException ex) {
                if (clickBucketRepository.incrementCount(code, bucketStart) == 0) {
                    throw ex;
                }
            }
        }
    }

    public ClickStats stats(String code) {
        shortLinkRepository.findById(code).orElseThrow(() ->
                new com.urlshortener.error.ApiException(
                        com.urlshortener.error.ErrorCode.LINK_NOT_FOUND,
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "No link exists for code " + code));

        List<ClickBucket> buckets = clickBucketRepository.findByCodeOrderByBucketStartAsc(code);
        long total = buckets.stream().mapToLong(ClickBucket::getCount).sum();
        return new ClickStats(code, total, buckets);
    }

    public record ClickStats(String code, long total, List<ClickBucket> buckets) {
    }
}
