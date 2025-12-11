package com.example.back_end.controller;

import com.example.back_end.dto.SurveyDtos;
import com.example.back_end.service.SurveyPathService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SurveyPathController {
    private final SurveyPathService service;
    public SurveyPathController(SurveyPathService service){ this.service = service; }

    @GetMapping("/public/survey")
    public ResponseEntity<List<SurveyDtos.Question>> survey(){ return ResponseEntity.ok(service.loadSurvey()); }

    @PostMapping("/survey/submit")
    public ResponseEntity<SurveyDtos.PathResponse> submit(@RequestBody SurveyDtos.SubmitRequest req, Authentication auth){
        String email = auth!=null? String.valueOf(auth.getPrincipal()): null;
        return ResponseEntity.ok(service.submitSurvey(email, req.selectedCodes));
    }

    @GetMapping("/paths/{id}")
    public ResponseEntity<SurveyDtos.PathResponse> pathDetail(@PathVariable Long id){ return ResponseEntity.ok(service.getPathDetail(id)); }
}

