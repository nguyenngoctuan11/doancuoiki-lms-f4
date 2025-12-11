package com.example.back_end.model;

import com.example.back_end.model.enums.SupportSenderType;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "support_messages", schema = "dbo")
public class SupportMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    private SupportThread thread;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 16)
    private SupportSenderType senderType;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SupportMessageAttachment> attachments = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SupportThread getThread() { return thread; }
    public void setThread(SupportThread thread) { this.thread = thread; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public SupportSenderType getSenderType() { return senderType; }
    public void setSenderType(SupportSenderType senderType) { this.senderType = senderType; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<SupportMessageAttachment> getAttachments() { return attachments; }
    public void setAttachments(List<SupportMessageAttachment> attachments) { this.attachments = attachments; }
}
