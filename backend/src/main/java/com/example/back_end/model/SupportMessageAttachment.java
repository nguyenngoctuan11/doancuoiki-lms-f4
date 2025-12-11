package com.example.back_end.model;

import jakarta.persistence.*;

@Entity
@Table(name = "support_message_attachments", schema = "dbo")
public class SupportMessageAttachment {
    @EmbeddedId
    @AttributeOverrides({
            @AttributeOverride(name = "messageId", column = @Column(name = "message_id")),
            @AttributeOverride(name = "attachmentUrl", column = @Column(name = "attachment_url", length = 1024))
    })
    private SupportMessageAttachmentId id = new SupportMessageAttachmentId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("messageId")
    @JoinColumn(name = "message_id", nullable = false)
    private SupportMessage message;

    public SupportMessageAttachmentId getId() { return id; }
    public void setId(SupportMessageAttachmentId id) { this.id = id; }

    public SupportMessage getMessage() { return message; }
    public void setMessage(SupportMessage message) { this.message = message; }

    public String getAttachmentUrl() {
        return id != null ? id.getAttachmentUrl() : null;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        if (this.id == null) {
            this.id = new SupportMessageAttachmentId();
        }
        this.id.setAttachmentUrl(attachmentUrl);
    }
}
