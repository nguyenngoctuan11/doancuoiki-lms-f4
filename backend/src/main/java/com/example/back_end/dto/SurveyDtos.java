package com.example.back_end.dto;

import java.util.List;

public class SurveyDtos {
    public static class Question {
        public Long id; public String code; public String text; public List<Option> options;
    }
    public static class Option {
        public Long id; public String code; public String text;
    }
    public static class SubmitRequest {
        public List<String> selectedCodes; // e.g., ["area_backend","level_beginner"]
    }
    public static class PathResponse {
        public Long pathId; public String name; public List<CourseItem> items;
    }
    public static class CourseItem {
        public Long courseId; public String title; public String slug; public String level; public String thumbnailUrl;
    }
}

