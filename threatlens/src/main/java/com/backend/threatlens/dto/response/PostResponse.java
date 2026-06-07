package com.backend.threatlens.dto.response;

import java.util.Map;

public record PostResponse(String id,
                           String source,
                           String title,
                           String content,
                           String author,
                           String createdAt,
                           String category,
                           ClassificationResponse classification,
                           Map<String, Object> meta) {
}
