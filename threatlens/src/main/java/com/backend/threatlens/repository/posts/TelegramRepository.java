package com.backend.threatlens.repository.posts;

import com.backend.threatlens.dto.request.PostsFilter;
import com.backend.threatlens.dto.response.posts.ClassificationResponse;
import com.backend.threatlens.dto.response.posts.PostResponse;
import com.backend.threatlens.dto.response.posts.TelegramMeta;
import com.backend.threatlens.enums.PostSource;
import com.backend.threatlens.enums.RelevanceLevel;
import com.backend.threatlens.enums.SortBy;
import com.backend.threatlens.enums.SortOrder;
import com.backend.threatlens.utils.StopWords;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class TelegramRepository implements PostsSourceRepository {

    private static final double LOW_THRESHOLD    = 0.3;
    private static final double MEDIUM_THRESHOLD = 0.7;

    private static final String RELEVANCE_CASE =
            "CASE WHEN c.probability < 0.3 THEN 'LOW' WHEN c.probability < 0.7 THEN 'MEDIUM' ELSE 'HIGH' END";

    private static final String RELEVANCE_FILTER =
            "(:relevance::text IS NULL OR " + RELEVANCE_CASE + " = :relevance)";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TelegramRepository(@Qualifier("postsJdbcTemplate") NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PostSource source() {
        return PostSource.TELEGRAM;
    }

    @Override
    public List<PostResponse> findPage(PostsFilter filter, int offset, int limit, SortBy sortBy, SortOrder sortOrder) {
        String orderColumn = switch (sortBy) {
            case SCORE   -> "score";
            case DATE    -> "created_at";
            case ID      -> "id";
            case CONTENT -> "content";
            // Só há uma fonte hoje (Telegram); ordenar por "fonte" não distingue nada,
            // então cai de volta para "id" como critério estável.
            case SOURCE  -> "id";
        };
        String orderDir = sortOrder == SortOrder.DESC ? "DESC" : "ASC";

        String sql = """
                SELECT
                    m.id,
                    m.message                AS content,
                    m."createdAt"            AS created_at,
                    m."telegramChannelId"    AS telegram_channel_id,
                    ch.name                  AS channel_name,
                    ch.username              AS channel_username,
                    c.relevant,
                    c.probability            AS score,
                    c.range,
                    c.ioc,
                    c.classified_at
                FROM telegram_message m
                LEFT JOIN telegram_channel ch ON ch.id = m."telegramChannelId"
                LEFT JOIN telegram_classified_messages c ON c.id_post = m.id
                WHERE\s""" + RELEVANCE_FILTER + """
                  AND (:category::text IS NULL OR c.content = :category)
                  AND (:search::text IS NULL OR m.message ILIKE '%' || :search || '%')
                  AND (:from::timestamp IS NULL OR m."createdAt" >= :from::timestamp)
                  AND (:to::timestamp IS NULL OR m."createdAt" <= :to::timestamp)
                ORDER BY\s""" + orderColumn + " " + orderDir + " NULLS LAST" + """
                \sLIMIT :limit OFFSET :offset
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("relevance", filter.relevance() != null ? filter.relevance().name() : null);
        params.put("category", filter.category());
        params.put("search", filter.search());
        params.put("from", filter.from());
        params.put("to", filter.to());
        params.put("limit", limit);
        params.put("offset", offset);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            double score = rs.getDouble("score");
            boolean scoreNull = rs.wasNull();

            ClassificationResponse clf = scoreNull ? null : new ClassificationResponse(
                    rs.getBoolean("relevant"),
                    score,
                    resolveLevel(score),
                    rs.getString("range"),
                    rs.getObject("ioc", Boolean.class),
                    rs.getObject("classified_at", LocalDateTime.class)
            );

            LocalDateTime createdAt = rs.getObject("created_at", LocalDateTime.class);

            TelegramMeta meta = new TelegramMeta(
                    rs.getLong("telegram_channel_id"),
                    rs.getString("channel_name"),
                    rs.getString("channel_username")
            );

            return PostResponse.fromTelegram(rs.getString("id"), rs.getString("content"), createdAt, clf, meta);
        });
    }

    @Override
    public long count(PostsFilter filter) {
        String sql = """
                SELECT COUNT(*)
                FROM telegram_message m
                LEFT JOIN telegram_classified_messages c ON c.id_post = m.id
                WHERE\s""" + RELEVANCE_FILTER + """
                  AND (:category::text IS NULL OR c.content = :category)
                  AND (:search::text IS NULL OR m.message ILIKE '%' || :search || '%')
                  AND (:from::timestamp IS NULL OR m."createdAt" >= :from::timestamp)
                  AND (:to::timestamp IS NULL OR m."createdAt" <= :to::timestamp)
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("relevance", filter.relevance() != null ? filter.relevance().name() : null);
        params.put("category", filter.category());
        params.put("search", filter.search());
        params.put("from", filter.from());
        params.put("to", filter.to());

        return queryCount(sql, params);
    }

    @Override
    public SourceStats getStats(PostsFilter filter) {
        String sql = """
                SELECT
                    COUNT(*)                                                       AS total,
                    COUNT(*) FILTER (WHERE c.probability >= 0.7)                  AS relevant_count,
                    COUNT(*) FILTER (WHERE c.probability < 0.3)                   AS low_count,
                    COUNT(*) FILTER (WHERE c.probability >= 0.3
                                      AND c.probability < 0.7)                    AS medium_count,
                    COUNT(*) FILTER (WHERE c.probability >= 0.7)                  AS high_count
                FROM telegram_message m
                LEFT JOIN telegram_classified_messages c ON c.id_post = m.id
                WHERE (:since::timestamp IS NULL OR m."createdAt" >= :since::timestamp)
                  AND (:until::timestamp IS NULL OR m."createdAt" <= :until::timestamp)
                  AND\s""" + RELEVANCE_FILTER + """
                  AND (:category::text IS NULL OR c.content = :category)
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("since", filter.from());
        params.put("until", filter.to());
        params.put("relevance", filter.relevance() != null ? filter.relevance().name() : null);
        params.put("category", filter.category());

        return jdbcTemplate.queryForObject(sql, params, (rs, rowNum) -> new SourceStats(
                rs.getLong("total"),
                rs.getLong("relevant_count"),
                rs.getLong("low_count"),
                rs.getLong("medium_count"),
                rs.getLong("high_count")
        ));
    }

    @Override
    public List<WordCount> wordFrequencies(PostsFilter filter, int limit) {
        String sql = """
                SELECT word, COUNT(*) AS occurrences
                FROM (
                    SELECT regexp_split_to_table(
                               lower(regexp_replace(m.message, '[^[:alpha:]\\s]', ' ', 'g')),
                               '\\s+'
                           ) AS word
                    FROM telegram_message m
                    LEFT JOIN telegram_classified_messages c ON c.id_post = m.id
                    WHERE\s""" + RELEVANCE_FILTER + """
                      AND (:category::text IS NULL OR c.content = :category)
                      AND (:from::timestamp IS NULL OR m."createdAt" >= :from::timestamp)
                      AND (:to::timestamp IS NULL OR m."createdAt" <= :to::timestamp)
                ) words
                WHERE length(word) > 2
                  AND word NOT IN (:stopwords)
                GROUP BY word
                ORDER BY occurrences DESC
                LIMIT :limit
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("relevance", filter.relevance() != null ? filter.relevance().name() : null);
        params.put("category", filter.category());
        params.put("from", filter.from());
        params.put("to", filter.to());
        params.put("stopwords", List.copyOf(StopWords.ALL));
        params.put("limit", limit);

        return jdbcTemplate.query(sql, params, (rs, rowNum) ->
                new WordCount(rs.getString("word"), rs.getLong("occurrences")));
    }

    private long queryCount(String sql, Map<String, Object> params) {
        Long result = jdbcTemplate.queryForObject(sql, params, Long.class);
        return result != null ? result : 0L;
    }

    private RelevanceLevel resolveLevel(double score) {
        if (score < LOW_THRESHOLD)    return RelevanceLevel.LOW;
        if (score < MEDIUM_THRESHOLD) return RelevanceLevel.MEDIUM;
        return RelevanceLevel.HIGH;
    }
}
