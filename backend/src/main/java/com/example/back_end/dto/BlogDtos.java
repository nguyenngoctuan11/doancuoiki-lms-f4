package com.example.back_end.dto;

import java.time.LocalDateTime;
import java.util.List;

public class BlogDtos {
    public static class CreateRequest {
        public String title;
        public String slug;
        public String summary;
        public String content;
        public String thumbnailUrl;
    }

    public static class UpdateRequest {
        public String title;
        public String summary;
        public String content;
        public String thumbnailUrl;
        public String slug;
    }

    public static class RejectRequest {
        public String reason;
    }

    public static class PostSummary {
        public Long id;
        public String slug;
        public String title;
        public String summary;
        public String thumbnailUrl;
        public String status;
        public String authorName;
        public LocalDateTime publishedAt;
        public LocalDateTime createdAt;
        public String rejectionReason;
    }

    public static class DetailResponse extends PostSummary {
        public String content;
    }

    public static class PageResponse {
        public List<PostSummary> items;
    }
}
