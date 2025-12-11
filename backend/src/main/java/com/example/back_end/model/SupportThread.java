package com.example.back_end.model;

import com.example.back_end.model.enums.SupportSenderType;
import com.example.back_end.model.enums.SupportThreadStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "support_threads", schema = "dbo")
public class SupportThread {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(name = "topic", nullable = false, length = 64)
    private String topic;

    @Column(name = "subject", length = 255)
    private String subject;

    @Column(name = "origin", length = 64)
    private String origin;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SupportThreadStatus status = SupportThreadStatus.NEW;

    @Column(name = "priority", length = 16)
    private String priority;

    @Column(name = "channel", length = 32)
    private String channel;

    @Column(name = "metadata")
    private String metadata;

    @Column(name = "last_message_preview", length = 512)
    private String lastMessagePreview;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_sender", length = 16)
    private SupportSenderType lastSender;

    @Column(name = "last_student_activity_at")
    private LocalDateTime lastStudentActivityAt;

    @Column(name = "last_manager_activity_at")
    private LocalDateTime lastManagerActivityAt;

    @Column(name = "has_unread_for_student", nullable = false)
    private boolean hasUnreadForStudent = false;

    @Column(name = "has_unread_for_manager", nullable = false)
    private boolean hasUnreadForManager = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public User getManager() { return manager; }
    public void setManager(User manager) { this.manager = manager; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public SupportThreadStatus getStatus() { return status; }
    public void setStatus(SupportThreadStatus status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public String getLastMessagePreview() { return lastMessagePreview; }
    public void setLastMessagePreview(String lastMessagePreview) { this.lastMessagePreview = lastMessagePreview; }

    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public SupportSenderType getLastSender() { return lastSender; }
    public void setLastSender(SupportSenderType lastSender) { this.lastSender = lastSender; }

    public LocalDateTime getLastStudentActivityAt() { return lastStudentActivityAt; }
    public void setLastStudentActivityAt(LocalDateTime lastStudentActivityAt) { this.lastStudentActivityAt = lastStudentActivityAt; }

    public LocalDateTime getLastManagerActivityAt() { return lastManagerActivityAt; }
    public void setLastManagerActivityAt(LocalDateTime lastManagerActivityAt) { this.lastManagerActivityAt = lastManagerActivityAt; }

    public boolean isHasUnreadForStudent() { return hasUnreadForStudent; }
    public void setHasUnreadForStudent(boolean hasUnreadForStudent) { this.hasUnreadForStudent = hasUnreadForStudent; }

    public boolean isHasUnreadForManager() { return hasUnreadForManager; }
    public void setHasUnreadForManager(boolean hasUnreadForManager) { this.hasUnreadForManager = hasUnreadForManager; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
}
