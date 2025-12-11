package com.example.back_end.service;

import com.example.back_end.dto.SupportDtos;
import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class SupportRealtimeGateway {
    private static final String THREAD_CHANNEL_PREFIX = "/topic/support/threads/";
    private static final String THREAD_META_CHANNEL = "/topic/support/thread-updates";
    private static final String MANAGER_ALERT_CHANNEL = "/topic/support/manager-alerts";

    private final SimpMessagingTemplate messagingTemplate;

    public SupportRealtimeGateway(@Nullable SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void threadCreated(SupportDtos.ThreadDetail detail) {
        send(MANAGER_ALERT_CHANNEL, detail);
        send(THREAD_META_CHANNEL, detail);
    }

    public void threadUpdated(SupportDtos.ThreadSummary summary) {
        send(THREAD_META_CHANNEL, summary);
    }

    public void messageAppended(Long threadId, SupportDtos.MessageDto message) {
        if (threadId == null) return;
        send(THREAD_CHANNEL_PREFIX + threadId, message);
    }

    public void threadTransferred(SupportDtos.ThreadSummary summary) {
        send(MANAGER_ALERT_CHANNEL, summary);
    }

    public void threadRated(Long threadId, SupportDtos.RatingDto rating) {
        if (threadId == null) return;
        send(THREAD_CHANNEL_PREFIX + threadId + "/rating", rating);
    }

    private void send(String destination, Object payload) {
        if (messagingTemplate == null || destination == null || payload == null) return;
        messagingTemplate.convertAndSend(destination, payload);
    }
}
