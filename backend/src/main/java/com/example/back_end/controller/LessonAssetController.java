package com.example.back_end.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/lessons")
public class LessonAssetController {
    @PersistenceContext
    private EntityManager em;

    @PostMapping("/{lessonId}/assets")
    @PreAuthorize("hasAnyRole('TEACHER','MANAGER')")
    @Transactional
    public ResponseEntity<?> addAsset(@PathVariable Long lessonId, @RequestBody Map<String, Object> body) {
        String kind = String.valueOf(body.getOrDefault("kind", "video"));
        String url = String.valueOf(body.get("url"));
        String mime = body.get("mime_type") != null ? String.valueOf(body.get("mime_type")) : null;
        if (url == null || url.isBlank()) return ResponseEntity.badRequest().body("url is required");
        em.createNativeQuery("INSERT INTO dbo.lesson_assets(lesson_id, kind, url, mime_type) VALUES (?,?,?,?)")
                .setParameter(1, lessonId)
                .setParameter(2, kind)
                .setParameter(3, url)
                .setParameter(4, mime)
                .executeUpdate();
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
