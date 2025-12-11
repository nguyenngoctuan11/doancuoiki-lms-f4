package com.example.back_end.dto;

import com.example.back_end.model.enums.SupportSenderType;
import com.example.back_end.model.enums.SupportThreadStatus;

import java.time.LocalDateTime;
import java.util.List;

public class SupportDtos {
    public static class CreateThreadRequest {
        public String topic;
        public Long courseId;
        public String subject;
        public String origin;
        public String channel;
        public String priority;
        public String metadata;
        public String message;
        public List<String> attachments;
    }

    public static class SendMessageRequest {
        public String content;
        public List<String> attachments;
    }

    public static class UpdateStatusRequest {
        public String status;
        public String note;
    }

    public static class TransferRequest {
        public Long newManagerId;
    }

    public static class RatingRequest {
        public Integer rating;
        public String comment;
    }

    public static class ThreadFilter {
        public String status;
        public Long courseId;
        public String studentKeyword;
        public Boolean mineOnly;
        public LocalDateTime from;
        public LocalDateTime to;
    }

    public static class ThreadSummary {
        public Long id;
        public String topic;
        public String subject;
        public String origin;
        public String channel;
        public String metadata;
        public Long courseId;
        public String courseTitle;
        public SupportThreadStatus status;
        public String priority;
        public String lastMessagePreview;
        public LocalDateTime lastMessageAt;
        public SupportSenderType lastSender;
        public boolean unreadForStudent;
        public boolean unreadForManager;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;
        public ParticipantInfo student;
        public ParticipantInfo manager;
    }

    public static class ThreadDetail extends ThreadSummary {
        public List<MessageDto> messages;
        public RatingDto rating;
    }

    public static class MessageDto {
        public Long id;
        public Long threadId;
        public SupportSenderType senderType;
        public ParticipantInfo sender;
        public String content;
        public LocalDateTime createdAt;
        public List<String> attachments;
    }

    public static class ParticipantInfo {
        public Long id;
        public String fullName;
        public String email;
        public String avatarUrl;
    }

    public static class RatingDto {
        public Long id;
        public Integer rating;
        public String comment;
        public LocalDateTime createdAt;
    }

    public static class ThreadListResponse {
        public List<ThreadSummary> data;
        public long totalElements;
        public int page;
        public int size;
    }
}
