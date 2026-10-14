package com.urlshortener.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "short_link")
public class ShortLink {

    @Id
    private String code;

    @Column(nullable = false, length = 2048)
    private String destination;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(name = "active_destination_key", unique = true, length = 2048)
    private String activeDestinationKey;

    protected ShortLink() {
    }

    public ShortLink(
            String code,
            String destination,
            boolean active,
            Instant createdAt,
            String activeDestinationKey
    ) {
        this.code = code;
        this.destination = destination;
        this.active = active;
        this.createdAt = createdAt;
        this.activeDestinationKey = activeDestinationKey;
    }

    public String getCode() {
        return code;
    }

    public String getDestination() {
        return destination;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getActiveDestinationKey() {
        return activeDestinationKey;
    }

    public void deactivate() {
        this.active = false;
        this.activeDestinationKey = null;
    }
}
