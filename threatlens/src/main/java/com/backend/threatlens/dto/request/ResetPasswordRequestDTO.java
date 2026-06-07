package com.backend.threatlens.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDTO(
        @NotBlank
        @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$",
            message = "A senha deve conter letras maiúsculas, minúsculas, números e um caractere especial (@$!%*?&#)"
        )
        String password,
        @NotBlank String passwordConfirm
) {
}
