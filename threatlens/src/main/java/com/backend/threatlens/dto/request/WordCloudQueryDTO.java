package com.backend.threatlens.dto.request;

import com.backend.threatlens.enums.PostSource;
import com.backend.threatlens.enums.RelevanceLevel;
import com.backend.threatlens.enums.StatsPeriod;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class WordCloudQueryDTO {

    private List<PostSource> sources;
    private RelevanceLevel relevance;
    private String category;
    private StatsPeriod period = StatsPeriod.ALL;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime to;

    @Min(1)
    @Max(200)
    private int limit = 50;
}
