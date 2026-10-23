package com.backend.threatlens.dto.response.posts;

import java.util.List;

public record PostsPageResponse(
        List<PostResponse> data,
        Pagination pagination
) {
    // total e totalPages são nulos quando a requisição pede includeTotal=false
    public record Pagination(int page, int size, Long total, Integer totalPages) {}
}
