package com.example.back_end.controller;

import com.example.back_end.dto.SupportDtos;
import com.example.back_end.model.User;
import com.example.back_end.repository.UserRepository;
import com.example.back_end.service.SupportChatService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/support")
public class SupportChatController {
    private final SupportChatService supportChatService;
    private final UserRepository userRepository;

    public SupportChatController(SupportChatService supportChatService,
                                 UserRepository userRepository) {
        this.supportChatService = supportChatService;
        this.userRepository = userRepository;
    }

    @PostMapping("/threads")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<SupportDtos.ThreadDetail> createThread(
            Authentication auth,
            @RequestBody SupportDtos.CreateThreadRequest request
    ) {
        User student = requireCurrentUser(auth);
        return ResponseEntity.ok(supportChatService.createThread(student.getId(), request));
    }

    @GetMapping("/threads/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<SupportDtos.ThreadListResponse> myThreads(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        User student = requireCurrentUser(auth);
        return ResponseEntity.ok(supportChatService.studentThreads(student.getId(), page, size));
    }

    @GetMapping("/threads/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SupportDtos.ThreadDetail> viewThread(
            Authentication auth,
            @PathVariable Long id
    ) {
        User current = requireCurrentUser(auth);
        if (hasRole(auth, "MANAGER")) {
            return ResponseEntity.ok(supportChatService.managerThreadDetail(id, current.getId()));
        }
        return ResponseEntity.ok(supportChatService.studentThreadDetail(id, current.getId()));
    }

    @PostMapping("/threads/{id}/messages")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<SupportDtos.MessageDto> sendStudentMessage(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody SupportDtos.SendMessageRequest request
    ) {
        User student = requireCurrentUser(auth);
        SupportDtos.SendMessageRequest payload = request != null ? request : new SupportDtos.SendMessageRequest();
        return ResponseEntity.ok(supportChatService.studentSendMessage(id, student.getId(), payload));
    }

    @PostMapping("/threads/{id}/rating")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<SupportDtos.RatingDto> rateThread(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody SupportDtos.RatingRequest request
    ) {
        User student = requireCurrentUser(auth);
        return ResponseEntity.ok(supportChatService.submitRating(id, student.getId(), request));
    }

    @GetMapping("/manager/threads")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<SupportDtos.ThreadListResponse> managerThreads(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String studentKeyword,
            @RequestParam(required = false) Boolean mineOnly,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        User manager = requireCurrentUser(auth);
        SupportDtos.ThreadFilter filter = new SupportDtos.ThreadFilter();
        filter.status = status;
        filter.courseId = courseId;
        filter.studentKeyword = studentKeyword;
        filter.mineOnly = mineOnly;
        filter.from = from;
        filter.to = to;
        return ResponseEntity.ok(supportChatService.managerThreads(manager.getId(), filter, page, size));
    }

    @PostMapping("/manager/threads/{id}/claim")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<SupportDtos.ThreadSummary> claimThread(
            Authentication auth,
            @PathVariable Long id
    ) {
        User manager = requireCurrentUser(auth);
        return ResponseEntity.ok(supportChatService.claimThread(id, manager.getId()));
    }

    @PostMapping("/manager/threads/{id}/messages")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<SupportDtos.MessageDto> sendManagerMessage(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody SupportDtos.SendMessageRequest request
    ) {
        User manager = requireCurrentUser(auth);
        SupportDtos.SendMessageRequest payload = request != null ? request : new SupportDtos.SendMessageRequest();
        return ResponseEntity.ok(supportChatService.managerSendMessage(id, manager.getId(), payload));
    }

    @PostMapping("/manager/threads/{id}/status")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<SupportDtos.ThreadSummary> updateStatus(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody SupportDtos.UpdateStatusRequest request
    ) {
        User manager = requireCurrentUser(auth);
        return ResponseEntity.ok(supportChatService.updateStatus(id, manager.getId(), request));
    }

    @PostMapping("/manager/threads/{id}/transfer")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<SupportDtos.ThreadSummary> transferThread(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody SupportDtos.TransferRequest request
    ) {
        User manager = requireCurrentUser(auth);
        return ResponseEntity.ok(supportChatService.transferThread(id, manager.getId(), request));
    }

    private User requireCurrentUser(Authentication auth) {
        if (auth == null) throw new IllegalStateException("ChA?a xA?c thA?c danh tA?nh");
        String email = String.valueOf(auth.getPrincipal());
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("KhA'ng tA?m th���y ngA??i dA�ng"));
    }

    private boolean hasRole(Authentication auth, String roleCode) {
        if (auth == null || auth.getAuthorities() == null) return false;
        String expected = "ROLE_" + roleCode.toUpperCase();
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (expected.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
