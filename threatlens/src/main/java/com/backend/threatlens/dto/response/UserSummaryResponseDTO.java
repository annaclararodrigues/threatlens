package com.backend.threatlens.dto.response;

import com.backend.threatlens.enums.Role;

import java.util.UUID;

public record UserSummaryResponseDTO(
        UUID id,
        String username,
        String email,
        Role role,
        boolean isEmailVerified
) {}
