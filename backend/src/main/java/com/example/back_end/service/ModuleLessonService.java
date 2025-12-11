package com.example.back_end.service;

import com.example.back_end.dto.ModuleLessonDtos;
import com.example.back_end.model.Course;
import com.example.back_end.model.CourseModule;
import com.example.back_end.model.Lesson;
import com.example.back_end.repository.CourseRepository;
import com.example.back_end.repository.LessonRepository;
import com.example.back_end.repository.ModuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ModuleLessonService {
    private final CourseRepository courseRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;

    public ModuleLessonService(CourseRepository courseRepository, ModuleRepository moduleRepository, LessonRepository lessonRepository) {
        this.courseRepository = courseRepository;
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
    }

    public List<ModuleLessonDtos.ModuleResponse> listModules(Long courseId) {
        return moduleRepository.findByCourse_IdOrderBySortOrderAscIdAsc(courseId).stream()
                .map(m -> toModuleResponse(m, true))
                .collect(Collectors.toList());
    }

    @Transactional
    public ModuleLessonDtos.ModuleResponse addModule(Long courseId, ModuleLessonDtos.ModuleRequest req) {
        Course c = courseRepository.findById(courseId).orElseThrow();
        CourseModule m = new CourseModule();
        m.setCourse(c);
        m.setTitle(req.title);
        m.setSortOrder(req.sortOrder != null ? req.sortOrder : 0);
        m = moduleRepository.save(m);
        return toModuleResponse(m, false);
    }

    @Transactional
    public ModuleLessonDtos.ModuleResponse updateModule(Long moduleId, ModuleLessonDtos.ModuleRequest req) {
        CourseModule m = moduleRepository.findById(moduleId).orElseThrow();
        if (req.title != null) m.setTitle(req.title);
        if (req.sortOrder != null) m.setSortOrder(req.sortOrder);
        return toModuleResponse(moduleRepository.save(m), false);
    }

    @Transactional
    public void deleteModule(Long moduleId) { moduleRepository.deleteById(moduleId); }

    public List<ModuleLessonDtos.LessonResponse> listLessons(Long moduleId) {
        return lessonRepository.findByModule_IdOrderBySortOrderAscIdAsc(moduleId).stream().map(this::toLessonResponse).collect(Collectors.toList());
    }

    @Transactional
    public ModuleLessonDtos.LessonResponse addLesson(Long moduleId, ModuleLessonDtos.LessonRequest req) {
        CourseModule m = moduleRepository.findById(moduleId).orElseThrow();
        Lesson l = new Lesson();
        l.setModule(m);
        l.setTitle(req.title);
        l.setType(req.type != null ? req.type : "article");
        l.setDurationSeconds(req.durationSeconds);
        l.setSortOrder(req.sortOrder != null ? req.sortOrder : 0);
        l.setStatus(req.status != null ? req.status : "draft");
        return toLessonResponse(lessonRepository.save(l));
    }

    @Transactional
    public ModuleLessonDtos.LessonResponse updateLesson(Long lessonId, ModuleLessonDtos.LessonRequest req) {
        Lesson l = lessonRepository.findById(lessonId).orElseThrow();
        if (req.title != null) l.setTitle(req.title);
        if (req.type != null) l.setType(req.type);
        if (req.durationSeconds != null) l.setDurationSeconds(req.durationSeconds);
        if (req.sortOrder != null) l.setSortOrder(req.sortOrder);
        if (req.status != null) l.setStatus(req.status);
        return toLessonResponse(lessonRepository.save(l));
    }

    @Transactional
    public void deleteLesson(Long lessonId) { lessonRepository.deleteById(lessonId); }

    private ModuleLessonDtos.ModuleResponse toModuleResponse(CourseModule m, boolean withLessons) {
        ModuleLessonDtos.ModuleResponse r = new ModuleLessonDtos.ModuleResponse();
        r.id = m.getId(); r.title = m.getTitle(); r.sortOrder = m.getSortOrder();
        if (withLessons) {
            r.lessons = lessonRepository.findByModule_IdOrderBySortOrderAscIdAsc(m.getId()).stream().map(this::toLessonResponse).collect(Collectors.toList());
        }
        return r;
    }

    private ModuleLessonDtos.LessonResponse toLessonResponse(Lesson l) {
        ModuleLessonDtos.LessonResponse r = new ModuleLessonDtos.LessonResponse();
        r.id = l.getId(); r.title = l.getTitle(); r.type = l.getType(); r.durationSeconds = l.getDurationSeconds(); r.sortOrder = l.getSortOrder(); r.status = l.getStatus();
        return r;
    }
}
