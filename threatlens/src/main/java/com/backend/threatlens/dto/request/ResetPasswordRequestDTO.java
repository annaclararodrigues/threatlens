package com.backend.threatlens.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequestDTO(
        @NotBlank String password,
        @NotBlank String passwordConfirm
) {
}
