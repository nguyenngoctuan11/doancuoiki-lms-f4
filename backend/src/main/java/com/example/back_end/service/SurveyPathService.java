package com.example.back_end.service;

import com.example.back_end.dto.SurveyDtos;
import com.example.back_end.model.User;
import com.example.back_end.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SurveyPathService {
    @PersistenceContext private EntityManager em;
    private final UserRepository userRepository;
    public SurveyPathService(UserRepository userRepository){ this.userRepository = userRepository; }

    @Transactional(readOnly = true)
    public List<SurveyDtos.Question> loadSurvey(){
        @SuppressWarnings("unchecked")
        List<Object[]> qs = (List<Object[]>) em.createNativeQuery("SELECT id, code, text FROM dbo.survey_questions ORDER BY sort_order, id").getResultList();
        List<SurveyDtos.Question> out = new ArrayList<>();
        for(Object[] q: qs){
            SurveyDtos.Question qq = new SurveyDtos.Question();
            qq.id = toLong(q[0]); qq.code = str(q[1]); qq.text = str(q[2]);
            @SuppressWarnings("unchecked")
            List<Object[]> ops = (List<Object[]>) em.createNativeQuery("SELECT id, code, text FROM dbo.survey_options WHERE question_id = :qid ORDER BY sort_order, id")
                    .setParameter("qid", qq.id).getResultList();
            qq.options = ops.stream().map(o -> { SurveyDtos.Option op = new SurveyDtos.Option(); op.id=toLong(o[0]); op.code=str(o[1]); op.text=str(o[2]); return op; }).collect(Collectors.toList());
            out.add(qq);
        }
        return out;
    }

    @Transactional
    public SurveyDtos.PathResponse submitSurvey(String email, List<String> selectedCodes){
        // Determine target tag and level from selected option codes
        String tagSlug = pick("SELECT TOP 1 weight_tag_slug FROM dbo.survey_options WHERE code IN :codes AND weight_tag_slug IS NOT NULL ORDER BY sort_order", selectedCodes);
        String level = pick("SELECT TOP 1 weight_level FROM dbo.survey_options WHERE code IN :codes AND weight_level IS NOT NULL ORDER BY sort_order", selectedCodes);
        if(level == null) level = "beginner";

        // Query courses matching (build params conditionally)
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT TOP 6 c.id, c.title, c.slug, c.level, c.thumbnail_url FROM dbo.courses c ");
        if(tagSlug != null){ sql.append("JOIN dbo.course_tags ct ON ct.course_id = c.id JOIN dbo.tags t ON t.id = ct.tag_id "); }
        sql.append("WHERE c.status = N'published' ");
        if(level != null){ sql.append(" AND c.level = :level "); }
        if(tagSlug != null){ sql.append(" AND t.slug = :tag "); }
        sql.append(" ORDER BY c.created_at DESC");
        var q = em.createNativeQuery(sql.toString());
        if(level != null) q.setParameter("level", level);
        if(tagSlug != null) q.setParameter("tag", tagSlug);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = (List<Object[]>) q.getResultList();
        if(rows.isEmpty()){
            @SuppressWarnings("unchecked")
            List<Object[]> fb = (List<Object[]>) em.createNativeQuery("SELECT TOP 6 id, title, slug, level, thumbnail_url FROM dbo.courses WHERE status = N'published' ORDER BY created_at DESC").getResultList();
            rows = fb;
        }

        // Create learning path
        Long userId = null;
        if(email != null){ userId = userRepository.findByEmailIgnoreCase(email).map(User::getId).orElse(null); }
        if(userId == null){
            @SuppressWarnings("unchecked")
            List<Object> ids = (List<Object>) em.createNativeQuery("SELECT TOP 1 id FROM dbo.users ORDER BY id").getResultList();
            if(ids.isEmpty()) throw new IllegalArgumentException("Chưa có người dùng nào trong hệ thống để gán created_by cho lộ trình");
            userId = ((Number) ids.get(0)).longValue();
        }
        Object pidObj = em.createNativeQuery(
                "INSERT INTO dbo.learning_paths(name, description, created_by) OUTPUT inserted.id VALUES (?,?,?)")
                .setParameter(1, "Lộ trình đề xuất")
                .setParameter(2, tagSlug!=null? ("Theo mảng: "+tagSlug+", level: "+level): ("Theo level: "+level))
                .setParameter(3, userId)
                .getSingleResult();
        Long pathId = ((Number) pidObj).longValue();
        int sort=0; for(Object[] r: rows){ sort++;
            em.createNativeQuery("INSERT INTO dbo.learning_path_items(path_id, item_type, item_id, sort_order) VALUES (?,?,?,?)")
                    .setParameter(1, pathId).setParameter(2, "course").setParameter(3, toLong(r[0])).setParameter(4, sort).executeUpdate();
        }

        SurveyDtos.PathResponse res = new SurveyDtos.PathResponse();
        res.pathId = pathId; res.name = "Lộ trình đề xuất";
        res.items = rows.stream().map(r -> { SurveyDtos.CourseItem it = new SurveyDtos.CourseItem(); it.courseId = toLong(r[0]); it.title=str(r[1]); it.slug=str(r[2]); it.level=str(r[3]); it.thumbnailUrl=str(r[4]); return it;}).collect(Collectors.toList());
        return res;
    }

    @Transactional(readOnly = true)
    public SurveyDtos.PathResponse getPathDetail(Long pathId){
        @SuppressWarnings("unchecked")
        List<Object[]> items = (List<Object[]>) em.createNativeQuery(
                "SELECT c.id, c.title, c.slug, c.level, c.thumbnail_url FROM dbo.learning_path_items i JOIN dbo.courses c ON c.id = i.item_id WHERE i.path_id = :pid ORDER BY i.sort_order, i.id")
                .setParameter("pid", pathId).getResultList();
        SurveyDtos.PathResponse res = new SurveyDtos.PathResponse();
        res.pathId = pathId; res.name = "Lộ trình"; res.items = items.stream().map(r->{ SurveyDtos.CourseItem it=new SurveyDtos.CourseItem(); it.courseId=toLong(r[0]); it.title=str(r[1]); it.slug=str(r[2]); it.level=str(r[3]); it.thumbnailUrl=str(r[4]); return it;}).collect(Collectors.toList());
        return res;
    }

    private static Long toLong(Object o){ return o==null?null: ((Number)o).longValue(); }
    private static String str(Object o){ return o==null?null:o.toString(); }
    private String pick(String sql, List<String> codes){
        if(codes==null||codes.isEmpty()) return null;
        List<?> r = em.createNativeQuery(sql).unwrap(org.hibernate.query.NativeQuery.class).setParameterList("codes", codes).getResultList();
        return r.isEmpty()? null: String.valueOf(r.get(0));
    }
}
