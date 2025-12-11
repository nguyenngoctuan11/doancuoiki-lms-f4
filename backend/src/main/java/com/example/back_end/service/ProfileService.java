package com.example.back_end.service;

import com.example.back_end.dto.ProfileDtos;
import com.example.back_end.model.User;
import com.example.back_end.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class ProfileService {
    private static final Pattern NON_ALLOWED = Pattern.compile("[^a-z0-9._-]");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public ProfileDtos.ProfileResponse getProfile(String email) {
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        return ProfileDtos.fromUser(user);
    }

    @Transactional
    public ProfileDtos.ProfileResponse updateProfile(String email, ProfileDtos.UpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Không có dữ liệu để cập nhật");
        }
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        boolean changed = false;

        if (request.fullName != null) {
            String name = request.fullName.trim();
            if (name.length() < 2) throw new IllegalArgumentException("Họ tên phải có ít nhất 2 ký tự");
            if (name.length() > 255) throw new IllegalArgumentException("Họ tên quá dài");
            user.setFullName(name);
            changed = true;
        }

        if (request.username != null) {
            String normalized = normalizeUsername(request.username);
            normalized = ensureUniqueUsername(normalized, user.getId());
            user.setUsername(normalized);
            changed = true;
        }

        if (request.bio != null) {
            String bio = request.bio.trim();
            if (bio.length() > 2000) throw new IllegalArgumentException("Giới thiệu tối đa 2000 ký tự");
            user.setBio(bio.isBlank() ? null : bio);
            changed = true;
        }

        if (request.avatarUrl != null) {
            String avatar = request.avatarUrl.trim();
            if (avatar.length() > 512) throw new IllegalArgumentException("Ảnh đại diện tối đa 512 ký tự");
            user.setAvatarUrl(avatar.isBlank() ? null : avatar);
            changed = true;
        }

        if (changed) {
            user.setUpdatedAt(LocalDateTime.now());
            user = userRepository.save(user);
        }
        return ProfileDtos.fromUser(user);
    }

    @Transactional
    public void changePassword(String email, ProfileDtos.ChangePasswordRequest request) {
        if (request == null) throw new IllegalArgumentException("Thiếu thông tin mật khẩu");
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        if (request.currentPassword == null || request.currentPassword.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu hiện tại");
        }
        if (!passwordEncoder.matches(request.currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác");
        }
        if (request.newPassword == null || request.newPassword.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải dài ít nhất 6 ký tự");
        }
        if (passwordEncoder.matches(request.newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu cũ");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public String generateInitialUsername(String preferred, String email) {
        String seed = preferred != null && !preferred.isBlank() ? preferred : email;
        if (seed == null || seed.isBlank()) seed = "user";
        return ensureUniqueUsername(normalizeUsername(seed), null);
    }

    public String normalizeUsername(String value) {
        if (value == null) value = "";
        String ascii = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        ascii = ascii.toLowerCase(Locale.ROOT);
        ascii = NON_ALLOWED.matcher(ascii).replaceAll("");
        ascii = ascii.replaceAll("^[._-]+", "").replaceAll("[._-]+$", "");
        if (ascii.isBlank()) ascii = "user";
        if (ascii.length() < 3) {
            while (ascii.length() < 3) {
                ascii = ascii + "0";
            }
        }
        if (ascii.length() > 32) {
            ascii = ascii.substring(0, 32);
        }
        return ascii;
    }

    public String ensureAvailableUsername(String desired, Long ignoreId) {
        return ensureUniqueUsername(normalizeUsername(desired), ignoreId);
    }

    private String ensureUniqueUsername(String desired, Long ignoreId) {
        String base = desired;
        String candidate = base;
        int attempt = 0;
        while (true) {
            Optional<User> existing = userRepository.findByUsernameIgnoreCase(candidate);
            if (existing.isEmpty() || (ignoreId != null && existing.get().getId().equals(ignoreId))) {
                return candidate;
            }
            attempt++;
            String suffix = String.valueOf(attempt);
            int maxBaseLen = Math.max(1, 32 - suffix.length());
            String trimmed = base.length() > maxBaseLen ? base.substring(0, maxBaseLen) : base;
            candidate = trimmed + suffix;
        }
    }
}
