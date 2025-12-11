package com.example.back_end.dto;

import java.time.LocalDateTime;

public class LessonResourceDtos {

    public static class CreateRequest {
        public String title;
        public String description;
        public String sourceType; // file | link
        public String storagePath;
        public String fileUrl;
        public String externalUrl;
        public String fileType;
        public Long fileSize;
        public String visibility;
    }

    public static class UpdateRequest {
        public String title;
        public String description;
        public String visibility;
        public String status;
        public String sourceType;
        public String storagePath;
        public String fileUrl;
        public String externalUrl;
        public String fileType;
        public Long fileSize;
    }

    public static class ResourceResponse {
        public Long id;
        public Long courseId;
        public Long lessonId;
        public String courseTitle;
        public String lessonTitle;
        public String title;
        public String description;
        public String sourceType;
        public String fileUrl;
        public String externalUrl;
        public Long fileSize;
        public String fileType;
        public String visibility;
        public String status;
        public Integer downloadCount;
        public SimpleUser createdBy;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;
        public String downloadUrl;
    }

    public static class SimpleUser {
        public Long id;
        public String name;
        public String avatar;
    }

    public static class DownloadPayload {
        public boolean redirect;
        public String url;
    }
}
