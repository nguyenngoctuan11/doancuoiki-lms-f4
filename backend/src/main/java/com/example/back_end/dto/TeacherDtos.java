package com.example.back_end.dto;

public class TeacherDtos {
    public static class TeacherHighlight {
        public Long id;
        public String fullName;
        public String avatarUrl;
        public String bio;
        public long courseCount;
        public long lessonCount;
    }

    public static class CourseSummary {
        public Long id;
        public String title;
        public String slug;
        public String level;
        public String thumbnailUrl;
        public String status;
    }

    public static class TeacherProfile extends TeacherHighlight {
        public String email;
        public String username;
        public String expertise;
        public java.util.List<CourseSummary> courses;
    }
}
