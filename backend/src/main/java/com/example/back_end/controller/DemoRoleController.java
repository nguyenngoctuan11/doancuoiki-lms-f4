package com.example.back_end.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DemoRoleController {
    @GetMapping("/student/hello")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<String> student() { return ResponseEntity.ok("Hello Student"); }

    @GetMapping("/teacher/hello")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<String> teacher() { return ResponseEntity.ok("Hello Teacher"); }

    @GetMapping("/manager/hello")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<String> manager() { return ResponseEntity.ok("Hello Manager"); }
}

