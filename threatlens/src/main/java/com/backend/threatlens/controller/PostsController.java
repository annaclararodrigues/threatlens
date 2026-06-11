package com.backend.threatlens.controller;

import com.backend.threatlens.dto.request.PostsQueryDTO;
import com.backend.threatlens.dto.request.StatsQueryDTO;
import com.backend.threatlens.dto.response.PostStatsDTO;
import com.backend.threatlens.dto.response.posts.PostsPageResponse;
import com.backend.threatlens.service.PostsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostsController {

    private final PostsService postsService;

    @GetMapping
    public PostsPageResponse getPosts(@ModelAttribute PostsQueryDTO query) {
        return postsService.getPosts(query);
    }

    @GetMapping("/stats")
    public PostStatsDTO getStats(@ModelAttribute StatsQueryDTO query) {
        return postsService.getStats(query);
    }

}
