package com.example.back_end.controller;

import com.example.back_end.dto.ExamDtos;
import com.example.back_end.model.User;
import com.example.back_end.repository.UserRepository;
import com.example.back_end.service.StudentExamService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student/exams")
public class StudentExamController {
    private final UserRepository userRepository;
    private final StudentExamService studentExamService;

    public StudentExamController(UserRepository userRepository, StudentExamService studentExamService) {
        this.userRepository = userRepository;
        this.studentExamService = studentExamService;
    }

    private User currentUser(Authentication auth) {
        return userRepository.findByEmailIgnoreCase(String.valueOf(auth.getPrincipal())).orElseThrow();
    }

    @GetMapping("/courses/{courseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ExamDtos.ExamSummary>> listForCourse(
            @PathVariable Long courseId,
            Authentication auth
    ) {
        currentUser(auth); // ensure authenticated user exists
        return ResponseEntity.ok(studentExamService.listCourseExams(courseId));
    }

    @GetMapping("/courses/{courseId}/exams/{examId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExamDtos.ExamOverview> overview(
            @PathVariable Long courseId,
            @PathVariable Long examId,
            Authentication auth
    ) {
        User user = currentUser(auth);
        return ResponseEntity.ok(studentExamService.loadOverview(user.getId(), courseId, examId));
    }

    @PostMapping("/courses/{courseId}/exams/{examId}/attempts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExamDtos.AttemptView> startAttempt(
            @PathVariable Long courseId,
            @PathVariable Long examId,
            @RequestBody(required = false) ExamDtos.StartAttemptRequest body,
            Authentication auth
    ) {
        User user = currentUser(auth);
        Long resumeId = body != null ? body.resumeAttemptId : null;
        return ResponseEntity.ok(studentExamService.createOrResumeAttempt(user.getId(), courseId, examId, resumeId));
    }

    @GetMapping("/attempts/{attemptId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExamDtos.AttemptView> attempt(
            @PathVariable Long attemptId,
            Authentication auth
    ) {
        User user = currentUser(auth);
        return ResponseEntity.ok(studentExamService.loadAttempt(user.getId(), attemptId));
    }

    @PatchMapping("/attempts/{attemptId}/answers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> saveAnswer(
            @PathVariable Long attemptId,
            @RequestBody ExamDtos.AnswerUpdateRequest request,
            Authentication auth
    ) {
        User user = currentUser(auth);
        studentExamService.saveAnswer(user.getId(), attemptId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/attempts/{attemptId}/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExamDtos.SubmitResponse> submit(
            @PathVariable Long attemptId,
            Authentication auth
    ) {
        User user = currentUser(auth);
        return ResponseEntity.ok(studentExamService.submitAttempt(user.getId(), attemptId));
    }

    @PostMapping("/courses/{courseId}/certificate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StudentExamService.CertificateInfo> ensureCertificate(
            @PathVariable Long courseId,
            Authentication auth
    ) {
        User user = currentUser(auth);
        return ResponseEntity.ok(studentExamService.ensureCertificate(user.getId(), courseId));
    }

    @GetMapping("/certificates")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<StudentExamService.CertificateSummary>> certificates(Authentication auth) {
        User user = currentUser(auth);
        return ResponseEntity.ok(studentExamService.listCertificates(user.getId()));
    }
}
