package com.urlshortener.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
        name = "click_bucket",
        uniqueConstraints = @UniqueConstraint(columnNames = {"code", "bucket_start"})
)
public class ClickBucket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 16)
    private String code;

    @Column(name = "bucket_start", nullable = false)
    private Instant bucketStart;

    @Column(nullable = false)
    private long count;

    protected ClickBucket() {
    }

    public ClickBucket(String code, Instant bucketStart, long count) {
        this.code = code;
        this.bucketStart = bucketStart;
        this.count = count;
    }

    public String getCode() {
        return code;
    }

    public Instant getBucketStart() {
        return bucketStart;
    }

    public long getCount() {
        return count;
    }

    public void increment() {
        this.count++;
    }
}
