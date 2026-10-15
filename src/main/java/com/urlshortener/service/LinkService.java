package com.urlshortener.service;

import com.urlshortener.config.AppProperties;
import com.urlshortener.domain.ShortLink;
import com.urlshortener.error.ApiException;
import com.urlshortener.error.ErrorCode;
import com.urlshortener.repository.ShortLinkRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class LinkService {

    private static final int MAX_CODE_RETRIES = 5;

    private final ShortLinkRepository repository;
    private final UrlNormalizer normalizer;
    private final ShortCodeGenerator codeGenerator;
    private final AppProperties appProperties;
    private final LinkCursorCodec cursorCodec;
    private final RedirectMetadataCache redirectMetadataCache;

    public LinkService(
            ShortLinkRepository repository,
            UrlNormalizer normalizer,
            ShortCodeGenerator codeGenerator,
            AppProperties appProperties,
            LinkCursorCodec cursorCodec,
            RedirectMetadataCache redirectMetadataCache) {
        this.repository = repository;
        this.normalizer = normalizer;
        this.codeGenerator = codeGenerator;
        this.appProperties = appProperties;
        this.cursorCodec = cursorCodec;
        this.redirectMetadataCache = redirectMetadataCache;
    }

    public CreateOutcome create(String rawDestination) {
        String destination = normalizer.normalize(rawDestination);
        Optional<ShortLink> existing =
                repository.findByActiveDestinationKey(destination);

        if (existing.isPresent()) {
            return CreateOutcome.existing(existing.get());
        }

        for (int attempt = 0; attempt < MAX_CODE_RETRIES; attempt++) {
            String code = codeGenerator.nextCode();
            ShortLink link = new ShortLink(
                    code, destination, true, Instant.now(), destination);

            try {
                repository.saveAndFlush(link);
                invalidateAfterCommit(code);
                return CreateOutcome.created(link);
            } catch (DataIntegrityViolationException ex) {
                Optional<ShortLink> winner =
                        repository.findByActiveDestinationKey(destination);

                if (winner.isPresent()) {
                    return CreateOutcome.existing(winner.get());
                }
            }
        }

        throw new ApiException(
                ErrorCode.SHORT_CODE_COLLISION_EXHAUSTED,
                HttpStatus.CONFLICT,
                "Could not allocate a unique short code");
    }

    public ShortLink require(String code) {
        return repository.findById(code).orElseThrow(() -> notFound(code));
    }

    public LinkPage list(int limit, String cursor, Boolean active) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        var pageable = PageRequest.of(0, safeLimit + 1);
        List<ShortLink> results;
        if (cursor == null) {
            results = repository.findFiltered(active, pageable);
        } else {
            LinkCursorCodec.Position position = cursorCodec.decode(cursor);
            results = repository.findFilteredAfter(active, position.createdAt(), position.code(), pageable);
        }
        boolean hasMore = results.size() > safeLimit;
        List<ShortLink> links = hasMore ? results.subList(0, safeLimit) : results;
        String nextCursor = hasMore ? cursorCodec.encode(links.get(links.size() - 1)) : null;
        return new LinkPage(links, nextCursor);
    }

    @Transactional
    public ShortLink deactivate(String code) {
        ShortLink link = require(code);
        if (link.isActive()) {
            link.deactivate();
            repository.save(link);
        }
        invalidateAfterCommit(code);
        return link;
    }

    public RedirectTarget resolveForRedirect(String code) {
        return redirectMetadataCache.getOrLoad(code, requestedCode -> RedirectTarget.from(require(requestedCode)));
    }

    private void invalidateAfterCommit(String code) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    redirectMetadataCache.invalidate(code);
                }
            });
        } else {
            redirectMetadataCache.invalidate(code);
        }
    }

    public String shortUrl(String code) {
        String base = appProperties.baseUrl();
        if (base.endsWith("/")) {
            return base + code;
        }
        return base + "/" + code;
    }

    private static ApiException notFound(String code) {
        return new ApiException(
                ErrorCode.LINK_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "No link exists for code " + code);
    }

    public record CreateOutcome(ShortLink link, boolean newlyCreated) {

        static CreateOutcome created(ShortLink link) {
            return new CreateOutcome(link, true);
        }

        static CreateOutcome existing(ShortLink link) {
            return new CreateOutcome(link, false);
        }
    }

    public record LinkPage(List<ShortLink> links, String nextCursor) {
    }
}
