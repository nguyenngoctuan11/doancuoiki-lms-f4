package com.example.back_end.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ExamDtos {
    public static class ExamOverview {
        public Long examId;
        public Long courseId;
        public String courseTitle;
        public String courseSlug;
        public String title;
        public Integer timeLimitSec;
        public Integer questionCount;
        public Integer maxAttempts;
        public Integer attemptsUsed;
        public Integer attemptsRemaining;
        public Double passingScore;
        public String instructions;
        public String reviewPolicy;
        public LocalDateTime windowStart;
        public LocalDateTime windowEnd;
        public Integer autoSubmitGraceSec;
        public Integer retakeCooldownMinutes;
        public boolean enrolled;
        public boolean canAttempt;
        public List<String> blockers;
        public List<PrerequisiteStatus> prerequisites;
        public Long activeAttemptId;
        public LocalDateTime lastAttemptFinishedAt;
    }

    public static class ExamSummary {
        public Long id;
        public Long courseId;
        public String title;
        public Integer timeLimitSec;
        public Integer questionCount;
        public Integer maxAttempts;
        public Double passingScore;
    }

    public static class PrerequisiteStatus {
        public Long courseId;
        public String courseTitle;
        public boolean met;
    }

    public static class StartAttemptRequest {
        public Long resumeAttemptId;
    }

    public static class AttemptQuestion {
        public Long id;
        public String text;
        public Integer points;
        public List<AttemptOption> options;
        public Long selectedOptionId;
        public Boolean markedForReview;
        public Long correctOptionId;
        public Boolean answeredCorrectly;
    }

    public static class AttemptOption {
        public Long id;
        public String text;
        public Boolean correct;
    }

    public static class AttemptView {
        public Long attemptId;
        public Long examId;
        public Long courseId;
        public String examTitle;
        public Integer timeLimitSec;
        public LocalDateTime startAt;
        public LocalDateTime endAt;
        public Integer countdownSec;
        public Integer questionCount;
        public Long lastSeenQuestionId;
        public String status;
        public LocalDateTime finishedAt;
        public LocalDateTime gradedAt;
        public Double score;
        public Double passingScore;
        public Boolean passed;
        public boolean reviewEnabled;
        public List<AttemptQuestion> questions;
    }

    public static class AnswerUpdateRequest {
        public Long questionId;
        public Long selectedOptionId;
        public Boolean markedForReview;
        public Long lastSeenQuestionId;
    }

    public static class SubmitResponse {
        public Long attemptId;
        public String status;
        public LocalDateTime finishedAt;
        public LocalDateTime gradedAt;
        public Double scorePercent;
        public Double totalPoints;
        public Double maxPoints;
        public Boolean passed;
        public Double passingScore;
        public boolean autoSubmitted;
        public Integer attemptsUsed;
        public Integer attemptsAllowed;
        public Integer attemptsRemaining;
        public String message;
        public Boolean locked;
    }
}
