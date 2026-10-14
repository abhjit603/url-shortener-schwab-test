package com.urlshortener.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        MDC.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    public static void logEvent(String event, String code, String outcome) {
        MDC.put("event", event);
        if (code != null) {
            MDC.put("code", code);
        }
        if (outcome != null) {
            MDC.put("outcome", outcome);
        }
        log.info("handled");
        MDC.remove("event");
        if (code != null) {
            MDC.remove("code");
        }
        if (outcome != null) {
            MDC.remove("outcome");
        }
    }
}
