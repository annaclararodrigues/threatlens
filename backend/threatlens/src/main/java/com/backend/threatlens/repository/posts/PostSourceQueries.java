package com.backend.threatlens.repository.posts;

import com.backend.threatlens.enums.PostSource;
import com.backend.threatlens.enums.ReplyKind;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * SELECT de cada fonte do banco asgard. Todos devolvem as mesmas colunas, na mesma ordem,
 * para que o {@link PostsRepository} possa combiná-los com UNION ALL:
 * source, id, title, content, author, created_at, category,
 * relevant, score, range, ioc, classified_at e meta (text[] com pares chave/valor).
 */
final class PostSourceQueries {

    private record MetaField(String key, String expression) {}

    private record Language(String tableSuffix, String label) {}

    private static final List<Language> HA_LANGUAGES =
            List.of(new Language("english", "en"), new Language("ptbr", "pt-BR"));

    private static final List<Language> DEEP_LANGUAGES = List.of(
            new Language("enus", "en-US"), new Language("eses", "es-ES"), new Language("ptbr", "pt-BR"));

    /**
     * Onde estão as respostas de uma fonte. Com idiomas, as tabelas levam o sufixo do idioma
     * (ha_answer_ptbr); sem idiomas, o nome é fixo. commentTable é null quando a fonte não
     * tem comentários nas respostas. As colunas já vêm prontas para o SQL (com aspas se preciso).
     */
    private record ReplySource(ReplyKind kind, List<Language> languages, String answerTable, String parentColumn,
                               String authorColumn, String contentColumn, List<MetaField> answerMeta,
                               String commentTable) {

        static ReplySource questions(String prefix, List<Language> languages, List<MetaField> answerMeta) {
            return new ReplySource(ReplyKind.ANSWER, languages, prefix + "_answer", "question_id", "author",
                    "answer_content", answerMeta, prefix + "_comment");
        }

        static ReplySource forum(String prefix, String parentColumn) {
            return new ReplySource(ReplyKind.ANSWER, List.of(), prefix + "_answer", parentColumn, "author",
                    "answer_body", List.of(), null);
        }
    }

    private static final Map<PostSource, ReplySource> REPLY_SOURCES = Map.of(
            PostSource.HA, ReplySource.questions("ha", HA_LANGUAGES, List.of(
                    new MetaField("up_votes", "a.up_vote"),
                    new MetaField("down_votes", "a.down_vote"),
                    new MetaField("points", "a.points"))),
            PostSource.DEEP, ReplySource.questions("deep", DEEP_LANGUAGES, List.of(
                    new MetaField("votes", "a.votes"),
                    new MetaField("points", "a.points"))),
            PostSource.NULLEDBB, ReplySource.forum("nulledbb", "post_id"),
            PostSource.NIFLHEIM, ReplySource.forum("niflheim", "post_id"),
            PostSource.HACKONOLOGY, ReplySource.forum("hackonology", "post_id"),
            PostSource.LEGITCARDERS, ReplySource.forum("legitcarders", "card_id"),
            // Raddle só tem comentários, feitos direto no post; "user" é palavra reservada
            PostSource.RADDLE, new ReplySource(ReplyKind.COMMENT, List.of(), "raddle_commentary", "post_id",
                    "\"user\"", "commentary", List.of(new MetaField("votes", "a.votes")), null));

    private static final Map<PostSource, String> QUERIES = buildQueries();

    private static final Map<PostSource, String> LAST_COLLECTED = buildLastCollected();

    private PostSourceQueries() {}

    static String of(PostSource source) {
        return QUERIES.get(source);
    }

    /**
     * Respostas de um post (colunas id, author, created_at, content e meta) e os comentários
     * dessas respostas (id, answer_id, author, created_at e content), filtrados por :postId.
     * comments é null quando a fonte não tem comentários. kind diz se os itens de primeiro nível
     * são respostas ou comentários diretos no post.
     */
    record ReplyQueries(ReplyKind kind, String answers, String comments) {}

    /**
     * Consultas de respostas e comentários da fonte, no idioma do post. Vazio quando a fonte
     * não tem respostas ou o idioma não é dela; o sufixo vira nome de tabela, então só passa
     * se for um dos idiomas conhecidos (e fontes sem idiomas não aceitam sufixo).
     */
    static Optional<ReplyQueries> replies(PostSource source, String tableSuffix) {
        ReplySource replySource = REPLY_SOURCES.get(source);
        if (replySource == null || !acceptsSuffix(replySource, tableSuffix)) {
            return Optional.empty();
        }
        String suffix = tableSuffix != null ? "_" + tableSuffix : "";
        String answerTable = replySource.answerTable() + suffix;

        String answers = """
                SELECT a.id,
                       a.%s::text AS author,
                       a.created_at,
                       a.%s::text AS content,
                       %s AS meta
                FROM %s a
                WHERE a.%s = :postId
                ORDER BY a.created_at NULLS LAST, a.id""".formatted(replySource.authorColumn(),
                replySource.contentColumn(), metaArray(replySource.answerMeta()), answerTable,
                replySource.parentColumn());

        String comments = replySource.commentTable() == null ? null : """
                SELECT c.id,
                       c.answer_id,
                       c.author::text AS author,
                       c.created_at,
                       c.commentary::text AS content
                FROM %s c
                JOIN %s a ON a.id = c.answer_id
                WHERE a.%s = :postId
                ORDER BY c.created_at NULLS LAST, c.id""".formatted(replySource.commentTable() + suffix,
                answerTable, replySource.parentColumn());

        return Optional.of(new ReplyQueries(replySource.kind(), answers, comments));
    }

    private static boolean acceptsSuffix(ReplySource replySource, String tableSuffix) {
        if (replySource.languages().isEmpty()) {
            return tableSuffix == null;
        }
        return replySource.languages().stream().anyMatch(language -> language.tableSuffix().equals(tableSuffix));
    }

    /** Expressão escalar com o momento da última coleta da fonte (NULL se ainda não há dados). */
    static String lastCollectedAt(PostSource source) {
        return LAST_COLLECTED.get(source);
    }

    // No Telegram, "createdAt" é a gravação da linha pelo coletor. As demais fontes só guardam a data
    // de publicação original, então usamos a classificação mais recente, feita logo após a coleta.
    private static Map<PostSource, String> buildLastCollected() {
        Map<PostSource, String> queries = new EnumMap<>(PostSource.class);
        queries.put(PostSource.TELEGRAM, "(SELECT MAX(m.\"createdAt\") FROM telegram_message m)");
        for (PostSource source : List.of(PostSource.NULLEDBB, PostSource.NIFLHEIM, PostSource.HACKONOLOGY,
                PostSource.LEGITCARDERS, PostSource.RADDLE)) {
            queries.put(source, maxClassifiedAt(prefix(source) + "_classified_posts"));
        }
        queries.put(PostSource.HA, greatestClassifiedAt(PostSource.HA, List.of("english", "ptbr")));
        queries.put(PostSource.DEEP, greatestClassifiedAt(PostSource.DEEP, List.of("enus", "eses", "ptbr")));
        return queries;
    }

    private static String maxClassifiedAt(String table) {
        return "(SELECT MAX(classified_at) FROM " + table + ")";
    }

    // GREATEST ignora NULLs, então idiomas sem dados não atrapalham
    private static String greatestClassifiedAt(PostSource source, List<String> tableSuffixes) {
        return tableSuffixes.stream()
                .map(suffix -> maxClassifiedAt(prefix(source) + "_classified_posts_" + suffix))
                .collect(Collectors.joining(", ", "GREATEST(", ")"));
    }

    private static Map<PostSource, String> buildQueries() {
        Map<PostSource, String> queries = new EnumMap<>(PostSource.class);
        queries.put(PostSource.TELEGRAM, telegram());

        queries.put(PostSource.NULLEDBB, forum(PostSource.NULLEDBB, "nulledbb_post", "body", "p.category", List.of()));
        queries.put(PostSource.NIFLHEIM, forum(PostSource.NIFLHEIM, "niflheim_post", "body", "p.category", List.of()));
        queries.put(PostSource.HACKONOLOGY, forum(PostSource.HACKONOLOGY, "hackonology_post", "body", "p.category", List.of()));
        queries.put(PostSource.LEGITCARDERS, forum(PostSource.LEGITCARDERS, "legitcarders_card", "body", "p.category", List.of()));
        // raddle_post não tem coluna category; ela só existe na tabela de classificação
        queries.put(PostSource.RADDLE, forum(PostSource.RADDLE, "raddle_post", "post", "c.category", List.of(
                new MetaField("forum", "p.forum"),
                new MetaField("votes", "p.votes"),
                new MetaField("link", "p.link")
        )));

        queries.put(PostSource.HA, questions(PostSource.HA, HA_LANGUAGES,
                List.of(new MetaField("up_votes", "q.up_vote"), new MetaField("down_votes", "q.down_vote"))));
        queries.put(PostSource.DEEP, questions(PostSource.DEEP, DEEP_LANGUAGES,
                List.of(new MetaField("votes", "q.votes"))));
        return queries;
    }

    private static String telegram() {
        return select(PostSource.TELEGRAM, "m.id", "NULL", "m.message", "NULL", "m.\"createdAt\"", "NULL",
                List.of(
                        new MetaField("telegram_channel_id", "m.\"telegramChannelId\""),
                        new MetaField("channel_name", "ch.name"),
                        new MetaField("channel_username", "ch.username")
                ),
                """
                telegram_message m
                LEFT JOIN telegram_channel ch ON ch.id = m."telegramChannelId"
                LEFT JOIN telegram_classified_messages c ON c.id_post = m.id""");
    }

    // Fóruns: {prefixo}_post (ou equivalente) + {prefixo}_classified_posts
    private static String forum(PostSource source, String postTable, String bodyColumn, String category,
                                List<MetaField> meta) {
        String prefix = prefix(source);
        return select(source, "p.id", "p.title", "p." + bodyColumn, "p.author", "p.created_at", category, meta,
                postTable + " p\nLEFT JOIN " + prefix + "_classified_posts c ON c.id_post = p.id");
    }

    // Perguntas e respostas: uma tabela de perguntas e uma de classificação por idioma
    private static String questions(PostSource source, List<Language> languages, List<MetaField> votes) {
        String prefix = prefix(source);
        return languages.stream()
                .map(language -> {
                    List<MetaField> meta = new ArrayList<>();
                    meta.add(new MetaField("language", "'" + language.label() + "'"));
                    meta.add(new MetaField("tags", "array_to_string(q.tags, ', ')"));
                    meta.addAll(votes);
                    meta.add(new MetaField("points", "q.points"));
                    return select(source, "'" + language.tableSuffix() + ":' || q.id",
                            "q.title", "q.question", "q.author", "q.created_at", "q.category", meta,
                            prefix + "_question_" + language.tableSuffix() + " q\n"
                                    + "LEFT JOIN " + prefix + "_classified_posts_" + language.tableSuffix()
                                    + " c ON c.id_post = q.id");
                })
                .collect(Collectors.joining("\nUNION ALL\n"));
    }

    // O id é prefixado com a fonte para ser único entre fontes (ex.: "nulledbb:42")
    private static String select(PostSource source, String id, String title, String content, String author,
                                 String createdAt, String category, List<MetaField> meta, String from) {
        return """
                SELECT '%s' AS source,
                       '%s:' || %s AS id,
                       %s::text AS title,
                       %s::text AS content,
                       %s::text AS author,
                       %s AS created_at,
                       %s::text AS category,
                       c.relevant,
                       c.probability AS score,
                       c.range,
                       c.ioc,
                       c.classified_at,
                       %s AS meta
                FROM %s""".formatted(source.name(), prefix(source), id, title, content, author,
                createdAt, category, metaArray(meta), from);
    }

    private static String metaArray(List<MetaField> meta) {
        if (meta.isEmpty()) {
            return "ARRAY[]::text[]";
        }
        return meta.stream()
                .map(field -> "'" + field.key() + "', (" + field.expression() + ")::text")
                .collect(Collectors.joining(", ", "ARRAY[", "]"));
    }

    private static String prefix(PostSource source) {
        return source.name().toLowerCase();
    }
}
