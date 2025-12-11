package com.example.back_end.dto;

import java.math.BigDecimal;
import java.util.List;

public class CourseDtos {
    public static class CreateRequest {
        public String title;
        public String slug;
        public String shortDesc;
        public String language; // vi,en
        public String level;    // beginner,intermediate,advanced
        public String status;   // draft,published,archived
        public BigDecimal price;
        public Boolean isFree;
        public String thumbnailUrl; // optional direct set
        public java.util.List<String> targetRoles;
        public java.util.List<String> categories;
    }

    public static class UpdateRequest extends CreateRequest { }

    public static class CourseResponse {
        public Long id;
        public String title;
        public String slug;
        public String shortDesc;
        public String language;
        public String level;
        public String status;
        public BigDecimal price;
        public Boolean isFree;
        public Long createdById;
        public String createdByEmail;
        public String thumbnailUrl;
        public String approvalStatus;
        public java.util.List<String> targetRoles;
        public java.util.List<String> categories;
    }
}
