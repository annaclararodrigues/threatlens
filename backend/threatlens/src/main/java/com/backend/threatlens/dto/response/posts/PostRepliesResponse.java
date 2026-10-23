package com.backend.threatlens.dto.response.posts;

import com.backend.threatlens.enums.ReplyKind;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Respostas de um post, cada uma com os comentários feitos nela; com kind = COMMENT (Raddle),
// os itens de answers são comentários diretos no post e não têm comentários próprios
public record PostRepliesResponse(String postId, ReplyKind kind, List<Answer> answers) {

    public record Answer(String id,
                         String author,
                         @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
                         LocalDateTime createdAt,
                         String content,
                         Map<String, Object> meta,
                         List<Comment> comments) {}

    public record Comment(String id,
                          String author,
                          @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
                          LocalDateTime createdAt,
                          String content) {}
}
