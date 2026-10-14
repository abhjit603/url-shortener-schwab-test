package com.urlshortener.error;

public enum ErrorCode {
    INVALID_URL,
    INVALID_CURSOR,
    LINK_NOT_FOUND,
    LINK_INACTIVE,
    SHORT_CODE_COLLISION_EXHAUSTED,
    INTERNAL_ERROR,
    STORE_UNAVAILABLE
}
