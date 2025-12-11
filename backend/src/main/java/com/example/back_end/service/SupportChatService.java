package com.example.back_end.service;

import com.example.back_end.dto.SupportDtos;
import com.example.back_end.model.*;
import com.example.back_end.model.enums.SupportSenderType;
import com.example.back_end.model.enums.SupportThreadStatus;
import com.example.back_end.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SupportChatService {
    private final SupportThreadRepository threadRepository;
    private final SupportMessageRepository messageRepository;
    private final SupportRatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final SupportRealtimeGateway realtimeGateway;

    public SupportChatService(SupportThreadRepository threadRepository,
                              SupportMessageRepository messageRepository,
                              SupportRatingRepository ratingRepository,
                              UserRepository userRepository,
                              CourseRepository courseRepository,
                              SupportRealtimeGateway realtimeGateway) {
        this.threadRepository = threadRepository;
        this.messageRepository = messageRepository;
        this.ratingRepository = ratingRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.realtimeGateway = realtimeGateway;
    }

    @Transactional
    public SupportDtos.ThreadDetail createThread(Long studentId, SupportDtos.CreateThreadRequest req) {
        User student = requireUser(studentId);
        validateCreateRequest(req);
        Course course = null;
        if (req.courseId != null) {
            course = courseRepository.findById(req.courseId)
                    .orElseThrow(() -> new IllegalArgumentException("KhA'ng tA�m th���y khoA� h���c #" + req.courseId));
        }
        LocalDateTime now = LocalDateTime.now();
        SupportThread thread = new SupportThread();
        thread.setStudent(student);
        thread.setCourse(course);
        thread.setTopic(req.topic.trim());
        thread.setSubject(trimToNull(req.subject));
        thread.setOrigin(trimToNull(req.origin));
        thread.setChannel(trimToNull(req.channel));
        thread.setPriority(trimToNull(req.priority));
        thread.setMetadata(trimToNull(req.metadata));
        thread.setStatus(SupportThreadStatus.NEW);
        thread.setHasUnreadForManager(true);
        thread.setHasUnreadForStudent(false);
        thread.setCreatedAt(now);
        thread.setUpdatedAt(now);
        thread.setLastStudentActivityAt(now);

        thread = threadRepository.save(thread);

        SupportMessage firstMessage = persistMessage(thread, student, SupportSenderType.STUDENT, req.message, req.attachments);
        SupportDtos.ThreadDetail detail = toDetail(thread, List.of(firstMessage), null);
        realtimeGateway.threadCreated(detail);
        realtimeGateway.messageAppended(thread.getId(), toMessageDto(firstMessage));
        return detail;
    }

    @Transactional(readOnly = true)
    public SupportDtos.ThreadListResponse studentThreads(Long studentId, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<SupportThread> paged = threadRepository.findByStudent_Id(studentId, pageable);
        return toPagedResponse(paged);
    }

    @Transactional
    public SupportDtos.ThreadDetail studentThreadDetail(Long threadId, Long studentId) {
        SupportThread thread = threadRepository.findByIdAndStudent_Id(threadId, studentId)
                .orElseThrow(() -> new IllegalArgumentException("KhA'ng tA?m th???y h??? sA? chat #" + threadId));
        if (thread.isHasUnreadForStudent()) {
            thread.setHasUnreadForStudent(false);
        }
        List<SupportMessage> messages = messageRepository.findByThread_IdOrderByCreatedAtAsc(thread.getId());
        SupportRating rating = ratingRepository.findByThread_Id(thread.getId()).orElse(null);
        return toDetail(thread, messages, rating);
    }

    @Transactional
    public SupportDtos.MessageDto studentSendMessage(Long threadId, Long studentId, SupportDtos.SendMessageRequest req) {
        SupportThread thread = threadRepository.findByIdAndStudent_Id(threadId, studentId)
                .orElseThrow(() -> new IllegalArgumentException("KhA'ng tA�m th���y h��� sA? chat #" + threadId));
        if (thread.getStatus() == SupportThreadStatus.CLOSED) {
            throw new IllegalStateException("H���i thoA?i �`A� �`A!ng �`ong");
        }
        SupportMessage message = persistMessage(thread, thread.getStudent(), SupportSenderType.STUDENT, req.content, req.attachments);
        SupportDtos.MessageDto dto = toMessageDto(message);
        realtimeGateway.messageAppended(thread.getId(), dto);
        realtimeGateway.threadUpdated(toSummary(thread));
        return dto;
    }

    @Transactional(readOnly = true)
    public SupportDtos.ThreadListResponse managerThreads(Long managerId, SupportDtos.ThreadFilter filter, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        SupportThreadStatus status = filter != null ? SupportThreadStatus.from(valueOrNull(filter.status)) : null;
        Long courseId = filter != null ? filter.courseId : null;
        String studentKeyword = filter != null ? trimToNull(filter.studentKeyword) : null;
        LocalDateTime fromDate = filter != null ? filter.from : null;
        LocalDateTime toDate = filter != null ? filter.to : null;
        boolean mineOnly = filter != null && Boolean.TRUE.equals(filter.mineOnly);
        Page<SupportThread> paged = threadRepository.searchForManager(
                status, courseId, studentKeyword, fromDate, toDate, mineOnly, managerId, pageable
        );
        return toPagedResponse(paged);
    }

    @Transactional
    public SupportDtos.ThreadDetail managerThreadDetail(Long threadId, Long viewerManagerId) {
        SupportThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new IllegalArgumentException("KhA'ng tA?m th???y h??? sA? chat #" + threadId));
        if (viewerManagerId != null
                && thread.getManager() != null
                && thread.getManager().getId().equals(viewerManagerId)
                && thread.isHasUnreadForManager()) {
            thread.setHasUnreadForManager(false);
        }
        List<SupportMessage> messages = messageRepository.findByThread_IdOrderByCreatedAtAsc(thread.getId());
        SupportRating rating = ratingRepository.findByThread_Id(thread.getId()).orElse(null);
        return toDetail(thread, messages, rating);
    }

    @Transactional
    public SupportDtos.ThreadSummary claimThread(Long threadId, Long managerId) {
        SupportThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new IllegalArgumentException("KhA'ng tA�m th���y h��� sA? chat #" + threadId));
        if (thread.getManager() != null && !thread.getManager().getId().equals(managerId)) {
            throw new IllegalStateException("H���i thoA?i �`A� cA3 quA?n lA� khA?c phAÏ nhiA?m");
        }
        if (thread.getManager() == null) {
            User manager = requireUser(managerId);
            thread.setManager(manager);
        }
        thread.setStatus(SupportThreadStatus.IN_PROGRESS);
        thread.setUpdatedAt(LocalDateTime.now());
        SupportDtos.ThreadSummary summary = toSummary(thread);
        realtimeGateway.threadUpdated(summary);
        return summary;
    }

    @Transactional
    public SupportDtos.MessageDto managerSendMessage(Long threadId, Long managerId, SupportDtos.SendMessageRequest req) {
        SupportThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new IllegalArgumentException("KhA'ng tA�m th���y h��� sA? chat #" + threadId));
        if (thread.getStatus() == SupportThreadStatus.CLOSED) {
            throw new IllegalStateException("H���i thoA?i �`A� �`A!ng �`ong");
        }
        if (thread.getManager() == null) {
            thread.setManager(requireUser(managerId));
        } else if (!thread.getManager().getId().equals(managerId)) {
            throw new IllegalStateException("ChA� quA?n lA� phAÏ nA'y mA?i �`A� �`A!ng xA? ly�");
        }
        SupportMessage message = persistMessage(thread, thread.getManager(), SupportSenderType.MANAGER, req.content, req.attachments);
        SupportDtos.MessageDto dto = toMessageDto(message);
        realtimeGateway.messageAppended(thread.getId(), dto);
        realtimeGateway.threadUpdated(toSummary(thread));
        return dto;
    }

    @Transactional
    public SupportDtos.ThreadSummary updateStatus(Long threadId, Long managerId, SupportDtos.UpdateStatusRequest req) {
        SupportThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new IllegalArgumentException("KhA'ng tA�m th���y h��� sA? chat #" + threadId));
        if (thread.getManager() == null || !thread.getManager().getId().equals(managerId)) {
            throw new IllegalStateException("ChA� quA?n lA� phAÏ nA'y mA?i �`A� �`A!ng xA? ly�");
        }
        SupportThreadStatus status = SupportThreadStatus.from(valueOrNull(req.status));
        if (status == null) {
            throw new IllegalArgumentException("TrA?ng thA�i khA'ng h���p l���");
        }
        thread.setStatus(status);
        LocalDateTime now = LocalDateTime.now();
        thread.setUpdatedAt(now);
        if (status == SupportThreadStatus.CLOSED) {
            thread.setClosedAt(now);
        }
        SupportDtos.ThreadSummary summary = toSummary(thread);
        realtimeGateway.threadUpdated(summary);
        return summary;
    }

    @Transactional
    public SupportDtos.ThreadSummary transferThread(Long threadId, Long managerId, SupportDtos.TransferRequest req) {
        if (req == null || req.newManagerId == null) {
            throw new IllegalArgumentException("CAn phA?i chA?n quA?n lA� nhA?n h���i thoA?i");
        }
        SupportThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new IllegalArgumentException("KhA'ng tA?m th���y h��� sA? chat #" + threadId));
        if (thread.getManager() == null || !thread.getManager().getId().equals(managerId)) {
            throw new IllegalStateException("ChA� quA?n lA� phAÏ nA'y mA?i �`A� �`A!ng xA? ly�");
        }
        if (thread.getManager().getId().equals(req.newManagerId)) {
            return toSummary(thread);
        }
        User newManager = requireUser(req.newManagerId);
        thread.setManager(newManager);
        thread.setStatus(SupportThreadStatus.IN_PROGRESS);
        thread.setUpdatedAt(LocalDateTime.now());
        SupportDtos.ThreadSummary summary = toSummary(thread);
        realtimeGateway.threadUpdated(summary);
        realtimeGateway.threadTransferred(summary);
        return summary;
    }

    @Transactional
    public SupportDtos.RatingDto submitRating(Long threadId, Long studentId, SupportDtos.RatingRequest req) {
        SupportThread thread = threadRepository.findByIdAndStudent_Id(threadId, studentId)
                .orElseThrow(() -> new IllegalArgumentException("KhA'ng tA?m th���y h��� sA? chat #" + threadId));
        if (thread.getStatus() != SupportThreadStatus.CLOSED) {
            throw new IllegalStateException("ChA? thA? giAi quyA?t xong, chA?a thA? danh giA?");
        }
        if (ratingRepository.existsByThread_Id(threadId)) {
            throw new IllegalStateException("BAn �`A� gA?i danh giA? cho h���i thoA?i nA�y");
        }
        int ratingValue = req != null && req.rating != null ? req.rating : 0;
        if (ratingValue < 1 || ratingValue > 5) {
            throw new IllegalArgumentException("Rating phA?i tA? 1 ��?n 5 sao");
        }
        SupportRating rating = new SupportRating();
        rating.setThread(thread);
        rating.setStudent(thread.getStudent());
        rating.setRating(ratingValue);
        rating.setComment(trimToNull(req.comment));
        rating.setCreatedAt(LocalDateTime.now());
        SupportRating saved = ratingRepository.save(rating);
        SupportDtos.RatingDto dto = toRatingDto(saved);
        realtimeGateway.threadRated(thread.getId(), dto);
        return dto;
    }

    private void validateCreateRequest(SupportDtos.CreateThreadRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("ThiA'u thA?ng tin h��� trA? chat");
        }
        if (req.topic == null || req.topic.isBlank()) {
            throw new IllegalArgumentException("ChA? �`A? chA? dA?ng bA?t buA?c");
        }
        if (req.message == null || req.message.isBlank()) {
            throw new IllegalArgumentException("Tin nhA?n mA? tA? phA?i cA? nA?i dung");
        }
    }

    private SupportMessage persistMessage(SupportThread thread, User sender, SupportSenderType senderType, String content, List<String> attachments) {
        String normalized = normalizeMessage(content);
        LocalDateTime now = LocalDateTime.now();
        SupportMessage message = new SupportMessage();
        message.setThread(thread);
        message.setSender(sender);
        message.setSenderType(senderType);
        message.setContent(normalized);
        message.setCreatedAt(now);
        if (attachments != null && !attachments.isEmpty()) {
            List<SupportMessageAttachment> attachmentEntities = new ArrayList<>();
            for (String attachmentUrl : attachments) {
                String url = trimToNull(attachmentUrl);
                if (url == null) continue;
                SupportMessageAttachment attachment = new SupportMessageAttachment();
                attachment.setMessage(message);
                attachment.setAttachmentUrl(url);
                attachmentEntities.add(attachment);
            }
            message.setAttachments(attachmentEntities);
        }
        SupportMessage saved = messageRepository.save(message);
        touchThreadAfterMessage(thread, senderType, normalized, now);
        return saved;
    }

    private void touchThreadAfterMessage(SupportThread thread, SupportSenderType senderType, String content, LocalDateTime timestamp) {
        thread.setLastMessageAt(timestamp);
        thread.setLastSender(senderType);
        thread.setLastMessagePreview(content == null ? null : truncate(content, 480));
        thread.setUpdatedAt(timestamp);
        if (senderType == SupportSenderType.STUDENT) {
            thread.setLastStudentActivityAt(timestamp);
            thread.setHasUnreadForManager(true);
            thread.setHasUnreadForStudent(false);
            if (thread.getStatus() == SupportThreadStatus.WAITING_STUDENT) {
                thread.setStatus(SupportThreadStatus.IN_PROGRESS);
            }
        } else if (senderType == SupportSenderType.MANAGER) {
            thread.setLastManagerActivityAt(timestamp);
            thread.setHasUnreadForStudent(true);
            thread.setHasUnreadForManager(false);
            if (thread.getStatus() == SupportThreadStatus.NEW) {
                thread.setStatus(SupportThreadStatus.IN_PROGRESS);
            }
        }
    }

    private SupportDtos.ThreadListResponse toPagedResponse(Page<SupportThread> paged) {
        SupportDtos.ThreadListResponse resp = new SupportDtos.ThreadListResponse();
        resp.data = paged.getContent().stream().map(this::toSummary).collect(Collectors.toList());
        resp.totalElements = paged.getTotalElements();
        resp.page = paged.getNumber();
        resp.size = paged.getSize();
        return resp;
    }

    private SupportDtos.ThreadSummary toSummary(SupportThread thread) {
        SupportDtos.ThreadSummary summary = new SupportDtos.ThreadSummary();
        summary.id = thread.getId();
        summary.topic = thread.getTopic();
        summary.subject = thread.getSubject();
        summary.origin = thread.getOrigin();
        summary.channel = thread.getChannel();
        summary.metadata = thread.getMetadata();
        summary.courseId = thread.getCourse() != null ? thread.getCourse().getId() : null;
        summary.courseTitle = thread.getCourse() != null ? thread.getCourse().getTitle() : null;
        summary.status = thread.getStatus();
        summary.priority = thread.getPriority();
        summary.lastMessagePreview = thread.getLastMessagePreview();
        summary.lastMessageAt = thread.getLastMessageAt();
        summary.lastSender = thread.getLastSender();
        summary.unreadForStudent = thread.isHasUnreadForStudent();
        summary.unreadForManager = thread.isHasUnreadForManager();
        summary.createdAt = thread.getCreatedAt();
        summary.updatedAt = thread.getUpdatedAt();
        summary.student = toParticipant(thread.getStudent());
        summary.manager = thread.getManager() != null ? toParticipant(thread.getManager()) : null;
        return summary;
    }

    private SupportDtos.ThreadDetail toDetail(SupportThread thread, List<SupportMessage> messages, SupportRating rating) {
        SupportDtos.ThreadDetail detail = new SupportDtos.ThreadDetail();
        SupportDtos.ThreadSummary summary = toSummary(thread);
        detail.id = summary.id;
        detail.topic = summary.topic;
        detail.subject = summary.subject;
        detail.origin = summary.origin;
        detail.channel = summary.channel;
        detail.metadata = summary.metadata;
        detail.courseId = summary.courseId;
        detail.courseTitle = summary.courseTitle;
        detail.status = summary.status;
        detail.priority = summary.priority;
        detail.lastMessagePreview = summary.lastMessagePreview;
        detail.lastMessageAt = summary.lastMessageAt;
        detail.lastSender = summary.lastSender;
        detail.unreadForStudent = summary.unreadForStudent;
        detail.unreadForManager = summary.unreadForManager;
        detail.createdAt = summary.createdAt;
        detail.updatedAt = summary.updatedAt;
        detail.student = summary.student;
        detail.manager = summary.manager;
        detail.messages = messages.stream().map(this::toMessageDto).collect(Collectors.toList());
        detail.rating = rating != null ? toRatingDto(rating) : null;
        return detail;
    }

    private SupportDtos.MessageDto toMessageDto(SupportMessage message) {
        SupportDtos.MessageDto dto = new SupportDtos.MessageDto();
        dto.id = message.getId();
        dto.threadId = message.getThread() != null ? message.getThread().getId() : null;
        dto.senderType = message.getSenderType();
        dto.sender = message.getSender() != null ? toParticipant(message.getSender()) : null;
        dto.content = message.getContent();
        dto.createdAt = message.getCreatedAt();
        List<String> attachments = message.getAttachments() == null ? List.of()
                : message.getAttachments().stream()
                .map(SupportMessageAttachment::getAttachmentUrl)
                .filter(url -> url != null && !url.isBlank())
                .collect(Collectors.toList());
        dto.attachments = attachments;
        return dto;
    }

    private SupportDtos.ParticipantInfo toParticipant(User user) {
        if (user == null) return null;
        SupportDtos.ParticipantInfo info = new SupportDtos.ParticipantInfo();
        info.id = user.getId();
        info.fullName = user.getFullName();
        info.email = user.getEmail();
        info.avatarUrl = user.getAvatarUrl();
        return info;
    }

    private SupportDtos.RatingDto toRatingDto(SupportRating rating) {
        SupportDtos.RatingDto dto = new SupportDtos.RatingDto();
        dto.id = rating.getId();
        dto.rating = rating.getRating();
        dto.comment = rating.getComment();
        dto.createdAt = rating.getCreatedAt();
        return dto;
    }

    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("KhA'ng tA?m th���y ngA??i dA�ng #" + id));
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeMessage(String content) {
        String trimmed = trimToNull(content);
        if (trimmed == null) {
            throw new IllegalArgumentException("Nha?n tin phA?i cA? nA?i dung");
        }
        if (trimmed.length() > 5000) {
            trimmed = trimmed.substring(0, 5000);
        }
        return trimmed;
    }

    private String truncate(String content, int max) {
        if (content == null || content.length() <= max) return content;
        return content.substring(0, max);
    }

    private String valueOrNull(String value) {
        return trimToNull(value);
    }
}
