package com.example.back_end.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LessonNoteDtos {
    public static class NoteResponse {
        public Long id;
        public String content;
        public SimpleUser author;
        public Long courseId;
        public Long lessonId;
        public String courseTitle;
        public String lessonTitle;
        public String status;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;
        public LocalDateTime lastCommentAt;
        public boolean mine;
        public int commentCount;
        public List<CommentResponse> comments = new ArrayList<>();
    }

    public static class CommentResponse {
        public Long id;
        public String content;
        public SimpleUser author;
        public LocalDateTime createdAt;
    }

    public static class SimpleUser {
        public Long id;
        public String name;
        public String avatar;
        public String role;
    }

    public static class CreateRequest {
        public String content;
    }

    public static class CommentRequest {
        public String content;
    }
}
