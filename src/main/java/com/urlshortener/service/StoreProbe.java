package com.urlshortener.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class StoreProbe {

    private final JdbcTemplate jdbcTemplate;

    public StoreProbe(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void assertReachable() {
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM short_link", Long.class);
    }
}
