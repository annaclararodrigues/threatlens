package com.backend.threatlens.repository.posts;

import com.backend.threatlens.dto.request.PostsFilter;
import com.backend.threatlens.dto.response.posts.PostResponse;
import com.backend.threatlens.enums.PostSource;
import com.backend.threatlens.enums.SortBy;
import com.backend.threatlens.enums.SortOrder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PostsSourceRepository {

    PostSource source();

    List<PostResponse> findPage(PostsFilter filter, int offset, int limit, SortBy sortBy, SortOrder sortOrder);

    long count(PostsFilter filter);

    SourceStats getStats(PostsFilter filter);

    List<WordCount> wordFrequencies(PostsFilter filter, int limit);

    // Momento em que a última mensagem desta fonte foi gravada pelo coletor
    Optional<LocalDateTime> lastCollectedAt();

    record SourceStats(long totalPosts, long relevantCount, long lowCount, long mediumCount, long highCount) {}

    record WordCount(String word, long count) {}
}
