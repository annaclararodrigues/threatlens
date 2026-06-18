package com.backend.threatlens.dto.response;

import com.backend.threatlens.enums.Role;

public record AuthTokens(
        String accessToken,
        String refreshToken,
        String username,
        String email,
        Role role
) {
}
