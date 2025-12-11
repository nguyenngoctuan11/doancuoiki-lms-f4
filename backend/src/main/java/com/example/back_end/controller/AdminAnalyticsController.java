package com.example.back_end.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {

    @PersistenceContext
    private EntityManager em;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> summary() {
        Map<String, Object> data = new HashMap<>();

        Number totalUsers = (Number) em.createNativeQuery("SELECT COUNT(*) FROM dbo.users").getSingleResult();
        Number newUsers7d = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM dbo.users WHERE created_at >= DATEADD(day, -7, SYSUTCDATETIME())")
                .getSingleResult();
        Number newUsers30d = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM dbo.users WHERE created_at >= DATEADD(day, -30, SYSUTCDATETIME())")
                .getSingleResult();

        // Active users tạm tính: có enrollment cập nhật 30 ngày gần nhất
        Number activeUsers = (Number) em.createNativeQuery(
                        "SELECT COUNT(DISTINCT user_id) FROM dbo.enrollments WHERE start_at >= DATEADD(day, -30, SYSUTCDATETIME())")
                .getSingleResult();

        // Retention sơ bộ: % user có enrollment trong 30 ngày / tổng
        double retention = totalUsers != null && totalUsers.longValue() > 0
                ? (activeUsers.doubleValue() / totalUsers.doubleValue()) * 100.0
                : 0.0;

        data.put("totalUsers", totalUsers != null ? totalUsers.longValue() : 0);
        data.put("newUsers7d", newUsers7d != null ? newUsers7d.longValue() : 0);
        data.put("newUsers30d", newUsers30d != null ? newUsers30d.longValue() : 0);
        data.put("activeUsers30d", activeUsers != null ? activeUsers.longValue() : 0);
        data.put("retentionPercent", Math.round(retention * 10.0) / 10.0);

        return ResponseEntity.ok(data);
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<List<Map<String, Object>>> roleDistribution() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                        "SELECT UPPER(r.code) AS role, COUNT(*) AS total " +
                                "FROM dbo.user_roles ur " +
                                "JOIN dbo.roles r ON r.id = ur.role_id " +
                                "GROUP BY UPPER(r.code)")
                .getResultList();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("role", r[0] != null ? r[0].toString() : "UNKNOWN");
            item.put("count", r[1] != null ? ((Number) r[1]).longValue() : 0L);
            list.add(item);
        }
        return ResponseEntity.ok(list);
    }

    /**
    * Thống kê hoạt động theo ngày/tháng:
    * - logins: lấy từ quiz_attempts (bắt đầu làm bài)
    * - activeUsers: distinct user trong quiz_attempts
    * - newUsers: lấy từ users.created_at
    */
    @GetMapping("/logins")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<List<Map<String, Object>>> logins(
            @RequestParam(value = "from", required = false) String fromStr,
            @RequestParam(value = "to", required = false) String toStr,
            @RequestParam(value = "groupBy", required = false, defaultValue = "day") String groupBy
    ) {
        LocalDate to = (toStr != null && !toStr.isBlank()) ? LocalDate.parse(toStr) : LocalDate.now(ZoneOffset.UTC);
        LocalDate from = (fromStr != null && !fromStr.isBlank()) ? LocalDate.parse(fromStr) : to.minusDays(30);
        String dateExpr;
        if ("month".equalsIgnoreCase(groupBy)) {
            dateExpr = "FORMAT(d, 'yyyy-MM')";
        } else {
            dateExpr = "CONVERT(varchar(10), d, 23)"; // yyyy-MM-dd
        }

        // logins / active via quiz_attempts
        @SuppressWarnings("unchecked")
        List<Object[]> attemptRows = em.createNativeQuery(
                        "WITH t AS (" +
                                "  SELECT CONVERT(date, started_at) AS d, user_id " +
                                "  FROM dbo.quiz_attempts " +
                                "  WHERE started_at >= :from AND started_at <= DATEADD(day, 1, :to)" +
                                ")" +
                                "SELECT " + dateExpr + " AS bucket, COUNT(*) AS logins, COUNT(DISTINCT user_id) AS activeUsers " +
                                "FROM t GROUP BY " + dateExpr + " ORDER BY " + dateExpr)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();

        // new users
        @SuppressWarnings("unchecked")
        List<Object[]> newRows = em.createNativeQuery(
                        "WITH t AS (" +
                                "  SELECT CONVERT(date, created_at) AS d " +
                                "  FROM dbo.users " +
                                "  WHERE created_at >= :from AND created_at <= DATEADD(day, 1, :to)" +
                                ")" +
                                "SELECT " + dateExpr + " AS bucket, COUNT(*) AS newUsers " +
                                "FROM t GROUP BY " + dateExpr + " ORDER BY " + dateExpr)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();

        Map<String, Map<String, Object>> buckets = new LinkedHashMap<>();
        for (Object[] r : attemptRows) {
            String bucket = r[0] != null ? r[0].toString() : "";
            Map<String, Object> m = buckets.computeIfAbsent(bucket, k -> new HashMap<>());
            m.put("bucket", bucket);
            m.put("logins", r[1] != null ? ((Number) r[1]).longValue() : 0L);
            m.put("activeUsers", r[2] != null ? ((Number) r[2]).longValue() : 0L);
        }
        for (Object[] r : newRows) {
            String bucket = r[0] != null ? r[0].toString() : "";
            Map<String, Object> m = buckets.computeIfAbsent(bucket, k -> new HashMap<>());
            m.put("bucket", bucket);
            m.put("newUsers", r[1] != null ? ((Number) r[1]).longValue() : 0L);
        }

        List<Map<String, Object>> result = new ArrayList<>(buckets.values());
        return ResponseEntity.ok(result);
    }

    // ---------- Courses ----------
    @GetMapping("/courses")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> courseSummary() {
        Map<String, Object> data = new HashMap<>();
        Number active = (Number) em.createNativeQuery("SELECT COUNT(*) FROM dbo.courses WHERE status = N'published'")
                .getSingleResult();
        Number newMonth = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM dbo.courses WHERE created_at >= DATEADD(day,-30,SYSUTCDATETIME())")
                .getSingleResult();
        Number paused = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM dbo.courses WHERE status IN (N'archived', N'draft')")
                .getSingleResult();
        Number totalProgress = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM dbo.lesson_progress WHERE progress_percent = 100")
                .getSingleResult();
        Number totalEnroll = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM dbo.enrollments")
                .getSingleResult();
        double completionRate = (totalEnroll != null && totalEnroll.longValue() > 0)
                ? (totalProgress.doubleValue() / totalEnroll.doubleValue()) * 100.0
                : 0.0;
        data.put("active", active != null ? active.longValue() : 0);
        data.put("newPerMonth", newMonth != null ? newMonth.longValue() : 0);
        data.put("paused", paused != null ? paused.longValue() : 0);
        data.put("completionRate", Math.round(completionRate * 10.0) / 10.0);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/courses/trend")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<List<Map<String, Object>>> courseTrend(
            @RequestParam(value = "from", required = false) String fromStr,
            @RequestParam(value = "to", required = false) String toStr,
            @RequestParam(value = "groupBy", required = false, defaultValue = "day") String groupBy
    ) {
        LocalDateRange range = parseRange(fromStr, toStr, 30);
        String dateExpr = "month".equalsIgnoreCase(groupBy) ? "FORMAT(d, 'yyyy-MM')" : "CONVERT(varchar(10), d, 23)";

        @SuppressWarnings("unchecked")
        List<Object[]> createdRows = em.createNativeQuery(
                        "WITH t AS (SELECT CONVERT(date, created_at) AS d FROM dbo.courses WHERE created_at >= :from AND created_at <= DATEADD(day,1,:to)) " +
                                "SELECT " + dateExpr + " AS bucket, COUNT(*) AS created FROM t GROUP BY " + dateExpr + " ORDER BY " + dateExpr)
                .setParameter("from", java.sql.Date.valueOf(range.from))
                .setParameter("to", java.sql.Date.valueOf(range.to))
                .getResultList();

        @SuppressWarnings("unchecked")
        List<Object[]> enrollRows = em.createNativeQuery(
                        "WITH t AS (SELECT CONVERT(date, start_at) AS d FROM dbo.enrollments WHERE start_at >= :from AND start_at <= DATEADD(day,1,:to)) " +
                                "SELECT " + dateExpr + " AS bucket, COUNT(*) AS enroll FROM t GROUP BY " + dateExpr + " ORDER BY " + dateExpr)
                .setParameter("from", java.sql.Date.valueOf(range.from))
                .setParameter("to", java.sql.Date.valueOf(range.to))
                .getResultList();

        Map<String, Map<String, Object>> buckets = new LinkedHashMap<>();
        for (Object[] r : createdRows) {
            String b = Objects.toString(r[0], "");
            Map<String, Object> m = buckets.computeIfAbsent(b, k -> new HashMap<>());
            m.put("bucket", b);
            m.put("enroll", 0);
            m.put("complete", 0);
            m.put("dropoff", 0);
            m.put("created", r[1] != null ? ((Number) r[1]).longValue() : 0);
        }
        for (Object[] r : enrollRows) {
            String b = Objects.toString(r[0], "");
            Map<String, Object> m = buckets.computeIfAbsent(b, k -> new HashMap<>());
            m.put("bucket", b);
            m.put("enroll", r[1] != null ? ((Number) r[1]).longValue() : 0);
            m.putIfAbsent("complete", 0);
            m.putIfAbsent("dropoff", 0);
        }
        return ResponseEntity.ok(new ArrayList<>(buckets.values()));
    }

    // ---------- Content / Lessons ----------
    @GetMapping("/content")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> contentSummary() {
        Map<String, Object> data = new HashMap<>();
        Number lessonCount = (Number) em.createNativeQuery("SELECT COUNT(*) FROM dbo.lessons").getSingleResult();
        Number lessonDone = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM dbo.lesson_progress WHERE progress_percent = 100")
                .getSingleResult();
        double lessonCompletion = (lessonCount != null && lessonCount.longValue() > 0)
                ? (lessonDone.doubleValue() / lessonCount.doubleValue()) * 100.0
                : 0.0;
        Number submits = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM dbo.quiz_attempts WHERE status IN (N'submitted', N'graded')")
                .getSingleResult();
        Number pass = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM dbo.quiz_attempts WHERE passed = 1")
                .getSingleResult();
        double passRate = (submits != null && submits.longValue() > 0)
                ? (pass.doubleValue() / submits.doubleValue()) * 100.0
                : 0.0;
        data.put("views", 0); // không có log xem video
        data.put("lessonCompletionRate", Math.round(lessonCompletion * 10.0) / 10.0);
        data.put("submitRate", submits != null ? submits.longValue() : 0);
        data.put("passRate", Math.round(passRate * 10.0) / 10.0);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/content/trend")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<List<Map<String, Object>>> contentTrend(
            @RequestParam(value = "from", required = false) String fromStr,
            @RequestParam(value = "to", required = false) String toStr,
            @RequestParam(value = "groupBy", required = false, defaultValue = "day") String groupBy
    ) {
        LocalDateRange range = parseRange(fromStr, toStr, 30);
        String dateExpr = "month".equalsIgnoreCase(groupBy) ? "FORMAT(d, 'yyyy-MM')" : "CONVERT(varchar(10), d, 23)";
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                        "WITH lp AS (" +
                                "  SELECT CONVERT(date, completed_at) AS d FROM dbo.lesson_progress WHERE completed_at IS NOT NULL " +
                                "    AND completed_at >= :from AND completed_at < DATEADD(day,1,:to)" +
                                "), qa AS (" +
                                "  SELECT CONVERT(date, graded_at) AS d, passed FROM dbo.quiz_attempts WHERE graded_at IS NOT NULL " +
                                "    AND graded_at >= :from AND graded_at < DATEADD(day,1,:to)" +
                                ")" +
                                "SELECT " + dateExpr + " AS bucket, " +
                                "  (SELECT COUNT(*) FROM lp WHERE lp.d = t.d) AS completion, " +
                                "  (SELECT COUNT(*) FROM qa WHERE qa.d = t.d) AS attempts, " +
                                "  (SELECT COUNT(*) FROM qa WHERE qa.d = t.d AND passed = 1) AS pass " +
                                "FROM (SELECT DISTINCT d FROM lp UNION SELECT DISTINCT d FROM qa) t ORDER BY bucket")
                .setParameter("from", java.sql.Date.valueOf(range.from))
                .setParameter("to", java.sql.Date.valueOf(range.to))
                .getResultList();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("bucket", r[0] != null ? r[0].toString() : "");
            m.put("views", r[1] != null ? ((Number) r[1]).longValue() : 0);
            m.put("completion", r[1] != null ? ((Number) r[1]).longValue() : 0);
            m.put("pass", r[3] != null ? ((Number) r[3]).longValue() : 0);
            list.add(m);
        }
        return ResponseEntity.ok(list);
    }

    // ---------- Revenue ----------
    @GetMapping("/revenue")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> revenueSummary() {
        Map<String, Object> data = new HashMap<>();
        try {
            Number total = (Number) em.createNativeQuery(
                            "SELECT COALESCE(SUM(revenue),0) FROM (" +
                                    " SELECT CAST(amount AS DECIMAL(18,2)) AS revenue" +
                                    "   FROM dbo.payments WHERE status IN (N'succeeded',N'paid',N'approved')" +
                                    " UNION ALL" +
                                    " SELECT CAST(o.total AS DECIMAL(18,2))" +
                                    "   FROM dbo.orders o" +
                                    "   WHERE o.status IN (N'approved',N'paid',N'completed')" +
                                    "     AND NOT EXISTS (" +
                                    "         SELECT 1 FROM dbo.payments p" +
                                    "         WHERE p.order_id = o.id AND p.status IN (N'succeeded',N'paid',N'approved')" +
                                    "     )" +
                                    ") t")
                    .getSingleResult();
            Number orders = (Number) em.createNativeQuery(
                            "SELECT COUNT(*) FROM dbo.orders WHERE status IS NOT NULL")
                    .getSingleResult();
            Number success = (Number) em.createNativeQuery(
                            "SELECT COUNT(*) FROM dbo.orders WHERE status IN (N'approved', N'paid', N'completed')")
                    .getSingleResult();
            double successRate = (orders != null && orders.longValue() > 0)
                    ? (success.doubleValue() / orders.doubleValue()) * 100.0
                    : 0.0;
            Object topChannel = em.createNativeQuery(
                            "SELECT TOP 1 provider FROM dbo.payments WHERE status IN (N'succeeded',N'paid',N'approved') GROUP BY provider ORDER BY COUNT(*) DESC")
                    .getResultStream().findFirst().orElse("--");
            data.put("total", total != null ? total : 0);
            data.put("orders", orders != null ? orders.longValue() : 0);
            data.put("successRate", Math.round(successRate * 10.0) / 10.0);
            data.put("topChannel", topChannel != null ? topChannel.toString() : "--");
        } catch (Exception e) {
            data.put("total", 0);
            data.put("orders", 0);
            data.put("successRate", 0);
            data.put("topChannel", "--");
        }
        return ResponseEntity.ok(data);
    }

    @GetMapping("/revenue/trend")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<List<Map<String, Object>>> revenueTrend(
            @RequestParam(value = "from", required = false) String fromStr,
            @RequestParam(value = "to", required = false) String toStr,
            @RequestParam(value = "groupBy", required = false, defaultValue = "day") String groupBy
    ) {
        LocalDateRange range = parseRange(fromStr, toStr, 30);
        String dateExpr = "month".equalsIgnoreCase(groupBy) ? "FORMAT(d, 'yyyy-MM')" : "CONVERT(varchar(10), d, 23)";
        String bucketExpr = "month".equalsIgnoreCase(groupBy)
                ? "FORMAT(CONVERT(date, created_at), 'yyyy-MM')"
                : "CONVERT(varchar(10), CONVERT(date, created_at), 23)";
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                            "WITH pay AS ( " +
                                    "  SELECT " + bucketExpr + " AS bucket, " +
                                    "         SUM(CASE WHEN status IN (N'succeeded',N'paid',N'approved') THEN CAST(amount AS DECIMAL(18,2)) ELSE 0 END) AS revenue, " +
                                    "         COUNT(*) AS orders, " +
                                    "         SUM(CASE WHEN status IN (N'succeeded',N'paid',N'approved') THEN 1 ELSE 0 END) AS success " +
                                    "  FROM dbo.payments WHERE created_at >= :from AND created_at < DATEADD(day,1,:to) GROUP BY " + bucketExpr +
                                    "), ord AS ( " +
                                    "  SELECT " + bucketExpr + " AS bucket, " +
                                    "         SUM(CAST(o.total AS DECIMAL(18,2))) AS revenue, " +
                                    "         COUNT(*) AS orders, " +
                                    "         SUM(CASE WHEN o.status IN (N'approved',N'paid',N'completed') THEN 1 ELSE 0 END) AS success " +
                                    "  FROM dbo.orders o " +
                                    "  WHERE o.created_at >= :from AND o.created_at < DATEADD(day,1,:to) " +
                                    "    AND o.status IN (N'approved',N'paid',N'completed') " +
                                    "    AND NOT EXISTS (" +
                                    "         SELECT 1 FROM dbo.payments p" +
                                    "         WHERE p.order_id = o.id AND p.status IN (N'succeeded',N'paid',N'approved')" +
                                    "    ) " +
                                    "  GROUP BY " + bucketExpr +
                                    "), buckets AS (SELECT bucket FROM pay UNION SELECT bucket FROM ord) " +
                                    "SELECT b.bucket, COALESCE(p.revenue,0) + COALESCE(o.revenue,0) AS revenue, " +
                                    "       COALESCE(p.orders,0) + COALESCE(o.orders,0) AS orders, " +
                                    "       COALESCE(p.success,0) + COALESCE(o.success,0) AS success " +
                                    "FROM buckets b " +
                                    "LEFT JOIN pay p ON p.bucket = b.bucket " +
                                    "LEFT JOIN ord o ON o.bucket = b.bucket " +
                                    "ORDER BY b.bucket")
                    .setParameter("from", java.sql.Date.valueOf(range.from))
                    .setParameter("to", java.sql.Date.valueOf(range.to))
                    .getResultList();
            List<Map<String, Object>> list = new ArrayList<>();
            for (Object[] r : rows) {
                Map<String, Object> m = new HashMap<>();
                m.put("bucket", r[0] != null ? r[0].toString() : "");
                m.put("revenue", r[1] != null ? ((Number) r[1]).doubleValue() : 0d);
                m.put("orders", r[2] != null ? ((Number) r[2]).longValue() : 0L);
                m.put("success", r[3] != null ? ((Number) r[3]).longValue() : 0L);
                list.add(m);
            }
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    // ---------- Behavior ----------
    @GetMapping("/behavior")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<List<Map<String, Object>>> behaviorTrend(
            @RequestParam(value = "from", required = false) String fromStr,
            @RequestParam(value = "to", required = false) String toStr,
            @RequestParam(value = "groupBy", required = false, defaultValue = "day") String groupBy
    ) {
        LocalDateRange range = parseRange(fromStr, toStr, 30);
        String dateExpr = "month".equalsIgnoreCase(groupBy) ? "FORMAT(la.d, 'yyyy-MM')" : "CONVERT(varchar(10), la.d, 23)";
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                        "WITH a AS ( " +
                                "   SELECT CONVERT(date, started_at) AS d, user_id " +
                                "   FROM dbo.quiz_attempts WHERE started_at >= :from AND started_at < DATEADD(day,1,:to)" +
                                "), lp AS ( " +
                                "   SELECT CONVERT(date, updated_at) AS d, user_id, progress_percent " +
                                "   FROM dbo.lesson_progress WHERE updated_at >= :from AND updated_at < DATEADD(day,1,:to)" +
                                ") " +
                                "SELECT " + dateExpr + " AS bucket, " +
                                "  COUNT(DISTINCT lp.user_id) AS sessions, " +
                                "  AVG(CAST(lp.progress_percent AS FLOAT)) AS interact, " +
                                "  COUNT(DISTINCT a.user_id) AS timeVal " +
                                "FROM lp FULL OUTER JOIN a ON lp.d = a.d " +
                                "GROUP BY " + dateExpr + " ORDER BY " + dateExpr)
                .setParameter("from", java.sql.Date.valueOf(range.from))
                .setParameter("to", java.sql.Date.valueOf(range.to))
                .getResultList();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("bucket", r[0] != null ? r[0].toString() : "");
            m.put("sessions", r[1] != null ? ((Number) r[1]).longValue() : 0);
            m.put("interact", r[2] != null ? ((Number) r[2]).doubleValue() : 0);
            m.put("time", r[3] != null ? ((Number) r[3]).longValue() : 0);
            list.add(m);
        }
        return ResponseEntity.ok(list);
    }

    // ---------- System ----------
    @GetMapping("/system")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<List<Map<String, Object>>> systemTrend(
            @RequestParam(value = "from", required = false) String fromStr,
            @RequestParam(value = "to", required = false) String toStr,
            @RequestParam(value = "groupBy", required = false, defaultValue = "day") String groupBy
    ) {
        // Chưa có log hệ thống -> trả rỗng
        return ResponseEntity.ok(List.of());
    }

    // ---------- Instructor ----------
    @GetMapping("/instructor")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<List<Map<String, Object>>> instructorTrend(
            @RequestParam(value = "from", required = false) String fromStr,
            @RequestParam(value = "to", required = false) String toStr,
            @RequestParam(value = "groupBy", required = false, defaultValue = "day") String groupBy
    ) {
        LocalDateRange range = parseRange(fromStr, toStr, 30);
        String dateExpr = "month".equalsIgnoreCase(groupBy) ? "FORMAT(d, 'yyyy-MM')" : "CONVERT(varchar(10), d, 23)";
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                        "WITH ci AS ( " +
                                "  SELECT c.id, ci.user_id FROM dbo.course_instructors ci JOIN dbo.courses c ON c.id = ci.course_id " +
                                "), e AS ( " +
                                "  SELECT course_id, CONVERT(date, start_at) AS d FROM dbo.enrollments WHERE start_at >= :from AND start_at <= DATEADD(day,1,:to) " +
                                "), a AS ( " +
                                "  SELECT quiz_id, user_id, passed, CONVERT(date, graded_at) AS d FROM dbo.quiz_attempts WHERE graded_at IS NOT NULL AND graded_at >= :from AND graded_at <= DATEADD(day,1,:to) " +
                                ") " +
                                "SELECT " + dateExpr + " AS bucket, " +
                                "  COUNT(DISTINCT e.course_id) AS courses, " +
                                "  COUNT(*) AS students, " +
                                "  SUM(CASE WHEN a.passed = 1 THEN 1 ELSE 0 END) AS complete " +
                                "FROM e LEFT JOIN ci ON e.course_id = ci.id " +
                                "LEFT JOIN a ON a.quiz_id IN (SELECT id FROM dbo.quizzes WHERE course_id = e.course_id) " +
                                "GROUP BY " + dateExpr + " ORDER BY " + dateExpr)
                .setParameter("from", range.from)
                .setParameter("to", range.to)
                .getResultList();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("bucket", r[0] != null ? r[0].toString() : "");
            m.put("students", r[2] != null ? ((Number) r[2]).longValue() : 0);
            m.put("complete", r[3] != null ? ((Number) r[3]).longValue() : 0);
            m.put("rating", 0);
            list.add(m);
        }
        return ResponseEntity.ok(list);
    }

    // ---------- Support ----------
    @GetMapping("/support")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<List<Map<String, Object>>> supportTrend(
            @RequestParam(value = "from", required = false) String fromStr,
            @RequestParam(value = "to", required = false) String toStr,
            @RequestParam(value = "groupBy", required = false, defaultValue = "day") String groupBy
    ) {
        LocalDateRange range = parseRange(fromStr, toStr, 30);
        String dateExpr = "month".equalsIgnoreCase(groupBy) ? "FORMAT(created_at, 'yyyy-MM')" : "CONVERT(varchar(10), created_at, 23)";
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                        "SELECT " + dateExpr + " AS bucket, " +
                                "SUM(CASE WHEN status = N'new' THEN 1 ELSE 0 END) AS newTicket, " +
                                "SUM(CASE WHEN status IN (N'in_progress', N'waiting_student') THEN 1 ELSE 0 END) AS openTicket, " +
                                "SUM(CASE WHEN status = N'closed' THEN 1 ELSE 0 END) AS closedTicket " +
                                "FROM dbo.support_threads WHERE created_at >= :from AND created_at <= DATEADD(day,1,:to) " +
                                "GROUP BY " + dateExpr + " ORDER BY " + dateExpr)
                .setParameter("from", range.from)
                .setParameter("to", range.to)
                .getResultList();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("bucket", r[0] != null ? r[0].toString() : "");
            m.put("newTicket", r[1] != null ? ((Number) r[1]).longValue() : 0);
            m.put("openTicket", r[2] != null ? ((Number) r[2]).longValue() : 0);
            m.put("closedTicket", r[3] != null ? ((Number) r[3]).longValue() : 0);
            list.add(m);
        }
        return ResponseEntity.ok(list);
    }

    private LocalDateRange parseRange(String fromStr, String toStr, int defaultDays) {
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        LocalDate to = LocalDate.now(ZoneOffset.UTC);
        LocalDate from = to.minusDays(defaultDays);
        try {
            if (toStr != null && !toStr.isBlank()) {
                to = LocalDate.parse(toStr, fmt);
            }
        } catch (DateTimeParseException ignored) { }
        try {
            if (fromStr != null && !fromStr.isBlank()) {
                from = LocalDate.parse(fromStr, fmt);
            } else {
                from = to.minusDays(defaultDays);
            }
        } catch (DateTimeParseException ignored) {
            from = to.minusDays(defaultDays);
        }
        LocalDateRange r = new LocalDateRange();
        r.from = from;
        r.to = to;
        return r;
    }

    private static class LocalDateRange {
        LocalDate from;
        LocalDate to;
    }
}
