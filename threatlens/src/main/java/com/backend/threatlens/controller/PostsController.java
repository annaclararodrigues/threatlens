package com.backend.threatlens.controller;

import com.backend.threatlens.dto.response.PostResponse;
import com.backend.threatlens.repository.posts.TelegramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostsController {

    private final TelegramRepository telegramRepository;

    @GetMapping("/telegram")
    public List<PostResponse> getTelegramPosts() {
        return telegramRepository.findAll(null, null, 15, 0);
    }
}
