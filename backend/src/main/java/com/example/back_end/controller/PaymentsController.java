package com.example.back_end.controller;

import com.example.back_end.model.User;
import com.example.back_end.repository.UserRepository;
import com.example.back_end.service.PaymentGatewayService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentsController {

    private final PaymentGatewayService gateway;
    private final UserRepository userRepository;
    @PersistenceContext
    private EntityManager em;

    public PaymentsController(PaymentGatewayService gateway, UserRepository userRepository) {
        this.gateway = gateway;
        this.userRepository = userRepository;
    }

    @PostMapping(value = "/checkout", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<Map<String, Object>> checkout(@RequestBody Map<String, Object> body,
                                                        HttpServletRequest request) {
        String method = str(body.get("method"));
        BigDecimal amount = toDecimal(body.get("amount"));
        String courseKey = str(body.get("course_key"));
        Long courseId = toLong(body.get("course_id"));
        if (courseId == null && courseKey != null) {
            try { courseId = Long.parseLong(courseKey); } catch (NumberFormatException ignored) {}
        }

        Long userId = resolveUserId(body);
        String orderId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String orderInfo = "Payment for course " + (courseKey != null ? courseKey : orderId);
        long amountVnd = amount != null ? amount.longValue() : 0L;

        Map<String, Object> res = new LinkedHashMap<>();

        try {
            if (eq(method, "VNPAY")) {
                String clientIp = clientIp(request);
                String url = gateway.createVnpayUrl(orderId, amountVnd, orderInfo, clientIp);
                res.put("paymentUrl", url);
                res.put("gateway", "VNPAY");
                res.put("orderId", orderId);
                persistOrder(userId, courseId, orderId, amount, method, "pending",
                        "VNPAY", orderId, str(body.get("return_url")), str(body.get("cancel_url")),
                        str(body.get("buyer_name")));
                return ResponseEntity.ok(res);
            } else if (eq(method, "MOMO")) {
                String url = gateway.createMomoUrl(orderId, amountVnd, orderInfo);
                res.put("paymentUrl", url);
                res.put("gateway", "MOMO");
                res.put("orderId", orderId);
                persistOrder(userId, courseId, orderId, amount, method, "pending",
                        "MOMO", orderId, str(body.get("return_url")), str(body.get("cancel_url")),
                        str(body.get("buyer_name")));
                return ResponseEntity.ok(res);
            }
        } catch (Exception ex) {
            res.put("error", "gateway_error");
            res.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(res);
        }

        // BANK/COD: ghi nhận đơn pending (duyệt thủ công), không auto enroll
        persistOrder(userId, courseId, orderId, amount, method, "pending",
                method, orderId, str(body.get("return_url")), str(body.get("cancel_url")),
                str(body.get("buyer_name")));
        res.put("status", "PENDING");
        res.put("paid", true);
        res.put("orderId", orderId);
        return ResponseEntity.ok(res);
    }

    private static String str(Object v) { return v == null ? null : String.valueOf(v); }
    private static boolean eq(String a, String b) { return a != null && a.equalsIgnoreCase(b); }
    private static BigDecimal toDecimal(Object v) { try { return v==null? null : new BigDecimal(String.valueOf(v)); } catch (Exception e) { return null; } }
    private static Long toLong(Object v) { try { return v==null? null : Long.parseLong(String.valueOf(v)); } catch (Exception e) { return null; } }
    private static String clientIp(HttpServletRequest req){
        String xf = req.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) return xf.split(",")[0].trim();
        return req.getRemoteAddr();
    }

    private Long resolveUserId(Map<String, Object> body) {
        String email = str(body.get("buyer_email"));
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof String) {
            email = (String) auth.getPrincipal();
        }
        if (email != null) {
            Optional<User> u = userRepository.findByEmailIgnoreCase(email);
            if (u.isPresent()) return u.get().getId();
        }
        try {
            Object idObj = em.createNativeQuery("SELECT TOP 1 id FROM dbo.users ORDER BY id").getSingleResult();
            if (idObj != null) return ((Number) idObj).longValue();
        } catch (Exception ignored) {}
        return null;
    }

    private void persistOrder(Long userId, Long courseId, String externalCode, BigDecimal amount,
                              String method, String status, String provider, String providerTxn,
                              String returnUrl, String cancelUrl, String note) {
        if (userId == null || courseId == null) return;
        BigDecimal safeAmount = amount != null ? amount : BigDecimal.ZERO;
        String methodVal = method != null ? method.toUpperCase() : "BANK";
        String statusVal = status != null ? status.toLowerCase() : "pending";
        Object oidObj = em.createNativeQuery(
                        "INSERT INTO dbo.orders (user_id, course_id, external_code, amount, currency, method, status, provider, provider_txn, return_url, cancel_url, note, created_at, updated_at, paid_at) " +
                                "VALUES (:uid, :cid, :code, :amount, N'VND', :method, :status, :provider, :ptxn, :returnUrl, :cancelUrl, :note, SYSUTCDATETIME(), SYSUTCDATETIME(), CASE WHEN :status = 'paid' THEN SYSUTCDATETIME() ELSE NULL END); " +
                                "SELECT CAST(SCOPE_IDENTITY() AS BIGINT);")
                .setParameter("uid", userId)
                .setParameter("cid", courseId)
                .setParameter("code", externalCode)
                .setParameter("amount", safeAmount)
                .setParameter("method", methodVal)
                .setParameter("status", statusVal)
                .setParameter("provider", provider)
                .setParameter("ptxn", providerTxn)
                .setParameter("returnUrl", returnUrl)
                .setParameter("cancelUrl", cancelUrl)
                .setParameter("note", note)
                .getSingleResult();
        Long orderDbId = oidObj != null ? ((Number) oidObj).longValue() : null;

        if (orderDbId != null) {
            em.createNativeQuery(
                            "INSERT INTO dbo.payments (order_id, provider, provider_txn_id, channel, amount, status, created_at, updated_at) " +
                                    "VALUES (:oid, :provider, :ptxn, :channel, :amount, :pstatus, SYSUTCDATETIME(), SYSUTCDATETIME())")
                    .setParameter("oid", orderDbId)
                    .setParameter("provider", provider != null ? provider : methodVal)
                    .setParameter("ptxn", providerTxn)
                    .setParameter("channel", methodVal)
                    .setParameter("amount", safeAmount)
                    .setParameter("pstatus", statusVal.equals("paid") ? "succeeded" : "pending")
                    .executeUpdate();
        }
    }
}
