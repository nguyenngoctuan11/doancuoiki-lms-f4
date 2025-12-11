package com.example.back_end.service;

import com.example.back_end.dto.ExamDtos;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudentExamService {
    @PersistenceContext private EntityManager em;
    private final StudentProgressService studentProgressService;
    private static final int HARD_MAX_ATTEMPTS = 2;

    public StudentExamService(StudentProgressService studentProgressService) {
        this.studentProgressService = studentProgressService;
    }
    @Transactional(readOnly = true)
    public ExamDtos.ExamOverview loadOverview(Long userId, Long courseId, Long examId) {
        QuizInfo quiz = requireQuiz(courseId, examId);
        ExamAccess access = evaluateAccess(quiz, userId);
        QuestionStats stats = loadQuestionStats(examId);

        ExamDtos.ExamOverview dto = new ExamDtos.ExamOverview();
        dto.examId = examId;
        dto.courseId = courseId;
        dto.courseSlug = quiz.courseSlug;
        dto.courseTitle = quiz.courseTitle;
        dto.title = quiz.title;
        dto.instructions = quiz.instructions;
        dto.reviewPolicy = quiz.reviewPolicy;
        dto.timeLimitSec = quiz.timeLimitSec;
        dto.questionCount = stats.count;
        dto.maxAttempts = HARD_MAX_ATTEMPTS;
        dto.attemptsUsed = access.attemptCount;
        dto.attemptsRemaining = Math.max(0, HARD_MAX_ATTEMPTS - access.attemptCount);
        dto.passingScore = quiz.passingScore;
        dto.windowStart = quiz.windowStart;
        dto.windowEnd = quiz.windowEnd;
        dto.autoSubmitGraceSec = quiz.autoSubmitGrace;
        dto.retakeCooldownMinutes = quiz.retakeCooldownMinutes;
        dto.blockers = access.blockers;
        dto.prerequisites = access.prerequisites;
        dto.activeAttemptId = access.activeAttemptId;
        dto.enrolled = access.enrolled;
        dto.canAttempt = access.canAttempt;
        dto.lastAttemptFinishedAt = access.lastFinishedAt;
        return dto;
    }

    private QuizInfo requireQuiz(Long courseId, Long examId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                        "SELECT q.id, q.course_id, q.lesson_id, q.title, q.time_limit_sec, q.shuffle, q.max_attempts, q.passing_score, " +
                                "q.instructions, q.review_policy, q.attempt_window_start, q.attempt_window_end, q.auto_submit_grace_sec, q.retake_cooldown_minutes, " +
                                "c.slug, c.title " +
                                "FROM dbo.quizzes q JOIN dbo.courses c ON c.id = q.course_id WHERE q.id = :examId AND q.course_id = :courseId")
                .setParameter("examId", examId)
                .setParameter("courseId", courseId)
                .getResultList();
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm th?y bài ki?m tra");
        }
        Object[] r = rows.get(0);
        QuizInfo info = new QuizInfo();
        info.id = ((Number) r[0]).longValue();
        info.courseId = ((Number) r[1]).longValue();
        info.lessonId = r[2] != null ? ((Number) r[2]).longValue() : null;
        info.title = r[3] != null ? r[3].toString() : "";
        info.timeLimitSec = r[4] != null ? ((Number) r[4]).intValue() : null;
        info.shuffle = toBool(r[5]);
        info.maxAttempts = HARD_MAX_ATTEMPTS;
        info.passingScore = r[7] != null ? toDouble(r[7]) : 50.0;
        info.instructions = r[8] != null ? r[8].toString() : null;
        info.reviewPolicy = r[9] != null ? r[9].toString() : "score_only";
        info.windowStart = toDateTime(r[10]);
        info.windowEnd = toDateTime(r[11]);
        info.autoSubmitGrace = r[12] != null ? ((Number) r[12]).intValue() : 0;
        info.retakeCooldownMinutes = r[13] != null ? ((Number) r[13]).intValue() : null;
        info.courseSlug = r[14] != null ? r[14].toString() : null;
        info.courseTitle = r[15] != null ? r[15].toString() : null;
        return info;
    }
    @Transactional(readOnly = true)
    public List<ExamDtos.ExamSummary> listCourseExams(Long courseId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                        "SELECT q.id, q.course_id, q.title, q.time_limit_sec, q.max_attempts, q.passing_score, " +
                                "(SELECT COUNT(*) FROM dbo.questions WHERE quiz_id = q.id) AS question_count " +
                                "FROM dbo.quizzes q WHERE q.course_id = :cid ORDER BY q.id")
                .setParameter("cid", courseId)
                .getResultList();
        List<ExamDtos.ExamSummary> list = new ArrayList<>();
        for (Object[] row : rows) {
            ExamDtos.ExamSummary summary = new ExamDtos.ExamSummary();
            summary.id = ((Number) row[0]).longValue();
            summary.courseId = ((Number) row[1]).longValue();
            summary.title = row[2] != null ? row[2].toString() : "";
            summary.timeLimitSec = row[3] != null ? ((Number) row[3]).intValue() : null;
            summary.maxAttempts = HARD_MAX_ATTEMPTS;
            summary.passingScore = row[5] != null ? toDouble(row[5]) : null;
            summary.questionCount = row[6] != null ? ((Number) row[6]).intValue() : 0;
            list.add(summary);
        }
        return list;
    }

    private ExamAccess evaluateAccess(QuizInfo quiz, Long userId) {
        ExamAccess result = new ExamAccess();
        result.blockers = new ArrayList<>();
        result.prerequisites = loadPrerequisites(quiz.courseId, userId);
        String enrollmentStatus = getEnrollmentStatus(userId, quiz.courseId);
        result.enrolled = enrollmentStatus != null;

        if (!result.enrolled) {
            result.blockers.add("B?n c?n ghi danh khóa h?c tru?c");
        } else if ("locked".equalsIgnoreCase(enrollmentStatus)) {
            result.blockers.add("Khóa h?c dã b? khóa do không d?t sau 2 l?n thi. Vui lòng dang ký l?i.");
        }

        LocalDateTime now = nowUtc();
        if (quiz.windowStart != null && now.isBefore(quiz.windowStart)) {
            result.blockers.add("Bài ki?m tra chua m?");
        }
        if (quiz.windowEnd != null && now.isAfter(quiz.windowEnd)) {
            result.blockers.add("Bài ki?m tra dã dóng");
        }

        int attemptsRaw = ((Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM dbo.quiz_attempts WHERE quiz_id = :qid AND user_id = :uid")
                .setParameter("qid", quiz.id)
                .setParameter("uid", userId)
                .getSingleResult()).intValue();
        result.attemptCount = Math.min(attemptsRaw, HARD_MAX_ATTEMPTS);

        @SuppressWarnings("unchecked")
        List<Number> activeAttemptIds = em.createNativeQuery(
                        "SELECT TOP 1 id FROM dbo.quiz_attempts WHERE quiz_id = :qid AND user_id = :uid AND status = N'in_progress' ORDER BY started_at DESC")
                .setParameter("qid", quiz.id)
                .setParameter("uid", userId)
                .getResultList();
        result.activeAttemptId = activeAttemptIds.stream()
                .map(Number::longValue)
                .findFirst()
                .orElse(null);

        @SuppressWarnings("unchecked")
        List<Object> lastFinishedRows = em.createNativeQuery(
                        "SELECT TOP 1 COALESCE(finished_at, ends_at) FROM dbo.quiz_attempts WHERE quiz_id = :qid AND user_id = :uid " +
                                "AND status IN (N'submitted', N'graded') ORDER BY finished_at DESC, ends_at DESC")
                .setParameter("qid", quiz.id)
                .setParameter("uid", userId)
                .getResultList();
        result.lastFinishedAt = lastFinishedRows.stream()
                .map(StudentExamService::toDateTime)
                .findFirst()
                .orElse(null);

        if (result.attemptCount >= HARD_MAX_ATTEMPTS) {
            result.blockers.add("Ðã hết lượt làm bài");
        }
        if (result.prerequisites.stream().anyMatch(p -> !p.met)) {
            result.blockers.add("chưa đạt điều kiện tiên quyết");
        }
        if (quiz.retakeCooldownMinutes != null && result.lastFinishedAt != null) {
            LocalDateTime allowAt = result.lastFinishedAt.plusMinutes(quiz.retakeCooldownMinutes);
            if (now.isBefore(allowAt)) {
                result.blockers.add("Cần ôn tập trước khi thi lại");
            }
        }

        result.canAttempt = result.blockers.isEmpty();
        return result;
    }

    private QuestionStats loadQuestionStats(Long examId) {
        Object[] row = (Object[]) em.createNativeQuery(
                        "SELECT COUNT(*), COALESCE(SUM(points),0) FROM dbo.questions WHERE quiz_id = :qid")
                .setParameter("qid", examId)
                .getSingleResult();
        QuestionStats stats = new QuestionStats();
        stats.count = row[0] != null ? ((Number) row[0]).intValue() : 0;
        stats.totalPoints = row[1] != null ? toDouble(row[1]) : 0;
        return stats;
    }
    @Transactional
    public ExamDtos.AttemptView createOrResumeAttempt(Long userId, Long courseId, Long examId, Long resumeAttemptId) {
        QuizInfo quiz = requireQuiz(courseId, examId);
        if (resumeAttemptId != null) {
            AttemptContext resume = loadAttemptContext(resumeAttemptId, userId);
            return buildAttemptView(resume);
        }

        ExamAccess access = evaluateAccess(quiz, userId);
        if (!access.canAttempt) {
            String reason = access.blockers.isEmpty() ? "Không đủ điều kiện để thi" : access.blockers.get(0);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
        }

        List<QuestionRecord> questions = loadQuestions(examId);
        if (questions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ðề thi chưa có câu hỏi");
        }

        int timeLimit = quiz.timeLimitSec != null && quiz.timeLimitSec > 0
                ? quiz.timeLimitSec
                : Math.max(questions.size() * 60, 300);

        int seed = (int) (System.currentTimeMillis() & 0x7fffffff);
        List<QuestionRecord> ordered = new ArrayList<>(questions);
        if (quiz.shuffle) {
            Collections.shuffle(ordered, new Random(seed));
            ordered.forEach(q -> Collections.shuffle(q.options, new Random(seed + q.id.intValue())));
        }

        double maxPoints = ordered.stream()
                .mapToDouble(q -> q.points != null ? q.points : 1)
                .sum();

        Long attemptId = insertAttempt(quiz, userId, timeLimit, seed, maxPoints);
        persistAttemptItems(attemptId, ordered);
        AttemptContext context = loadAttemptContext(attemptId, userId);
        return buildAttemptView(context);
    }

    private List<QuestionRecord> loadQuestions(Long examId) {
        @SuppressWarnings("unchecked")
        List<Object[]> qrows = em.createNativeQuery(
                        "SELECT id, text, points FROM dbo.questions WHERE quiz_id = :qid ORDER BY sort_order, id")
                .setParameter("qid", examId)
                .getResultList();
        Map<Long, QuestionRecord> questions = new LinkedHashMap<>();
        for (Object[] row : qrows) {
            QuestionRecord q = new QuestionRecord();
            q.id = ((Number) row[0]).longValue();
            q.text = row[1] != null ? row[1].toString() : "";
            q.points = row[2] != null ? ((Number) row[2]).intValue() : 1;
            q.options = new ArrayList<>();
            questions.put(q.id, q);
        }
        if (questions.isEmpty()) return List.of();

        @SuppressWarnings("unchecked")
        List<Object[]> orows = em.createNativeQuery(
                        "SELECT id, question_id, text, is_correct FROM dbo.question_options WHERE question_id IN :ids ORDER BY sort_order, id")
                .setParameter("ids", questions.keySet())
                .getResultList();
        for (Object[] row : orows) {
            Long qid = ((Number) row[1]).longValue();
            QuestionRecord q = questions.get(qid);
            if (q == null) continue;
            OptionRecord opt = new OptionRecord();
            opt.id = ((Number) row[0]).longValue();
            opt.text = row[2] != null ? row[2].toString() : "";
            opt.correct = toBool(row[3]);
            q.options.add(opt);
        }
        return new ArrayList<>(questions.values());
    }

    private Long insertAttempt(QuizInfo quiz, Long userId, int timeLimitSec, int seed, double maxPoints) {
        Object res = em.createNativeQuery(
                        "INSERT INTO dbo.quiz_attempts(quiz_id, user_id, started_at, status, time_limit_sec, ends_at, seed, max_points) " +
                                "OUTPUT inserted.id VALUES (:qid, :uid, SYSUTCDATETIME(), N'in_progress', :limit, DATEADD(SECOND, :limit, SYSUTCDATETIME()), :seed, :max)")
                .setParameter("qid", quiz.id)
                .setParameter("uid", userId)
                .setParameter("limit", timeLimitSec)
                .setParameter("seed", seed)
                .setParameter("max", BigDecimal.valueOf(maxPoints))
                .getSingleResult();
        return ((Number) res).longValue();
    }

    private void persistAttemptItems(Long attemptId, List<QuestionRecord> ordered) {
        int order = 0;
        for (QuestionRecord q : ordered) {
            order++;
            String optionOrder = q.options.stream()
                    .map(o -> String.valueOf(o.id))
                    .collect(Collectors.joining(","));
            em.createNativeQuery(
                            "INSERT INTO dbo.quiz_attempt_items(attempt_id, question_id, display_order, option_order) VALUES (:aid, :qid, :ord, :opts)")
                    .setParameter("aid", attemptId)
                    .setParameter("qid", q.id)
                    .setParameter("ord", order)
                    .setParameter("opts", optionOrder)
                    .executeUpdate();
        }
    }

    @Transactional(readOnly = true)
    public ExamDtos.AttemptView loadAttempt(Long userId, Long attemptId) {
        AttemptContext context = loadAttemptContext(attemptId, userId);
        return buildAttemptView(context);
    }
    @Transactional
    public void saveAnswer(Long userId, Long attemptId, ExamDtos.AnswerUpdateRequest request) {
        if (request == null || request.questionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thi?u câu h?i");
        }
        AttemptContext ctx = loadAttemptContext(attemptId, userId);
        if (!"in_progress".equalsIgnoreCase(ctx.status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bài làm dã k?t thúc");
        }
        ensureQuestionBelongs(attemptId, request.questionId);
        if (request.selectedOptionId != null) {
            ensureOptionBelongs(request.questionId, request.selectedOptionId);
        }
        upsertAnswer(attemptId, request.questionId, request.selectedOptionId, Boolean.TRUE.equals(request.markedForReview));
        updateAttemptTracking(attemptId, request.lastSeenQuestionId);
    }

    private void ensureQuestionBelongs(Long attemptId, Long questionId) {
        Number count = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM dbo.quiz_attempt_items WHERE attempt_id = :aid AND question_id = :qid")
                .setParameter("aid", attemptId)
                .setParameter("qid", questionId)
                .getSingleResult();
        if (count == null || count.intValue() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Câu h?i không thu?c attempt");
        }
    }

    private void ensureOptionBelongs(Long questionId, Long optionId) {
        Number count = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM dbo.question_options WHERE id = :oid AND question_id = :qid")
                .setParameter("oid", optionId)
                .setParameter("qid", questionId)
                .getSingleResult();
        if (count == null || count.intValue() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ðáp án không hợp lệ");
        }
    }

    private void upsertAnswer(Long attemptId, Long questionId, Long optionId, boolean marked) {
        int updated = em.createNativeQuery(
                        "UPDATE dbo.quiz_answers SET selected_option_id = :opt, marked_for_review = :marked WHERE attempt_id = :aid AND question_id = :qid")
                .setParameter("opt", optionId)
                .setParameter("marked", marked ? 1 : 0)
                .setParameter("aid", attemptId)
                .setParameter("qid", questionId)
                .executeUpdate();
        if (updated == 0) {
            em.createNativeQuery(
                            "INSERT INTO dbo.quiz_answers(attempt_id, question_id, selected_option_id, marked_for_review) VALUES (:aid, :qid, :opt, :marked)")
                    .setParameter("aid", attemptId)
                    .setParameter("qid", questionId)
                    .setParameter("opt", optionId)
                    .setParameter("marked", marked ? 1 : 0)
                    .executeUpdate();
        }
    }

    private void updateAttemptTracking(Long attemptId, Long lastSeenQuestionId) {
        em.createNativeQuery(
                        "UPDATE dbo.quiz_attempts SET last_seen_question_id = COALESCE(:lastSeen, last_seen_question_id), last_saved_at = SYSUTCDATETIME() WHERE id = :id")
                .setParameter("lastSeen", lastSeenQuestionId)
                .setParameter("id", attemptId)
                .executeUpdate();
    }
    @Transactional
    public ExamDtos.SubmitResponse submitAttempt(Long userId, Long attemptId) {
        AttemptContext ctx = loadAttemptContext(attemptId, userId);
        if (!"in_progress".equalsIgnoreCase(ctx.status)) {
            return buildSubmitResponse(ctx);
        }
        List<QuestionRecord> questions = loadQuestions(ctx.quizId);
        Map<Long, AnswerState> answers = loadAnswers(attemptId);

        double totalPoints = 0;
        for (QuestionRecord q : questions) {
            AnswerState state = answers.get(q.id);
            boolean correct = state != null && state.selectedOptionId != null &&
                    q.options.stream().anyMatch(o -> o.id.equals(state.selectedOptionId) && o.correct);
            double awarded = correct ? (q.points != null ? q.points : 1) : 0;
            totalPoints += awarded;
            upsertAnswerResult(attemptId, q.id, correct, awarded);
        }
        double maxPoints = ctx.maxPoints != null ? ctx.maxPoints :
                questions.stream().mapToDouble(q -> q.points != null ? q.points : 1).sum();
        double scorePercent = maxPoints > 0 ? (totalPoints / maxPoints) * 100.0 : 0;
        boolean passed = scorePercent >= ctx.passingScore;

        em.createNativeQuery(
                        "UPDATE dbo.quiz_attempts SET status = N'graded', finished_at = SYSUTCDATETIME(), graded_at = SYSUTCDATETIME(), " +
                                "score = :score, passed = :passed, total_points = :total, max_points = :max WHERE id = :id")
                .setParameter("score", BigDecimal.valueOf(scorePercent))
                .setParameter("passed", passed ? 1 : 0)
                .setParameter("total", BigDecimal.valueOf(totalPoints))
                .setParameter("max", BigDecimal.valueOf(maxPoints))
                .setParameter("id", attemptId)
                .executeUpdate();

        int attemptsUsed = ((Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM dbo.quiz_attempts WHERE quiz_id = :qid AND user_id = :uid")
                .setParameter("qid", ctx.quizId)
                .setParameter("uid", userId)
                .getSingleResult()).intValue();

        boolean locked = false;
        if (passed) {
            em.createNativeQuery(
                            "UPDATE dbo.enrollments SET status = N'active' WHERE user_id = :uid AND course_id = :cid")
                    .setParameter("uid", userId)
                    .setParameter("cid", ctx.courseId)
                    .executeUpdate();
            issueCertificate(ctx.courseId, userId, attemptId);
            if (ctx.lessonId != null) {
                studentProgressService.markLessonCompleted(userId, ctx.lessonId);
            }
        } else {
            if (attemptsUsed >= HARD_MAX_ATTEMPTS) {
                locked = true;
                em.createNativeQuery(
                                "UPDATE dbo.enrollments SET status = N'revoked' WHERE user_id = :uid AND course_id = :cid")
                        .setParameter("uid", userId)
                        .setParameter("cid", ctx.courseId)
                        .executeUpdate();
            }
        }

        AttemptContext refreshed = loadAttemptContext(attemptId, userId);
        ExamDtos.SubmitResponse dto = buildSubmitResponse(refreshed);
        dto.attemptsUsed = attemptsUsed;
        dto.attemptsAllowed = HARD_MAX_ATTEMPTS;
        dto.attemptsRemaining = Math.max(0, HARD_MAX_ATTEMPTS - attemptsUsed);
        dto.locked = locked;
        if (passed) {
            dto.message = "Chuc mung! Ban da hoan thanh bai kiem tra.";
        } else if (attemptsUsed == 1) {
            dto.message = "Ban chua du diem de hoan thanh khoa hoc, ban con 1 lan thu. Hay on tap lai.";
        } else if (locked) {
            dto.message = "Ban da het luot lam bai. Khoa hoc da bi khoa. Muon hoc lai, vui long dang ky khoa hoc.";
        }
        return dto;
    }

    private void upsertAnswerResult(Long attemptId, Long questionId, boolean correct, double points) {
        em.createNativeQuery(
                        "UPDATE dbo.quiz_answers SET is_correct = :correct, points_awarded = :points WHERE attempt_id = :aid AND question_id = :qid")
                .setParameter("correct", correct ? 1 : 0)
                .setParameter("points", BigDecimal.valueOf(points))
                .setParameter("aid", attemptId)
                .setParameter("qid", questionId)
                .executeUpdate();
    }

    private void issueCertificate(Long courseId, Long userId, Long attemptId) {
        em.createNativeQuery(
                        "IF NOT EXISTS (SELECT 1 FROM dbo.course_certificates WHERE course_id = :cid AND user_id = :uid) " +
                                "INSERT INTO dbo.course_certificates(course_id, user_id, attempt_id) VALUES (:cid, :uid, :aid)")
                .setParameter("cid", courseId)
                .setParameter("uid", userId)
                .setParameter("aid", attemptId)
                .executeUpdate();
    }

    private ExamDtos.SubmitResponse buildSubmitResponse(AttemptContext ctx) {
        ExamDtos.SubmitResponse dto = new ExamDtos.SubmitResponse();
        dto.attemptId = ctx.attemptId;
        dto.status = ctx.status;
        dto.finishedAt = ctx.finishedAt;
        dto.gradedAt = ctx.gradedAt;
        dto.scorePercent = ctx.score;
        dto.totalPoints = ctx.totalPoints;
        dto.maxPoints = ctx.maxPoints;
        dto.passed = ctx.passed;
        dto.passingScore = ctx.passingScore;
        dto.autoSubmitted = false;
        return dto;
    }
    private AttemptContext loadAttemptContext(Long attemptId, Long userId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                        "SELECT a.id, a.quiz_id, a.user_id, a.status, a.started_at, a.ends_at, a.finished_at, a.graded_at, " +
                                "a.last_seen_question_id, a.time_limit_sec, a.score, a.passed, a.max_points, a.total_points, " +
                                "q.course_id, q.lesson_id, q.title, q.passing_score, q.review_policy " +
                                "FROM dbo.quiz_attempts a JOIN dbo.quizzes q ON q.id = a.quiz_id WHERE a.id = :id")
                .setParameter("id", attemptId)
                .getResultList();
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy attempt");
        }
        Object[] r = rows.get(0);
        Long owner = ((Number) r[2]).longValue();
        if (!Objects.equals(owner, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không được phép truy cập attempt này");
        }
        AttemptContext ctx = new AttemptContext();
        ctx.attemptId = ((Number) r[0]).longValue();
        ctx.quizId = ((Number) r[1]).longValue();
        ctx.userId = owner;
        ctx.status = r[3].toString();
        ctx.startedAt = toDateTime(r[4]);
        ctx.endsAt = toDateTime(r[5]);
        ctx.finishedAt = toDateTime(r[6]);
        ctx.gradedAt = toDateTime(r[7]);
        ctx.lastSeenQuestionId = r[8] != null ? ((Number) r[8]).longValue() : null;
        ctx.timeLimitSec = r[9] != null ? ((Number) r[9]).intValue() : null;
        ctx.score = r[10] != null ? toDouble(r[10]) : null;
        ctx.passed = r[11] != null && toBool(r[11]);
        ctx.maxPoints = r[12] != null ? toDouble(r[12]) : null;
        ctx.totalPoints = r[13] != null ? toDouble(r[13]) : null;
        ctx.courseId = ((Number) r[14]).longValue();
        ctx.lessonId = r[15] != null ? ((Number) r[15]).longValue() : null;
        ctx.examTitle = r[16] != null ? r[16].toString() : "";
        ctx.passingScore = r[17] != null ? toDouble(r[17]) : 50.0;
        ctx.reviewPolicy = r[18] != null ? r[18].toString() : "score_only";
        return ctx;
    }

    private ExamDtos.AttemptView buildAttemptView(AttemptContext ctx) {
        ExamDtos.AttemptView view = new ExamDtos.AttemptView();
        view.attemptId = ctx.attemptId;
        view.examId = ctx.quizId;
        view.courseId = ctx.courseId;
        view.examTitle = ctx.examTitle;
        view.timeLimitSec = ctx.timeLimitSec;
        view.startAt = ctx.startedAt;
        view.endAt = ctx.endsAt;
        view.finishedAt = ctx.finishedAt;
        view.gradedAt = ctx.gradedAt;
        view.score = ctx.score;
        view.passed = ctx.passed;
        view.passingScore = ctx.passingScore;
        view.status = ctx.status;
        view.countdownSec = remainingSeconds(ctx);
        if (ctx.timeLimitSec != null && view.countdownSec > ctx.timeLimitSec) {
            view.countdownSec = ctx.timeLimitSec;
        }
        view.lastSeenQuestionId = ctx.lastSeenQuestionId;
        boolean review = !"score_only".equalsIgnoreCase(ctx.reviewPolicy);
        if ("in_progress".equalsIgnoreCase(ctx.status) || review) {
            view.questions = buildAttemptQuestions(ctx.attemptId, review);
            view.questionCount = view.questions.size();
        } else {
            view.questions = List.of();
            view.questionCount = 0;
        }
        view.reviewEnabled = review;
        return view;
    }

    private List<ExamDtos.AttemptQuestion> buildAttemptQuestions(Long attemptId, boolean revealAnswers) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                        "SELECT i.question_id, i.display_order, i.option_order, q.text, q.points " +
                                "FROM dbo.quiz_attempt_items i JOIN dbo.questions q ON q.id = i.question_id WHERE i.attempt_id = :aid ORDER BY i.display_order")
                .setParameter("aid", attemptId)
                .getResultList();

        Map<Long, List<ExamDtos.AttemptOption>> optionMap = loadAttemptOptions(rows, revealAnswers);
        Map<Long, AnswerState> answers = loadAnswers(attemptId);

        List<ExamDtos.AttemptQuestion> list = new ArrayList<>();
        for (Object[] row : rows) {
            Long qid = ((Number) row[0]).longValue();
            ExamDtos.AttemptQuestion q = new ExamDtos.AttemptQuestion();
            q.id = qid;
            q.text = row[3] != null ? row[3].toString() : "";
            q.points = row[4] != null ? ((Number) row[4]).intValue() : 1;
            q.options = optionMap.getOrDefault(qid, List.of());
            AnswerState st = answers.get(qid);
            q.selectedOptionId = st != null ? st.selectedOptionId : null;
            q.markedForReview = st != null && st.markedForReview;
            if (revealAnswers) {
                q.correctOptionId = q.options.stream()
                        .filter(opt -> Boolean.TRUE.equals(opt.correct))
                        .map(opt -> opt.id)
                        .findFirst()
                        .orElse(null);
                if (st != null) {
                    q.answeredCorrectly = q.correctOptionId != null && Objects.equals(q.correctOptionId, st.selectedOptionId);
                } else {
                    q.answeredCorrectly = Boolean.FALSE;
                }
            }
            list.add(q);
        }
        return list;
    }

    private Map<Long, List<ExamDtos.AttemptOption>> loadAttemptOptions(List<Object[]> rows, boolean revealAnswers) {
        Set<Long> questionIds = rows.stream()
                .map(r -> ((Number) r[0]).longValue())
                .collect(Collectors.toSet());
        if (questionIds.isEmpty()) return Map.of();

        @SuppressWarnings("unchecked")
        List<Object[]> options = em.createNativeQuery(
                        "SELECT id, question_id, text, is_correct FROM dbo.question_options WHERE question_id IN :ids")
                .setParameter("ids", questionIds)
                .getResultList();
        Map<Long, ExamDtos.AttemptOption> optionById = new HashMap<>();
        Map<Long, List<ExamDtos.AttemptOption>> byQuestion = new HashMap<>();
        for (Object[] opt : options) {
            Long id = ((Number) opt[0]).longValue();
            Long qid = ((Number) opt[1]).longValue();
            ExamDtos.AttemptOption dto = new ExamDtos.AttemptOption();
            dto.id = id;
            dto.text = opt[2] != null ? opt[2].toString() : "";
            if (revealAnswers) {
                dto.correct = opt[3] != null && toBool(opt[3]);
            } else {
                dto.correct = null;
            }
            optionById.put(id, dto);
            byQuestion.computeIfAbsent(qid, k -> new ArrayList<>()).add(dto);
        }
        for (Object[] row : rows) {
            Long qid = ((Number) row[0]).longValue();
            String order = row[2] != null ? row[2].toString() : "";
            if (order.isBlank()) continue;
            List<Long> ids = Arrays.stream(order.split(","))
                    .filter(s -> !s.isBlank())
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
            List<ExamDtos.AttemptOption> reordered = new ArrayList<>();
            for (Long id : ids) {
                ExamDtos.AttemptOption opt = optionById.get(id);
                if (opt != null) reordered.add(opt);
            }
            byQuestion.put(qid, reordered);
        }
        return byQuestion;
    }
    private Map<Long, AnswerState> loadAnswers(Long attemptId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                        "SELECT question_id, selected_option_id, marked_for_review FROM dbo.quiz_answers WHERE attempt_id = :aid")
                .setParameter("aid", attemptId)
                .getResultList();
        Map<Long, AnswerState> map = new HashMap<>();
        for (Object[] row : rows) {
            AnswerState st = new AnswerState();
            st.questionId = ((Number) row[0]).longValue();
            st.selectedOptionId = row[1] != null ? ((Number) row[1]).longValue() : null;
            st.markedForReview = row[2] != null && toBool(row[2]);
            map.put(st.questionId, st);
        }
        return map;
    }

    private int remainingSeconds(AttemptContext ctx) {
        if (ctx.endsAt == null) return ctx.timeLimitSec != null ? ctx.timeLimitSec : 0;
        long seconds = java.time.Duration.between(nowUtc(), ctx.endsAt).getSeconds();
        return (int) Math.max(0, seconds);
    }

    private List<ExamDtos.PrerequisiteStatus> loadPrerequisites(Long courseId, Long userId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                        "SELECT p.required_course_id, c.title, " +
                                "CASE WHEN EXISTS (SELECT 1 FROM dbo.course_certificates cert WHERE cert.course_id = p.required_course_id AND cert.user_id = :uid) THEN 1 ELSE 0 END AS met " +
                                "FROM dbo.prerequisites p JOIN dbo.courses c ON c.id = p.required_course_id WHERE p.course_id = :cid")
                .setParameter("cid", courseId)
                .setParameter("uid", userId)
                .getResultList();
        List<ExamDtos.PrerequisiteStatus> list = new ArrayList<>();
        for (Object[] r : rows) {
            ExamDtos.PrerequisiteStatus status = new ExamDtos.PrerequisiteStatus();
            status.courseId = ((Number) r[0]).longValue();
            status.courseTitle = r[1] != null ? r[1].toString() : "";
            status.met = toBool(r[2]);
            list.add(status);
        }
        return list;
    }

    private String getEnrollmentStatus(Long userId, Long courseId) {
        @SuppressWarnings("unchecked")
        List<Object> rows = em.createNativeQuery(
                        "SELECT TOP 1 status FROM dbo.enrollments WHERE user_id = :uid AND course_id = :cid")
                .setParameter("uid", userId)
                .setParameter("cid", courseId)
                .getResultList();
        return rows.isEmpty() ? null : (rows.get(0) != null ? rows.get(0).toString() : null);
    }

    private static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private static boolean toBool(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.intValue() != 0;
        return Boolean.parseBoolean(value.toString());
    }

    private static double toDouble(Object value) {
        if (value == null) return 0;
        if (value instanceof BigDecimal bd) return bd.doubleValue();
        if (value instanceof Number n) return n.doubleValue();
        return Double.parseDouble(value.toString());
    }

    private static LocalDateTime toDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        if (value instanceof LocalDateTime dt) return dt;
        return LocalDateTime.parse(value.toString());
    }

    private static class QuizInfo {
        Long id;
        Long courseId;
        Long lessonId;
        String title;
        Integer timeLimitSec;
        boolean shuffle;
        int maxAttempts;
        double passingScore;
        String instructions;
        String reviewPolicy;
        LocalDateTime windowStart;
        LocalDateTime windowEnd;
        Integer autoSubmitGrace;
        Integer retakeCooldownMinutes;
        String courseSlug;
        String courseTitle;
    }

    private static class ExamAccess {
        boolean enrolled;
        boolean canAttempt;
        int attemptCount;
        Long activeAttemptId;
        LocalDateTime lastFinishedAt;
        List<String> blockers;
        List<ExamDtos.PrerequisiteStatus> prerequisites;
    }

    private static class QuestionStats {
        int count;
        double totalPoints;
    }

    private static class QuestionRecord {
        Long id;
        String text;
        Integer points;
        List<OptionRecord> options;
    }

    private static class OptionRecord {
        Long id;
        String text;
        boolean correct;
    }

    private static class AttemptContext {
        Long attemptId;
        Long quizId;
        Long courseId;
        Long lessonId;
        Long userId;
        String status;
        LocalDateTime startedAt;
        LocalDateTime endsAt;
        LocalDateTime finishedAt;
        LocalDateTime gradedAt;
        Long lastSeenQuestionId;
        Integer timeLimitSec;
        Double score;
        Double totalPoints;
        Double maxPoints;
        Boolean passed;
        double passingScore;
        String reviewPolicy;
        String examTitle;
    }

    private static class AnswerState {
        Long questionId;
        Long selectedOptionId;
        boolean markedForReview;
    }

    public static class CertificateInfo {
        public Long courseId;
        public Long userId;
        public Long attemptId;
        public boolean created;
    }

    public static class CertificateSummary {
        public Long id;
        public Long courseId;
        public String courseTitle;
        public Long attemptId;
        public Double scorePercent;
        public LocalDateTime issuedAt;
    }

    @Transactional
    public CertificateInfo ensureCertificate(Long userId, Long courseId) {
        @SuppressWarnings("unchecked")
        List<Object[]> existing = em.createNativeQuery(
                        "SELECT TOP 1 id, attempt_id FROM dbo.course_certificates WHERE course_id = :cid AND user_id = :uid ORDER BY id DESC")
                .setParameter("cid", courseId)
                .setParameter("uid", userId)
                .getResultList();
        if (!existing.isEmpty()) {
            Object[] r = existing.get(0);
            CertificateInfo info = new CertificateInfo();
            info.courseId = courseId;
            info.userId = userId;
            info.attemptId = r[1] != null ? ((Number) r[1]).longValue() : null;
            info.created = false;
            return info;
        }

        @SuppressWarnings("unchecked")
        List<Number> passedAttempts = em.createNativeQuery(
                        "SELECT TOP 1 a.id FROM dbo.quiz_attempts a " +
                                "JOIN dbo.quizzes q ON q.id = a.quiz_id " +
                                "WHERE q.course_id = :cid AND a.user_id = :uid AND a.passed = 1 " +
                                "ORDER BY a.graded_at DESC, a.finished_at DESC, a.id DESC")
                .setParameter("cid", courseId)
                .setParameter("uid", userId)
                .getResultList();
        if (passedAttempts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chua có bài ki?m tra d?t d? c?p ch?ng nh?n");
        }
        Long attemptId = passedAttempts.get(0).longValue();

        em.createNativeQuery(
                        "INSERT INTO dbo.course_certificates(course_id, user_id, attempt_id) VALUES (:cid, :uid, :aid)")
                .setParameter("cid", courseId)
                .setParameter("uid", userId)
                .setParameter("aid", attemptId)
                .executeUpdate();

        CertificateInfo info = new CertificateInfo();
        info.courseId = courseId;
        info.userId = userId;
        info.attemptId = attemptId;
        info.created = true;
        return info;
    }

    @Transactional(readOnly = true)
    public List<CertificateSummary> listCertificates(Long userId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                        "SELECT cert.id, cert.course_id, c.title, cert.attempt_id, a.score, " +
                                "COALESCE(a.graded_at, a.finished_at) AS issued_at " +
                                "FROM dbo.course_certificates cert " +
                                "JOIN dbo.courses c ON c.id = cert.course_id " +
                                "LEFT JOIN dbo.quiz_attempts a ON a.id = cert.attempt_id " +
                                "WHERE cert.user_id = :uid " +
                                "ORDER BY issued_at DESC, cert.id DESC")
                .setParameter("uid", userId)
                .getResultList();
        List<CertificateSummary> list = new ArrayList<>();
        for (Object[] r : rows) {
            CertificateSummary dto = new CertificateSummary();
            dto.id = r[0] != null ? ((Number) r[0]).longValue() : null;
            dto.courseId = r[1] != null ? ((Number) r[1]).longValue() : null;
            dto.courseTitle = r[2] != null ? r[2].toString() : "";
            dto.attemptId = r[3] != null ? ((Number) r[3]).longValue() : null;
            dto.scorePercent = r[4] != null ? toDouble(r[4]) : null;
            dto.issuedAt = toDateTime(r[5]);
            list.add(dto);
        }
        return list;
    }
}





