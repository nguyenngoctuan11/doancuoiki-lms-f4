package com.example.back_end.dto;

import com.example.back_end.model.Role;
import com.example.back_end.model.User;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ProfileDtos {

    public static class UpdateRequest {
        public String fullName;
        public String username;
        public String bio;
        public String avatarUrl;
    }

    public static class ChangePasswordRequest {
        public String currentPassword;
        public String newPassword;
    }

    public static class PasswordOtpStartRequest {
        public String email;
    }

    public static class PasswordOtpCompleteRequest {
        public String email;
        public String code;
        public String newPassword;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProfileResponse {
        public Long id;
        public String email;
        public String fullName;
        public String username;
        public String bio;
        public String avatarUrl;
        public String locale;
        public boolean twoFactorEnabled;
        public boolean hasPassword;
        public List<String> roles;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;
    }

    public static ProfileResponse fromUser(User user) {
        ProfileResponse res = new ProfileResponse();
        res.id = user.getId();
        res.email = user.getEmail();
        res.fullName = user.getFullName();
        res.username = user.getUsername();
        res.bio = user.getBio();
        res.avatarUrl = user.getAvatarUrl();
        res.locale = user.getLocale();
        res.twoFactorEnabled = user.isTwoFactorEnabled();
        res.hasPassword = user.getPasswordHash() != null && !user.getPasswordHash().isBlank();
        res.createdAt = user.getCreatedAt();
        res.updatedAt = user.getUpdatedAt();
        res.roles = user.getRoles().stream().map(Role::getCode).collect(Collectors.toList());
        return res;
    }
}
