package com.backend.threatlens.dto.response;

public record AuthTokens(
        String accessToken,
        String refreshToken,
        String username,
        String email
) {}
