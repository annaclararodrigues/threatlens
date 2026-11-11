package com.backend.threatlens.repository.posts;

import com.backend.threatlens.enums.PostSource;
import com.backend.threatlens.enums.ReplyKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class PostSourceQueriesTest {

    @ParameterizedTest
    @EnumSource(PostSource.class)
    void everySourceHasAQueryWithTheStandardColumns(PostSource source) {
        String sql = PostSourceQueries.of(source);

        assertThat(sql).isNotBlank();
        assertThat(sql).contains("'" + source.name() + "' AS source", "AS id", "AS title", "AS content",
                "AS author", "AS created_at", "AS category", "c.relevant", "AS score", "c.range", "c.ioc",
                "c.classified_at", "AS meta");
    }

    @ParameterizedTest
    @EnumSource(PostSource.class)
    void idIsPrefixedWithTheSourceName(PostSource source) {
        assertThat(PostSourceQueries.of(source)).contains("'" + source.name().toLowerCase() + ":' ||");
    }

    @Test
    void forumUsesItsPostAndClassificationTables() {
        assertThat(PostSourceQueries.of(PostSource.LEGITCARDERS))
                .contains("FROM legitcarders_card p", "LEFT JOIN legitcarders_classified_posts c ON c.id_post = p.id");
    }

    @Test
    void raddleReadsTheBodyFromThePostColumn() {
        assertThat(PostSourceQueries.of(PostSource.RADDLE)).contains("p.post::text AS content");
    }

    @Test
    void raddleTakesTheCategoryFromTheClassificationTable() {
        assertThat(PostSourceQueries.of(PostSource.RADDLE))
                .contains("c.category::text AS category")
                .doesNotContain("p.category");
    }

    @Test
    void questionSourcesUnionOneSelectPerLanguage() {
        String deep = PostSourceQueries.of(PostSource.DEEP);

        assertThat(deep).contains("deep_question_enus", "deep_question_eses", "deep_question_ptbr",
                "deep_classified_posts_enus", "deep_classified_posts_eses", "deep_classified_posts_ptbr");
        assertThat(deep.split("UNION ALL")).hasSize(3);
        assertThat(PostSourceQueries.of(PostSource.HA).split("UNION ALL")).hasSize(2);
    }

    @Test
    void telegramHasNoCategory() {
        assertThat(PostSourceQueries.of(PostSource.TELEGRAM)).contains("NULL::text AS category");
    }

    @Test
    void haRepliesReadTheAnswerAndCommentTablesOfTheLanguage() {
        PostSourceQueries.ReplyQueries queries = PostSourceQueries.replies(PostSource.HA, "ptbr").orElseThrow();

        assertThat(queries.answers()).contains("FROM ha_answer_ptbr a", "a.question_id = :postId", "AS meta");
        assertThat(queries.comments()).contains("FROM ha_comment_ptbr c",
                "JOIN ha_answer_ptbr a ON a.id = c.answer_id", "a.question_id = :postId");
    }

    @Test
    void deepRepliesUseItsLanguagesAndSingleVoteColumn() {
        PostSourceQueries.ReplyQueries queries = PostSourceQueries.replies(PostSource.DEEP, "enus").orElseThrow();

        assertThat(queries.answers()).contains("FROM deep_answer_enus a", "'votes', (a.votes)::text")
                .doesNotContain("up_vote");
        assertThat(queries.comments()).contains("FROM deep_comment_enus c");
        assertThat(PostSourceQueries.replies(PostSource.DEEP, "english")).isEmpty();
    }

    @Test
    void repliesRejectAnUnknownLanguage() {
        assertThat(PostSourceQueries.replies(PostSource.HA, "enus")).isEmpty();
        assertThat(PostSourceQueries.replies(PostSource.HA, "english; DROP TABLE x")).isEmpty();
        assertThat(PostSourceQueries.replies(PostSource.HA, null)).isEmpty();
    }

    @Test
    void telegramHasNoReplies() {
        assertThat(PostSourceQueries.replies(PostSource.TELEGRAM, null)).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = PostSource.class, names = "TELEGRAM", mode = EnumSource.Mode.EXCLUDE)
    void everyOtherSourceHasReplies(PostSource source) {
        String suffix = switch (source) {
            case HA -> "english";
            case DEEP -> "enus";
            default -> null;
        };
        assertThat(PostSourceQueries.replies(source, suffix)).isPresent();
    }

    @Test
    void legitcardersLinksAnswersByCardId() {
        assertThat(PostSourceQueries.replies(PostSource.LEGITCARDERS, null).orElseThrow().answers())
                .contains("FROM legitcarders_answer a", "a.card_id = :postId");
    }

    @Test
    void raddleRepliesAreCommentsOnThePost() {
        PostSourceQueries.ReplyQueries queries = PostSourceQueries.replies(PostSource.RADDLE, null).orElseThrow();

        assertThat(queries.kind()).isEqualTo(ReplyKind.COMMENT);
        assertThat(queries.answers()).contains("FROM raddle_commentary a", "a.\"user\"::text AS author",
                "a.commentary::text AS content", "'votes', (a.votes)::text");
        assertThat(queries.comments()).isNull();
    }

    @Test
    void forumRepliesReadTheAnswerTableWithoutComments() {
        PostSourceQueries.ReplyQueries queries = PostSourceQueries.replies(PostSource.NULLEDBB, null).orElseThrow();

        assertThat(queries.answers()).contains("FROM nulledbb_answer a", "a.post_id = :postId",
                "a.answer_body::text AS content", "ARRAY[]::text[] AS meta");
        assertThat(queries.comments()).isNull();
    }

    @Test
    void forumRepliesRejectALanguageSuffix() {
        assertThat(PostSourceQueries.replies(PostSource.NULLEDBB, "ptbr")).isEmpty();
    }
}
