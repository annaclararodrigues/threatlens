package com.backend.threatlens.dto.response;

import java.util.List;

public record WordCloudDTO(List<WordEntry> words) {

    public record WordEntry(String word, long count) {}
}
