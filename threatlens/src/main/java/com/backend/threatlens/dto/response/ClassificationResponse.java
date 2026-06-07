package com.backend.threatlens.dto.response;

public record ClassificationResponse(boolean relevant,
                                     double  score,
                                     String  level,
                                     String  range,
                                     String  ioc,
                                     String  classifiedAt) {
}
