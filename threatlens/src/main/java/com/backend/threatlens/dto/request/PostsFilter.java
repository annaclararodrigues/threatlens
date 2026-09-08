package com.backend.threatlens.dto.request;

import com.backend.threatlens.enums.RelevanceLevel;

import java.time.LocalDateTime;

public record PostsFilter(
        RelevanceLevel relevance,
        String category,
        LocalDateTime from,
        LocalDateTime to,
        String search
) {}
