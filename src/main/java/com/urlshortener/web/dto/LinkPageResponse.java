package com.urlshortener.web.dto;

import java.util.List;

public record LinkPageResponse(List<LinkResponse> links, String nextCursor) {
}