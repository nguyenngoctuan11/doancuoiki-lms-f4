package com.example.back_end.controller;

import com.example.back_end.dto.QuizDtos;
import com.example.back_end.model.Course;
import com.example.back_end.model.User;
import com.example.back_end.repository.CourseRepository;
import com.example.back_end.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/quizzes")
public class TeacherQuizController {
    @PersistenceContext private EntityManager em;
    private final CourseRepository courseRepository; private final UserRepository userRepository;
    public TeacherQuizController(CourseRepository courseRepository, UserRepository userRepository){ this.courseRepository=courseRepository; this.userRepository=userRepository; }
    private User currentUser(Authentication auth){ return userRepository.findByEmailIgnoreCase(String.valueOf(auth.getPrincipal())).orElseThrow(); }
    private boolean isManager(Authentication auth){ return auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_MANAGER")); }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','MANAGER')")
    @Transactional
    public ResponseEntity<?> create(@RequestBody QuizDtos.CreateQuizRequest req, Authentication auth){
        Course c = courseRepository.findById(req.courseId).orElseThrow();
        // ownership check (teacher owner) — managers bypass
        boolean isManager = auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_MANAGER"));
        if(!isManager && !c.getCreatedBy().getEmail().equalsIgnoreCase(String.valueOf(auth.getPrincipal()))) return ResponseEntity.status(403).body("Not owner");
        Object idObj = em.createNativeQuery(
                "INSERT INTO dbo.quizzes(course_id, title, time_limit_sec, shuffle, grading_policy) " +
                "OUTPUT inserted.id VALUES (?,?,?,?,N'manual')")
                .setParameter(1, c.getId())
                .setParameter(2, (req.title!=null && !req.title.isBlank()) ? req.title : "Bài kiểm tra")
                .setParameter(3, req.timeLimitSec!=null? req.timeLimitSec: 1200)
                .setParameter(4, Boolean.TRUE.equals(req.shuffle)? 1: 0)
                .getSingleResult();
        Long id = ((Number) idObj).longValue();
        return ResponseEntity.ok(Map.of("id", id));
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('TEACHER','MANAGER')")
    public ResponseEntity<List<Map<String,Object>>> listForCourse(@PathVariable Long courseId){
        @SuppressWarnings("unchecked")
        List<Object[]> rows = (List<Object[]>) em.createNativeQuery(
                "SELECT q.id, q.title, q.time_limit_sec, (SELECT COUNT(*) FROM dbo.questions WHERE quiz_id=q.id) FROM dbo.quizzes q WHERE q.course_id = :cid ORDER BY q.id DESC")
                .setParameter("cid", courseId).getResultList();
        List<Map<String,Object>> list = new java.util.ArrayList<>();
        for(Object[] r : rows){
            java.util.Map<String,Object> m = new java.util.LinkedHashMap<>();
            m.put("id", ((Number)r[0]).longValue());
            m.put("title", String.valueOf(r[1]));
            m.put("timeLimitSec", r[2]==null? null: ((Number)r[2]).intValue());
            m.put("questions", ((Number)r[3]).intValue());
            list.add(m);
        }
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{quizId}")
    @PreAuthorize("hasAnyRole('TEACHER','MANAGER')")
    public ResponseEntity<QuizDtos.QuizDetail> detail(@PathVariable Long quizId, Authentication auth){
        QuizInfo info = requireQuiz(quizId, auth);
        List<QuizDtos.QuestionDetail> questions = loadQuestionDetails(quizId);
        QuizDtos.QuizDetail dto = new QuizDtos.QuizDetail();
        dto.id = info.id;
        dto.courseId = info.courseId;
        dto.title = info.title;
        dto.timeLimitSec = info.timeLimitSec;
        dto.shuffle = info.shuffle;
        dto.questions = questions;
        dto.questionCount = questions.size();
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{quizId}")
    @PreAuthorize("hasAnyRole('TEACHER','MANAGER')")
    @Transactional
    public ResponseEntity<Map<String,Object>> update(@PathVariable Long quizId, @RequestBody QuizDtos.UpdateQuizRequest req, Authentication auth){
        if(req == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu dữ liệu cập nhật");
        }
        QuizInfo info = requireQuiz(quizId, auth);
        String title = (req.title!=null && !req.title.isBlank()) ? req.title.trim() : info.title;
        Integer time = (req.timeLimitSec!=null && req.timeLimitSec>0)? req.timeLimitSec : info.timeLimitSec;
        boolean shuffle = req.shuffle!=null ? req.shuffle : Boolean.TRUE.equals(info.shuffle);
        em.createNativeQuery("UPDATE dbo.quizzes SET title=?, time_limit_sec=?, shuffle=?, updated_at=SYSUTCDATETIME() WHERE id=?")
                .setParameter(1, title)
                .setParameter(2, time)
                .setParameter(3, shuffle?1:0)
                .setParameter(4, quizId)
                .executeUpdate();
        Map<String,Object> res = new LinkedHashMap<>();
        res.put("id", quizId);
        res.put("title", title);
        res.put("timeLimitSec", time);
        res.put("shuffle", shuffle);
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/{quizId}")
    @PreAuthorize("hasAnyRole('TEACHER','MANAGER')")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long quizId, Authentication auth){
        requireQuiz(quizId, auth);
        em.createNativeQuery(
                        "UPDATE dbo.course_certificates SET attempt_id = NULL WHERE attempt_id IN (SELECT id FROM dbo.quiz_attempts WHERE quiz_id = :qid)")
                .setParameter("qid", quizId)
                .executeUpdate();
        em.createNativeQuery("DELETE FROM dbo.quiz_attempts WHERE quiz_id = :qid")
                .setParameter("qid", quizId)
                .executeUpdate();
        em.createNativeQuery("DELETE FROM dbo.quizzes WHERE id=:id")
                .setParameter("id", quizId)
                .executeUpdate();
        return ResponseEntity.noContent().build();
    }

    private QuizInfo requireQuiz(Long quizId, Authentication auth){
        @SuppressWarnings("unchecked")
        List<Object[]> rows = (List<Object[]>) em.createNativeQuery(
                "SELECT q.id, q.title, q.time_limit_sec, q.shuffle, q.course_id, c.created_by FROM dbo.quizzes q JOIN dbo.courses c ON c.id=q.course_id WHERE q.id=:id")
                .setParameter("id", quizId).getResultList();
        if(rows.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đề kiểm tra");
        }
        Object[] r = rows.get(0);
        if(!isManager(auth)){
            Long ownerId = ((Number) r[5]).longValue();
            if(!ownerId.equals(currentUser(auth).getId())){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thao tác đề này");
            }
        }
        QuizInfo info = new QuizInfo();
        info.id = ((Number) r[0]).longValue();
        info.title = String.valueOf(r[1]);
        info.timeLimitSec = r[2]==null? null: ((Number) r[2]).intValue();
        info.shuffle = toBoolean(r[3]);
        info.courseId = ((Number) r[4]).longValue();
        info.ownerId = ((Number) r[5]).longValue();
        return info;
    }

    private boolean toBoolean(Object value){
        if(value == null) return false;
        if(value instanceof Boolean b) return b;
        if(value instanceof Number n) return n.intValue() != 0;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private List<QuizDtos.QuestionDetail> loadQuestionDetails(Long quizId){
        @SuppressWarnings("unchecked")
        List<Object[]> rows = (List<Object[]>) em.createNativeQuery(
                "SELECT id, text, points, sort_order FROM dbo.questions WHERE quiz_id=:qid ORDER BY sort_order")
                .setParameter("qid", quizId).getResultList();
        @SuppressWarnings("unchecked")
        List<Object[]> optRows = (List<Object[]>) em.createNativeQuery(
                "SELECT q.id, o.id, o.text, o.is_correct, o.sort_order FROM dbo.question_options o JOIN dbo.questions q ON q.id=o.question_id WHERE q.quiz_id=:qid ORDER BY q.id, o.sort_order")
                .setParameter("qid", quizId).getResultList();
        Map<Long, List<QuizDtos.QuestionOptionDetail>> optionMap = new HashMap<>();
        for(Object[] o : optRows){
            Long qid = ((Number) o[0]).longValue();
            QuizDtos.QuestionOptionDetail opt = new QuizDtos.QuestionOptionDetail();
            opt.id = ((Number) o[1]).longValue();
            opt.text = o[2]==null? "" : String.valueOf(o[2]);
            opt.correct = toBoolean(o[3]);
            optionMap.computeIfAbsent(qid, k->new ArrayList<>()).add(opt);
        }
        List<QuizDtos.QuestionDetail> details = new ArrayList<>();
        for(Object[] row : rows){
            QuizDtos.QuestionDetail q = new QuizDtos.QuestionDetail();
            Long qid = ((Number) row[0]).longValue();
            q.id = qid;
            q.text = row[1]==null? "" : String.valueOf(row[1]);
            q.points = row[2]==null? null: ((Number) row[2]).intValue();
            q.sortOrder = row[3]==null? null: ((Number) row[3]).intValue();
            q.options = optionMap.getOrDefault(qid, java.util.Collections.emptyList());
            details.add(q);
        }
        return details;
    }

    private static class QuizInfo {
        Long id;
        String title;
        Integer timeLimitSec;
        Boolean shuffle;
        Long courseId;
        Long ownerId;
    }

    @PostMapping("/{quizId}/questions")
    @PreAuthorize("hasAnyRole('TEACHER','MANAGER')")
    @Transactional
    public ResponseEntity<?> addQuestion(@PathVariable Long quizId, @RequestBody QuizDtos.AddQuestionRequest req, Authentication auth){
        requireQuiz(quizId, auth);
        Object qidObj = em.createNativeQuery(
                "INSERT INTO dbo.questions(quiz_id, type, text, points, sort_order) " +
                "OUTPUT inserted.id VALUES(?, N'sc', ?, ?, (SELECT ISNULL(MAX(sort_order),0)+1 FROM dbo.questions WHERE quiz_id=?))")
                .setParameter(1, quizId)
                .setParameter(2, req.text)
                .setParameter(3, req.points!=null? req.points: 1)
                .setParameter(4, quizId)
                .getSingleResult();
        Long qid = ((Number) qidObj).longValue();
        if(req.options!=null){
            int i=0; for(QuizDtos.AddQuestionRequest.Option o : req.options){ i++;
                em.createNativeQuery("INSERT INTO dbo.question_options(question_id, text, is_correct, sort_order) VALUES (?,?,?,?)")
                        .setParameter(1, qid)
                        .setParameter(2, o.text)
                        .setParameter(3, Boolean.TRUE.equals(o.correct)?1:0)
                        .setParameter(4, i)
                        .executeUpdate();
            }
        }
        return ResponseEntity.ok(Map.of("id", qid));
    }
}
