package com.example.back_end.controller;

import com.example.back_end.dto.CourseDtos;
import com.example.back_end.model.Category;
import com.example.back_end.model.Course;
import com.example.back_end.model.User;
import com.example.back_end.repository.CategoryRepository;
import com.example.back_end.repository.CourseRepository;
import com.example.back_end.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teacher/courses")
public class TeacherCourseController {
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public TeacherCourseController(CourseRepository courseRepository, UserRepository userRepository, CategoryRepository categoryRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    private User currentUser(Authentication auth) {
        return userRepository.findByEmailIgnoreCase(String.valueOf(auth.getPrincipal())).orElseThrow();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','MANAGER')")
    public ResponseEntity<?> create(@RequestBody CourseDtos.CreateRequest req, Authentication auth) {
        try {
            User creator = currentUser(auth);
            Course c = new Course();
            c.setTitle(req.title);
            c.setSlug(req.slug);
            c.setShortDesc(req.shortDesc);
            c.setLanguage(req.language != null ? req.language : "vi");
            c.setLevel(req.level != null ? req.level : "beginner");
            c.setTargetRoles(joinRoles(req.targetRoles));
            c.setCategories(resolveCategories(req.categories));
            c.setStatus("draft");
            applyPricing(c, req, true);
            c.setThumbnailUrl(null);
            c.setCreatedBy(creator);
            c.setApprovalStatus("draft");
            c = courseRepository.save(c);
            return ResponseEntity.ok(Map.of("id", c.getId(), "slug", c.getSlug()));
        } catch (org.springframework.dao.DataIntegrityViolationException dive) {
            String msg = String.valueOf(dive.getMostSpecificCause());
            if (msg != null && msg.toLowerCase().contains("slug")) {
                return ResponseEntity.badRequest().body("Slug đã tồn tại, hãy chọn slug khác");
            }
            if (msg != null && msg.toLowerCase().contains("fk_courses_creator")) {
                return ResponseEntity.badRequest().body("Tài khoản hiện tại không hợp lệ (created_by)");
            }
            return ResponseEntity.badRequest().body("Dữ liệu không hợp lệ: " + msg);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping("/{id}/thumbnail")
    @PreAuthorize("hasAnyRole('TEACHER','MANAGER')")
    @Transactional
    public ResponseEntity<?> setThumbnail(@PathVariable Long id, @RequestBody Map<String, String> body, Authentication auth) {
        Course c = courseRepository.findById(id).orElseThrow();
        if (!c.getCreatedBy().getEmail().equalsIgnoreCase(String.valueOf(auth.getPrincipal()))) {
            return ResponseEntity.status(403).body("Not owner");
        }
        c.setThumbnailUrl(body.get("url"));
        courseRepository.save(c);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','MANAGER')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getOne(@PathVariable Long id, Authentication auth) {
        Course c = courseRepository.findById(id).orElseThrow();
        boolean isOwner = c.getCreatedBy().getEmail().equalsIgnoreCase(String.valueOf(auth.getPrincipal()));
        boolean isManager = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
        if (!(isOwner || isManager)) return ResponseEntity.status(403).body("Not owner");
        CourseDtos.CourseResponse res = new CourseDtos.CourseResponse();
        res.id = c.getId(); res.title = c.getTitle(); res.slug = c.getSlug(); res.shortDesc = c.getShortDesc();
        res.language = c.getLanguage(); res.level = c.getLevel(); res.status = c.getStatus(); res.price = c.getPrice();
        res.isFree = c.getIsFree();
        res.createdById = c.getCreatedBy().getId(); res.createdByEmail = c.getCreatedBy().getEmail();
        res.thumbnailUrl = c.getThumbnailUrl(); res.approvalStatus = c.getApprovalStatus();
        res.targetRoles = splitRoles(c.getTargetRoles());
        res.categories = courseCategories(c);
        return ResponseEntity.ok(res);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','MANAGER')")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody CourseDtos.UpdateRequest req, Authentication auth) {
        Course c = courseRepository.findById(id).orElseThrow();
        boolean isOwner = c.getCreatedBy().getEmail().equalsIgnoreCase(String.valueOf(auth.getPrincipal()));
        boolean isManager = ((org.springframework.security.core.GrantedAuthority) () -> "ROLE_MANAGER")
                .getAuthority() != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
        if (!(isOwner || isManager)) return ResponseEntity.status(403).body("Not owner");
        if (req.title != null) {
            String t = req.title.trim();
            if (t.isEmpty()) return ResponseEntity.badRequest().body("Tiêu đề không được để trống");
            c.setTitle(t);
        }
        if (req.slug != null) {
            String slug = req.slug.trim();
            if (slug.isEmpty()) return ResponseEntity.badRequest().body("Slug không được để trống");
            if (courseRepository.existsBySlugIgnoreCaseAndIdNot(slug, id)) {
                return ResponseEntity.badRequest().body("Slug đã tồn tại, hãy chọn slug khác");
            }
            c.setSlug(slug);
        }
        if (req.shortDesc != null) c.setShortDesc(req.shortDesc);
        if (req.language != null) c.setLanguage(req.language);
        if (req.level != null) c.setLevel(req.level);
        if (req.status != null) c.setStatus(req.status);
        if (req.targetRoles != null) c.setTargetRoles(joinRoles(req.targetRoles));
        if (req.price != null || req.isFree != null) applyPricing(c, req, false);
        if (req.thumbnailUrl != null) c.setThumbnailUrl(req.thumbnailUrl);
        if (req.categories != null) c.setCategories(resolveCategories(req.categories));
        try {
            courseRepository.save(c);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (org.springframework.dao.DataIntegrityViolationException dive) {
            String msg = String.valueOf(dive.getMostSpecificCause());
            if (msg != null && msg.toLowerCase().contains("slug")) {
                return ResponseEntity.badRequest().body("Slug đã tồn tại, hãy chọn slug khác");
            }
            return ResponseEntity.badRequest().body("Dữ liệu không hợp lệ: " + msg);
        }
    }

    private void applyPricing(Course course, CourseDtos.CreateRequest req, boolean creating) {
        boolean priceProvided = req.price != null;
        BigDecimal normalizedPrice = priceProvided ? normalizePrice(req.price) : course.getPrice();
        boolean isFreeFlag;
        if (req.isFree != null) {
            isFreeFlag = req.isFree;
        } else if (priceProvided) {
            isFreeFlag = normalizedPrice == null;
        } else if (creating) {
            isFreeFlag = true;
        } else {
            Boolean current = course.getIsFree();
            isFreeFlag = current != null ? current : (course.getPrice() == null);
        }

        if (isFreeFlag) {
            course.setIsFree(true);
            course.setPrice(null);
        } else {
            if (normalizedPrice == null) {
                throw new IllegalArgumentException("Vui long nhap gia lon hon 0 cho khoa Pro");
            }
            course.setIsFree(false);
            course.setPrice(normalizedPrice);
        }
    }

    private String joinRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) return null;
        return roles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(","));
    }

    private List<String> splitRoles(String stored) {
        if (stored == null || stored.trim().isEmpty()) return Collections.emptyList();
        return Arrays.stream(stored.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private Set<Category> resolveCategories(List<String> slugs) {
        if (slugs == null || slugs.isEmpty()) {
            return Collections.emptySet();
        }
        List<String> cleaned = slugs.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        if (cleaned.isEmpty()) {
            return Collections.emptySet();
        }
        Map<String, Category> lookup = categoryRepository.findBySlugIn(cleaned)
                .stream()
                .filter(cat -> cat.getSlug() != null)
                .collect(Collectors.toMap(cat -> cat.getSlug().toLowerCase(), cat -> cat, (a, b) -> a));
        Set<Category> result = new LinkedHashSet<>();
        for (String slug : cleaned) {
            Category cat = lookup.get(slug.toLowerCase());
            if (cat != null) {
                result.add(cat);
            }
        }
        return result;
    }

    private List<String> courseCategories(Course course) {
        return course.getCategories().stream()
                .map(Category::getSlug)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private BigDecimal normalizePrice(BigDecimal price) {
        if (price == null) return null;
        return price.compareTo(BigDecimal.ZERO) > 0 ? price : null;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','MANAGER')")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication auth) {
        Course c = courseRepository.findById(id).orElseThrow();
        boolean isOwner = c.getCreatedBy().getEmail().equalsIgnoreCase(String.valueOf(auth.getPrincipal()));
        boolean isManager = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
        if (!(isOwner || isManager)) return ResponseEntity.status(403).body("Not owner");
        courseRepository.delete(c);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('TEACHER','MANAGER')")
    @Transactional
    public ResponseEntity<?> submitForReview(@PathVariable Long id, Authentication auth) {
        Course c = courseRepository.findById(id).orElseThrow();
        if (!c.getCreatedBy().getEmail().equalsIgnoreCase(String.valueOf(auth.getPrincipal()))) {
            return ResponseEntity.status(403).body("Not owner");
        }
        c.setApprovalStatus("pending");
        c.setSubmittedAt(LocalDateTime.now());
        courseRepository.save(c);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
