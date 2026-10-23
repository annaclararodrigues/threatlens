package com.backend.threatlens.dto.response.posts;

import com.backend.threatlens.enums.PostSource;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Map;

public record PostResponse(String id,
                           PostSource source,
                           String title,
                           String content,
                           String author,
                           @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
                           LocalDateTime createdAt,
                           String category,
                           ClassificationResponse classification,
                           Map<String, Object> meta) {
}
