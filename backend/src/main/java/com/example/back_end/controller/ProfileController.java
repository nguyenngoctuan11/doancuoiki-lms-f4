package com.example.back_end.controller;

import com.example.back_end.dto.ProfileDtos;
import com.example.back_end.service.OtpService;
import com.example.back_end.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final OtpService otpService;

    public ProfileController(ProfileService profileService, OtpService otpService) {
        this.profileService = profileService;
        this.otpService = otpService;
    }

    @GetMapping
    public ResponseEntity<?> getProfile(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).body("Unauthenticated");
        }
        return ResponseEntity.ok(profileService.getProfile(authentication.getName()));
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(@RequestBody ProfileDtos.UpdateRequest request, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).body("Unauthenticated");
        }
        try {
            return ResponseEntity.ok(profileService.updateProfile(authentication.getName(), request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody ProfileDtos.ChangePasswordRequest request, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).body("Unauthenticated");
        }
        try {
            profileService.changePassword(authentication.getName(), request);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping({"/password/otp/start", "/password/otp/start/"})
    public ResponseEntity<?> startPasswordOtp(@RequestBody ProfileDtos.PasswordOtpStartRequest request, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).body("Unauthenticated");
        }
        try {
            String email = request != null ? request.email : null;
            return ResponseEntity.ok(otpService.startPasswordChange(email, authentication.getName()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping({"/password/otp/complete", "/password/otp/complete/"})
    public ResponseEntity<?> completePasswordOtp(@RequestBody ProfileDtos.PasswordOtpCompleteRequest request, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).body("Unauthenticated");
        }
        try {
            otpService.completePasswordChange(
                    request != null ? request.email : null,
                    request != null ? request.code : null,
                    request != null ? request.newPassword : null,
                    authentication.getName()
            );
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }
}
