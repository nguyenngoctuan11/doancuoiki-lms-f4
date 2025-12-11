package com.example.back_end.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    @PersistenceContext
    private EntityManager em;

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER')")
    public ResponseEntity<List<Map<String, Object>>> list() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                        "SELECT o.id, o.external_code, o.user_id, u.email, u.full_name, " +
                                "o.course_id, c.title, o.amount, o.status, o.method, o.created_at " +
                                "FROM dbo.orders o " +
                                "LEFT JOIN dbo.users u ON u.id = o.user_id " +
                                "LEFT JOIN dbo.courses c ON c.id = o.course_id " +
                                "ORDER BY o.id DESC")
                .getResultList();
        return ResponseEntity.ok(rows.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", num(r[0]));
            m.put("code", str(r[1]));
            m.put("userId", num(r[2]));
            m.put("email", str(r[3]));
            m.put("fullName", str(r[4]));
            m.put("courseId", num(r[5]));
            m.put("courseTitle", str(r[6]));
            m.put("amount", r[7]);
            m.put("status", str(r[8]));
            m.put("method", str(r[9]));
            m.put("createdAt", str(r[10]));
            return m;
        }).toList());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER')")
    @Transactional
    public ResponseEntity<?> approve(@PathVariable Long id) {
        Object[] row = (Object[]) em.createNativeQuery(
                        "SELECT user_id, course_id FROM dbo.orders WHERE id = :id")
                .setParameter("id", id)
                .getResultStream().findFirst().orElse(null);
        if (row == null) return ResponseEntity.notFound().build();
        Long userId = num(row[0]);
        Long courseId = num(row[1]);

        em.createNativeQuery("UPDATE dbo.orders SET status = N'approved', paid_at = COALESCE(paid_at, SYSUTCDATETIME()), updated_at = SYSUTCDATETIME() WHERE id = :id")
                .setParameter("id", id)
                .executeUpdate();
        em.createNativeQuery("UPDATE dbo.payments SET status = N'succeeded', updated_at = SYSUTCDATETIME() WHERE order_id = :id")
                .setParameter("id", id)
                .executeUpdate();
        // enroll user to course if not exists
        em.createNativeQuery(
                        "IF NOT EXISTS (SELECT 1 FROM dbo.enrollments WHERE user_id = :uid AND course_id = :cid) " +
                                "INSERT INTO dbo.enrollments(user_id, course_id, source, status, start_at, created_at) " +
                                "VALUES (:uid, :cid, N'purchase', N'active', SYSUTCDATETIME(), SYSUTCDATETIME())")
                .setParameter("uid", userId)
                .setParameter("cid", courseId)
                .executeUpdate();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('MANAGER')")
    @Transactional
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        em.createNativeQuery("UPDATE dbo.orders SET status = N'cancelled', updated_at = SYSUTCDATETIME() WHERE id = :id")
                .setParameter("id", id)
                .executeUpdate();
        em.createNativeQuery("UPDATE dbo.payments SET status = N'failed', updated_at = SYSUTCDATETIME() WHERE order_id = :id")
                .setParameter("id", id)
                .executeUpdate();
        return ResponseEntity.ok().build();
    }

    private static Long num(Object v){ return v==null? null: ((Number)v).longValue(); }
    private static String str(Object v){ return v==null? null: v.toString(); }
}
