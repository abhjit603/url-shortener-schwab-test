package com.urlshortener.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateLinkRequest(@NotBlank String destination) {
}
