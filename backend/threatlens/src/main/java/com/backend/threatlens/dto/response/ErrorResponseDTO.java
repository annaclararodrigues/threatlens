package com.backend.threatlens.dto.response;

import java.util.Map;

public record ErrorResponseDTO(String message, Map<String, String> fieldErrors) {

    public static ErrorResponseDTO of(String message) {
        return new ErrorResponseDTO(message, null);
    }

    public static ErrorResponseDTO ofFields(String message, Map<String, String> fieldErrors) {
        return new ErrorResponseDTO(message, fieldErrors);
    }
}
