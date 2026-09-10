package com.backend.threatlens.controller;

import com.backend.threatlens.dto.request.PostsQueryDTO;
import com.backend.threatlens.dto.request.StatsQueryDTO;
import com.backend.threatlens.dto.request.WordCloudQueryDTO;
import com.backend.threatlens.dto.response.PostStatsDTO;
import com.backend.threatlens.dto.response.WordCloudDTO;
import com.backend.threatlens.dto.response.posts.PostsPageResponse;
import com.backend.threatlens.service.PostsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<PostsPageResponse> getPosts(@Valid @ModelAttribute PostsQueryDTO query) {
        return ResponseEntity.ok(postsService.getPosts(query));
    }

    @GetMapping("/stats")
    public ResponseEntity<PostStatsDTO> getStats(@Valid @ModelAttribute StatsQueryDTO query) {
        return ResponseEntity.ok(postsService.getStats(query));
    }

    @GetMapping("/wordcloud")
    public WordCloudDTO getWordCloud(@Valid @ModelAttribute WordCloudQueryDTO query) {
        return postsService.getWordCloud(query);
    }

}
