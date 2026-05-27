package com.backend.threatlens.dto.request;

import com.backend.threatlens.enums.CodeType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VerifyCodeRequestDTO(
        @NotBlank @Email String email,
        @NotNull CodeType codeType,
        @NotBlank @Size(min = 4, max = 4) String code
) {}
