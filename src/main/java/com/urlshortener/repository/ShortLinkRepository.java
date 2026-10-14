package com.urlshortener.repository;

import com.urlshortener.domain.ShortLink;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShortLinkRepository extends JpaRepository<ShortLink, String> {

    Optional<ShortLink> findByActiveDestinationKey(String activeDestinationKey);

        @Query("SELECT s FROM ShortLink s WHERE (:active IS NULL OR s.active = :active) "
            + "ORDER BY s.createdAt DESC, s.code DESC")
        List<ShortLink> findFiltered(@Param("active") Boolean active, Pageable pageable);

        @Query("SELECT s FROM ShortLink s WHERE (:active IS NULL OR s.active = :active) "
            + "AND (s.createdAt < :createdAt OR (s.createdAt = :createdAt AND s.code < :code)) "
            + "ORDER BY s.createdAt DESC, s.code DESC")
        List<ShortLink> findFilteredAfter(
            @Param("active") Boolean active,
            @Param("createdAt") Instant createdAt,
            @Param("code") String code,
            Pageable pageable);
}
