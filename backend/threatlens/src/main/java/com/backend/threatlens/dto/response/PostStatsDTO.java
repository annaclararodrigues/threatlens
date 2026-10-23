package com.backend.threatlens.dto.response;

import java.util.List;

public record PostStatsDTO(
        Summary summary,
        RelevanceDistribution relevanceDistribution,
        List<SourceCount> sources
) {
    public record Summary(long totalPosts, long relevantPostsInPeriod, double relevantPostsPct) {}

    public record RelevanceDistribution(long lowCount, long mediumCount, long highCount) {}

    // relevantCount: posts de relevância alta da fonte (mesmo critério de Summary.relevantPostsInPeriod)
    public record SourceCount(String sourceName, long postCount, long relevantCount) {}
}
