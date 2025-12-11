package com.example.back_end.dto;

import java.util.List;

public class ModuleLessonDtos {
    public static class ModuleRequest { public String title; public Integer sortOrder; }
    public static class LessonRequest {
        public String title; public String type; public Integer durationSeconds; public Integer sortOrder; public String status;
    }
    public static class ModuleResponse { public Long id; public String title; public Integer sortOrder; public List<LessonResponse> lessons; }
    public static class LessonResponse { public Long id; public String title; public String type; public Integer durationSeconds; public Integer sortOrder; public String status; }
}

