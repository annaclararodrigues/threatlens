package com.backend.threatlens.dto.request;

import com.backend.threatlens.enums.PostSource;
import com.backend.threatlens.enums.RelevanceLevel;

import java.time.LocalDateTime;
import java.util.List;

public record PostsFilter(
        List<PostSource> sources,
        RelevanceLevel relevance,
        String category,
        LocalDateTime from,
        LocalDateTime to,
        String search
) {}
