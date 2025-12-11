package com.example.back_end.service;

import com.example.back_end.dto.CourseDtos;
import com.example.back_end.model.Category;
import com.example.back_end.model.Course;
import com.example.back_end.model.User;
import com.example.back_end.repository.CategoryRepository;
import com.example.back_end.repository.CourseRepository;
import com.example.back_end.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CourseService {
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public CourseService(
            CourseRepository courseRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository
    ) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public CourseDtos.CourseResponse create(CourseDtos.CreateRequest req) {
        if (req == null) throw new IllegalArgumentException("Thiếu dữ liệu khóa học");
        Course course = new Course();
        course.setCreatedBy(getCurrentUser());
        applyWritableFields(course, req, true);
        course = courseRepository.save(course);
        return toResponse(course);
    }

    @Transactional(readOnly = true)
    public List<CourseDtos.CourseResponse> list(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return courseRepository.findAllByOrderByIdDesc(PageRequest.of(0, safeLimit))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CourseDtos.CourseResponse get(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khóa học #" + id));
        return toResponse(course);
    }

    @Transactional
    public CourseDtos.CourseResponse update(Long id, CourseDtos.UpdateRequest req) {
        if (req == null) throw new IllegalArgumentException("Thiếu dữ liệu cập nhật");
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khóa học #" + id));
        applyWritableFields(course, req, false);
        course = courseRepository.save(course);
        return toResponse(course);
    }

    @Transactional
    public void delete(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy khóa học #" + id);
        }
        courseRepository.deleteById(id);
    }

    private CourseDtos.CourseResponse toResponse(Course course) {
        CourseDtos.CourseResponse dto = new CourseDtos.CourseResponse();
        dto.id = course.getId();
        dto.title = course.getTitle();
        dto.slug = course.getSlug();
        dto.shortDesc = course.getShortDesc();
        dto.language = course.getLanguage();
        dto.level = course.getLevel();
        dto.status = course.getStatus();
        dto.price = course.getPrice();
        dto.isFree = course.getIsFree();
        dto.thumbnailUrl = course.getThumbnailUrl();
        dto.approvalStatus = course.getApprovalStatus();
        dto.createdById = course.getCreatedBy() != null ? course.getCreatedBy().getId() : null;
        dto.createdByEmail = course.getCreatedBy() != null ? course.getCreatedBy().getEmail() : null;
        dto.targetRoles = splitRoles(course.getTargetRoles());
        dto.categories = course.getCategories().stream()
                .map(Category::getSlug)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return dto;
    }

    private void applyWritableFields(Course course, CourseDtos.CreateRequest req, boolean creating) {
        if (req.title != null || creating) {
            String title = trimToNull(req.title);
            if (title == null) throw new IllegalArgumentException("Tiêu đề không được bỏ trống");
            course.setTitle(title);
        }

        if (req.slug != null || creating) {
            String slug = trimToNull(req.slug);
            if (slug == null) throw new IllegalArgumentException("Slug không được bỏ trống");
            boolean exists = courseRepository.existsBySlugIgnoreCaseAndIdNot(slug, course.getId() != null ? course.getId() : -1L);
            if (exists && (!creating || course.getId() == null || course.getId() > 0)) {
                throw new IllegalArgumentException("Slug đã tồn tại");
            }
            course.setSlug(slug);
        }

        if (req.shortDesc != null || creating) {
            String desc = trimToNull(req.shortDesc);
            course.setShortDesc(desc);
        }

        if (req.language != null || creating) {
            String language = trimToNull(req.language);
            course.setLanguage(language != null ? language.toLowerCase(Locale.ROOT) : "vi");
        }

        if (req.level != null || creating) {
            String level = trimToNull(req.level);
            course.setLevel(level != null ? level.toLowerCase(Locale.ROOT) : "beginner");
        }

        if (req.status != null || creating) {
            String status = trimToNull(req.status);
            course.setStatus(status != null ? status.toLowerCase(Locale.ROOT) : "draft");
        }

        if (req.targetRoles != null || creating) {
            course.setTargetRoles(joinRoles(req.targetRoles));
        }

        if (req.categories != null || creating) {
            course.setCategories(resolveCategories(req.categories));
        }

        if (req.price != null || req.isFree != null || creating) {
            applyPricing(course, req, creating);
        }

        if (req.thumbnailUrl != null || creating) {
            String thumbnail = trimToNull(req.thumbnailUrl);
            course.setThumbnailUrl(thumbnail);
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
                throw new IllegalArgumentException("Vui lòng nhập giá lớn hơn 0 cho khóa học Pro");
            }
            course.setIsFree(false);
            course.setPrice(normalizedPrice);
        }
    }

    private BigDecimal normalizePrice(BigDecimal price) {
        if (price == null) return null;
        return price.compareTo(BigDecimal.ZERO) > 0 ? price : null;
    }

    private String trimToNull(String value){
        if(value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
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

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth != null ? String.valueOf(auth.getPrincipal()) : null;
        if (email == null) throw new IllegalStateException("Chưa đăng nhập");
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng hiện tại"));
    }
}
