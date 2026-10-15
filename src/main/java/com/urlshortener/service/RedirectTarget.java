package com.urlshortener.service;

import com.urlshortener.domain.ShortLink;

public record RedirectTarget(String code, String destination, boolean active) {

    static RedirectTarget from(ShortLink link) {
        return new RedirectTarget(link.getCode(), link.getDestination(), link.isActive());
    }
}
