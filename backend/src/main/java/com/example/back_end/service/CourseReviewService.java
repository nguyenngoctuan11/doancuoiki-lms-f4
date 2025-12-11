package com.example.back_end.service;

import com.example.back_end.dto.CourseReviewDtos;
import com.example.back_end.model.Course;
import com.example.back_end.model.CourseReview;
import com.example.back_end.model.User;
import com.example.back_end.repository.CourseRepository;
import com.example.back_end.repository.CourseReviewRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CourseReviewService {
    private final CourseReviewRepository reviewRepository;
    private final CourseRepository courseRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public CourseReviewService(CourseReviewRepository reviewRepository, CourseRepository courseRepository) {
        this.reviewRepository = reviewRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional
    public CourseReviewDtos.ReviewResponse createOrUpdateReview(User student, Long courseId, CourseReviewDtos.CreateRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khóa học không tồn tại"));
        if (!isEnrolled(student.getId(), courseId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn cần tham gia khóa học trước khi đánh giá");
        }
        CourseReview review = reviewRepository.findByCourse_IdAndStudent_Id(courseId, student.getId())
                .orElseGet(CourseReview::new);
        if (review.getCourse() == null) {
            review.setCourse(course);
            review.setStudent(student);
            review.setInstructor(course.getCreatedBy());
            review.setStatus("pending");
        }
        review.setCourseScore(boundScore(request.courseScore));
        review.setInstructorScore(boundScore(request.instructorScore));
        review.setSupportScore(boundScore(request.supportScore));
        review.setWouldRecommend(request.wouldRecommend != null && request.wouldRecommend);
        review.setComment(truncate(clean(request.comment), 2000));
        review.setHighlight(truncate(clean(request.highlight), 500));
        review.setImprovement(truncate(clean(request.improvement), 500));
        if (!Objects.equals(review.getStatus(), "pending")) {
            review.setStatus("pending");
        }
        CourseReview saved = reviewRepository.save(review);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<CourseReviewDtos.ReviewResponse> listApprovedReviews(Long courseId, int limit) {
        List<CourseReview> reviews = reviewRepository.findByCourse_IdAndStatusOrderByCreatedAtDesc(courseId, "approved");
        return reviews.stream()
                .sorted(Comparator.comparing(CourseReview::getCreatedAt).reversed())
                .limit(Math.max(1, limit))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CourseReviewDtos.SummaryResponse courseSummary(Long courseId) {
        CourseReviewRepository.CourseReviewStats stats = reviewRepository.summaryForCourse(courseId);
        return buildSummary(stats, reviewRepository.findByCourse_IdAndStatusOrderByCreatedAtDesc(courseId, "approved"));
    }

    @Transactional(readOnly = true)
    public CourseReviewDtos.SummaryResponse instructorSummary(Long instructorId) {
        CourseReviewRepository.CourseReviewStats stats = reviewRepository.summaryForInstructor(instructorId);
        return buildSummary(stats, reviewRepository.findByStatusOrderByCreatedAtDesc("approved")
                .stream()
                .filter(r -> r.getInstructor() != null && Objects.equals(r.getInstructor().getId(), instructorId))
                .collect(Collectors.toList()));
    }

    @Transactional(readOnly = true)
    public List<CourseReviewDtos.ReviewResponse> moderationList(String status) {
        String normalized = status != null && !status.isBlank() ? status.trim().toLowerCase() : "pending";
        return reviewRepository.findByStatusOrderByCreatedAtDesc(normalized)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CourseReviewDtos.ReviewResponse> instructorModerationList(User instructor, Long courseId, String status) {
        if (instructor == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem danh sách này");
        }
        String normalized = status != null && !status.isBlank() ? status.trim().toLowerCase() : "pending";
        List<CourseReview> source;
        if (courseId != null) {
            source = reviewRepository.findByCourse_IdAndStatusOrderByCreatedAtDesc(courseId, normalized)
                    .stream()
                    .filter(r -> r.getInstructor() != null && Objects.equals(r.getInstructor().getId(), instructor.getId()))
                    .collect(Collectors.toList());
        } else {
            source = reviewRepository.findByInstructor_IdAndStatusOrderByCreatedAtDesc(instructor.getId(), normalized);
        }
        return source.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public CourseReviewDtos.ReviewResponse updateStatus(Long id, String status, String adminNote) {
        return updateStatusInternal(id, status, adminNote, null, false);
    }

    @Transactional
    public CourseReviewDtos.ReviewResponse updateStatusAs(User actor, Long id, String status, String adminNote) {
        return updateStatusInternal(id, status, adminNote, actor, true);
    }

    private CourseReviewDtos.ReviewResponse updateStatusInternal(Long id, String status, String adminNote, User actor, boolean allowInstructor) {
        CourseReview review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đánh giá"));
        if (actor != null) {
            boolean isManager = hasRole(actor, "manager");
            boolean isInstructor = review.getInstructor() != null && Objects.equals(review.getInstructor().getId(), actor.getId());
            if (!(isManager || (allowInstructor && isInstructor))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền duyệt đánh giá này");
            }
        }
        String normalized = normalizeStatus(status);
        review.setStatus(normalized);
        review.setAdminNote(truncate(clean(adminNote), 1000));
        return toDto(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public CourseReviewDtos.ReviewResponse findReviewOfStudent(User student, Long courseId) {
        if (student == null) return null;
        return reviewRepository.findByCourse_IdAndStudent_Id(courseId, student.getId())
                .map(this::toDto)
                .orElse(null);
    }

    private boolean hasRole(User user, String role) {
        if (user == null || user.getRoles() == null) return false;
        return user.getRoles().stream().anyMatch(r -> r.getCode() != null && r.getCode().equalsIgnoreCase(role));
    }

    private CourseReviewDtos.SummaryResponse buildSummary(CourseReviewRepository.CourseReviewStats stats, List<CourseReview> reviews) {
        CourseReviewDtos.SummaryResponse summary = new CourseReviewDtos.SummaryResponse();
        summary.total = stats != null ? stats.getTotal() : 0L;
        summary.courseAverage = stats != null ? round(stats.getCourseAverage()) : 0d;
        summary.instructorAverage = stats != null ? round(stats.getInstructorAverage()) : 0d;
        summary.supportAverage = stats != null ? round(stats.getSupportAverage()) : 0d;
        long recommendCount = stats != null && stats.getRecommendCount() != null ? stats.getRecommendCount() : 0L;
        summary.recommendRatio = (summary.total != null && summary.total > 0)
                ? round((double) recommendCount / summary.total * 5)
                : 0d;
        int[] buckets = new int[5];
        reviews.forEach(r -> {
            int score = r.getCourseScore() != null ? r.getCourseScore() : 0;
            if (score >= 1 && score <= 5) {
                buckets[score - 1]++;
            }
        });
        for (int value : buckets) {
            summary.histogram.add(value);
        }
        return summary;
    }

    private boolean isEnrolled(Long userId, Long courseId) {
        Number result = (Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM dbo.enrollments WHERE user_id = :uid AND course_id = :cid")
                .setParameter("uid", userId)
                .setParameter("cid", courseId)
                .getSingleResult();
        return result != null && result.intValue() > 0;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() > max ? value.substring(0, max) : value;
    }

    private int boundScore(Integer score) {
        int val = score == null ? 5 : score;
        if (val < 1) val = 1;
        if (val > 5) val = 5;
        return val;
    }

    private String normalizeStatus(String status) {
        String value = status == null ? "" : status.trim().toLowerCase();
        return switch (value) {
            case "approved", "reject", "rejected" -> value.startsWith("reject") ? "rejected" : "approved";
            case "pending" -> "pending";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái không hợp lệ");
        };
    }

    private double round(Double value) {
        if (value == null) return 0d;
        return Math.round(value * 10.0) / 10.0;
    }

    private CourseReviewDtos.ReviewResponse toDto(CourseReview review) {
        CourseReviewDtos.ReviewResponse dto = new CourseReviewDtos.ReviewResponse();
        dto.id = review.getId();
        dto.courseId = review.getCourse() != null ? review.getCourse().getId() : null;
        dto.courseTitle = review.getCourse() != null ? review.getCourse().getTitle() : null;
        dto.instructorId = review.getInstructor() != null ? review.getInstructor().getId() : null;
        dto.instructorName = review.getInstructor() != null ? review.getInstructor().getFullName() : null;
        dto.studentId = review.getStudent() != null ? review.getStudent().getId() : null;
        dto.studentName = review.getStudent() != null ? review.getStudent().getFullName() : null;
        dto.courseScore = review.getCourseScore();
        dto.instructorScore = review.getInstructorScore();
        dto.supportScore = review.getSupportScore();
        dto.wouldRecommend = review.getWouldRecommend();
        dto.comment = review.getComment();
        dto.highlight = review.getHighlight();
        dto.improvement = review.getImprovement();
        dto.status = review.getStatus();
        dto.adminNote = review.getAdminNote();
        dto.createdAt = review.getCreatedAt();
        dto.updatedAt = review.getUpdatedAt();
        return dto;
    }
}
