package com.example.back_end.service;

import com.example.back_end.dto.LessonNoteDtos;
import com.example.back_end.model.Course;
import com.example.back_end.model.Lesson;
import com.example.back_end.model.LessonNote;
import com.example.back_end.model.LessonNoteComment;
import com.example.back_end.model.Role;
import com.example.back_end.model.User;
import com.example.back_end.repository.LessonNoteCommentRepository;
import com.example.back_end.repository.LessonNoteRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LessonNoteService {
    private final LessonNoteRepository noteRepository;
    private final LessonNoteCommentRepository commentRepository;
    @PersistenceContext private EntityManager em;

    public LessonNoteService(LessonNoteRepository noteRepository, LessonNoteCommentRepository commentRepository) {
        this.noteRepository = noteRepository;
        this.commentRepository = commentRepository;
    }

    @Transactional(readOnly = true)
    public List<LessonNoteDtos.NoteResponse> listNotes(User user, Long courseId, Long lessonId) {
        assertLessonInCourse(courseId, lessonId);
        AccessInfo access = resolveAccess(user, courseId);
        if (!access.canView()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn chưa được phép xem ghi chú của bài học này");
        }
        List<LessonNote> notes = noteRepository.findByLesson_IdAndCourse_IdOrderByCreatedAtAsc(lessonId, courseId);
        return notes.stream().map(note -> toNoteDto(note, user.getId())).collect(Collectors.toList());
    }

    @Transactional
    public LessonNoteDtos.NoteResponse createNote(User user, Long courseId, Long lessonId, String content) {
        assertLessonInCourse(courseId, lessonId);
        AccessInfo access = resolveAccess(user, courseId);
        if (!hasRole(user, "student") || !access.enrolled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ học viên của khóa học mới có thể ghi chú");
        }
        String body = normalizeContent(content);
        if (body.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nội dung ghi chú không được để trống");
        }
        if (body.length() > 2000) {
            body = body.substring(0, 2000);
        }
        LessonNote note = new LessonNote();
        note.setCourse(referenceCourse(courseId));
        note.setLesson(referenceLesson(lessonId));
        note.setStudent(user);
        note.setContent(body);
        note.setLastCommentAt(LocalDateTime.now(ZoneOffset.UTC));
        LessonNote saved = noteRepository.save(note);
        return toNoteDto(requireNoteWithDetails(saved.getId()), user.getId());
    }

    @Transactional
    public LessonNoteDtos.NoteResponse addComment(User user, Long noteId, String content) {
        LessonNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy ghi chú"));
        AccessInfo access = resolveAccess(user, note.getCourse().getId());
        boolean mine = note.getStudent() != null && Objects.equals(note.getStudent().getId(), user.getId());
        if (!(mine || access.isInstructor || access.isManager)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền trả lời ghi chú này");
        }
        String body = normalizeContent(content);
        if (body.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nội dung phản hồi không được để trống");
        }
        if (body.length() > 2000) {
            body = body.substring(0, 2000);
        }
        LessonNoteComment comment = new LessonNoteComment();
        comment.setNote(note);
        comment.setUser(user);
        comment.setAuthorRole(resolveAuthorRole(user, mine, access));
        comment.setContent(body);
        commentRepository.save(comment);
        note.setLastCommentAt(LocalDateTime.now(ZoneOffset.UTC));
        noteRepository.save(note);
        return toNoteDto(requireNoteWithDetails(note.getId()), user.getId());
    }

    private LessonNoteDtos.NoteResponse toNoteDto(LessonNote note, Long currentUserId) {
        LessonNoteDtos.NoteResponse dto = new LessonNoteDtos.NoteResponse();
        dto.id = note.getId();
        dto.content = note.getContent();
        dto.courseId = note.getCourse() != null ? note.getCourse().getId() : null;
        dto.lessonId = note.getLesson() != null ? note.getLesson().getId() : null;
        dto.courseTitle = note.getCourse() != null ? note.getCourse().getTitle() : null;
        dto.lessonTitle = note.getLesson() != null ? note.getLesson().getTitle() : null;
        dto.createdAt = note.getCreatedAt();
        dto.updatedAt = note.getUpdatedAt();
        dto.lastCommentAt = note.getLastCommentAt();
        dto.mine = note.getStudent() != null && Objects.equals(note.getStudent().getId(), currentUserId);
        dto.author = toUserDto(note.getStudent(), "student");
        List<LessonNoteComment> comments = note.getComments() != null ? note.getComments() : new ArrayList<>();
        dto.comments = comments.stream().map(this::toCommentDto).collect(Collectors.toList());
        dto.commentCount = dto.comments.size();
        dto.status = computeStatus(comments);
        return dto;
    }

    private LessonNoteDtos.CommentResponse toCommentDto(LessonNoteComment comment) {
        LessonNoteDtos.CommentResponse dto = new LessonNoteDtos.CommentResponse();
        dto.id = comment.getId();
        dto.content = comment.getContent();
        dto.createdAt = comment.getCreatedAt();
        dto.author = toUserDto(comment.getUser(), comment.getAuthorRole());
        return dto;
    }

    private LessonNoteDtos.SimpleUser toUserDto(User user, String fallbackRole) {
        LessonNoteDtos.SimpleUser dto = new LessonNoteDtos.SimpleUser();
        if (user != null) {
            dto.id = user.getId();
            dto.name = user.getFullName();
            dto.avatar = user.getAvatarUrl();
        }
        dto.role = fallbackRole;
        return dto;
    }

    private LessonNote requireNoteWithDetails(Long noteId) {
        Optional<LessonNote> note = noteRepository.findWithDetailsById(noteId);
        return note.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy ghi chú"));
    }

    private void assertLessonInCourse(Long courseId, Long lessonId) {
        Number count = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM dbo.lessons l JOIN dbo.modules m ON m.id = l.module_id WHERE l.id = :lessonId AND m.course_id = :courseId")
                .setParameter("lessonId", lessonId)
                .setParameter("courseId", courseId)
                .getSingleResult();
        if (count == null || count.intValue() == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài học thuộc khóa học này");
        }
    }

    private AccessInfo resolveAccess(User user, Long courseId) {
        AccessInfo info = new AccessInfo();
        info.isManager = hasRole(user, "manager");
        info.isInstructor = info.isManager || isCourseInstructor(user.getId(), courseId);
        info.enrolled = isEnrolled(user.getId(), courseId);
        return info;
    }

    private boolean isEnrolled(Long userId, Long courseId) {
        Number count = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM dbo.enrollments WHERE user_id = :uid AND course_id = :cid")
                .setParameter("uid", userId)
                .setParameter("cid", courseId)
                .getSingleResult();
        return count != null && count.intValue() > 0;
    }

    private boolean isCourseInstructor(Long userId, Long courseId) {
        Number owner = (Number) em.createNativeQuery("SELECT COUNT(*) FROM dbo.courses WHERE id = :cid AND created_by = :uid")
                .setParameter("cid", courseId)
                .setParameter("uid", userId)
                .getSingleResult();
        if (owner != null && owner.intValue() > 0) {
            return true;
        }
        Number assigned = (Number) em.createNativeQuery("SELECT COUNT(*) FROM dbo.course_instructors WHERE course_id = :cid AND user_id = :uid")
                .setParameter("cid", courseId)
                .setParameter("uid", userId)
                .getSingleResult();
        return assigned != null && assigned.intValue() > 0;
    }

    private boolean hasRole(User user, String code) {
        if (user == null || user.getRoles() == null) return false;
        String expected = code == null ? null : code.trim().toLowerCase();
        for (Role role : user.getRoles()) {
            if (role.getCode() != null && role.getCode().trim().equalsIgnoreCase(expected)) {
                return true;
            }
        }
        return false;
    }

    private Lesson referenceLesson(Long lessonId) {
        return em.getReference(Lesson.class, lessonId);
    }

    private Course referenceCourse(Long courseId) {
        return em.getReference(Course.class, courseId);
    }

    private String resolveAuthorRole(User user, boolean mine, AccessInfo access) {
        if (mine) return "student";
        if (access.isManager) return "manager";
        if (access.isInstructor) return "teacher";
        return "student";
    }

    private String normalizeContent(String content) {
        return content == null ? "" : content.trim();
    }

    private String computeStatus(List<LessonNoteComment> comments) {
        if (comments == null || comments.isEmpty()) {
            return "pending";
        }
        boolean hasTeacherReply = comments.stream().anyMatch(c -> {
            String role = c.getAuthorRole() == null ? "" : c.getAuthorRole().trim().toLowerCase();
            return role.equals("teacher") || role.equals("manager");
        });
        return hasTeacherReply ? "resolved" : "pending";
    }

    @Transactional(readOnly = true)
    public LessonNoteDtos.NoteResponse getNoteForUser(Long noteId, Long userId) {
        LessonNote note = requireNoteWithDetails(noteId);
        return toNoteDto(note, userId);
    }

    @Transactional(readOnly = true)
    public List<LessonNoteDtos.NoteResponse> listNotesForTeacher(User user, Long courseId, String status, String search) {
        boolean isManager = hasRole(user, "manager");
        StringBuilder sql = new StringBuilder("SELECT DISTINCT ln.id FROM dbo.lesson_notes ln JOIN dbo.courses c ON c.id = ln.course_id ");
        sql.append("WHERE 1=1 ");
        if (!isManager) {
            sql.append("AND (c.created_by = :uid OR EXISTS (SELECT 1 FROM dbo.course_instructors ci WHERE ci.course_id = ln.course_id AND ci.user_id = :uid)) ");
        }
        if (courseId != null) {
            sql.append("AND ln.course_id = :courseId ");
        }
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(ln.content) LIKE :q OR LOWER(c.title) LIKE :q) ");
        }

        Query query = em.createNativeQuery(sql.toString());
        query.setParameter("uid", user.getId());
        if (courseId != null) {
            query.setParameter("courseId", courseId);
        }
        if (search != null && !search.trim().isEmpty()) {
            query.setParameter("q", "%" + search.trim().toLowerCase() + "%");
        }
        @SuppressWarnings("unchecked")
        List<Number> ids = query.getResultList();
        if (ids.isEmpty()) return List.of();

        List<Long> noteIds = ids.stream().map(Number::longValue).collect(Collectors.toList());
        List<LessonNote> notes = noteRepository.findByIdIn(noteIds);
        // giữ thứ tự theo last_comment_at desc
        notes.sort((a, b) -> {
            LocalDateTime la = a.getLastCommentAt();
            LocalDateTime lb = b.getLastCommentAt();
            if (la == null && lb == null) return 0;
            if (la == null) return 1;
            if (lb == null) return -1;
            return lb.compareTo(la);
        });

        return notes.stream()
                .map(n -> {
                    LessonNoteDtos.NoteResponse dto = toNoteDto(n, user.getId());
                    if (status != null && !status.isBlank()) {
                        String filter = status.trim().toLowerCase();
                        if (!filter.equals(dto.status != null ? dto.status.toLowerCase() : "")) {
                            return null;
                        }
                    }
                    return dto;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static class AccessInfo {
        boolean enrolled;
        boolean isInstructor;
        boolean isManager;

        boolean canView() {
            return enrolled || isInstructor || isManager;
        }
    }
}
