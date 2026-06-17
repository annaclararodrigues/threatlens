package com.backend.threatlens.dto.response;

import com.backend.threatlens.enums.RelevanceLevel;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record ClassificationResponse(boolean relevant,
                                     double score,
                                     RelevanceLevel level,
                                     String range,
                                     Boolean ioc,
                                     @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
                                     LocalDateTime classifiedAt) {
}
