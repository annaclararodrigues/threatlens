package com.backend.threatlens.dto.response;

public record AuthResponseDTO( String token,
                               String username,
                               String email) {
}
