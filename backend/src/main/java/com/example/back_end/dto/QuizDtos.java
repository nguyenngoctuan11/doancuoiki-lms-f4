package com.example.back_end.dto;

import java.util.List;

public class QuizDtos {
    public static class CreateQuizRequest { public Long courseId; public String title; public Integer timeLimitSec; public Boolean shuffle; }
    public static class UpdateQuizRequest { public String title; public Integer timeLimitSec; public Boolean shuffle; }
    public static class QuizDetail {
        public Long id; public Long courseId; public String title; public Integer timeLimitSec; public Boolean shuffle;
        public Integer questionCount; public List<QuestionDetail> questions;
    }
    public static class QuestionDetail {
        public Long id; public String text; public Integer points; public Integer sortOrder; public List<QuestionOptionDetail> options;
    }
    public static class QuestionOptionDetail {
        public Long id; public String text; public Boolean correct;
    }
    public static class AddQuestionRequest {
        public String text; public Integer points; public List<Option> options; // up to 4
        public static class Option { public String text; public Boolean correct; }
    }
}

