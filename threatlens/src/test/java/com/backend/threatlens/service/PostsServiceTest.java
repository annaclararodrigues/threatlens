package com.backend.threatlens.service;

import com.backend.threatlens.dto.request.PostsFilter;
import com.backend.threatlens.dto.request.PostsQueryDTO;
import com.backend.threatlens.dto.request.StatsQueryDTO;
import com.backend.threatlens.dto.response.ClassificationResponse;
import com.backend.threatlens.dto.response.PostStatsDTO;
import com.backend.threatlens.dto.response.posts.PostResponse;
import com.backend.threatlens.dto.response.posts.PostsPageResponse;
import com.backend.threatlens.enums.PostSource;
import com.backend.threatlens.enums.RelevanceLevel;
import com.backend.threatlens.enums.SortBy;
import com.backend.threatlens.enums.SortOrder;
import com.backend.threatlens.enums.StatsPeriod;
import com.backend.threatlens.exception.BusinessRuleViolationException;
import com.backend.threatlens.repository.posts.PostsSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostsServiceTest {

    @Mock
    private PostsSourceRepository telegramRepo;

    @Mock
    private PostsSourceRepository secondTelegramRepo;

    private PostsService singleSourceService;
    private PostsService multiSourceService;

    @BeforeEach
    void setUp() {
        singleSourceService = new PostsService(List.of(telegramRepo));
        multiSourceService  = new PostsService(List.of(telegramRepo, secondTelegramRepo));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private PostsSourceRepository.SourceStats sourceStats(long total, long relevant,
                                                           long low, long medium, long high) {
        return new PostsSourceRepository.SourceStats(total, relevant, low, medium, high);
    }

    private PostResponse post(String id, LocalDateTime createdAt, Double score) {
        ClassificationResponse clf = score == null ? null : new ClassificationResponse(
                score >= 0.7, score,
                score < 0.3 ? RelevanceLevel.LOW : score < 0.7 ? RelevanceLevel.MEDIUM : RelevanceLevel.HIGH,
                null, null, null
        );
        return new PostResponse(id, PostSource.TELEGRAM, null, "content", null, createdAt, null, clf, null);
    }

    private PostsQueryDTO query(int page, int size) {
        PostsQueryDTO q = new PostsQueryDTO();
        q.setPage(page);
        q.setSize(size);
        return q;
    }

    // =========================================================================
    // getStats
    // =========================================================================

    @Nested
    @MockitoSettings(strictness = Strictness.LENIENT)
    class GetStats {

        @BeforeEach
        void stubSources() {
            when(telegramRepo.source()).thenReturn(PostSource.TELEGRAM);
            when(secondTelegramRepo.source()).thenReturn(PostSource.TELEGRAM);
        }

        @Nested
        class SourceAggregation {

            @Test
            void singleSource_totalsMatchSourceStats() {
                when(telegramRepo.getStats(any())).thenReturn(sourceStats(100, 40, 20, 30, 10));

                PostStatsDTO result = singleSourceService.getStats(new StatsQueryDTO());

                assertThat(result.summary().totalPosts()).isEqualTo(100);
                assertThat(result.summary().relevantPostsInPeriod()).isEqualTo(40);
                assertThat(result.relevanceDistribution().lowCount()).isEqualTo(20);
                assertThat(result.relevanceDistribution().mediumCount()).isEqualTo(30);
                assertThat(result.relevanceDistribution().highCount()).isEqualTo(10);
            }

            @Test
            void multipleSources_countsAreSummedAcrossSources() {
                when(telegramRepo.getStats(any())).thenReturn(sourceStats(100, 40, 20, 30, 10));
                when(secondTelegramRepo.getStats(any())).thenReturn(sourceStats(50, 10, 5, 20, 5));

                PostStatsDTO result = multiSourceService.getStats(new StatsQueryDTO());

                assertThat(result.summary().totalPosts()).isEqualTo(150);
                assertThat(result.summary().relevantPostsInPeriod()).isEqualTo(50);
                assertThat(result.relevanceDistribution().lowCount()).isEqualTo(25);
                assertThat(result.relevanceDistribution().mediumCount()).isEqualTo(50);
                assertThat(result.relevanceDistribution().highCount()).isEqualTo(15);
            }

            @Test
            void sourceCountsContainOneEntryPerRepo() {
                when(telegramRepo.getStats(any())).thenReturn(sourceStats(100, 0, 0, 0, 0));
                when(secondTelegramRepo.getStats(any())).thenReturn(sourceStats(50, 0, 0, 0, 0));

                PostStatsDTO result = multiSourceService.getStats(new StatsQueryDTO());

                assertThat(result.sources()).hasSize(2);
                assertThat(result.sources().get(0).postCount()).isEqualTo(100);
                assertThat(result.sources().get(1).postCount()).isEqualTo(50);
            }

            @Test
            void sourceNameIsLowercaseEnumName() {
                when(telegramRepo.getStats(any())).thenReturn(sourceStats(10, 5, 0, 0, 0));

                PostStatsDTO result = singleSourceService.getStats(new StatsQueryDTO());

                assertThat(result.sources().getFirst().sourceName()).isEqualTo("telegram");
            }
        }

        @Nested
        class RelevancePct {

            @Test
            void zeroTotal_returnsPctZero() {
                when(telegramRepo.getStats(any())).thenReturn(sourceStats(0, 0, 0, 0, 0));

                PostStatsDTO result = singleSourceService.getStats(new StatsQueryDTO());

                assertThat(result.summary().relevantPostsPct()).isEqualTo(0.0);
            }

            @Test
            void halfRelevant_returns50() {
                when(telegramRepo.getStats(any())).thenReturn(sourceStats(100, 50, 0, 0, 0));

                PostStatsDTO result = singleSourceService.getStats(new StatsQueryDTO());

                assertThat(result.summary().relevantPostsPct()).isEqualTo(50.0);
            }

            @Test
            void nonIntegerResult_isRoundedTo2Decimals() {
                when(telegramRepo.getStats(any())).thenReturn(sourceStats(3, 1, 0, 0, 0));

                PostStatsDTO result = singleSourceService.getStats(new StatsQueryDTO());

                assertThat(result.summary().relevantPostsPct()).isEqualTo(33.33);
            }
        }

        @Nested
        class TimeWindow {

            @Test
            void periodALL_passesNullDatesToRepo() {
                when(telegramRepo.getStats(any())).thenReturn(sourceStats(0, 0, 0, 0, 0));

                StatsQueryDTO query = new StatsQueryDTO();
                query.setPeriod(StatsPeriod.ALL);

                ArgumentCaptor<PostsFilter> captor = ArgumentCaptor.forClass(PostsFilter.class);
                singleSourceService.getStats(query);
                verify(telegramRepo).getStats(captor.capture());

                assertThat(captor.getValue().from()).isNull();
                assertThat(captor.getValue().to()).isNull();
            }

            @Test
            void periodDAY_passesFromAsApproximatelyOneDayAgo() {
                when(telegramRepo.getStats(any())).thenReturn(sourceStats(0, 0, 0, 0, 0));

                LocalDateTime before = LocalDateTime.now().minusDays(1);

                StatsQueryDTO query = new StatsQueryDTO();
                query.setPeriod(StatsPeriod.DAY);

                ArgumentCaptor<PostsFilter> captor = ArgumentCaptor.forClass(PostsFilter.class);
                singleSourceService.getStats(query);
                verify(telegramRepo).getStats(captor.capture());

                LocalDateTime after = LocalDateTime.now().minusDays(1);
                assertThat(captor.getValue().from()).isBetween(before, after);
                assertThat(captor.getValue().to()).isNull();
            }

            @Test
            void explicitFromTo_overridesPeriod() {
                when(telegramRepo.getStats(any())).thenReturn(sourceStats(0, 0, 0, 0, 0));

                LocalDateTime from = LocalDateTime.of(2024, 1, 1, 0, 0);
                LocalDateTime to   = LocalDateTime.of(2024, 1, 31, 23, 59);

                StatsQueryDTO query = new StatsQueryDTO();
                query.setPeriod(StatsPeriod.DAY);
                query.setFrom(from);
                query.setTo(to);

                ArgumentCaptor<PostsFilter> captor = ArgumentCaptor.forClass(PostsFilter.class);
                singleSourceService.getStats(query);
                verify(telegramRepo).getStats(captor.capture());

                assertThat(captor.getValue().from()).isEqualTo(from);
                assertThat(captor.getValue().to()).isEqualTo(to);
            }

            @Test
            void onlyFromProvided_overridesPeriod() {
                when(telegramRepo.getStats(any())).thenReturn(sourceStats(0, 0, 0, 0, 0));

                LocalDateTime from = LocalDateTime.of(2024, 6, 1, 0, 0);

                StatsQueryDTO query = new StatsQueryDTO();
                query.setPeriod(StatsPeriod.WEEK);
                query.setFrom(from);

                ArgumentCaptor<PostsFilter> captor = ArgumentCaptor.forClass(PostsFilter.class);
                singleSourceService.getStats(query);
                verify(telegramRepo).getStats(captor.capture());

                assertThat(captor.getValue().from()).isEqualTo(from);
                assertThat(captor.getValue().to()).isNull();
            }
        }

        @Nested
        class SourceFilter {

            @Test
            void noFilter_queriesAllSources() {
                when(telegramRepo.getStats(any())).thenReturn(sourceStats(0, 0, 0, 0, 0));
                when(secondTelegramRepo.getStats(any())).thenReturn(sourceStats(0, 0, 0, 0, 0));

                multiSourceService.getStats(new StatsQueryDTO());

                verify(telegramRepo).getStats(any());
                verify(secondTelegramRepo).getStats(any());
            }

            @Test
            void filterBySource_queriesOnlyMatchingSourceRepos() {
                when(telegramRepo.getStats(any())).thenReturn(sourceStats(10, 5, 0, 0, 0));
                when(secondTelegramRepo.getStats(any())).thenReturn(sourceStats(5, 2, 0, 0, 0));

                StatsQueryDTO query = new StatsQueryDTO();
                query.setSources(List.of(PostSource.TELEGRAM));

                PostStatsDTO result = multiSourceService.getStats(query);

                verify(telegramRepo).getStats(any());
                verify(secondTelegramRepo).getStats(any());
                assertThat(result.summary().totalPosts()).isEqualTo(15);
            }
        }

        @Nested
        class Validation {

            @Test
            void fromAfterTo_throwsBusinessRuleViolationException() {
                StatsQueryDTO query = new StatsQueryDTO();
                query.setFrom(LocalDateTime.of(2024, 2, 1, 0, 0));
                query.setTo(LocalDateTime.of(2024, 1, 1, 0, 0));

                assertThatThrownBy(() -> singleSourceService.getStats(query))
                        .isInstanceOf(BusinessRuleViolationException.class)
                        .hasMessageContaining("'from' can not be after 'to'");
            }
        }
    }

    // =========================================================================
    // getPosts
    // =========================================================================

    @Nested
    class GetPosts {

        @Nested
        class SingleSourcePath {

            @Test
            void delegatesToRepoCountAndFindPage() {
                when(telegramRepo.count(any())).thenReturn(100L);
                when(telegramRepo.findPage(any(), anyInt(), anyInt(), any(), any())).thenReturn(List.of());

                singleSourceService.getPosts(query(0, 20));

                verify(telegramRepo).count(any());
                verify(telegramRepo).findPage(any(), eq(0), eq(20), any(), any());
            }

            @Test
            void paginationMetadata_totalComesFromRepoCount() {
                when(telegramRepo.count(any())).thenReturn(45L);
                when(telegramRepo.findPage(any(), anyInt(), anyInt(), any(), any())).thenReturn(List.of());

                PostsPageResponse result = singleSourceService.getPosts(query(0, 20));

                assertThat(result.pagination().total()).isEqualTo(45);
                assertThat(result.pagination().totalPages()).isEqualTo(3);
                assertThat(result.pagination().page()).isEqualTo(0);
                assertThat(result.pagination().size()).isEqualTo(20);
            }

            @Test
            void page2Size10_offsetIs20() {
                when(telegramRepo.count(any())).thenReturn(100L);
                when(telegramRepo.findPage(any(), anyInt(), anyInt(), any(), any())).thenReturn(List.of());

                singleSourceService.getPosts(query(2, 10));

                verify(telegramRepo).findPage(any(), eq(20), eq(10), any(), any());
            }

            @Test
            void filterFields_passedToRepo() {
                when(telegramRepo.count(any())).thenReturn(0L);
                when(telegramRepo.findPage(any(), anyInt(), anyInt(), any(), any())).thenReturn(List.of());

                PostsQueryDTO q = query(0, 20);
                q.setRelevance(RelevanceLevel.HIGH);
                q.setCategory("malware");

                ArgumentCaptor<PostsFilter> captor = ArgumentCaptor.forClass(PostsFilter.class);
                singleSourceService.getPosts(q);
                verify(telegramRepo).findPage(captor.capture(), anyInt(), anyInt(), any(), any());

                assertThat(captor.getValue().relevance()).isEqualTo(RelevanceLevel.HIGH);
                assertThat(captor.getValue().category()).isEqualTo("malware");
            }

            @Test
            void sortAndOrder_passedToRepo() {
                when(telegramRepo.count(any())).thenReturn(0L);
                when(telegramRepo.findPage(any(), anyInt(), anyInt(), any(), any())).thenReturn(List.of());

                PostsQueryDTO q = query(0, 20);
                q.setSort(SortBy.SCORE);
                q.setOrder(SortOrder.ASC);

                singleSourceService.getPosts(q);

                verify(telegramRepo).findPage(any(), anyInt(), anyInt(), eq(SortBy.SCORE), eq(SortOrder.ASC));
            }
        }

        @Nested
        class MultiSourcePath {

            @Test
            void totalIsInMemoryMergedCount() {
                when(telegramRepo.findPage(any(), anyInt(), anyInt(), any(), any()))
                        .thenReturn(List.of(post("a", null, null), post("b", null, null)));
                when(secondTelegramRepo.findPage(any(), anyInt(), anyInt(), any(), any()))
                        .thenReturn(List.of(post("c", null, null)));

                PostsPageResponse result = multiSourceService.getPosts(query(0, 10));

                assertThat(result.pagination().total()).isEqualTo(3);
            }

            @Test
            void sortByDateDESC_returnsNewestFirst() {
                LocalDateTime t1 = LocalDateTime.of(2024, 6, 1, 10, 0);
                LocalDateTime t2 = LocalDateTime.of(2024, 6, 1, 9, 0);
                LocalDateTime t3 = LocalDateTime.of(2024, 6, 1, 8, 0);

                when(telegramRepo.findPage(any(), anyInt(), anyInt(), any(), any()))
                        .thenReturn(List.of(post("newest", t1, null)));
                when(secondTelegramRepo.findPage(any(), anyInt(), anyInt(), any(), any()))
                        .thenReturn(List.of(post("middle", t2, null), post("oldest", t3, null)));

                PostsQueryDTO q = query(0, 10);
                q.setSort(SortBy.DATE);
                q.setOrder(SortOrder.DESC);

                PostsPageResponse result = multiSourceService.getPosts(q);

                assertThat(result.data()).extracting(PostResponse::id)
                        .containsExactly("newest", "middle", "oldest");
            }

            @Test
            void sortByDateASC_returnsOldestFirst() {
                LocalDateTime t1 = LocalDateTime.of(2024, 6, 1, 10, 0);
                LocalDateTime t2 = LocalDateTime.of(2024, 6, 1, 9, 0);

                when(telegramRepo.findPage(any(), anyInt(), anyInt(), any(), any()))
                        .thenReturn(List.of(post("newer", t1, null)));
                when(secondTelegramRepo.findPage(any(), anyInt(), anyInt(), any(), any()))
                        .thenReturn(List.of(post("older", t2, null)));

                PostsQueryDTO q = query(0, 10);
                q.setSort(SortBy.DATE);
                q.setOrder(SortOrder.ASC);

                PostsPageResponse result = multiSourceService.getPosts(q);

                assertThat(result.data()).extracting(PostResponse::id)
                        .containsExactly("older", "newer");
            }

            @Test
            void sortByScoreDESC_returnsHighestScoreFirst() {
                when(telegramRepo.findPage(any(), anyInt(), anyInt(), any(), any()))
                        .thenReturn(List.of(post("low", null, 0.2)));
                when(secondTelegramRepo.findPage(any(), anyInt(), anyInt(), any(), any()))
                        .thenReturn(List.of(post("high", null, 0.9)));

                PostsQueryDTO q = query(0, 10);
                q.setSort(SortBy.SCORE);
                q.setOrder(SortOrder.DESC);

                PostsPageResponse result = multiSourceService.getPosts(q);

                assertThat(result.data()).extracting(PostResponse::id)
                        .containsExactly("high", "low");
            }

            @Test
            void nullClassification_treatedAsScore0WhenSortingByScore() {
                when(telegramRepo.findPage(any(), anyInt(), anyInt(), any(), any()))
                        .thenReturn(List.of(post("noScore", null, null)));
                when(secondTelegramRepo.findPage(any(), anyInt(), anyInt(), any(), any()))
                        .thenReturn(List.of(post("hasScore", null, 0.5)));

                PostsQueryDTO q = query(0, 10);
                q.setSort(SortBy.SCORE);
                q.setOrder(SortOrder.DESC);

                PostsPageResponse result = multiSourceService.getPosts(q);

                assertThat(result.data()).extracting(PostResponse::id)
                        .containsExactly("hasScore", "noScore");
            }

            @Test
            void secondPage_returnsCorrectSliceOfMergedList() {
                when(telegramRepo.findPage(any(), anyInt(), anyInt(), any(), any()))
                        .thenReturn(List.of(
                                post("a", LocalDateTime.of(2024, 1, 4, 0, 0), null),
                                post("b", LocalDateTime.of(2024, 1, 3, 0, 0), null),
                                post("c", LocalDateTime.of(2024, 1, 2, 0, 0), null)
                        ));
                when(secondTelegramRepo.findPage(any(), anyInt(), anyInt(), any(), any()))
                        .thenReturn(List.of(
                                post("d", LocalDateTime.of(2024, 1, 1, 0, 0), null)
                        ));

                PostsQueryDTO q = query(1, 2); // page=1, size=2 → offset=2
                q.setSort(SortBy.DATE);
                q.setOrder(SortOrder.DESC);

                PostsPageResponse result = multiSourceService.getPosts(q);

                assertThat(result.data()).extracting(PostResponse::id)
                        .containsExactly("c", "d");
                assertThat(result.pagination().totalPages()).isEqualTo(2);
            }

            @Test
            void offsetBeyondMergedTotal_returnsEmptyPage() {
                when(telegramRepo.findPage(any(), anyInt(), anyInt(), any(), any()))
                        .thenReturn(List.of(post("a", null, null)));
                when(secondTelegramRepo.findPage(any(), anyInt(), anyInt(), any(), any()))
                        .thenReturn(List.of());

                PostsPageResponse result = multiSourceService.getPosts(query(5, 20));

                assertThat(result.data()).isEmpty();
                assertThat(result.pagination().total()).isEqualTo(1);
            }
        }

        @Nested
        class DateWindowValidation {

            @Test
            void fromAfterTo_throwsBusinessRuleViolationException() {
                PostsQueryDTO q = query(0, 20);
                q.setFrom(LocalDateTime.of(2024, 2, 1, 0, 0));
                q.setTo(LocalDateTime.of(2024, 1, 1, 0, 0));

                assertThatThrownBy(() -> singleSourceService.getPosts(q))
                        .isInstanceOf(BusinessRuleViolationException.class)
                        .hasMessageContaining("'from' can not be after 'to'");
            }

            @Test
            void equalFromAndTo_doesNotThrow() {
                LocalDateTime dt = LocalDateTime.of(2024, 1, 1, 0, 0);

                when(telegramRepo.count(any())).thenReturn(0L);
                when(telegramRepo.findPage(any(), anyInt(), anyInt(), any(), any())).thenReturn(List.of());

                PostsQueryDTO q = query(0, 20);
                q.setFrom(dt);
                q.setTo(dt);

                PostsPageResponse result = singleSourceService.getPosts(q);

                assertThat(result).isNotNull();
            }

            @Test
            void onlyFromProvided_doesNotThrow() {
                when(telegramRepo.count(any())).thenReturn(0L);
                when(telegramRepo.findPage(any(), anyInt(), anyInt(), any(), any())).thenReturn(List.of());

                PostsQueryDTO q = query(0, 20);
                q.setFrom(LocalDateTime.of(2024, 1, 1, 0, 0));

                PostsPageResponse result = singleSourceService.getPosts(q);

                assertThat(result).isNotNull();
            }

            @Test
            void dateWindowPassedToRepo() {
                LocalDateTime from = LocalDateTime.of(2024, 1, 1, 0, 0);
                LocalDateTime to   = LocalDateTime.of(2024, 1, 31, 23, 59);

                when(telegramRepo.count(any())).thenReturn(0L);
                when(telegramRepo.findPage(any(), anyInt(), anyInt(), any(), any())).thenReturn(List.of());

                PostsQueryDTO q = query(0, 20);
                q.setFrom(from);
                q.setTo(to);

                ArgumentCaptor<PostsFilter> captor = ArgumentCaptor.forClass(PostsFilter.class);
                singleSourceService.getPosts(q);
                verify(telegramRepo).findPage(captor.capture(), anyInt(), anyInt(), any(), any());

                assertThat(captor.getValue().from()).isEqualTo(from);
                assertThat(captor.getValue().to()).isEqualTo(to);
            }
        }

        @Nested
        class TimeWindow {

            @Test
            void periodALL_passesNullDatesToRepo() {
                when(telegramRepo.count(any())).thenReturn(0L);
                when(telegramRepo.findPage(any(), anyInt(), anyInt(), any(), any())).thenReturn(List.of());

                PostsQueryDTO q = query(0, 20);
                q.setPeriod(StatsPeriod.ALL);

                ArgumentCaptor<PostsFilter> captor = ArgumentCaptor.forClass(PostsFilter.class);
                singleSourceService.getPosts(q);
                verify(telegramRepo).findPage(captor.capture(), anyInt(), anyInt(), any(), any());

                assertThat(captor.getValue().from()).isNull();
                assertThat(captor.getValue().to()).isNull();
            }

            @Test
            void periodDAY_passesFromAsApproximatelyOneDayAgo() {
                when(telegramRepo.count(any())).thenReturn(0L);
                when(telegramRepo.findPage(any(), anyInt(), anyInt(), any(), any())).thenReturn(List.of());

                LocalDateTime before = LocalDateTime.now().minusDays(1);

                PostsQueryDTO q = query(0, 20);
                q.setPeriod(StatsPeriod.DAY);

                ArgumentCaptor<PostsFilter> captor = ArgumentCaptor.forClass(PostsFilter.class);
                singleSourceService.getPosts(q);
                verify(telegramRepo).findPage(captor.capture(), anyInt(), anyInt(), any(), any());

                LocalDateTime after = LocalDateTime.now().minusDays(1);
                assertThat(captor.getValue().from()).isBetween(before, after);
                assertThat(captor.getValue().to()).isNull();
            }

            @Test
            void explicitFromTo_overridesPeriod() {
                when(telegramRepo.count(any())).thenReturn(0L);
                when(telegramRepo.findPage(any(), anyInt(), anyInt(), any(), any())).thenReturn(List.of());

                LocalDateTime from = LocalDateTime.of(2024, 1, 1, 0, 0);
                LocalDateTime to   = LocalDateTime.of(2024, 1, 31, 23, 59);

                PostsQueryDTO q = query(0, 20);
                q.setPeriod(StatsPeriod.DAY);
                q.setFrom(from);
                q.setTo(to);

                ArgumentCaptor<PostsFilter> captor = ArgumentCaptor.forClass(PostsFilter.class);
                singleSourceService.getPosts(q);
                verify(telegramRepo).findPage(captor.capture(), anyInt(), anyInt(), any(), any());

                assertThat(captor.getValue().from()).isEqualTo(from);
                assertThat(captor.getValue().to()).isEqualTo(to);
            }
        }
    }
}
