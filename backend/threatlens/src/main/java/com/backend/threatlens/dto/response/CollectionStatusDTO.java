package com.backend.threatlens.dto.response;

import java.time.LocalDateTime;
import java.util.List;

// lastCollectedAt é null quando a fonte ainda não tem nenhuma mensagem gravada
public record CollectionStatusDTO(
        LocalDateTime lastCollectedAt,
        List<SourceStatus> sources
) {
    public record SourceStatus(String sourceName, LocalDateTime lastCollectedAt) {}
}
