package com.example.back_end.service;

import com.example.back_end.dto.LessonResourceDtos;
import com.example.back_end.model.Course;
import com.example.back_end.model.Lesson;
import com.example.back_end.model.LessonResource;
import com.example.back_end.model.Role;
import com.example.back_end.model.User;
import com.example.back_end.repository.LessonResourceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class LessonResourceService {
    private final LessonResourceRepository resourceRepository;

    @PersistenceContext
    private EntityManager em;

    public LessonResourceService(LessonResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @Transactional(readOnly = true)
    public List<LessonResourceDtos.ResourceResponse> listForStudent(User user, Long courseId, Long lessonId) {
        assertLessonInCourse(courseId, lessonId);
        AccessInfo access = resolveAccess(user, courseId);
        if (!access.canView()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "B\u1EA1n ch\u01B0a \u0111\u01B0\u1EE3c truy c\u1EADp t\u00E0i li\u1EC7u b\u00E0i h\u1ECDc n\u00E0y");
        }
        List<LessonResource> resources = resourceRepository.findByLesson_IdAndCourse_IdOrderByCreatedAtDesc(lessonId, courseId);
        return resources.stream()
                .filter(r -> canSeeResource(access, r))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LessonResourceDtos.ResourceResponse> listForTeacher(User user, Long courseId, Long lessonId) {
        assertLessonInCourse(courseId, lessonId);
        AccessInfo access = resolveAccess(user, courseId);
        if (!access.canContribute()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "B\u1EA1n kh\u00F4ng c\u00F3 quy\u1EC1n xem t\u00E0i li\u1EC7u b\u00E0i h\u1ECDc n\u00E0y");
        }
        List<LessonResource> resources = resourceRepository.findByLesson_IdAndCourse_IdOrderByCreatedAtDesc(lessonId, courseId);
        return resources.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public LessonResourceDtos.ResourceResponse create(User user, Long courseId, Long lessonId, LessonResourceDtos.CreateRequest request) {
        assertLessonInCourse(courseId, lessonId);
        AccessInfo access = resolveAccess(user, courseId);
        if (!access.canContribute()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ch\u1EC9 gi\u1EA3ng vi\u00EAn m\u1EDBi c\u00F3 th\u1EC3 t\u1EA3i l\u00EAn t\u00E0i li\u1EC7u");
        }
        LessonResource resource = new LessonResource();
        resource.setCourse(referenceCourse(courseId));
        resource.setLesson(referenceLesson(lessonId));
        resource.setCreatedBy(user);
        applyResourcePayload(resource, request);
        resource.setStatus(access.isManager ? "approved" : "pending");
        if (resource.getDownloadCount() == null) {
            resource.setDownloadCount(0);
        }
        return toDto(resourceRepository.save(resource));
    }

    @Transactional(readOnly = true)
    public List<LessonResourceDtos.ResourceResponse> listPublicResources(Long courseId, Long lessonId) {
        if (courseId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thi\u1EBFu kh\u00F3a h\u1ECDc");
        }
        List<LessonResource> resources;
        if (lessonId != null) {
            assertLessonInCourse(courseId, lessonId);
            resources = resourceRepository
                    .findByCourse_IdAndLesson_IdAndVisibilityIgnoreCaseAndStatusIgnoreCaseOrderByCreatedAtDesc(
                            courseId,
                            lessonId,
                            "public",
                            "approved"
                    );
        } else {
            resources = resourceRepository
                    .findByCourse_IdAndVisibilityIgnoreCaseAndStatusIgnoreCaseOrderByCreatedAtDesc(
                            courseId,
                            "public",
                            "approved"
                    );
        }
        return resources.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public LessonResourceDtos.ResourceResponse update(User user, Long resourceId, LessonResourceDtos.UpdateRequest request) {
        LessonResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kh\u00F4ng t\u00ECm th\u1EA5y t\u00E0i li\u1EC7u"));
        AccessInfo access = resolveAccess(user, resource.getCourse().getId());
        boolean canManage = access.canContribute();
        if (!canManage) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "B\u1EA1n kh\u00F4ng c\u00F3 quy\u1EC1n s\u1EEDa t\u00E0i li\u1EC7u");
        }
        applyUpdate(resource, request, access.isManager);
        return toDto(resourceRepository.save(resource));
    }

    @Transactional
    public LessonResourceDtos.ResourceResponse changeStatus(User user, Long resourceId, String status) {
        LessonResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kh\u00F4ng t\u00ECm th\u1EA5y t\u00E0i li\u1EC7u"));
        AccessInfo access = resolveAccess(user, resource.getCourse().getId());
        if (!access.isManager) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ch\u1EC9 qu\u1EA3n l\u00FD m\u1EDBi \u0111\u01B0\u1EE3c duy\u1EC7t t\u00E0i li\u1EC7u");
        }
        resource.setStatus(normalizeStatus(status));
        return toDto(resourceRepository.save(resource));
    }

    @Transactional(readOnly = true)
    public List<LessonResourceDtos.ResourceResponse> listForManager(
            User user,
            Long courseId,
            Long lessonId,
            String status,
            String visibility,
            String keyword
    ) {
        if (!hasRole(user, "manager")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "B\u1EA1n kh\u00F4ng c\u00F3 quy\u1EC1n xem danh s\u00E1ch n\u00E0y");
        }
        String normalizedStatus = normalizeFilter(status);
        String normalizedVisibility = normalizeFilter(visibility);
        String keywordFilter = keyword != null && !keyword.trim().isEmpty() ? keyword.trim() : null;
        List<LessonResource> resources = resourceRepository.searchResources(
                courseId,
                lessonId,
                normalizedStatus,
                normalizedVisibility,
                keywordFilter
        );
        return resources.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public void delete(User user, Long resourceId) {
        LessonResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kh\u00F4ng t\u00ECm th\u1EA5y t\u00E0i li\u1EC7u"));
        AccessInfo access = resolveAccess(user, resource.getCourse().getId());
        if (!access.canContribute()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "B\u1EA1n kh\u00F4ng c\u00F3 quy\u1EC1n x\u00F3a t\u00E0i li\u1EC7u");
        }
        resource.setStatus("hidden");
        resourceRepository.save(resource);
    }

    @Transactional
    public LessonResourceDtos.DownloadPayload prepareDownload(User user, Long resourceId) {
        LessonResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kh\u00F4ng t\u00ECm th\u1EA5y t\u00E0i li\u1EC7u"));
        AccessInfo access = resolveAccess(user, resource.getCourse().getId());
        if (!canDownload(access, resource)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "B\u1EA1n kh\u00F4ng c\u00F3 quy\u1EC1n t\u1EA3i t\u00E0i li\u1EC7u n\u00E0y");
        }
        if (resource.getDownloadCount() == null) {
            resource.setDownloadCount(0);
        }
        resource.setDownloadCount(resource.getDownloadCount() + 1);
        resourceRepository.save(resource);

        LessonResourceDtos.DownloadPayload payload = new LessonResourceDtos.DownloadPayload();
        payload.redirect = true;
        if ("link".equalsIgnoreCase(resource.getSourceType()) && StringUtils.hasText(resource.getExternalUrl())) {
            payload.url = resource.getExternalUrl();
            return payload;
        }
        if (StringUtils.hasText(resource.getFileUrl())) {
            payload.url = resource.getFileUrl();
            return payload;
        }
        String fallback = buildUrlFromStorage(resource.getStoragePath());
        if (!StringUtils.hasText(fallback)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Kh\u00F4ng t\u00ECm th\u1EA5y \u0111\u01B0\u1EDDng d\u1EABn t\u00E0i li\u1EC7u");
        }
        payload.url = fallback;
        return payload;
    }

    private String buildUrlFromStorage(String storagePath) {
        if (!StringUtils.hasText(storagePath)) return null;
        String normalized = storagePath.replace("\\", "/");
        String lower = normalized.toLowerCase(Locale.ROOT);
        int idx = lower.indexOf("/uploads/");
        if (idx >= 0) {
            return normalized.substring(idx);
        }
        return null;
    }

    private void applyResourcePayload(LessonResource resource, LessonResourceDtos.CreateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thi\u1EBFu th\u00F4ng tin t\u00E0i li\u1EC7u");
        }
        String title = normalizeTitle(request.title);
        if (!StringUtils.hasText(title)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ti\u00EAu \u0111\u1EC1 t\u00E0i li\u1EC7u kh\u00F4ng h\u1EE3p l\u1EC7");
        }
        resource.setTitle(title);
        resource.setDescription(normalize(request.description, 1000));
        resource.setSourceType(normalizeSourceType(request.sourceType));
        resource.setVisibility(normalizeVisibility(request.visibility));
        resource.setFileUrl(trimToNull(request.fileUrl));
        resource.setExternalUrl(trimToNull(request.externalUrl));
        resource.setStoragePath(trimToNull(request.storagePath));
        resource.setFileType(trimToNull(request.fileType));
        resource.setFileSize(request.fileSize != null && request.fileSize >= 0 ? request.fileSize : null);
        if ("file".equalsIgnoreCase(resource.getSourceType()) && !StringUtils.hasText(resource.getStoragePath())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thi\u1EBFu \u0111\u01B0\u1EDDng d\u1EABn t\u1EC7p t\u1EA3i l\u00EAn");
        }
        if ("link".equalsIgnoreCase(resource.getSourceType()) && !StringUtils.hasText(resource.getExternalUrl())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui l\u00F2ng cung c\u1EA5p URL t\u00E0i li\u1EC7u");
        }
    }

    private void applyUpdate(LessonResource resource, LessonResourceDtos.UpdateRequest request, boolean manager) {
        if (request == null) return;
        if (StringUtils.hasText(request.title)) {
            resource.setTitle(normalizeTitle(request.title));
        }
        if (request.description != null) {
            resource.setDescription(normalize(request.description, 1000));
        }
        if (StringUtils.hasText(request.visibility)) {
            resource.setVisibility(normalizeVisibility(request.visibility));
        }
        if (StringUtils.hasText(request.sourceType)) {
            resource.setSourceType(normalizeSourceType(request.sourceType));
        }
        if (request.storagePath != null) resource.setStoragePath(trimToNull(request.storagePath));
        if (request.fileUrl != null) resource.setFileUrl(trimToNull(request.fileUrl));
        if (request.externalUrl != null) resource.setExternalUrl(trimToNull(request.externalUrl));
        if (request.fileType != null) resource.setFileType(trimToNull(request.fileType));
        if (request.fileSize != null) resource.setFileSize(Math.max(0, request.fileSize));
        if (manager && StringUtils.hasText(request.status)) {
            resource.setStatus(normalizeStatus(request.status));
        }
    }

    private boolean canSeeResource(AccessInfo access, LessonResource resource) {
        if (access.isInstructor || access.isManager || access.isTeacher) return true;
        if (!"approved".equalsIgnoreCase(resource.getStatus())) {
            return false;
        }
        String vis = resource.getVisibility() != null ? resource.getVisibility().toLowerCase(Locale.ROOT) : "enrolled";
        if ("public".equals(vis)) return true;
        if ("enrolled".equals(vis)) return access.enrolled;
        return false;
    }

    private boolean canDownload(AccessInfo access, LessonResource resource) {
        return canSeeResource(access, resource);
    }

    private LessonResourceDtos.ResourceResponse toDto(LessonResource resource) {
        LessonResourceDtos.ResourceResponse dto = new LessonResourceDtos.ResourceResponse();
        dto.id = resource.getId();
        dto.courseId = resource.getCourse() != null ? resource.getCourse().getId() : null;
        dto.lessonId = resource.getLesson() != null ? resource.getLesson().getId() : null;
        dto.courseTitle = resource.getCourse() != null ? resource.getCourse().getTitle() : null;
        dto.lessonTitle = resource.getLesson() != null ? resource.getLesson().getTitle() : null;
        dto.title = resource.getTitle();
        dto.description = resource.getDescription();
        dto.sourceType = resource.getSourceType();
        dto.fileUrl = resource.getFileUrl();
        dto.externalUrl = resource.getExternalUrl();
        dto.fileSize = resource.getFileSize();
        dto.fileType = resource.getFileType();
        dto.visibility = resource.getVisibility();
        dto.status = resource.getStatus();
        dto.downloadCount = resource.getDownloadCount();
        dto.createdAt = resource.getCreatedAt();
        dto.updatedAt = resource.getUpdatedAt();
        dto.downloadUrl = resolveDownloadUrl(resource);
        if (resource.getCreatedBy() != null) {
            LessonResourceDtos.SimpleUser user = new LessonResourceDtos.SimpleUser();
            user.id = resource.getCreatedBy().getId();
            user.name = resource.getCreatedBy().getFullName();
            user.avatar = resource.getCreatedBy().getAvatarUrl();
            dto.createdBy = user;
        }
        return dto;
    }

    private String resolveDownloadUrl(LessonResource resource) {
        if (resource == null) return null;
        if ("link".equalsIgnoreCase(resource.getSourceType()) && StringUtils.hasText(resource.getExternalUrl())) {
            return resource.getExternalUrl();
        }
        if (StringUtils.hasText(resource.getFileUrl())) {
            return resource.getFileUrl();
        }
        return buildUrlFromStorage(resource.getStoragePath());
    }

    private void assertLessonInCourse(Long courseId, Long lessonId) {
        Number count = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM dbo.lessons l JOIN dbo.modules m ON m.id = l.module_id WHERE l.id = :lessonId AND m.course_id = :courseId")
                .setParameter("lessonId", lessonId)
                .setParameter("courseId", courseId)
                .getSingleResult();
        if (count == null || count.intValue() == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "B\u00E0i h\u1ECDc kh\u00F4ng thu\u1ED9c kh\u00F3a h\u1ECDc");
        }
    }

    private AccessInfo resolveAccess(User user, Long courseId) {
        AccessInfo info = new AccessInfo();
        info.isManager = hasRole(user, "manager");
        info.isInstructor = info.isManager || isCourseInstructor(user.getId(), courseId);
        info.enrolled = isEnrolled(user.getId(), courseId);
        info.isTeacher = hasRole(user, "teacher");
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
        for (Role role : user.getRoles()) {
            if (role.getCode() != null && role.getCode().trim().equalsIgnoreCase(code)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeTitle(String title) {
        String value = title == null ? "" : title.trim();
        if (value.length() > 255) {
            value = value.substring(0, 255);
        }
        return value;
    }

    private String normalize(String value, int max) {
        if (value == null) return null;
        String v = value.trim();
        if (v.length() > max) {
            return v.substring(0, max);
        }
        return v;
    }

    private String normalizeSourceType(String type) {
        String value = (type == null ? "file" : type.trim().toLowerCase(Locale.ROOT));
        if (!value.equals("file") && !value.equals("link")) {
            return "file";
        }
        return value;
    }

    private String normalizeVisibility(String visibility) {
        String value = visibility == null ? "enrolled" : visibility.trim().toLowerCase(Locale.ROOT);
        if (value.equals("public") || value.equals("instructors")) {
            return value;
        }
        return "enrolled";
    }

    private String normalizeStatus(String status) {
        String value = status == null ? "pending" : status.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "approved" -> "approved";
            case "rejected" -> "rejected";
            case "hidden" -> "hidden";
            default -> "pending";
        };
    }

    private String normalizeFilter(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("all") || trimmed.equalsIgnoreCase("tat ca")) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Course referenceCourse(Long courseId) {
        return em.getReference(Course.class, courseId);
    }

    private Lesson referenceLesson(Long lessonId) {
        return em.getReference(Lesson.class, lessonId);
    }

    private static class AccessInfo {
        boolean enrolled;
        boolean isInstructor;
        boolean isManager;
        boolean isTeacher;

        boolean canView() {
            return enrolled || isInstructor || isManager;
        }

        boolean canContribute() {
            return isManager || isInstructor || isTeacher;
        }
    }
}
