package com.backend.threatlens.service;

import com.backend.threatlens.dto.request.PostsFilter;
import com.backend.threatlens.dto.request.PostsQueryDTO;
import com.backend.threatlens.dto.request.StatsQueryDTO;
import com.backend.threatlens.dto.request.WordCloudQueryDTO;
import com.backend.threatlens.dto.response.CollectionStatusDTO;
import com.backend.threatlens.dto.response.PostStatsDTO;
import com.backend.threatlens.dto.response.WordCloudDTO;
import com.backend.threatlens.dto.response.posts.PostRepliesResponse;
import com.backend.threatlens.dto.response.posts.PostResponse;
import com.backend.threatlens.dto.response.posts.PostsPageResponse;
import com.backend.threatlens.enums.PostSource;
import com.backend.threatlens.enums.StatsPeriod;
import com.backend.threatlens.exception.BusinessRuleViolationException;
import com.backend.threatlens.exception.InvalidRequestException;
import com.backend.threatlens.repository.posts.PostsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PostsService {

    private final PostsRepository postsRepository;

    public PostStatsDTO getStats(StatsQueryDTO query) {
        TimeWindow window = resolveWindow(query.getPeriod(), query.getFrom(), query.getTo());
        PostsFilter filter = new PostsFilter(resolveSources(query.getSources()), query.getRelevance(),
                query.getCategory(), window.from(), window.to(), null);

        List<PostsRepository.SourceStats> statsBySource = postsRepository.statsBySource(filter);

        long totalPosts = statsBySource.stream().mapToLong(PostsRepository.SourceStats::totalPosts).sum();
        long relevantCount = statsBySource.stream().mapToLong(PostsRepository.SourceStats::relevantCount).sum();
        long lowCount = statsBySource.stream().mapToLong(PostsRepository.SourceStats::lowCount).sum();
        long mediumCount = statsBySource.stream().mapToLong(PostsRepository.SourceStats::mediumCount).sum();
        long highCount = statsBySource.stream().mapToLong(PostsRepository.SourceStats::highCount).sum();

        List<PostStatsDTO.SourceCount> sourceCounts = statsBySource.stream()
                .map(stats -> new PostStatsDTO.SourceCount(stats.source().name().toLowerCase(),
                        stats.totalPosts(), stats.relevantCount()))
                .toList();

        return new PostStatsDTO(
                new PostStatsDTO.Summary(totalPosts, relevantCount, calculateRelevancePct(relevantCount, totalPosts)),
                new PostStatsDTO.RelevanceDistribution(lowCount, mediumCount, highCount),
                sourceCounts
        );
    }

    private double calculateRelevancePct(long relevantCount, long totalPosts) {
        return totalPosts > 0 ? Math.round((relevantCount * 10000.0 / totalPosts)) / 100.0 : 0.0;
    }

    public CollectionStatusDTO getCollectionStatus() {
        List<CollectionStatusDTO.SourceStatus> sourceStatuses = postsRepository.lastCollectedAtBySource().entrySet().stream()
                .map(entry -> new CollectionStatusDTO.SourceStatus(entry.getKey().name().toLowerCase(), entry.getValue()))
                .toList();

        LocalDateTime lastCollectedAt = sourceStatuses.stream()
                .map(CollectionStatusDTO.SourceStatus::lastCollectedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return new CollectionStatusDTO(lastCollectedAt, sourceStatuses);
    }

    public WordCloudDTO getWordCloud(WordCloudQueryDTO query) {
        TimeWindow window = resolveWindow(query.getPeriod(), query.getFrom(), query.getTo());
        PostsFilter filter = new PostsFilter(resolveSources(query.getSources()), query.getRelevance(),
                query.getCategory(), window.from(), window.to(), null);

        List<WordCloudDTO.WordEntry> words = postsRepository.wordFrequencies(filter, query.getLimit()).stream()
                .map(wordCount -> new WordCloudDTO.WordEntry(wordCount.word(), wordCount.count()))
                .toList();

        return new WordCloudDTO(words);
    }

    public PostsPageResponse getPosts(PostsQueryDTO query) {
        TimeWindow window = resolveWindow(query.getPeriod(), query.getFrom(), query.getTo());
        PostsFilter filter = new PostsFilter(resolveSources(query.getSources()), query.getRelevance(),
                query.getCategory(), window.from(), window.to(), query.getSearch());
        int offset = query.getPage() * query.getSize();

        List<PostResponse> page = postsRepository.findPage(filter, offset, query.getSize(), query.getSort(), query.getOrder());

        Long total = null;
        Integer totalPages = null;
        if (query.isIncludeTotal()) {
            total = postsRepository.count(filter);
            totalPages = (int) Math.ceil((double) total / query.getSize());
        }
        return new PostsPageResponse(page, new PostsPageResponse.Pagination(query.getPage(), query.getSize(), total, totalPages));
    }

    // O id vem como devolvido em /posts: "fonte:id" ou, nas fontes com uma tabela por idioma, "fonte:idioma:id"
    public PostRepliesResponse getReplies(String postId) {
        String[] parts = postId.split(":");
        if (parts.length < 2 || parts.length > 3) {
            throw new InvalidRequestException("Invalid post id: " + postId);
        }
        PostSource source = Arrays.stream(PostSource.values())
                .filter(candidate -> candidate.name().equalsIgnoreCase(parts[0]))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException("Unknown source in post id: " + postId));
        String tableSuffix = parts.length == 3 ? parts[1] : null;
        long rawId;
        try {
            rawId = Long.parseLong(parts[parts.length - 1]);
        } catch (NumberFormatException e) {
            throw new InvalidRequestException("Invalid post id: " + postId);
        }

        PostsRepository.Replies replies = postsRepository.findReplies(source, tableSuffix, rawId)
                .orElseThrow(() -> new InvalidRequestException("Replies are not available for post " + postId));
        return new PostRepliesResponse(postId, replies.kind(), replies.answers());
    }

    // Sem filtro de fonte, consulta todas
    private List<PostSource> resolveSources(List<PostSource> sourceFilter) {
        if (sourceFilter == null || sourceFilter.isEmpty()) {
            return List.of(PostSource.values());
        }
        return sourceFilter.stream().distinct().toList();
    }

    private TimeWindow buildWindow(LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessRuleViolationException("'from' can not be after 'to'");
        }
        return new TimeWindow(from, to);
    }

    private TimeWindow resolveWindow(StatsPeriod period, LocalDateTime from, LocalDateTime to) {
        if (from != null || to != null) {
            return buildWindow(from, to);
        }
        return new TimeWindow(period.since().orElse(null), null);
    }

    private record TimeWindow(LocalDateTime from, LocalDateTime to) {}
}
