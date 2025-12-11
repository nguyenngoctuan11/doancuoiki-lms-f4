package com.example.back_end.controller;

import com.example.back_end.dto.ModuleLessonDtos;
import com.example.back_end.service.ModuleLessonService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ModuleLessonController {
    private final ModuleLessonService service;
    public ModuleLessonController(ModuleLessonService service) { this.service = service; }

    // Modules of a course
    @GetMapping("/courses/{courseId}/modules")
    public ResponseEntity<List<ModuleLessonDtos.ModuleResponse>> listModules(@PathVariable Long courseId) {
        return ResponseEntity.ok(service.listModules(courseId));
    }

    @PostMapping("/courses/{courseId}/modules")
    @PreAuthorize("hasAnyRole('TEACHER','MANAGER')")
    public ResponseEntity<ModuleLessonDtos.ModuleResponse> addModule(@PathVariable Long courseId, @RequestBody ModuleLessonDtos.ModuleRequest req) {
        return ResponseEntity.ok(service.addModule(courseId, req));
    }

    @PatchMapping("/modules/{moduleId}")
    @PreAuthorize("hasAnyRole('TEACHER','MANAGER')")
    public ResponseEntity<ModuleLessonDtos.ModuleResponse> updateModule(@PathVariable Long moduleId, @RequestBody ModuleLessonDtos.ModuleRequest req) {
        return ResponseEntity.ok(service.updateModule(moduleId, req));
    }

    @DeleteMapping("/modules/{moduleId}")
    @PreAuthorize("hasAnyRole('TEACHER','MANAGER')")
    public ResponseEntity<Void> deleteModule(@PathVariable Long moduleId) {
        service.deleteModule(moduleId); return ResponseEntity.noContent().build();
    }

    // Lessons of a module
    @GetMapping("/modules/{moduleId}/lessons")
    public ResponseEntity<List<ModuleLessonDtos.LessonResponse>> listLessons(@PathVariable Long moduleId) {
        return ResponseEntity.ok(service.listLessons(moduleId));
    }

    @PostMapping("/modules/{moduleId}/lessons")
    @PreAuthorize("hasAnyRole('TEACHER','MANAGER')")
    public ResponseEntity<ModuleLessonDtos.LessonResponse> addLesson(@PathVariable Long moduleId, @RequestBody ModuleLessonDtos.LessonRequest req) {
        return ResponseEntity.ok(service.addLesson(moduleId, req));
    }

    @PatchMapping("/lessons/{lessonId}")
    @PreAuthorize("hasAnyRole('TEACHER','MANAGER')")
    public ResponseEntity<ModuleLessonDtos.LessonResponse> updateLesson(@PathVariable Long lessonId, @RequestBody ModuleLessonDtos.LessonRequest req) {
        return ResponseEntity.ok(service.updateLesson(lessonId, req));
    }

    @DeleteMapping("/lessons/{lessonId}")
    @PreAuthorize("hasAnyRole('TEACHER','MANAGER')")
    public ResponseEntity<Void> deleteLesson(@PathVariable Long lessonId) {
        service.deleteLesson(lessonId); return ResponseEntity.noContent().build();
    }
}

