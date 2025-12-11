package com.example.back_end.service;

import com.example.back_end.dto.public_.PublicCourseDetailDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PublicCourseQueryService {
    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public PublicCourseDetailDto loadCourseDetailBySlug(String slug) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = (List<Object[]>) em.createNativeQuery(
                "SELECT id, title, slug, level, status, price, is_free, thumbnail_url FROM dbo.courses WHERE slug = :slug")
                .setParameter("slug", slug)
                .getResultList();
        if (rows.isEmpty()) return null;
        Object[] c = rows.get(0);
        PublicCourseDetailDto dto = new PublicCourseDetailDto();
        dto.id = toLong(c[0]);
        dto.title = str(c[1]);
        dto.slug = str(c[2]);
        dto.level = str(c[3]);
        dto.status = str(c[4]);
        dto.price = (c[5] instanceof BigDecimal) ? (BigDecimal) c[5] : null;
        dto.is_free = bool(c[6]);
        dto.thumbnail_url = str(c[7]);

        @SuppressWarnings("unchecked")
        List<Object[]> modules = (List<Object[]>) em.createNativeQuery(
                "SELECT id, title, sort_order FROM dbo.modules WHERE course_id = :cid ORDER BY sort_order, id")
                .setParameter("cid", dto.id).getResultList();
        for (Object[] m : modules) {
            PublicCourseDetailDto.ModuleItem mi = new PublicCourseDetailDto.ModuleItem();
            mi.id = toLong(m[0]);
            mi.title = str(m[1]);
            mi.sort_order = toInt(m[2]);

            @SuppressWarnings("unchecked")
            List<Object[]> lessons = (List<Object[]>) em.createNativeQuery(
                    "SELECT id, title, type, duration_seconds, sort_order, status FROM dbo.lessons WHERE module_id = :mid ORDER BY sort_order, id")
                    .setParameter("mid", mi.id).getResultList();
            for (Object[] l : lessons) {
                PublicCourseDetailDto.LessonItem li = new PublicCourseDetailDto.LessonItem();
                li.id = toLong(l[0]);
                li.title = str(l[1]);
                li.type = str(l[2]);
                li.duration_seconds = toInt(l[3]);
                li.sort_order = toInt(l[4]);
                li.status = str(l[5]);
                @SuppressWarnings("unchecked")
                List<String> vids = (List<String>) em.createNativeQuery(
                        "SELECT TOP 1 url FROM dbo.lesson_assets WHERE lesson_id = :lid AND kind = N'video' ORDER BY id")
                        .setParameter("lid", li.id).getResultList();
                li.video_url = vids.isEmpty() ? null : vids.get(0);
                mi.lessons.add(li);
            }
            dto.modules.add(mi);
        }
        return dto;
    }

    @Transactional(readOnly = true)
    public PublicCourseDetailDto loadCourseDetailById(Long courseId, boolean includeCreator) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = (List<Object[]>) em.createNativeQuery(
                "SELECT c.id, c.title, c.slug, c.level, c.status, c.price, c.is_free, c.thumbnail_url" +
                        (includeCreator ? ", u.email, u.full_name" : "") +
                        " FROM dbo.courses c" +
                        (includeCreator ? " JOIN dbo.users u ON u.id = c.created_by" : "") +
                        " WHERE c.id = :cid")
                .setParameter("cid", courseId)
                .getResultList();
        if (rows.isEmpty()) return null;
        Object[] c = rows.get(0);
        PublicCourseDetailDto dto = new PublicCourseDetailDto();
        dto.id = toLong(c[0]);
        dto.title = str(c[1]);
        dto.slug = str(c[2]);
        dto.level = str(c[3]);
        dto.status = str(c[4]);
        dto.price = (c[5] instanceof java.math.BigDecimal) ? (java.math.BigDecimal) c[5] : null;
        dto.is_free = bool(c[6]);
        dto.thumbnail_url = str(c[7]);
        if (includeCreator) {
            dto.created_by_email = str(c[8]);
            dto.created_by_name = str(c[9]);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> modules = (List<Object[]>) em.createNativeQuery(
                        "SELECT id, title, sort_order FROM dbo.modules WHERE course_id = :cid ORDER BY sort_order, id")
                .setParameter("cid", dto.id).getResultList();
        for (Object[] m : modules) {
            PublicCourseDetailDto.ModuleItem mi = new PublicCourseDetailDto.ModuleItem();
            mi.id = toLong(m[0]);
            mi.title = str(m[1]);
            mi.sort_order = toInt(m[2]);

            @SuppressWarnings("unchecked")
            List<Object[]> lessons = (List<Object[]>) em.createNativeQuery(
                            "SELECT id, title, type, duration_seconds, sort_order, status FROM dbo.lessons WHERE module_id = :mid ORDER BY sort_order, id")
                    .setParameter("mid", mi.id).getResultList();
            for (Object[] l : lessons) {
                PublicCourseDetailDto.LessonItem li = new PublicCourseDetailDto.LessonItem();
                li.id = toLong(l[0]);
                li.title = str(l[1]);
                li.type = str(l[2]);
                li.duration_seconds = toInt(l[3]);
                li.sort_order = toInt(l[4]);
                li.status = str(l[5]);
                @SuppressWarnings("unchecked")
                List<String> vids = (List<String>) em.createNativeQuery(
                                "SELECT TOP 1 url FROM dbo.lesson_assets WHERE lesson_id = :lid AND kind = N'video' ORDER BY id")
                        .setParameter("lid", li.id).getResultList();
                li.video_url = vids.isEmpty() ? null : vids.get(0);
                mi.lessons.add(li);
            }
            dto.modules.add(mi);
        }
        return dto;
    }

    private static Long toLong(Object o){ return o==null?null: ((Number)o).longValue(); }
    private static Integer toInt(Object o){ return o==null?null: ((Number)o).intValue(); }
    private static String str(Object o){ return o==null?null:o.toString(); }
    private static Boolean bool(Object o){
        if(o == null) return null;
        if(o instanceof Boolean) return (Boolean) o;
        if(o instanceof Number) return ((Number) o).intValue() != 0;
        return Boolean.valueOf(o.toString());
    }
}
