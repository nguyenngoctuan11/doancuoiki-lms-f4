package com.example.back_end.model;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class SupportMessageAttachmentId implements Serializable {
    private Long messageId;
    private String attachmentUrl;

    public SupportMessageAttachmentId() { }

    public SupportMessageAttachmentId(Long messageId, String attachmentUrl) {
        this.messageId = messageId;
        this.attachmentUrl = attachmentUrl;
    }

    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }

    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SupportMessageAttachmentId that = (SupportMessageAttachmentId) o;
        return Objects.equals(messageId, that.messageId) &&
                Objects.equals(attachmentUrl, that.attachmentUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, attachmentUrl);
    }
}
