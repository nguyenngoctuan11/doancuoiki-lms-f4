package com.example.back_end.service;

import com.example.back_end.dto.TeacherAnalyticsDtos;
import com.example.back_end.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TeacherAnalyticsService {
    @PersistenceContext
    private EntityManager em;

    private static final DateTimeFormatter NOTE_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM");
    private static final String[] TREND_COLORS = new String[]{"#a855f7", "#10b981", "#f97316"};

    @Transactional(readOnly = true)
    public TeacherAnalyticsDtos.DashboardResponse buildDashboard(User user, int days) {
        TeacherAnalyticsDtos.DashboardResponse dto = new TeacherAnalyticsDtos.DashboardResponse();
        dto.name = user != null && user.getFullName() != null && !user.getFullName().isBlank()
                ? user.getFullName()
                : (user != null ? user.getEmail() : "Giảng viên");
        List<Long> courseIds = loadCourseIds(user != null ? user.getId() : null);
        if (CollectionUtils.isEmpty(courseIds)) {
            dto.summary = defaultSummary();
            dto.trends = defaultTrends(days);
            return dto;
        }
        String courseIn = joinIds(courseIds);
        SummaryStats summary = fetchSummary(courseIn);
        dto.summary = buildSummaryCards(summary, days);
        dto.hero.pending = summary.notesPending + summary.resourcesPending;
        dto.hero.resolved = summary.notesResolved;
        dto.courses = fetchCourseCards(courseIn);
        dto.interactions = fetchInteractions(courseIn);
        dto.reviews = fetchReviews(courseIn, days);
        dto.trends = fetchTrends(courseIn, days);
        return dto;
    }

    private List<Long> loadCourseIds(Long userId) {
        if (userId == null) return List.of();
        @SuppressWarnings("unchecked")
        List<Number> rows = em.createNativeQuery(
                        "SELECT DISTINCT c.id FROM dbo.courses c " +
                                "LEFT JOIN dbo.course_instructors ci ON ci.course_id = c.id " +
                                "WHERE c.created_by = :uid OR ci.user_id = :uid")
                .setParameter("uid", userId)
                .getResultList();
        return rows.stream().map(Number::longValue).collect(Collectors.toList());
    }

    private SummaryStats fetchSummary(String courseInClause) {
        SummaryStats stats = new SummaryStats();
        Number students = singleNumber("SELECT COUNT(DISTINCT user_id) FROM dbo.enrollments " +
                "WHERE status <> N'revoked' AND course_id IN (" + courseInClause + ")");
        Number activeCourses = singleNumber("SELECT COUNT(*) FROM dbo.courses " +
                "WHERE id IN (" + courseInClause + ") AND status = N'published'");
        Number avgProgress = singleNumber("SELECT AVG(CAST(lp.progress_percent AS FLOAT)) " +
                "FROM dbo.lesson_progress lp " +
                "JOIN dbo.lessons l ON l.id = lp.lesson_id " +
                "JOIN dbo.modules m ON m.id = l.module_id " +
                "WHERE m.course_id IN (" + courseInClause + ")");
        Number notesPending = singleNumber("SELECT COUNT(*) FROM dbo.lesson_notes ln " +
                "WHERE ln.course_id IN (" + courseInClause + ") " +
                "AND NOT EXISTS (SELECT 1 FROM dbo.lesson_note_comments c " +
                "WHERE c.note_id = ln.id AND LOWER(ISNULL(c.author_role,'')) IN (N'teacher', N'manager'))");
        Number noteTeacher = singleNumber("SELECT COUNT(*) FROM dbo.lesson_notes ln " +
                "WHERE ln.course_id IN (" + courseInClause + ") " +
                "AND EXISTS (SELECT 1 FROM dbo.lesson_note_comments c " +
                "WHERE c.note_id = ln.id AND LOWER(ISNULL(c.author_role,'')) IN (N'teacher', N'manager'))");
        Number noteAwaiting = singleNumber(
                "WITH t AS (" +
                        " SELECT ln.id, (" +
                        "   SELECT TOP 1 LOWER(ISNULL(c.author_role,'')) FROM dbo.lesson_note_comments c " +
                        "   WHERE c.note_id = ln.id ORDER BY c.created_at DESC" +
                        " ) AS last_role" +
                        " FROM dbo.lesson_notes ln " +
                        " WHERE ln.course_id IN (" + courseInClause + ") " +
                        "   AND EXISTS (SELECT 1 FROM dbo.lesson_note_comments c " +
                        "       WHERE c.note_id = ln.id AND LOWER(ISNULL(c.author_role,'')) IN (N'teacher',N'manager'))" +
                        ") SELECT COUNT(*) FROM t WHERE last_role = '' OR last_role = 'student'");
        Number resourcesPending = singleNumber("SELECT COUNT(*) FROM dbo.lesson_resources " +
                "WHERE course_id IN (" + courseInClause + ") AND status = N'pending'");
        Number reviewsPending = singleNumber("SELECT COUNT(*) FROM dbo.course_reviews " +
                "WHERE course_id IN (" + courseInClause + ") AND status = N'pending'");

        stats.studentsActive = students != null ? students.longValue() : 0L;
        stats.activeCourses = activeCourses != null ? activeCourses.longValue() : 0L;
        stats.avgProgress = avgProgress != null ? avgProgress.doubleValue() : 0d;
        stats.notesPending = notesPending != null ? notesPending.intValue() : 0;
        int teacherNotes = noteTeacher != null ? noteTeacher.intValue() : 0;
        int awaiting = noteAwaiting != null ? noteAwaiting.intValue() : 0;
        stats.notesResolved = Math.max(teacherNotes - awaiting, 0);
        stats.resourcesPending = resourcesPending != null ? resourcesPending.intValue() : 0;
        stats.reviewsPending = reviewsPending != null ? reviewsPending.intValue() : 0;
        return stats;
    }

    private List<TeacherAnalyticsDtos.SummaryCard> buildSummaryCards(SummaryStats stats, int days) {
        List<TeacherAnalyticsDtos.SummaryCard> cards = new ArrayList<>();
        cards.add(summaryCard("students", "Học viên đang học",
                formatNumber(stats.studentsActive), "Hoạt động trong " + days + " ngày qua"));
        cards.add(summaryCard("courses", "Khóa đang mở",
                formatNumber(stats.activeCourses), "Dựa trên trạng thái PUBLISHED"));
        long avg = Math.round(stats.avgProgress);
        cards.add(summaryCard("progress", "Tiến độ trung bình",
                avg + "%", "Tính trên tổng số bài học"));
        int pendingActions = stats.notesPending + stats.resourcesPending + stats.reviewsPending;
        cards.add(summaryCard("pending", "Mục cần xử lý",
                formatNumber(pendingActions), "Ghi chú, tài liệu, đánh giá"));
        return cards;
    }

    private TeacherAnalyticsDtos.SummaryCard summaryCard(String id, String label, String value, String diff) {
        TeacherAnalyticsDtos.SummaryCard card = new TeacherAnalyticsDtos.SummaryCard();
        card.id = id;
        card.label = label;
        card.value = value;
        card.diff = diff;
        return card;
    }

    private List<TeacherAnalyticsDtos.CourseCard> fetchCourseCards(String courseInClause) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                        "SELECT TOP 6 c.title, " +
                                "(SELECT COUNT(*) FROM dbo.enrollments e " +
                                " WHERE e.course_id = c.id AND e.status <> N'revoked') AS enrolled, " +
                                "(SELECT COUNT(*) FROM dbo.enrollments e " +
                                " WHERE e.course_id = c.id AND e.status IN (N'refunded', N'revoked')) AS dropouts, " +
                                "(SELECT AVG(CAST(lp.progress_percent AS FLOAT)) FROM dbo.lesson_progress lp " +
                                " JOIN dbo.lessons l ON l.id = lp.lesson_id " +
                                " JOIN dbo.modules m ON m.id = l.module_id " +
                                " WHERE m.course_id = c.id) AS avgProgress, " +
                                "(SELECT AVG(CAST((course_score + instructor_score + support_score) AS FLOAT)/3.0) " +
                                " FROM dbo.course_reviews r WHERE r.course_id = c.id AND r.status = N'approved') AS rating " +
                                "FROM dbo.courses c WHERE c.id IN (" + courseInClause + ") " +
                                "ORDER BY enrolled DESC")
                .getResultList();
        List<TeacherAnalyticsDtos.CourseCard> cards = new ArrayList<>();
        for (Object[] r : rows) {
            TeacherAnalyticsDtos.CourseCard card = new TeacherAnalyticsDtos.CourseCard();
            card.title = r[0] != null ? r[0].toString() : "Khóa học";
            card.enrolled = r[1] != null ? ((Number) r[1]).longValue() : 0L;
            double completion = r[3] != null ? ((Number) r[3]).doubleValue() : 0d;
            card.completion = (int) Math.round(completion);
            card.drop = r[2] != null ? ((Number) r[2]).longValue() : 0L;
            card.rating = r[4] != null ? roundOne(((Number) r[4]).doubleValue()) : 0d;
            cards.add(card);
        }
        return cards;
    }

    private TeacherAnalyticsDtos.InteractionBlock fetchInteractions(String courseInClause) {
        TeacherAnalyticsDtos.InteractionBlock block = new TeacherAnalyticsDtos.InteractionBlock();
        Number notesPending = singleNumber("SELECT COUNT(*) FROM dbo.lesson_notes ln " +
                "WHERE ln.course_id IN (" + courseInClause + ") " +
                "AND NOT EXISTS (SELECT 1 FROM dbo.lesson_note_comments c " +
                "WHERE c.note_id = ln.id AND LOWER(ISNULL(c.author_role,'')) IN (N'teacher', N'manager'))");
        Number noteTeacher = singleNumber("SELECT COUNT(*) FROM dbo.lesson_notes ln " +
                "WHERE ln.course_id IN (" + courseInClause + ") " +
                "AND EXISTS (SELECT 1 FROM dbo.lesson_note_comments c " +
                "WHERE c.note_id = ln.id AND LOWER(ISNULL(c.author_role,'')) IN (N'teacher', N'manager'))");
        Number noteAwaiting = singleNumber(
                "WITH t AS (" +
                        " SELECT ln.id, (" +
                        "   SELECT TOP 1 LOWER(ISNULL(c.author_role,'')) FROM dbo.lesson_note_comments c " +
                        "   WHERE c.note_id = ln.id ORDER BY c.created_at DESC" +
                        " ) AS last_role" +
                        " FROM dbo.lesson_notes ln " +
                        " WHERE ln.course_id IN (" + courseInClause + ") " +
                        "   AND EXISTS (SELECT 1 FROM dbo.lesson_note_comments c " +
                        "       WHERE c.note_id = ln.id AND LOWER(ISNULL(c.author_role,'')) IN (N'teacher',N'manager'))" +
                        ") SELECT COUNT(*) FROM t WHERE last_role = '' OR last_role = 'student'");
        block.notes.pending = notesPending != null ? notesPending.intValue() : 0;
        int teacherNotes = noteTeacher != null ? noteTeacher.intValue() : 0;
        int awaiting = noteAwaiting != null ? noteAwaiting.intValue() : 0;
        block.notes.awaiting = awaiting;
        block.notes.resolved = Math.max(teacherNotes - awaiting, 0);

        Number resPending = singleNumber("SELECT COUNT(*) FROM dbo.lesson_resources WHERE course_id IN (" + courseInClause + ") AND status = N'pending'");
        Number resApproved = singleNumber("SELECT COUNT(*) FROM dbo.lesson_resources WHERE course_id IN (" + courseInClause + ") AND status = N'approved'");
        Number resHidden = singleNumber("SELECT COUNT(*) FROM dbo.lesson_resources WHERE course_id IN (" + courseInClause + ") AND status = N'hidden'");
        block.resources.pending = resPending != null ? resPending.intValue() : 0;
        block.resources.approved = resApproved != null ? resApproved.intValue() : 0;
        block.resources.hidden = resHidden != null ? resHidden.intValue() : 0;

        Number supportOpen = singleNumber("SELECT COUNT(*) FROM dbo.support_threads WHERE course_id IN (" + courseInClause + ") " +
                "AND status IN (N'NEW', N'IN_PROGRESS', N'WAITING_STUDENT')");
        block.support.open = supportOpen != null ? supportOpen.intValue() : 0;
        Number avgMinutes = singleNumber("SELECT AVG(CASE WHEN last_student_activity_at IS NOT NULL AND last_manager_activity_at IS NOT NULL THEN " +
                "DATEDIFF(minute, last_student_activity_at, last_manager_activity_at) END) " +
                "FROM dbo.support_threads WHERE course_id IN (" + courseInClause + ") " +
                "AND last_manager_activity_at IS NOT NULL AND last_student_activity_at IS NOT NULL " +
                "AND DATEDIFF(minute, last_student_activity_at, last_manager_activity_at) >= 0");
        if (avgMinutes != null && avgMinutes.doubleValue() > 0) {
            block.support.avgResponse = "~" + Math.round(avgMinutes.doubleValue()) + " phút";
        } else {
            block.support.avgResponse = "Chưa có dữ liệu";
        }

        block.latestNotes = fetchLatestNotes(courseInClause);
        return block;
    }

    private List<TeacherAnalyticsDtos.NotePreview> fetchLatestNotes(String courseInClause) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                        "SELECT TOP 3 c.title, u.full_name, ln.content, ln.last_comment_at " +
                                "FROM dbo.lesson_notes ln " +
                                "JOIN dbo.courses c ON c.id = ln.course_id " +
                                "JOIN dbo.users u ON u.id = ln.student_id " +
                                "WHERE ln.course_id IN (" + courseInClause + ") " +
                                "ORDER BY ln.last_comment_at DESC")
                .getResultList();
        List<TeacherAnalyticsDtos.NotePreview> notes = new ArrayList<>();
        for (Object[] r : rows) {
            TeacherAnalyticsDtos.NotePreview preview = new TeacherAnalyticsDtos.NotePreview();
            preview.course = r[0] != null ? r[0].toString() : "";
            preview.student = r[1] != null ? r[1].toString() : "";
            preview.content = truncate(r[2] != null ? r[2].toString() : "", 120);
            if (r[3] instanceof LocalDateTime ts) {
                preview.at = NOTE_TIME.format(ts);
            } else if (r[3] instanceof java.sql.Timestamp ts) {
                preview.at = NOTE_TIME.format(ts.toLocalDateTime());
            } else {
                preview.at = "";
            }
            notes.add(preview);
        }
        return notes;
    }

    private List<TeacherAnalyticsDtos.ReviewCard> fetchReviews(String courseInClause, int days) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                        "SELECT TOP 5 c.title, " +
                                "AVG(CAST((r.course_score + r.instructor_score + r.support_score) AS FLOAT)/3.0) AS avgScore, " +
                                "AVG(CASE WHEN r.would_recommend = 1 THEN 100.0 ELSE 0 END) AS recommend, " +
                                "SUM(CASE WHEN r.created_at >= DATEADD(day, -" + days + ", SYSUTCDATETIME()) THEN 1 ELSE 0 END) AS newReviews " +
                                "FROM dbo.course_reviews r " +
                                "JOIN dbo.courses c ON c.id = r.course_id " +
                                "WHERE r.status = N'approved' AND r.course_id IN (" + courseInClause + ") " +
                                "GROUP BY c.title ORDER BY avgScore DESC")
                .getResultList();
        List<TeacherAnalyticsDtos.ReviewCard> list = new ArrayList<>();
        for (Object[] r : rows) {
            TeacherAnalyticsDtos.ReviewCard card = new TeacherAnalyticsDtos.ReviewCard();
            card.course = r[0] != null ? r[0].toString() : "Khóa học";
            card.avg = r[1] != null ? roundOne(((Number) r[1]).doubleValue()) : 0d;
            card.recommend = r[2] != null ? (int) Math.round(((Number) r[2]).doubleValue()) : 0;
            card.newReviews = r[3] != null ? ((Number) r[3]).intValue() : 0;
            list.add(card);
        }
        return list;
    }

    private List<TeacherAnalyticsDtos.TrendSeries> fetchTrends(String courseInClause, int days) {
        LocalDate to = LocalDate.now(ZoneOffset.UTC);
        LocalDate from = to.minusDays(days - 1L);
        Timestamp fromTs = Timestamp.valueOf(from.atStartOfDay());
        Timestamp toTs = Timestamp.valueOf(to.plusDays(1).atStartOfDay());

        Map<LocalDate, Integer> enrollMap = mapCounts("SELECT CONVERT(date, start_at) AS d, COUNT(*) FROM dbo.enrollments " +
                "WHERE course_id IN (" + courseInClause + ") AND start_at >= :from AND start_at < :to " +
                "GROUP BY CONVERT(date, start_at)", fromTs, toTs);
        Map<LocalDate, Integer> completionMap = mapCounts(
                "SELECT CONVERT(date, lp.completed_at) AS d, COUNT(*) FROM dbo.lesson_progress lp " +
                        "JOIN dbo.lessons l ON l.id = lp.lesson_id " +
                        "JOIN dbo.modules m ON m.id = l.module_id " +
                        "WHERE lp.completed_at IS NOT NULL AND lp.progress_percent = 100 " +
                        "AND m.course_id IN (" + courseInClause + ") " +
                        "AND lp.completed_at >= :from AND lp.completed_at < :to " +
                        "GROUP BY CONVERT(date, lp.completed_at)", fromTs, toTs);
        Map<LocalDate, Integer> reviewMap = mapCounts(
                "SELECT CONVERT(date, created_at) AS d, COUNT(*) FROM dbo.course_reviews " +
                        "WHERE status = N'approved' AND course_id IN (" + courseInClause + ") " +
                        "AND created_at >= :from AND created_at < :to " +
                        "GROUP BY CONVERT(date, created_at)", fromTs, toTs);

        List<Integer> enrollValues = buildSeries(from, to, enrollMap);
        List<Integer> completeValues = buildSeries(from, to, completionMap);
        List<Integer> reviewValues = buildSeries(from, to, reviewMap);

        List<TeacherAnalyticsDtos.TrendSeries> trends = new ArrayList<>();
        trends.add(trend("trendEnroll", "Đăng ký mới", enrollValues, TREND_COLORS[0]));
        trends.add(trend("trendCompletion", "Học viên hoàn thành", completeValues, TREND_COLORS[1]));
        trends.add(trend("trendReviews", "Đánh giá tích cực", reviewValues, TREND_COLORS[2]));
        return trends;
    }

    private List<TeacherAnalyticsDtos.TrendSeries> defaultTrends(int days) {
        List<Integer> zeros = new ArrayList<>(Collections.nCopies(days, 0));
        List<TeacherAnalyticsDtos.TrendSeries> trends = new ArrayList<>();
        trends.add(trend("trendEnroll", "Đăng ký mới", zeros, TREND_COLORS[0]));
        trends.add(trend("trendCompletion", "Học viên hoàn thành", zeros, TREND_COLORS[1]));
        trends.add(trend("trendReviews", "Đánh giá tích cực", zeros, TREND_COLORS[2]));
        return trends;
    }

    private TeacherAnalyticsDtos.TrendSeries trend(String id, String label, List<Integer> values, String color) {
        TeacherAnalyticsDtos.TrendSeries series = new TeacherAnalyticsDtos.TrendSeries();
        series.id = id;
        series.label = label;
        series.values = values;
        series.color = color;
        return series;
    }

    private Map<LocalDate, Integer> mapCounts(String sql, Timestamp from, Timestamp to) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
        Map<LocalDate, Integer> map = new HashMap<>();
        for (Object[] r : rows) {
            if (r[0] == null) continue;
            LocalDate date;
            if (r[0] instanceof LocalDate ld) {
                date = ld;
            } else if (r[0] instanceof java.sql.Date d) {
                date = d.toLocalDate();
            } else if (r[0] instanceof java.sql.Timestamp ts) {
                date = ts.toLocalDateTime().toLocalDate();
            } else {
                continue;
            }
            map.put(date, r[1] != null ? ((Number) r[1]).intValue() : 0);
        }
        return map;
    }

    private List<Integer> buildSeries(LocalDate from, LocalDate to, Map<LocalDate, Integer> data) {
        List<Integer> values = new ArrayList<>();
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            values.add(data.getOrDefault(cursor, 0));
            cursor = cursor.plusDays(1);
        }
        return values;
    }

    private Number singleNumber(String sql) {
        Query query = em.createNativeQuery(sql);
        Object result = query.getSingleResult();
        return result instanceof Number n ? n : null;
    }

    private List<TeacherAnalyticsDtos.SummaryCard> defaultSummary() {
        List<TeacherAnalyticsDtos.SummaryCard> list = new ArrayList<>();
        list.add(summaryCard("students", "Học viên đang học", "0", "Chưa có khóa học nào"));
        list.add(summaryCard("courses", "Khóa đang mở", "0", "Chưa có khóa học nào"));
        list.add(summaryCard("progress", "Tiến độ trung bình", "0%", "Chưa có dữ liệu"));
        list.add(summaryCard("pending", "Mục cần xử lý", "0", "Chưa có dữ liệu"));
        return list;
    }

    private String formatNumber(long value) {
        return String.format(Locale.US, "%,d", value).replace(',', '.');
    }

    private double roundOne(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private String truncate(String content, int max) {
        if (content == null) return "";
        String trimmed = content.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max - 1) + "…";
    }

    private String joinIds(List<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private static class SummaryStats {
        long studentsActive;
        long activeCourses;
        double avgProgress;
        int notesPending;
        int notesResolved;
        int resourcesPending;
        int reviewsPending;
    }
}
