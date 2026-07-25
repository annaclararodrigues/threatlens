package com.backend.threatlens.dto.request;

import com.backend.threatlens.enums.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequestDTO(
        @NotNull Role role
) {
}