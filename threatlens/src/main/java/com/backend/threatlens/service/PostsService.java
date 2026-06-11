package com.backend.threatlens.service;

import com.backend.threatlens.dto.request.PostsFilter;
import com.backend.threatlens.dto.request.PostsQueryDTO;
import com.backend.threatlens.dto.request.StatsQueryDTO;
import com.backend.threatlens.dto.response.PostStatsDTO;
import com.backend.threatlens.dto.response.posts.PostResponse;
import com.backend.threatlens.dto.response.posts.PostsPageResponse;
import com.backend.threatlens.enums.PostSource;
import com.backend.threatlens.enums.SortBy;
import com.backend.threatlens.enums.SortOrder;
import com.backend.threatlens.enums.StatsPeriod;
import com.backend.threatlens.exception.BusinessRuleViolationException;
import com.backend.threatlens.repository.posts.PostsSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostsService {

    private static final int MULTI_SOURCE_FETCH_CAP = 10_000;

    private final List<PostsSourceRepository> sources;

    public PostStatsDTO getStats(StatsQueryDTO query) {
        List<PostsSourceRepository> selectedSources = filterSources(query.getSources());
        TimeWindow window = resolveWindow(query.getPeriod(), query.getFrom(), query.getTo());
        PostsFilter filter = new PostsFilter(query.getRelevance(), query.getCategory(), window.from(), window.to());

        CombinedStats combinedStats = combineSourceStats(selectedSources, filter);
        double relevantPct = calculateRelevancePct(combinedStats.relevantCount(), combinedStats.totalPosts());

        return new PostStatsDTO(
                new PostStatsDTO.Summary(combinedStats.totalPosts(), combinedStats.relevantCount(), relevantPct),
                new PostStatsDTO.RelevanceDistribution(combinedStats.lowCount(), combinedStats.mediumCount(), combinedStats.highCount()),
                combinedStats.sourceCounts()
        );
    }

    private CombinedStats combineSourceStats(List<PostsSourceRepository> sourcesRepos, PostsFilter filter) {
        long totalPosts = 0;
        long relevantCount = 0;
        long lowCount = 0;
        long mediumCount = 0;
        long highCount = 0;
        List<PostStatsDTO.SourceCount> sourceCounts = new ArrayList<>();

        for (PostsSourceRepository repo : sourcesRepos) {
            PostsSourceRepository.SourceStats stats = repo.getStats(filter);
            totalPosts += stats.totalPosts();
            relevantCount += stats.relevantCount();
            lowCount += stats.lowCount();
            mediumCount += stats.mediumCount();
            highCount += stats.highCount();
            sourceCounts.add(new PostStatsDTO.SourceCount(repo.source().name().toLowerCase(), stats.totalPosts()));
        }

        return new CombinedStats(totalPosts, relevantCount, lowCount, mediumCount, highCount, sourceCounts);
    }

    private double calculateRelevancePct(long relevantCount, long totalPosts) {
        return totalPosts > 0 ? Math.round((relevantCount * 10000.0 / totalPosts)) / 100.0 : 0.0;
    }

    public PostsPageResponse getPosts(PostsQueryDTO query) {
        List<PostsSourceRepository> selectedSources = filterSources(query.getSources());
        TimeWindow window = buildWindow(query.getFrom(), query.getTo());
        PostsFilter filter = new PostsFilter(query.getRelevance(), query.getCategory(), window.from(), window.to());
        int offset = query.getPage() * query.getSize();

        if (selectedSources.size() == 1) {
            return pagedFromSingleSource(selectedSources.getFirst(), filter, query, offset);
        }
        return pagedFromMultipleSources(selectedSources, filter, query, offset);
    }

    private PostsPageResponse pagedFromSingleSource(PostsSourceRepository repo, PostsFilter filter,
                                                     PostsQueryDTO query, int offset) {
        long total = repo.count(filter);
        List<PostResponse> page = repo.findPage(filter, offset, query.getSize(), query.getSort(), query.getOrder());
        return buildPage(page, query, total);
    }

    private PostsPageResponse pagedFromMultipleSources(List<PostsSourceRepository> repos, PostsFilter filter,
                                                        PostsQueryDTO query, int offset) {
        List<PostResponse> all = repos.stream()
                .flatMap(repo -> repo.findPage(filter, 0, MULTI_SOURCE_FETCH_CAP, query.getSort(), query.getOrder()).stream())
                .sorted(comparator(query.getSort(), query.getOrder()))
                .toList();

        long total = all.size();
        List<PostResponse> page = offset < total
                ? all.subList(offset, (int) Math.min(offset + query.getSize(), total))
                : List.of();
        return buildPage(page, query, total);
    }

    private PostsPageResponse buildPage(List<PostResponse> page, PostsQueryDTO query, long total) {
        int totalPages = query.getSize() > 0 ? (int) Math.ceil((double) total / query.getSize()) : 0;
        return new PostsPageResponse(page, new PostsPageResponse.Pagination(query.getPage(), query.getSize(), total, totalPages));
    }

    private List<PostsSourceRepository> filterSources(List<PostSource> sourceFilter) {
        if (sourceFilter == null || sourceFilter.isEmpty()) {
            return sources;
        }
        return sources.stream()
                .filter(repo -> sourceFilter.contains(repo.source()))
                .toList();
    }

    private Comparator<PostResponse> comparator(SortBy sortBy, SortOrder sortOrder) {
        Comparator<PostResponse> base;
        if (sortBy == SortBy.SCORE) {
            base = Comparator.comparingDouble(post -> post.classification() != null ? post.classification().score() : 0.0);
        } else {
            base = Comparator.comparing(PostResponse::createdAt, Comparator.nullsLast(Comparator.naturalOrder()));
        }
        return sortOrder == SortOrder.DESC ? base.reversed() : base;
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

    private record CombinedStats(
            long totalPosts,
            long relevantCount,
            long lowCount,
            long mediumCount,
            long highCount,
            List<PostStatsDTO.SourceCount> sourceCounts
    ) {}
}
