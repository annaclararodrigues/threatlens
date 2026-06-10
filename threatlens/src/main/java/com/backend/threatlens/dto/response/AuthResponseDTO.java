package com.backend.threatlens.dto.response;

import com.backend.threatlens.enums.Role;

public record AuthResponseDTO(
        String username,
        String email,
        Role role
) {}
