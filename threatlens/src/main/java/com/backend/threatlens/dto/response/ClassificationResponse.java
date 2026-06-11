package com.backend.threatlens.dto.response;

import com.backend.threatlens.enums.RelevanceLevel;

import java.time.LocalDateTime;

public record ClassificationResponse(boolean relevant,
                                     double score,
                                     RelevanceLevel level,
                                     String range,
                                     Boolean ioc,
                                     LocalDateTime classifiedAt) {
}
