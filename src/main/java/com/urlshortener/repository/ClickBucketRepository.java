package com.urlshortener.repository;

import com.urlshortener.domain.ClickBucket;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ClickBucketRepository extends JpaRepository<ClickBucket, Long> {

    List<ClickBucket> findByCodeOrderByBucketStartAsc(String code);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE ClickBucket c SET c.count = c.count + 1 "
            + "WHERE c.code = :code AND c.bucketStart = :bucketStart")
    int incrementCount(
            @Param("code") String code,
            @Param("bucketStart") Instant bucketStart);
}
