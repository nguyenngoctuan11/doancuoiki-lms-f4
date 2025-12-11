package com.example.back_end.controller;

import com.example.back_end.dto.BlogDtos;
import com.example.back_end.model.BlogPost;
import com.example.back_end.model.User;
import com.example.back_end.repository.BlogPostRepository;
import com.example.back_end.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/blog")
public class BlogController {

    private final BlogPostRepository repository;
    private final UserRepository userRepository;

    public BlogController(BlogPostRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    private User currentUser(Authentication auth) {
        return userRepository.findByEmailIgnoreCase(String.valueOf(auth.getPrincipal()))
                .orElseThrow();
    }

    private BlogPost requireOwned(Long id, Authentication auth) {
        BlogPost post = repository.findWithAuthorById(id)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        String email = String.valueOf(auth.getPrincipal());
        if (!post.getAuthor().getEmail().equalsIgnoreCase(email)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Không có quyền thao tác");
        }
        return post;
    }

    private String slugifyCandidate(String input) {
        if (input == null || input.isBlank()) return "bai-viet";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^\\w\\s-]", " ")
                .trim()
                .replaceAll("\\s+", "-")
                .toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? "bai-viet" : normalized;
    }

    private String ensureSlug(String desired, Long ignoreId) {
        String base = slugifyCandidate(desired);
        String candidate = base;
        int suffix = 1;
        while (true) {
            Optional<BlogPost> existing = repository.findBySlugIgnoreCase(candidate);
            if (existing.isEmpty() || Objects.equals(existing.get().getId(), ignoreId)) {
                return candidate;
            }
            candidate = base + "-" + suffix++;
        }
    }

    private BlogDtos.PostSummary toSummary(BlogPost post) {
        BlogDtos.PostSummary dto = new BlogDtos.PostSummary();
        dto.id = post.getId();
        dto.slug = post.getSlug();
        dto.title = post.getTitle();
        dto.summary = post.getSummary();
        dto.thumbnailUrl = post.getThumbnailUrl();
        dto.status = post.getStatus();
        dto.authorName = post.getAuthor() != null ? post.getAuthor().getFullName() : null;
        dto.publishedAt = post.getPublishedAt();
        dto.createdAt = post.getCreatedAt();
        dto.rejectionReason = post.getRejectionReason();
        return dto;
    }

    private BlogDtos.DetailResponse toDetail(BlogPost post) {
        BlogDtos.DetailResponse dto = new BlogDtos.DetailResponse();
        BlogDtos.PostSummary summary = toSummary(post);
        dto.id = summary.id;
        dto.slug = summary.slug;
        dto.title = summary.title;
        dto.summary = summary.summary;
        dto.thumbnailUrl = summary.thumbnailUrl;
        dto.status = summary.status;
        dto.authorName = summary.authorName;
        dto.publishedAt = summary.publishedAt;
        dto.createdAt = summary.createdAt;
        dto.rejectionReason = summary.rejectionReason;
        dto.content = post.getContent();
        return dto;
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> create(@RequestBody BlogDtos.CreateRequest request, Authentication auth) {
        if (request.title == null || request.title.isBlank()) {
            return ResponseEntity.badRequest().body("Tiêu đề không được bỏ trống");
        }
        if (request.content == null || request.content.isBlank()) {
            return ResponseEntity.badRequest().body("Nội dung không được bỏ trống");
        }
        User author = currentUser(auth);
        BlogPost post = new BlogPost();
        post.setAuthor(author);
        post.setTitle(request.title.trim());
        post.setSummary(request.summary);
        post.setContent(request.content);
        post.setThumbnailUrl(request.thumbnailUrl);
        post.setSlug(ensureSlug(
                request.slug != null && !request.slug.isBlank() ? request.slug : request.title,
                null
        ));
        post.setStatus("draft");
        repository.save(post);
        return ResponseEntity.ok(toSummary(post));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody BlogDtos.UpdateRequest request, Authentication auth) {
        BlogPost post = requireOwned(id, auth);
        if (!post.getStatus().equals("draft") && !post.getStatus().equals("rejected")) {
            return ResponseEntity.badRequest().body("Chỉ bài viết nháp hoặc bị từ chối mới được chỉnh sửa");
        }
        if (request.title != null && !request.title.isBlank()) {
            post.setTitle(request.title.trim());
        }
        if (request.summary != null) {
            post.setSummary(request.summary);
        }
        if (request.content != null) {
            post.setContent(request.content);
        }
        if (request.thumbnailUrl != null) {
            post.setThumbnailUrl(request.thumbnailUrl);
        }
        if (request.slug != null && !request.slug.isBlank()) {
            post.setSlug(ensureSlug(request.slug, post.getId()));
        }
        if (post.getStatus().equals("rejected")) {
            post.setStatus("draft");
            post.setRejectionReason(null);
        }
        repository.save(post);
        return ResponseEntity.ok(toSummary(post));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> submit(@PathVariable Long id, Authentication auth) {
        BlogPost post = requireOwned(id, auth);
        if (post.getStatus().equals("published")) {
            return ResponseEntity.badRequest().body("Bài viết đã được xuất bản");
        }
        post.setStatus("pending");
        post.setSubmittedAt(LocalDateTime.now());
        post.setRejectionReason(null);
        repository.save(post);
        return ResponseEntity.ok(toSummary(post));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<BlogDtos.PostSummary>> myPosts(Authentication auth) {
        User author = currentUser(auth);
        List<BlogPost> posts = repository.findByAuthorIdOrderByCreatedAtDesc(author.getId());
        return ResponseEntity.ok(posts.stream().map(this::toSummary).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<BlogDtos.DetailResponse> detail(@PathVariable Long id, Authentication auth) {
        BlogPost post = requireOwned(id, auth);
        return ResponseEntity.ok(toDetail(post));
    }
}
