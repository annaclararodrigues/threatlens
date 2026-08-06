package com.backend.threatlens.controller;

import com.backend.threatlens.dto.request.PostsQueryDTO;
import com.backend.threatlens.dto.request.StatsQueryDTO;
import com.backend.threatlens.dto.request.WordCloudQueryDTO;
import com.backend.threatlens.dto.response.PostStatsDTO;
import com.backend.threatlens.dto.response.WordCloudDTO;
import com.backend.threatlens.dto.response.posts.PostResponse;
import com.backend.threatlens.dto.response.posts.PostsPageResponse;
import com.backend.threatlens.enums.PostSource;
import com.backend.threatlens.enums.RelevanceLevel;
import com.backend.threatlens.enums.SortBy;
import com.backend.threatlens.enums.SortOrder;
import com.backend.threatlens.enums.StatsPeriod;
import com.backend.threatlens.exception.BusinessRuleViolationException;
import com.backend.threatlens.exception.GlobalExceptionHandler;
import com.backend.threatlens.service.PostsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PostsControllerTest {

    @Mock
    private PostsService postsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PostsController(postsService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter())
                .build();
    }

    // =========================================================================
    // GET /posts
    // =========================================================================

    @Nested
    class GetPosts {

        @Nested
        class ResponseBody {

            @Test
            void returnsDataAndPaginationShape() throws Exception {
                PostResponse post = new PostResponse(
                        "id-1", PostSource.TELEGRAM, null, "content", null,
                        LocalDateTime.of(2024, 6, 1, 10, 30), null, null, null
                );
                PostsPageResponse response = new PostsPageResponse(
                        List.of(post),
                        new PostsPageResponse.Pagination(0, 20, 1, 1)
                );
                when(postsService.getPosts(any())).thenReturn(response);

                mockMvc.perform(get("/posts"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data").isArray())
                        .andExpect(jsonPath("$.data[0].id").value("id-1"))
                        .andExpect(jsonPath("$.data[0].source").value("TELEGRAM"))
                        .andExpect(jsonPath("$.data[0].createdAt").value("2024-06-01T10:30:00"))
                        .andExpect(jsonPath("$.pagination.page").value(0))
                        .andExpect(jsonPath("$.pagination.size").value(20))
                        .andExpect(jsonPath("$.pagination.total").value(1))
                        .andExpect(jsonPath("$.pagination.totalPages").value(1));
            }

            @Test
            void emptyResult_returnsEmptyDataArray() throws Exception {
                when(postsService.getPosts(any())).thenReturn(
                        new PostsPageResponse(List.of(), new PostsPageResponse.Pagination(0, 20, 0, 0))
                );

                mockMvc.perform(get("/posts"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data").isArray())
                        .andExpect(jsonPath("$.data").isEmpty());
            }
        }

        @Nested
        class QueryParamBinding {

            @Test
            void noParams_usesDefaults() throws Exception {
                when(postsService.getPosts(any())).thenReturn(emptyPage());

                mockMvc.perform(get("/posts")).andExpect(status().isOk());

                ArgumentCaptor<PostsQueryDTO> captor = ArgumentCaptor.forClass(PostsQueryDTO.class);
                verify(postsService).getPosts(captor.capture());

                PostsQueryDTO dto = captor.getValue();
                assertThat(dto.getPage()).isEqualTo(0);
                assertThat(dto.getSize()).isEqualTo(20);
                assertThat(dto.getSort()).isEqualTo(SortBy.DATE);
                assertThat(dto.getOrder()).isEqualTo(SortOrder.DESC);
                assertThat(dto.getPeriod()).isEqualTo(StatsPeriod.ALL);
                assertThat(dto.getSources()).isNull();
                assertThat(dto.getRelevance()).isNull();
            }

            @Test
            void paginationParams_bindCorrectly() throws Exception {
                when(postsService.getPosts(any())).thenReturn(emptyPage());

                mockMvc.perform(get("/posts").param("page", "2").param("size", "10"))
                        .andExpect(status().isOk());

                ArgumentCaptor<PostsQueryDTO> captor = ArgumentCaptor.forClass(PostsQueryDTO.class);
                verify(postsService).getPosts(captor.capture());

                assertThat(captor.getValue().getPage()).isEqualTo(2);
                assertThat(captor.getValue().getSize()).isEqualTo(10);
            }

            @Test
            void sortAndOrderParams_bindCorrectly() throws Exception {
                when(postsService.getPosts(any())).thenReturn(emptyPage());

                mockMvc.perform(get("/posts").param("sort", "SCORE").param("order", "ASC"))
                        .andExpect(status().isOk());

                ArgumentCaptor<PostsQueryDTO> captor = ArgumentCaptor.forClass(PostsQueryDTO.class);
                verify(postsService).getPosts(captor.capture());

                assertThat(captor.getValue().getSort()).isEqualTo(SortBy.SCORE);
                assertThat(captor.getValue().getOrder()).isEqualTo(SortOrder.ASC);
            }

            @Test
            void sourcesParam_bindsTelegramEnum() throws Exception {
                when(postsService.getPosts(any())).thenReturn(emptyPage());

                mockMvc.perform(get("/posts").param("sources", "TELEGRAM"))
                        .andExpect(status().isOk());

                ArgumentCaptor<PostsQueryDTO> captor = ArgumentCaptor.forClass(PostsQueryDTO.class);
                verify(postsService).getPosts(captor.capture());

                assertThat(captor.getValue().getSources()).containsExactly(PostSource.TELEGRAM);
            }

            @Test
            void relevanceParam_bindsEnumCorrectly() throws Exception {
                when(postsService.getPosts(any())).thenReturn(emptyPage());

                mockMvc.perform(get("/posts").param("relevance", "HIGH"))
                        .andExpect(status().isOk());

                ArgumentCaptor<PostsQueryDTO> captor = ArgumentCaptor.forClass(PostsQueryDTO.class);
                verify(postsService).getPosts(captor.capture());

                assertThat(captor.getValue().getRelevance()).isEqualTo(RelevanceLevel.HIGH);
            }

            @Test
            void periodParam_bindsEnumCorrectly() throws Exception {
                when(postsService.getPosts(any())).thenReturn(emptyPage());

                mockMvc.perform(get("/posts").param("period", "WEEK"))
                        .andExpect(status().isOk());

                ArgumentCaptor<PostsQueryDTO> captor = ArgumentCaptor.forClass(PostsQueryDTO.class);
                verify(postsService).getPosts(captor.capture());

                assertThat(captor.getValue().getPeriod()).isEqualTo(StatsPeriod.WEEK);
            }

            @Test
            void categoryParam_bindsAsString() throws Exception {
                when(postsService.getPosts(any())).thenReturn(emptyPage());

                mockMvc.perform(get("/posts").param("category", "malware"))
                        .andExpect(status().isOk());

                ArgumentCaptor<PostsQueryDTO> captor = ArgumentCaptor.forClass(PostsQueryDTO.class);
                verify(postsService).getPosts(captor.capture());

                assertThat(captor.getValue().getCategory()).isEqualTo("malware");
            }

            @Test
            void fromAndToParams_bindAsLocalDateTime() throws Exception {
                when(postsService.getPosts(any())).thenReturn(emptyPage());

                mockMvc.perform(get("/posts")
                                .param("from", "2024-01-01T00:00:00")
                                .param("to",   "2024-01-31T23:59:59"))
                        .andExpect(status().isOk());

                ArgumentCaptor<PostsQueryDTO> captor = ArgumentCaptor.forClass(PostsQueryDTO.class);
                verify(postsService).getPosts(captor.capture());

                assertThat(captor.getValue().getFrom()).isEqualTo(LocalDateTime.of(2024, 1, 1, 0, 0, 0));
                assertThat(captor.getValue().getTo()).isEqualTo(LocalDateTime.of(2024, 1, 31, 23, 59, 59));
            }
        }

        @Nested
        class ErrorHandling {

            @Test
            void serviceThrowsBusinessRuleViolation_returns422WithMessage() throws Exception {
                when(postsService.getPosts(any()))
                        .thenThrow(new BusinessRuleViolationException("'from' can not be after 'to'"));

                mockMvc.perform(get("/posts")
                                .param("from", "2024-02-01T00:00:00")
                                .param("to",   "2024-01-01T00:00:00"))
                        .andExpect(status().isUnprocessableEntity())
                        .andExpect(jsonPath("$.message").value("'from' can not be after 'to'"));
            }
        }
    }

    // =========================================================================
    // GET /posts/stats
    // =========================================================================

    @Nested
    class GetStats {

        @Nested
        class ResponseBody {

            @Test
            void returnsFullStatsShape() throws Exception {
                PostStatsDTO response = new PostStatsDTO(
                        new PostStatsDTO.Summary(200, 80, 40.0),
                        new PostStatsDTO.RelevanceDistribution(30, 90, 80),
                        List.of(new PostStatsDTO.SourceCount("telegram", 200))
                );
                when(postsService.getStats(any())).thenReturn(response);

                mockMvc.perform(get("/posts/stats"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.summary.totalPosts").value(200))
                        .andExpect(jsonPath("$.summary.relevantPostsInPeriod").value(80))
                        .andExpect(jsonPath("$.summary.relevantPostsPct").value(40.0))
                        .andExpect(jsonPath("$.relevanceDistribution.lowCount").value(30))
                        .andExpect(jsonPath("$.relevanceDistribution.mediumCount").value(90))
                        .andExpect(jsonPath("$.relevanceDistribution.highCount").value(80))
                        .andExpect(jsonPath("$.sources[0].sourceName").value("telegram"))
                        .andExpect(jsonPath("$.sources[0].postCount").value(200));
            }
        }

        @Nested
        class QueryParamBinding {

            @Test
            void noParams_usesDefaultPeriodALL() throws Exception {
                when(postsService.getStats(any())).thenReturn(emptyStats());

                mockMvc.perform(get("/posts/stats")).andExpect(status().isOk());

                ArgumentCaptor<StatsQueryDTO> captor = ArgumentCaptor.forClass(StatsQueryDTO.class);
                verify(postsService).getStats(captor.capture());

                assertThat(captor.getValue().getPeriod()).isEqualTo(StatsPeriod.ALL);
                assertThat(captor.getValue().getSources()).isNull();
                assertThat(captor.getValue().getRelevance()).isNull();
            }

            @Test
            void periodParam_bindsEnumCorrectly() throws Exception {
                when(postsService.getStats(any())).thenReturn(emptyStats());

                mockMvc.perform(get("/posts/stats").param("period", "WEEK"))
                        .andExpect(status().isOk());

                ArgumentCaptor<StatsQueryDTO> captor = ArgumentCaptor.forClass(StatsQueryDTO.class);
                verify(postsService).getStats(captor.capture());

                assertThat(captor.getValue().getPeriod()).isEqualTo(StatsPeriod.WEEK);
            }

            @Test
            void sourcesParam_bindsTelegramEnum() throws Exception {
                when(postsService.getStats(any())).thenReturn(emptyStats());

                mockMvc.perform(get("/posts/stats").param("sources", "TELEGRAM"))
                        .andExpect(status().isOk());

                ArgumentCaptor<StatsQueryDTO> captor = ArgumentCaptor.forClass(StatsQueryDTO.class);
                verify(postsService).getStats(captor.capture());

                assertThat(captor.getValue().getSources()).containsExactly(PostSource.TELEGRAM);
            }

            @Test
            void fromAndToParams_bindAsLocalDateTime() throws Exception {
                when(postsService.getStats(any())).thenReturn(emptyStats());

                mockMvc.perform(get("/posts/stats")
                                .param("from", "2024-01-01T00:00:00")
                                .param("to",   "2024-01-31T23:59:00"))
                        .andExpect(status().isOk());

                ArgumentCaptor<StatsQueryDTO> captor = ArgumentCaptor.forClass(StatsQueryDTO.class);
                verify(postsService).getStats(captor.capture());

                assertThat(captor.getValue().getFrom()).isEqualTo(LocalDateTime.of(2024, 1, 1, 0, 0, 0));
                assertThat(captor.getValue().getTo()).isEqualTo(LocalDateTime.of(2024, 1, 31, 23, 59, 0));
            }

            @Test
            void relevanceAndCategoryParams_bindCorrectly() throws Exception {
                when(postsService.getStats(any())).thenReturn(emptyStats());

                mockMvc.perform(get("/posts/stats")
                                .param("relevance", "MEDIUM")
                                .param("category", "ransomware"))
                        .andExpect(status().isOk());

                ArgumentCaptor<StatsQueryDTO> captor = ArgumentCaptor.forClass(StatsQueryDTO.class);
                verify(postsService).getStats(captor.capture());

                assertThat(captor.getValue().getRelevance()).isEqualTo(RelevanceLevel.MEDIUM);
                assertThat(captor.getValue().getCategory()).isEqualTo("ransomware");
            }
        }

        @Nested
        class ErrorHandling {

            @Test
            void serviceThrowsBusinessRuleViolation_returns422WithMessage() throws Exception {
                when(postsService.getStats(any()))
                        .thenThrow(new BusinessRuleViolationException("'from' can not be after 'to'"));

                mockMvc.perform(get("/posts/stats")
                                .param("from", "2024-02-01T00:00:00")
                                .param("to",   "2024-01-01T00:00:00"))
                        .andExpect(status().isUnprocessableEntity())
                        .andExpect(jsonPath("$.message").value("'from' can not be after 'to'"));
            }
        }
    }

    // =========================================================================
    // GET /posts/wordcloud
    // =========================================================================

    @Nested
    class GetWordCloud {

        @Nested
        class ResponseBody {

            @Test
            void returnsWordsShape() throws Exception {
                WordCloudDTO response = new WordCloudDTO(List.of(
                        new WordCloudDTO.WordEntry("malware", 42),
                        new WordCloudDTO.WordEntry("phishing", 30)
                ));
                when(postsService.getWordCloud(any())).thenReturn(response);

                mockMvc.perform(get("/posts/wordcloud"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.words").isArray())
                        .andExpect(jsonPath("$.words[0].word").value("malware"))
                        .andExpect(jsonPath("$.words[0].count").value(42))
                        .andExpect(jsonPath("$.words[1].word").value("phishing"))
                        .andExpect(jsonPath("$.words[1].count").value(30));
            }
        }

        @Nested
        class QueryParamBinding {

            @Test
            void noParams_usesDefaults() throws Exception {
                when(postsService.getWordCloud(any())).thenReturn(emptyWordCloud());

                mockMvc.perform(get("/posts/wordcloud")).andExpect(status().isOk());

                ArgumentCaptor<WordCloudQueryDTO> captor = ArgumentCaptor.forClass(WordCloudQueryDTO.class);
                verify(postsService).getWordCloud(captor.capture());

                assertThat(captor.getValue().getLimit()).isEqualTo(50);
                assertThat(captor.getValue().getPeriod()).isEqualTo(StatsPeriod.ALL);
                assertThat(captor.getValue().getSources()).isNull();
                assertThat(captor.getValue().getRelevance()).isNull();
            }

            @Test
            void limitParam_bindsAsInt() throws Exception {
                when(postsService.getWordCloud(any())).thenReturn(emptyWordCloud());

                mockMvc.perform(get("/posts/wordcloud").param("limit", "30"))
                        .andExpect(status().isOk());

                ArgumentCaptor<WordCloudQueryDTO> captor = ArgumentCaptor.forClass(WordCloudQueryDTO.class);
                verify(postsService).getWordCloud(captor.capture());

                assertThat(captor.getValue().getLimit()).isEqualTo(30);
            }

            @Test
            void relevanceAndCategoryParams_bindCorrectly() throws Exception {
                when(postsService.getWordCloud(any())).thenReturn(emptyWordCloud());

                mockMvc.perform(get("/posts/wordcloud")
                                .param("relevance", "HIGH")
                                .param("category", "ransomware"))
                        .andExpect(status().isOk());

                ArgumentCaptor<WordCloudQueryDTO> captor = ArgumentCaptor.forClass(WordCloudQueryDTO.class);
                verify(postsService).getWordCloud(captor.capture());

                assertThat(captor.getValue().getRelevance()).isEqualTo(RelevanceLevel.HIGH);
                assertThat(captor.getValue().getCategory()).isEqualTo("ransomware");
            }

            @Test
            void sourcesParam_bindsTelegramEnum() throws Exception {
                when(postsService.getWordCloud(any())).thenReturn(emptyWordCloud());

                mockMvc.perform(get("/posts/wordcloud").param("sources", "TELEGRAM"))
                        .andExpect(status().isOk());

                ArgumentCaptor<WordCloudQueryDTO> captor = ArgumentCaptor.forClass(WordCloudQueryDTO.class);
                verify(postsService).getWordCloud(captor.capture());

                assertThat(captor.getValue().getSources()).containsExactly(PostSource.TELEGRAM);
            }

            @Test
            void fromAndToParams_bindAsLocalDateTime() throws Exception {
                when(postsService.getWordCloud(any())).thenReturn(emptyWordCloud());

                mockMvc.perform(get("/posts/wordcloud")
                                .param("from", "2024-01-01T00:00:00")
                                .param("to",   "2024-01-31T23:59:59"))
                        .andExpect(status().isOk());

                ArgumentCaptor<WordCloudQueryDTO> captor = ArgumentCaptor.forClass(WordCloudQueryDTO.class);
                verify(postsService).getWordCloud(captor.capture());

                assertThat(captor.getValue().getFrom()).isEqualTo(LocalDateTime.of(2024, 1, 1, 0, 0, 0));
                assertThat(captor.getValue().getTo()).isEqualTo(LocalDateTime.of(2024, 1, 31, 23, 59, 59));
            }
        }

        @Nested
        class ErrorHandling {

            @Test
            void serviceThrowsBusinessRuleViolation_returns422WithMessage() throws Exception {
                when(postsService.getWordCloud(any()))
                        .thenThrow(new BusinessRuleViolationException("'from' can not be after 'to'"));

                mockMvc.perform(get("/posts/wordcloud")
                                .param("from", "2024-02-01T00:00:00")
                                .param("to",   "2024-01-01T00:00:00"))
                        .andExpect(status().isUnprocessableEntity())
                        .andExpect(jsonPath("$.message").value("'from' can not be after 'to'"));
            }

            @Test
            void negativeLimit_returns422WithoutCallingService() throws Exception {
                mockMvc.perform(get("/posts/wordcloud").param("limit", "-5"))
                        .andExpect(status().isUnprocessableEntity());

                verify(postsService, org.mockito.Mockito.never()).getWordCloud(any());
            }

            @Test
            void limitAboveMax_returns422WithoutCallingService() throws Exception {
                mockMvc.perform(get("/posts/wordcloud").param("limit", "201"))
                        .andExpect(status().isUnprocessableEntity());

                verify(postsService, org.mockito.Mockito.never()).getWordCloud(any());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private WordCloudDTO emptyWordCloud() {
        return new WordCloudDTO(List.of());
    }

    private PostsPageResponse emptyPage() {
        return new PostsPageResponse(List.of(), new PostsPageResponse.Pagination(0, 20, 0, 0));
    }

    private PostStatsDTO emptyStats() {
        return new PostStatsDTO(
                new PostStatsDTO.Summary(0, 0, 0.0),
                new PostStatsDTO.RelevanceDistribution(0, 0, 0),
                List.of()
        );
    }
}
