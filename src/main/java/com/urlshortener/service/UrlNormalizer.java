package com.urlshortener.service;

import com.urlshortener.error.ApiException;
import com.urlshortener.error.ErrorCode;
import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class UrlNormalizer {

    public String normalize(String raw) {
        if (raw == null) {
            reject("destination is required");
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            reject("destination must not be empty");
        }
        if (trimmed.length() > 2048) {
            reject("destination exceeds maximum length of 2048");
        }
        final URI uri;
        try {
            uri = new URI(trimmed);
        } catch (URISyntaxException e) {
            throw rejectException("destination is not a valid URL");
        }
        if (!uri.isAbsolute()) {
            reject("destination must be an absolute URL");
        }
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            reject("destination scheme must be http or https");
        }
        if (uri.getRawUserInfo() != null) {
            reject("destination must not contain user credentials");
        }
        String host = uri.getHost();
        int port = uri.getPort();
        if (host == null && uri.getRawAuthority() != null) {
            String authority = uri.getRawAuthority();
            if (authority.contains("@")) {
                reject("destination must not contain user credentials");
            }
            int portSeparator = authority.lastIndexOf(':');
            if (portSeparator > 0) {
                try {
                    port = Integer.parseInt(authority.substring(portSeparator + 1));
                } catch (NumberFormatException e) {
                    reject("destination must include a valid host and port");
                }
                host = authority.substring(0, portSeparator);
            } else {
                host = authority;
            }
        }
        if (host == null || host.isBlank()) {
            reject("destination must include a host");
        }
        scheme = scheme.toLowerCase(Locale.ROOT);
        host = host.toLowerCase(Locale.ROOT);
        if (!host.contains(":")) {
            try {
                host = IDN.toASCII(host);
            } catch (IllegalArgumentException e) {
                throw rejectException("destination must include a valid host");
            }
        }
        if (port == 80 && scheme.equals("http")) {
            port = -1;
        } else if (port == 443 && scheme.equals("https")) {
            port = -1;
        }
        String path = uri.getRawPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        String query = uri.getRawQuery();
        final String normalized;
        try {
            String authority = new URI(scheme, null, host, port, "/", null, null).getRawAuthority();
            normalized = new URI(scheme + "://" + authority + path
                    + (query == null ? "" : "?" + query)).toString();
        } catch (URISyntaxException e) {
            throw rejectException("destination could not be normalized");
        }
        if (normalized.length() > 2048) {
            reject("destination exceeds maximum length of 2048");
        }
        return normalized;
    }

    private static void reject(String message) {
        throw rejectException(message);
    }

    private static ApiException rejectException(String message) {
        return new ApiException(
                ErrorCode.INVALID_URL,
                HttpStatus.BAD_REQUEST,
                message,
                List.of(new ApiException.ValidationError("destination", message))
        );
    }
}
