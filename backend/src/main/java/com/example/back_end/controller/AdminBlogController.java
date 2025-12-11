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

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/blog")
@PreAuthorize("hasRole('MANAGER')")
public class AdminBlogController {

    private final BlogPostRepository repository;
    private final UserRepository userRepository;

    public AdminBlogController(BlogPostRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    private BlogPost getPost(Long id) {
        return repository.findWithAuthorById(id)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
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

    @GetMapping("/pending")
    public ResponseEntity<List<BlogDtos.PostSummary>> pending() {
        List<BlogPost> pending = repository.findPending();
        return ResponseEntity.ok(pending.stream().map(this::toSummary).toList());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id, Authentication auth) {
        BlogPost post = getPost(id);
        if (!"pending".equals(post.getStatus())) {
            return ResponseEntity.badRequest().body("Bài viết không ở trạng thái chờ duyệt");
        }
        User approver = userRepository.findByEmailIgnoreCase(String.valueOf(auth.getPrincipal()))
                .orElseThrow();
        post.setStatus("published");
        LocalDateTime now = LocalDateTime.now();
        post.setApprovedAt(now);
        post.setPublishedAt(now);
        post.setApprover(approver);
        post.setRejectionReason(null);
        repository.save(post);
        return ResponseEntity.ok(toSummary(post));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id, @RequestBody(required = false) BlogDtos.RejectRequest request) {
        BlogPost post = getPost(id);
        if (!"pending".equals(post.getStatus())) {
            return ResponseEntity.badRequest().body("Bài viết không ở trạng thái chờ duyệt");
        }
        post.setStatus("rejected");
        post.setRejectionReason(request != null ? request.reason : null);
        repository.save(post);
        return ResponseEntity.ok(toSummary(post));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BlogDtos.DetailResponse> getDetail(@PathVariable Long id) {
        BlogPost post = getPost(id);
        BlogDtos.DetailResponse dto = new BlogDtos.DetailResponse();
        dto.id = post.getId();
        dto.slug = post.getSlug();
        dto.title = post.getTitle();
        dto.summary = post.getSummary();
        dto.content = post.getContent();
        dto.status = post.getStatus();
        dto.authorName = post.getAuthor() != null ? post.getAuthor().getFullName() : null;
        dto.publishedAt = post.getPublishedAt();
        dto.createdAt = post.getCreatedAt();
        dto.rejectionReason = post.getRejectionReason();
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        BlogPost post = getPost(id);
        repository.delete(post);
        return ResponseEntity.noContent().build();
    }
}
