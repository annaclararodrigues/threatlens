package com.backend.threatlens.dto.response.posts;

import java.util.List;

public record PostsPageResponse(
        List<PostResponse> data,
        Pagination pagination
) {
    public record Pagination(int page, int size, long total, int totalPages) {}
}
