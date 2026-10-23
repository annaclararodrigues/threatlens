package com.backend.threatlens.service;

import com.backend.threatlens.dto.request.PostsFilter;
import com.backend.threatlens.dto.request.PostsQueryDTO;
import com.backend.threatlens.dto.request.StatsQueryDTO;
import com.backend.threatlens.dto.request.WordCloudQueryDTO;
import com.backend.threatlens.dto.response.CollectionStatusDTO;
import com.backend.threatlens.dto.response.PostStatsDTO;
import com.backend.threatlens.dto.response.WordCloudDTO;
import com.backend.threatlens.dto.response.posts.PostRepliesResponse;
import com.backend.threatlens.dto.response.posts.PostsPageResponse;
import com.backend.threatlens.enums.PostSource;
import com.backend.threatlens.enums.RelevanceLevel;
import com.backend.threatlens.enums.ReplyKind;
import com.backend.threatlens.enums.SortBy;
import com.backend.threatlens.enums.SortOrder;
import com.backend.threatlens.enums.StatsPeriod;
import com.backend.threatlens.exception.BusinessRuleViolationException;
import com.backend.threatlens.exception.InvalidRequestException;
import com.backend.threatlens.repository.posts.PostsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostsServiceTest {

    @Mock
    private PostsRepository postsRepository;

    private PostsService service;

    @BeforeEach
    void setUp() {
        service = new PostsService(postsRepository);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private PostsRepository.SourceStats sourceStats(PostSource source, long total, long relevant,
                                                    long low, long medium, long high) {
        return new PostsRepository.SourceStats(source, total, relevant, low, medium, high);
    }

    private PostsQueryDTO query(int page, int size) {
        PostsQueryDTO q = new PostsQueryDTO();
        q.setPage(page);
        q.setSize(size);
        return q;
    }

    private PostsFilter capturedStatsFilter() {
        ArgumentCaptor<PostsFilter> captor = ArgumentCaptor.forClass(PostsFilter.class);
        verify(postsRepository).statsBySource(captor.capture());
        return captor.getValue();
    }

    private PostsFilter capturedPageFilter() {
        ArgumentCaptor<PostsFilter> captor = ArgumentCaptor.forClass(PostsFilter.class);
        verify(postsRepository).findPage(captor.capture(), anyInt(), anyInt(), any(), any());
        return captor.getValue();
    }

    private void stubEmptyPage() {
        when(postsRepository.count(any())).thenReturn(0L);
        when(postsRepository.findPage(any(), anyInt(), anyInt(), any(), any())).thenReturn(List.of());
    }

    // =========================================================================
    // getStats
    // =========================================================================

    @Nested
    class GetStats {

        @Nested
        class SourceAggregation {

            @Test
            void countsAreSummedAcrossSources() {
                when(postsRepository.statsBySource(any())).thenReturn(List.of(
                        sourceStats(PostSource.TELEGRAM, 100, 40, 20, 30, 10),
                        sourceStats(PostSource.NULLEDBB, 50, 10, 5, 20, 5)
                ));

                PostStatsDTO result = service.getStats(new StatsQueryDTO());

                assertThat(result.summary().totalPosts()).isEqualTo(150);
                assertThat(result.summary().relevantPostsInPeriod()).isEqualTo(50);
                assertThat(result.relevanceDistribution().lowCount()).isEqualTo(25);
                assertThat(result.relevanceDistribution().mediumCount()).isEqualTo(50);
                assertThat(result.relevanceDistribution().highCount()).isEqualTo(15);
            }

            @Test
            void sourceCountsContainOneEntryPerSourceWithLowercaseName() {
                when(postsRepository.statsBySource(any())).thenReturn(List.of(
                        sourceStats(PostSource.TELEGRAM, 100, 30, 0, 0, 30),
                        sourceStats(PostSource.NULLEDBB, 0, 0, 0, 0, 0)
                ));

                PostStatsDTO result = service.getStats(new StatsQueryDTO());

                assertThat(result.sources()).extracting(PostStatsDTO.SourceCount::sourceName)
                        .containsExactly("telegram", "nulledbb");
                assertThat(result.sources()).extracting(PostStatsDTO.SourceCount::postCount)
                        .containsExactly(100L, 0L);
                assertThat(result.sources()).extracting(PostStatsDTO.SourceCount::relevantCount)
                        .containsExactly(30L, 0L);
            }
        }

        @Nested
        class RelevancePct {

            @Test
            void zeroTotal_returnsPctZero() {
                when(postsRepository.statsBySource(any())).thenReturn(List.of(
                        sourceStats(PostSource.TELEGRAM, 0, 0, 0, 0, 0)));

                assertThat(service.getStats(new StatsQueryDTO()).summary().relevantPostsPct()).isEqualTo(0.0);
            }

            @Test
            void halfRelevant_returns50() {
                when(postsRepository.statsBySource(any())).thenReturn(List.of(
                        sourceStats(PostSource.TELEGRAM, 100, 50, 0, 0, 0)));

                assertThat(service.getStats(new StatsQueryDTO()).summary().relevantPostsPct()).isEqualTo(50.0);
            }

            @Test
            void nonIntegerResult_isRoundedTo2Decimals() {
                when(postsRepository.statsBySource(any())).thenReturn(List.of(
                        sourceStats(PostSource.TELEGRAM, 3, 1, 0, 0, 0)));

                assertThat(service.getStats(new StatsQueryDTO()).summary().relevantPostsPct()).isEqualTo(33.33);
            }
        }

        @Nested
        class TimeWindow {

            @Test
            void periodALL_passesNullDatesToRepo() {
                StatsQueryDTO query = new StatsQueryDTO();
                query.setPeriod(StatsPeriod.ALL);

                service.getStats(query);

                PostsFilter filter = capturedStatsFilter();
                assertThat(filter.from()).isNull();
                assertThat(filter.to()).isNull();
            }

            @Test
            void periodDAY_passesFromAsApproximatelyOneDayAgo() {
                LocalDateTime before = LocalDateTime.now().minusDays(1);

                StatsQueryDTO query = new StatsQueryDTO();
                query.setPeriod(StatsPeriod.DAY);
                service.getStats(query);

                LocalDateTime after = LocalDateTime.now().minusDays(1);
                PostsFilter filter = capturedStatsFilter();
                assertThat(filter.from()).isBetween(before, after);
                assertThat(filter.to()).isNull();
            }

            @Test
            void explicitFromTo_overridesPeriod() {
                LocalDateTime from = LocalDateTime.of(2024, 1, 1, 0, 0);
                LocalDateTime to   = LocalDateTime.of(2024, 1, 31, 23, 59);

                StatsQueryDTO query = new StatsQueryDTO();
                query.setPeriod(StatsPeriod.DAY);
                query.setFrom(from);
                query.setTo(to);
                service.getStats(query);

                PostsFilter filter = capturedStatsFilter();
                assertThat(filter.from()).isEqualTo(from);
                assertThat(filter.to()).isEqualTo(to);
            }

            @Test
            void onlyFromProvided_overridesPeriod() {
                LocalDateTime from = LocalDateTime.of(2024, 6, 1, 0, 0);

                StatsQueryDTO query = new StatsQueryDTO();
                query.setPeriod(StatsPeriod.WEEK);
                query.setFrom(from);
                service.getStats(query);

                PostsFilter filter = capturedStatsFilter();
                assertThat(filter.from()).isEqualTo(from);
                assertThat(filter.to()).isNull();
            }
        }

        @Nested
        class SourceFilter {

            @Test
            void noFilter_queriesAllSources() {
                service.getStats(new StatsQueryDTO());

                assertThat(capturedStatsFilter().sources()).containsExactly(PostSource.values());
            }

            @Test
            void filterBySource_passesOnlySelectedSourcesWithoutDuplicates() {
                StatsQueryDTO query = new StatsQueryDTO();
                query.setSources(List.of(PostSource.NULLEDBB, PostSource.HA, PostSource.NULLEDBB));

                service.getStats(query);

                assertThat(capturedStatsFilter().sources()).containsExactly(PostSource.NULLEDBB, PostSource.HA);
            }
        }

        @Nested
        class Validation {

            @Test
            void fromAfterTo_throwsBusinessRuleViolationException() {
                StatsQueryDTO query = new StatsQueryDTO();
                query.setFrom(LocalDateTime.of(2024, 2, 1, 0, 0));
                query.setTo(LocalDateTime.of(2024, 1, 1, 0, 0));

                assertThatThrownBy(() -> service.getStats(query))
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

        @Test
        void includeTotalFalse_skipsCountAndReturnsNullTotals() {
            when(postsRepository.findPage(any(), anyInt(), anyInt(), any(), any())).thenReturn(List.of());

            PostsQueryDTO q = query(3, 20);
            q.setIncludeTotal(false);
            PostsPageResponse result = service.getPosts(q);

            verify(postsRepository, never()).count(any());
            verify(postsRepository).findPage(any(), eq(60), eq(20), any(), any());
            assertThat(result.pagination().total()).isNull();
            assertThat(result.pagination().totalPages()).isNull();
            assertThat(result.pagination().page()).isEqualTo(3);
        }

        @Test
        void delegatesToRepoCountAndFindPage() {
            stubEmptyPage();

            service.getPosts(query(0, 20));

            verify(postsRepository).count(any());
            verify(postsRepository).findPage(any(), eq(0), eq(20), any(), any());
        }

        @Test
        void paginationMetadata_totalComesFromRepoCount() {
            when(postsRepository.count(any())).thenReturn(45L);
            when(postsRepository.findPage(any(), anyInt(), anyInt(), any(), any())).thenReturn(List.of());

            PostsPageResponse result = service.getPosts(query(0, 20));

            assertThat(result.pagination().total()).isEqualTo(45);
            assertThat(result.pagination().totalPages()).isEqualTo(3);
            assertThat(result.pagination().page()).isEqualTo(0);
            assertThat(result.pagination().size()).isEqualTo(20);
        }

        @Test
        void page2Size10_offsetIs20() {
            stubEmptyPage();

            service.getPosts(query(2, 10));

            verify(postsRepository).findPage(any(), eq(20), eq(10), any(), any());
        }

        @Test
        void filterFields_passedToRepo() {
            stubEmptyPage();

            PostsQueryDTO q = query(0, 20);
            q.setRelevance(RelevanceLevel.HIGH);
            q.setCategory("malware");
            q.setSearch("ransomware");
            service.getPosts(q);

            PostsFilter filter = capturedPageFilter();
            assertThat(filter.relevance()).isEqualTo(RelevanceLevel.HIGH);
            assertThat(filter.category()).isEqualTo("malware");
            assertThat(filter.search()).isEqualTo("ransomware");
        }

        @Test
        void noSourceFilter_queriesAllSources() {
            stubEmptyPage();

            service.getPosts(query(0, 20));

            assertThat(capturedPageFilter().sources()).containsExactly(PostSource.values());
        }

        @Test
        void sortAndOrder_passedToRepo() {
            stubEmptyPage();

            PostsQueryDTO q = query(0, 20);
            q.setSort(SortBy.SCORE);
            q.setOrder(SortOrder.ASC);
            service.getPosts(q);

            verify(postsRepository).findPage(any(), anyInt(), anyInt(), eq(SortBy.SCORE), eq(SortOrder.ASC));
        }

        @Nested
        class DateWindow {

            @Test
            void fromAfterTo_throwsBusinessRuleViolationException() {
                PostsQueryDTO q = query(0, 20);
                q.setFrom(LocalDateTime.of(2024, 2, 1, 0, 0));
                q.setTo(LocalDateTime.of(2024, 1, 1, 0, 0));

                assertThatThrownBy(() -> service.getPosts(q))
                        .isInstanceOf(BusinessRuleViolationException.class)
                        .hasMessageContaining("'from' can not be after 'to'");
            }

            @Test
            void equalFromAndTo_doesNotThrow() {
                stubEmptyPage();
                LocalDateTime dt = LocalDateTime.of(2024, 1, 1, 0, 0);

                PostsQueryDTO q = query(0, 20);
                q.setFrom(dt);
                q.setTo(dt);

                assertThat(service.getPosts(q)).isNotNull();
            }

            @Test
            void periodALL_passesNullDatesToRepo() {
                stubEmptyPage();

                PostsQueryDTO q = query(0, 20);
                q.setPeriod(StatsPeriod.ALL);
                service.getPosts(q);

                PostsFilter filter = capturedPageFilter();
                assertThat(filter.from()).isNull();
                assertThat(filter.to()).isNull();
            }

            @Test
            void explicitFromTo_overridesPeriod() {
                stubEmptyPage();
                LocalDateTime from = LocalDateTime.of(2024, 1, 1, 0, 0);
                LocalDateTime to   = LocalDateTime.of(2024, 1, 31, 23, 59);

                PostsQueryDTO q = query(0, 20);
                q.setPeriod(StatsPeriod.DAY);
                q.setFrom(from);
                q.setTo(to);
                service.getPosts(q);

                PostsFilter filter = capturedPageFilter();
                assertThat(filter.from()).isEqualTo(from);
                assertThat(filter.to()).isEqualTo(to);
            }
        }
    }

    // =========================================================================
    // getWordCloud
    // =========================================================================

    @Nested
    class GetWordCloud {

        @Test
        void mapsRepoWordsInOrder() {
            when(postsRepository.wordFrequencies(any(), anyInt())).thenReturn(List.of(
                    new PostsRepository.WordCount("phishing", 25),
                    new PostsRepository.WordCount("malware", 10)
            ));

            WordCloudDTO result = service.getWordCloud(new WordCloudQueryDTO());

            assertThat(result.words()).extracting(WordCloudDTO.WordEntry::word)
                    .containsExactly("phishing", "malware");
            assertThat(result.words()).extracting(WordCloudDTO.WordEntry::count)
                    .containsExactly(25L, 10L);
        }

        @Test
        void filterAndLimit_passedToRepo() {
            WordCloudQueryDTO query = new WordCloudQueryDTO();
            query.setRelevance(RelevanceLevel.HIGH);
            query.setCategory("malware");
            query.setLimit(30);

            service.getWordCloud(query);

            ArgumentCaptor<PostsFilter> captor = ArgumentCaptor.forClass(PostsFilter.class);
            verify(postsRepository).wordFrequencies(captor.capture(), eq(30));
            assertThat(captor.getValue().relevance()).isEqualTo(RelevanceLevel.HIGH);
            assertThat(captor.getValue().category()).isEqualTo("malware");
            assertThat(captor.getValue().sources()).containsExactly(PostSource.values());
        }

        @Test
        void fromAfterTo_throwsBusinessRuleViolationException() {
            WordCloudQueryDTO query = new WordCloudQueryDTO();
            query.setFrom(LocalDateTime.of(2024, 2, 1, 0, 0));
            query.setTo(LocalDateTime.of(2024, 1, 1, 0, 0));

            assertThatThrownBy(() -> service.getWordCloud(query))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("'from' can not be after 'to'");
        }
    }

    // =========================================================================
    // getCollectionStatus
    // =========================================================================

    @Nested
    class GetCollectionStatus {

        private Map<PostSource, LocalDateTime> lastCollected(LocalDateTime telegram, LocalDateTime nulledbb) {
            Map<PostSource, LocalDateTime> map = new EnumMap<>(PostSource.class);
            map.put(PostSource.TELEGRAM, telegram);
            map.put(PostSource.NULLEDBB, nulledbb);
            return map;
        }

        @Test
        void returnsOneEntryPerSourceWithLowercaseName() {
            LocalDateTime last = LocalDateTime.of(2026, 10, 22, 10, 30);
            when(postsRepository.lastCollectedAtBySource()).thenReturn(lastCollected(last, null));

            CollectionStatusDTO result = service.getCollectionStatus();

            assertThat(result.sources()).containsExactly(
                    new CollectionStatusDTO.SourceStatus("telegram", last),
                    new CollectionStatusDTO.SourceStatus("nulledbb", null));
        }

        @Test
        void overallIsTheMostRecent() {
            LocalDateTime older = LocalDateTime.of(2026, 10, 20, 8, 0);
            LocalDateTime newer = LocalDateTime.of(2026, 10, 22, 10, 30);
            when(postsRepository.lastCollectedAtBySource()).thenReturn(lastCollected(older, newer));

            assertThat(service.getCollectionStatus().lastCollectedAt()).isEqualTo(newer);
        }

        @Test
        void sourceWithoutData_isIgnoredInOverall() {
            LocalDateTime last = LocalDateTime.of(2026, 10, 22, 10, 30);
            when(postsRepository.lastCollectedAtBySource()).thenReturn(lastCollected(null, last));

            assertThat(service.getCollectionStatus().lastCollectedAt()).isEqualTo(last);
        }

        @Test
        void noDataAtAll_returnsNull() {
            when(postsRepository.lastCollectedAtBySource()).thenReturn(lastCollected(null, null));

            assertThat(service.getCollectionStatus().lastCollectedAt()).isNull();
        }
    }

    // =========================================================================
    // getReplies
    // =========================================================================

    @Nested
    class GetReplies {

        @Test
        void splitsSourceLanguageAndIdFromThePostId() {
            PostRepliesResponse.Answer answer = new PostRepliesResponse.Answer(
                    "7", "alice", null, "resposta", Map.of(), List.of());
            when(postsRepository.findReplies(PostSource.HA, "ptbr", 42L))
                    .thenReturn(Optional.of(new PostsRepository.Replies(ReplyKind.ANSWER, List.of(answer))));

            PostRepliesResponse result = service.getReplies("ha:ptbr:42");

            assertThat(result.postId()).isEqualTo("ha:ptbr:42");
            assertThat(result.kind()).isEqualTo(ReplyKind.ANSWER);
            assertThat(result.answers()).containsExactly(answer);
        }

        @Test
        void postIdWithoutLanguage_passesNullSuffix() {
            when(postsRepository.findReplies(PostSource.NULLEDBB, null, 5L))
                    .thenReturn(Optional.of(new PostsRepository.Replies(ReplyKind.ANSWER, List.of())));

            assertThat(service.getReplies("nulledbb:5").answers()).isEmpty();
        }

        @Test
        void sourceWithoutReplies_throwsInvalidRequest() {
            when(postsRepository.findReplies(PostSource.TELEGRAM, null, 5L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getReplies("telegram:5"))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        void unknownSource_throwsInvalidRequestWithoutQuerying() {
            assertThatThrownBy(() -> service.getReplies("reddit:5"))
                    .isInstanceOf(InvalidRequestException.class);
            verify(postsRepository, never()).findReplies(any(), any(), anyLong());
        }

        @Test
        void nonNumericOrMalformedId_throwsInvalidRequestWithoutQuerying() {
            assertThatThrownBy(() -> service.getReplies("ha:ptbr:abc")).isInstanceOf(InvalidRequestException.class);
            assertThatThrownBy(() -> service.getReplies("ha")).isInstanceOf(InvalidRequestException.class);
            assertThatThrownBy(() -> service.getReplies("ha:a:b:1")).isInstanceOf(InvalidRequestException.class);
            verify(postsRepository, never()).findReplies(any(), any(), anyLong());
        }
    }
}
