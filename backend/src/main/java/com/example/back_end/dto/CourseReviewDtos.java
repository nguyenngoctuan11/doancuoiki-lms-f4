package com.example.back_end.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CourseReviewDtos {

    public static class CreateRequest {
        public Integer courseScore;
        public Integer instructorScore;
        public Integer supportScore;
        public Boolean wouldRecommend;
        public String comment;
        public String highlight;
        public String improvement;
    }

    public static class ReviewResponse {
        public Long id;
        public Long courseId;
        public String courseTitle;
        public Long instructorId;
        public String instructorName;
        public Long studentId;
        public String studentName;
        public Integer courseScore;
        public Integer instructorScore;
        public Integer supportScore;
        public Boolean wouldRecommend;
        public String comment;
        public String highlight;
        public String improvement;
        public String status;
        public String adminNote;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;
    }

    public static class SummaryResponse {
        public Long total;
        public Double courseAverage;
        public Double instructorAverage;
        public Double supportAverage;
        public Double recommendRatio;
        public List<Integer> histogram = new ArrayList<>();
    }

    public static class AdminUpdateRequest {
        public String status;
        public String adminNote;
    }
}
