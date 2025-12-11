package com.example.back_end.dto.public_;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// DTO đặt tên theo snake_case để khớp FE và T-SQL
public class PublicCourseDetailDto {
    public Long id;
    public String title;
    public String slug;
    public String level;
    public String status;
    public BigDecimal price;
    public Boolean is_free;
    public String thumbnail_url;
    public String created_by_email;
    public String created_by_name;
    public List<ModuleItem> modules = new ArrayList<>();

    public static class ModuleItem {
        public Long id;
        public String title;
        public Integer sort_order;
        public List<LessonItem> lessons = new ArrayList<>();
    }

    public static class LessonItem {
        public Long id;
        public String title;
        public String type;
        public Integer duration_seconds;
        public Integer sort_order;
        public String status;
        public String video_url; // preview/nguồn chính nếu có
    }
}
