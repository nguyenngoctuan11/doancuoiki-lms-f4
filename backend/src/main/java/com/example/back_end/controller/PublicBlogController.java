package com.example.back_end.controller;

import com.example.back_end.dto.BlogDtos;
import com.example.back_end.model.BlogPost;
import com.example.back_end.repository.BlogPostRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/blog")
public class PublicBlogController {

    private final BlogPostRepository repository;

    public PublicBlogController(BlogPostRepository repository) {
        this.repository = repository;
    }

    private BlogDtos.PostSummary toSummary(BlogPost post) {
        BlogDtos.PostSummary dto = new BlogDtos.PostSummary();
        dto.id = post.getId();
        dto.slug = post.getSlug();
        dto.title = post.getTitle();
        dto.summary = post.getSummary();
        dto.thumbnailUrl = post.getThumbnailUrl();
        dto.authorName = post.getAuthor() != null ? post.getAuthor().getFullName() : null;
        dto.publishedAt = post.getPublishedAt();
        return dto;
    }

    @GetMapping
    public ResponseEntity<List<BlogDtos.PostSummary>> list(@RequestParam(value = "limit", required = false) Integer limit) {
        List<BlogPost> posts = repository.findByStatusOrderByPublishedAtDesc("published");
        if (limit != null && limit > 0 && posts.size() > limit) {
            posts = posts.subList(0, limit);
        }
        return ResponseEntity.ok(posts.stream().map(this::toSummary).toList());
    }

    @GetMapping("/{slug}")
    public ResponseEntity<?> detail(@PathVariable String slug) {
        BlogPost post = repository.findBySlugIgnoreCase(slug)
                .filter(p -> "published".equals(p.getStatus()))
                .orElse(null);
        if (post == null) {
            return ResponseEntity.notFound().build();
        }
        BlogDtos.DetailResponse dto = new BlogDtos.DetailResponse();
        dto.id = post.getId();
        dto.slug = post.getSlug();
        dto.title = post.getTitle();
        dto.summary = post.getSummary();
        dto.content = post.getContent();
        dto.thumbnailUrl = post.getThumbnailUrl();
        dto.authorName = post.getAuthor() != null ? post.getAuthor().getFullName() : null;
        dto.publishedAt = post.getPublishedAt();
        return ResponseEntity.ok(dto);
    }
}
