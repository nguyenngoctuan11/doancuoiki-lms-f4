package com.example.back_end.service;

import com.example.back_end.dto.SupportDtos;
import com.example.back_end.model.*;
import com.example.back_end.model.enums.SupportSenderType;
import com.example.back_end.model.enums.SupportThreadStatus;
import com.example.back_end.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupportChatServiceTest {

    @Mock
    private SupportThreadRepository threadRepository;
    @Mock
    private SupportMessageRepository messageRepository;
    @Mock
    private SupportRatingRepository ratingRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private SupportRealtimeGateway realtimeGateway;

    @InjectMocks
    private SupportChatService service;

    private User student;
    private User manager;

    @Captor
    private ArgumentCaptor<SupportRating> ratingCaptor;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(1L);
        student.setEmail("student@example.com");
        student.setFullName("Student One");

        manager = new User();
        manager.setId(2L);
        manager.setEmail("manager@example.com");
        manager.setFullName("Manager Jane");
    }

    @Test
    void createThread_shouldPersistThreadAndFirstMessage() {
        SupportDtos.CreateThreadRequest request = new SupportDtos.CreateThreadRequest();
        request.topic = "course_advice";
        request.message = "Tư vấn giúp mình chọn khóa IELTS.";

        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(threadRepository.save(any(SupportThread.class))).thenAnswer(invocation -> {
            SupportThread thread = invocation.getArgument(0);
            thread.setId(10L);
            return thread;
        });
        when(messageRepository.save(any(SupportMessage.class))).thenAnswer(invocation -> {
            SupportMessage message = invocation.getArgument(0);
            message.setId(20L);
            return message;
        });

        SupportDtos.ThreadDetail detail = service.createThread(1L, request);

        assertThat(detail).isNotNull();
        assertThat(detail.topic).isEqualTo("course_advice");
        assertThat(detail.messages).hasSize(1);
        assertThat(detail.messages.get(0).content).contains("IELTS");
        verify(realtimeGateway).threadCreated(any(SupportDtos.ThreadDetail.class));
        verify(realtimeGateway).messageAppended(eq(10L), any(SupportDtos.MessageDto.class));
    }

    @Test
    void managerSendMessage_shouldUpdateThreadStateAndNotifyStudent() {
        SupportThread thread = new SupportThread();
        thread.setId(44L);
        thread.setStudent(student);
        thread.setManager(manager);
        thread.setStatus(SupportThreadStatus.IN_PROGRESS);

        when(threadRepository.findById(44L)).thenReturn(Optional.of(thread));
        when(messageRepository.save(any(SupportMessage.class))).thenAnswer(invocation -> {
            SupportMessage message = invocation.getArgument(0);
            message.setId(55L);
            return message;
        });

        SupportDtos.SendMessageRequest request = new SupportDtos.SendMessageRequest();
        request.content = "Chào bạn, mình sẽ hỗ trợ ngay.";

        SupportDtos.MessageDto dto = service.managerSendMessage(44L, 2L, request);

        assertThat(dto).isNotNull();
        assertThat(thread.isHasUnreadForStudent()).isTrue();
        assertThat(thread.getLastSender()).isEqualTo(SupportSenderType.MANAGER);
        verify(realtimeGateway).messageAppended(eq(44L), any(SupportDtos.MessageDto.class));
        verify(realtimeGateway).threadUpdated(any(SupportDtos.ThreadSummary.class));
    }

    @Test
    void updateStatus_toClosed_shouldSetClosedAtAndNotify() {
        SupportThread thread = new SupportThread();
        thread.setId(77L);
        thread.setStudent(student);
        thread.setManager(manager);
        thread.setStatus(SupportThreadStatus.IN_PROGRESS);

        when(threadRepository.findById(77L)).thenReturn(Optional.of(thread));

        SupportDtos.UpdateStatusRequest request = new SupportDtos.UpdateStatusRequest();
        request.status = "closed";

        SupportDtos.ThreadSummary summary = service.updateStatus(77L, 2L, request);

        assertThat(summary.status).isEqualTo(SupportThreadStatus.CLOSED);
        assertThat(thread.getClosedAt()).isNotNull();
        verify(realtimeGateway).threadUpdated(any(SupportDtos.ThreadSummary.class));
    }

    @Test
    void submitRating_shouldPersistRatingWhenThreadClosed() {
        SupportThread thread = new SupportThread();
        thread.setId(88L);
        thread.setStudent(student);
        thread.setStatus(SupportThreadStatus.CLOSED);

        when(threadRepository.findByIdAndStudent_Id(88L, 1L)).thenReturn(Optional.of(thread));
        when(ratingRepository.existsByThread_Id(88L)).thenReturn(false);
        when(ratingRepository.save(any(SupportRating.class))).thenAnswer(invocation -> {
            SupportRating rating = invocation.getArgument(0);
            rating.setId(99L);
            return rating;
        });

        SupportDtos.RatingRequest request = new SupportDtos.RatingRequest();
        request.rating = 5;
        request.comment = "Rất hài lòng";

        SupportDtos.RatingDto dto = service.submitRating(88L, 1L, request);

        assertThat(dto.rating).isEqualTo(5);
        verify(ratingRepository).save(ratingCaptor.capture());
        assertThat(ratingCaptor.getValue().getStudent()).isEqualTo(student);
        verify(realtimeGateway).threadRated(eq(88L), any(SupportDtos.RatingDto.class));
    }
}
