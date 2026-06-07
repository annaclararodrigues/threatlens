package com.backend.threatlens.repository.posts;

import com.backend.threatlens.dto.response.ClassificationResponse;
import com.backend.threatlens.dto.response.PostResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class TelegramRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TelegramRepository(@Qualifier("postsJdbcTemplate") NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PostResponse> findAll(String relevance, String category, int limit, int offset) {
        String sql = """
                SELECT
                    m.id,
                    m.message                AS content,
                    m."createdAt"            AS created_at,
                    m."telegramChannelId"    AS telegram_channel_id,
                    c.relevant,
                    c.content                AS category,
                    c.probability            AS score,
                    c.range,
                    c.ioc,
                    c.classified_at
                FROM telegram_message m
                LEFT JOIN telegram_classified_messages c ON c.id_post = m.id
                WHERE (:relevance::text IS NULL OR c.range   = :relevance)
                  AND (:category::text  IS NULL OR c.content = :category)
                ORDER BY m."createdAt" DESC
                LIMIT :limit OFFSET :offset
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("relevance", relevance);
        params.put("category", category);
        params.put("limit", limit);
        params.put("offset", offset);

        return jdbcTemplate.query(
                sql,
                params,
                (rs, rowNum) -> {
                    double score = rs.getDouble("score");
                    boolean scoreNull = rs.wasNull();

                    ClassificationResponse clf = scoreNull ? null : new ClassificationResponse(
                            rs.getBoolean("relevant"),
                            score,
                            resolveLevel(score),
                            rs.getString("range"),
                            rs.getString("ioc"),
                            rs.getString("classified_at")
                    );

                    return new PostResponse(
                            rs.getString("id"),
                            "telegram",
                            null,
                            rs.getString("content"),
                            null,
                            rs.getString("created_at"),
                            rs.getString("category"),
                            clf,
                            Map.of("telegram_channel_id", rs.getLong("telegram_channel_id"))
                    );
                }
        );
    }

    private String resolveLevel(double score) {
        if (score < 0.3) return "baixa";
        if (score < 0.7) return "media";
        return "alta";
    }
}