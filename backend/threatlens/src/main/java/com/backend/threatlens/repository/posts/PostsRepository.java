package com.backend.threatlens.repository.posts;

import com.backend.threatlens.dto.request.PostsFilter;
import com.backend.threatlens.dto.response.posts.ClassificationResponse;
import com.backend.threatlens.dto.response.posts.PostRepliesResponse;
import com.backend.threatlens.dto.response.posts.PostResponse;
import com.backend.threatlens.enums.PostSource;
import com.backend.threatlens.enums.RelevanceLevel;
import com.backend.threatlens.enums.ReplyKind;
import com.backend.threatlens.enums.SortBy;
import com.backend.threatlens.enums.SortOrder;
import com.backend.threatlens.utils.StopWords;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Consultas de posts sobre todas as fontes do banco asgard. As fontes selecionadas são
 * combinadas com UNION ALL (ver {@link PostSourceQueries}), e filtro, ordenação e paginação
 * acontecem no PostgreSQL, em uma única consulta.
 */
@Repository
public class PostsRepository {

    private static final double LOW_THRESHOLD  = 0.3;
    private static final double HIGH_THRESHOLD = 0.7;

    // Memória para agrupar as palavras da nuvem sem ordenar em disco
    private static final String WORD_CLOUD_WORK_MEM = "64MB";

    private static final String RELEVANCE_CASE = """
            CASE WHEN p.score IS NULL THEN NULL
                 WHEN p.score < %s THEN 'LOW'
                 WHEN p.score < %s THEN 'MEDIUM'
                 ELSE 'HIGH' END""".formatted(LOW_THRESHOLD, HIGH_THRESHOLD);

    private static final String WHERE = """
            WHERE (:relevance::text IS NULL OR %s = :relevance)
              AND (:category::text IS NULL OR p.category = :category)
              AND (:search::text IS NULL OR p.content ILIKE '%%' || :search || '%%'
                                         OR p.title ILIKE '%%' || :search || '%%')
              AND (:from::timestamp IS NULL OR p.created_at >= :from::timestamp)
              AND (:to::timestamp IS NULL OR p.created_at <= :to::timestamp)
            """.formatted(RELEVANCE_CASE);

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate postsTransaction;

    public PostsRepository(@Qualifier("postsJdbcTemplate") NamedParameterJdbcTemplate jdbcTemplate,
                           @Qualifier("postsTransactionManager") PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.postsTransaction = new TransactionTemplate(transactionManager);
        this.postsTransaction.setReadOnly(true);
    }

    public List<PostResponse> findPage(PostsFilter filter, int offset, int limit, SortBy sortBy, SortOrder sortOrder) {
        String orderColumn = switch (sortBy) {
            case SCORE   -> "p.score";
            case DATE    -> "p.created_at";
            case ID      -> "p.id";
            case CONTENT -> "p.content";
            case SOURCE  -> "p.source";
        };
        String orderDir = sortOrder == SortOrder.DESC ? "DESC" : "ASC";

        // O desempate por id mantém a ordem estável entre páginas
        String sql = "SELECT * FROM " + union(filter) + "\n" + WHERE
                + "ORDER BY " + orderColumn + " " + orderDir + " NULLS LAST, p.id " + orderDir
                + "\nLIMIT :limit OFFSET :offset";

        MapSqlParameterSource params = params(filter)
                .addValue("limit", limit)
                .addValue("offset", offset);

        return jdbcTemplate.query(sql, params, this::mapPost);
    }

    public long count(PostsFilter filter) {
        String sql = "SELECT COUNT(*) FROM " + union(filter) + "\n" + WHERE;
        Long result = jdbcTemplate.queryForObject(sql, params(filter), Long.class);
        return result != null ? result : 0L;
    }

    /** Estatísticas por fonte, na ordem das fontes do filtro; fontes sem posts vêm zeradas. */
    public List<SourceStats> statsBySource(PostsFilter filter) {
        String sql = """
                SELECT
                    p.source,
                    COUNT(*)                                                    AS total,
                    COUNT(*) FILTER (WHERE p.score >= %2$s)                    AS relevant_count,
                    COUNT(*) FILTER (WHERE p.score < %1$s)                     AS low_count,
                    COUNT(*) FILTER (WHERE p.score >= %1$s AND p.score < %2$s) AS medium_count,
                    COUNT(*) FILTER (WHERE p.score >= %2$s)                    AS high_count
                FROM %3$s
                """.formatted(LOW_THRESHOLD, HIGH_THRESHOLD, union(filter)) + WHERE + "GROUP BY p.source";

        Map<PostSource, SourceStats> bySource = new EnumMap<>(PostSource.class);
        jdbcTemplate.query(sql, params(filter), (RowCallbackHandler) rs -> {
            PostSource source = PostSource.valueOf(rs.getString("source"));
            bySource.put(source, new SourceStats(
                    source,
                    rs.getLong("total"),
                    rs.getLong("relevant_count"),
                    rs.getLong("low_count"),
                    rs.getLong("medium_count"),
                    rs.getLong("high_count")
            ));
        });

        return filter.sources().stream()
                .map(source -> bySource.getOrDefault(source, new SourceStats(source, 0, 0, 0, 0, 0)))
                .toList();
    }

    public List<WordCount> wordFrequencies(PostsFilter filter, int limit) {
        String sql = """
                SELECT word, COUNT(*) AS occurrences
                FROM (
                    SELECT regexp_split_to_table(
                               lower(regexp_replace(p.content, '[^[:alpha:]\\s]', ' ', 'g')),
                               '\\s+'
                           ) AS word
                    FROM %s
                    %s
                ) words
                WHERE length(word) > 2
                  AND word NOT IN (:stopwords)
                GROUP BY word
                ORDER BY occurrences DESC
                LIMIT :limit
                """.formatted(union(filter), WHERE);

        MapSqlParameterSource params = params(filter)
                .addValue("stopwords", List.copyOf(StopWords.ALL))
                .addValue("limit", limit);

        // SET LOCAL só vale dentro de uma transação, por isso a consulta roda numa transação do banco asgard
        return postsTransaction.execute(status -> {
            jdbcTemplate.getJdbcTemplate().execute("SET LOCAL work_mem = '" + WORD_CLOUD_WORK_MEM + "'");
            return jdbcTemplate.query(sql, params, (rs, rowNum) ->
                    new WordCount(rs.getString("word"), rs.getLong("occurrences")));
        });
    }

    /** Momento da última coleta de cada fonte, na ordem do enum; null quando a fonte ainda não tem dados. */
    public Map<PostSource, LocalDateTime> lastCollectedAtBySource() {
        String sql = Arrays.stream(PostSource.values())
                .map(source -> "SELECT '" + source.name() + "' AS source, "
                        + PostSourceQueries.lastCollectedAt(source) + " AS last_collected_at")
                .collect(Collectors.joining("\nUNION ALL\n"));

        Map<PostSource, LocalDateTime> lastCollected = new EnumMap<>(PostSource.class);
        jdbcTemplate.query(sql, (RowCallbackHandler) rs -> lastCollected.put(
                PostSource.valueOf(rs.getString("source")),
                rs.getObject("last_collected_at", LocalDateTime.class)));
        return lastCollected;
    }

    /**
     * Respostas de um post com os comentários de cada uma, em ordem cronológica. Em fontes
     * como o Raddle, os itens são comentários diretos no post (ver {@link ReplyKind}).
     * Vazio quando a fonte (ou o idioma da tabela) não tem respostas.
     */
    public Optional<Replies> findReplies(PostSource source, String tableSuffix, long postId) {
        Optional<PostSourceQueries.ReplyQueries> queries = PostSourceQueries.replies(source, tableSuffix);
        if (queries.isEmpty()) {
            return Optional.empty();
        }
        MapSqlParameterSource params = new MapSqlParameterSource("postId", postId);

        Map<Long, List<PostRepliesResponse.Comment>> commentsByAnswer = new HashMap<>();
        if (queries.get().comments() != null) {
            jdbcTemplate.query(queries.get().comments(), params, (RowCallbackHandler) rs ->
                    commentsByAnswer.computeIfAbsent(rs.getLong("answer_id"), id -> new ArrayList<>())
                            .add(new PostRepliesResponse.Comment(
                                    rs.getString("id"),
                                    rs.getString("author"),
                                    rs.getObject("created_at", LocalDateTime.class),
                                    rs.getString("content"))));
        }

        List<PostRepliesResponse.Answer> answers = jdbcTemplate.query(queries.get().answers(), params,
                (rs, rowNum) ->
                new PostRepliesResponse.Answer(
                        rs.getString("id"),
                        rs.getString("author"),
                        rs.getObject("created_at", LocalDateTime.class),
                        rs.getString("content"),
                        readMeta(rs.getArray("meta")),
                        commentsByAnswer.getOrDefault(rs.getLong("id"), List.of())));
        return Optional.of(new Replies(queries.get().kind(), answers));
    }

    private String union(PostsFilter filter) {
        return filter.sources().stream()
                .map(PostSourceQueries::of)
                .collect(Collectors.joining("\nUNION ALL\n", "(\n", "\n) p"));
    }

    private MapSqlParameterSource params(PostsFilter filter) {
        return new MapSqlParameterSource()
                .addValue("relevance", filter.relevance() != null ? filter.relevance().name() : null)
                .addValue("category", filter.category())
                .addValue("search", filter.search())
                .addValue("from", filter.from())
                .addValue("to", filter.to());
    }

    private PostResponse mapPost(ResultSet rs, int rowNum) throws SQLException {
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

        return new PostResponse(
                rs.getString("id"),
                PostSource.valueOf(rs.getString("source")),
                rs.getString("title"),
                rs.getString("content"),
                rs.getString("author"),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getString("category"),
                clf,
                readMeta(rs.getArray("meta"))
        );
    }

    // O meta vem como [chave1, valor1, chave2, valor2, ...]; valores nulos são descartados
    private Map<String, Object> readMeta(Array array) throws SQLException {
        Map<String, Object> meta = new LinkedHashMap<>();
        if (array == null) {
            return meta;
        }
        String[] pairs = (String[]) array.getArray();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            if (pairs[i + 1] != null) {
                meta.put(pairs[i], pairs[i + 1]);
            }
        }
        return meta;
    }

    private RelevanceLevel resolveLevel(double score) {
        if (score < LOW_THRESHOLD)  return RelevanceLevel.LOW;
        if (score < HIGH_THRESHOLD) return RelevanceLevel.MEDIUM;
        return RelevanceLevel.HIGH;
    }

    public record SourceStats(PostSource source, long totalPosts, long relevantCount,
                              long lowCount, long mediumCount, long highCount) {}

    public record WordCount(String word, long count) {}

    public record Replies(ReplyKind kind, List<PostRepliesResponse.Answer> answers) {}
}
