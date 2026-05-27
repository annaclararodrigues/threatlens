package com.backend.threatlens.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequestDTO(@NotBlank String refreshToken) {
}
