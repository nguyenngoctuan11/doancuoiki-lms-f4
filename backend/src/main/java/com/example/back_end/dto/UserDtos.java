package com.example.back_end.dto;

import com.example.back_end.model.Role;
import com.example.back_end.model.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UserDtos {

    public static class UserResponse {
        public Long id;
        public String email;
        public String fullName;
        public String username;
        public String avatarUrl;
        public String locale;
        public String bio;
        public boolean twoFactorEnabled;
        public String status;
        public List<String> roles;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;

        public static UserResponse fromEntity(User user) {
            UserResponse res = new UserResponse();
            res.id = user.getId();
            res.email = user.getEmail();
            res.fullName = user.getFullName();
            res.username = user.getUsername();
            res.avatarUrl = user.getAvatarUrl();
            res.locale = user.getLocale();
            res.bio = user.getBio();
            res.twoFactorEnabled = user.isTwoFactorEnabled();
            res.status = user.getStatus();
            res.roles = user.getRoles()
                    .stream()
                    .map(Role::getCode)
                    .sorted()
                    .collect(Collectors.toList());
            res.createdAt = user.getCreatedAt();
            res.updatedAt = user.getUpdatedAt();
            return res;
        }
    }

    public static class UpsertRequest {
        public String email;
        public String fullName;
        public String username;
        public String avatarUrl;
        public String locale;
        public String bio;
        public Boolean twoFactorEnabled;
        public String status;
        public List<String> roles = new ArrayList<>();
        public String password; // required khi tạo, tùy chọn khi sửa
    }

    public static class RoleInfo {
        public String code;
        public String name;

        public RoleInfo() {}

        public RoleInfo(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }
}
