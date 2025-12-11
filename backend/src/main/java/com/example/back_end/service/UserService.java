package com.example.back_end.service;

import com.example.back_end.dto.UserDtos;
import com.example.back_end.model.Role;
import com.example.back_end.model.User;
import com.example.back_end.repository.RoleRepository;
import com.example.back_end.repository.UserRepository;
import com.example.back_end.repository.CourseRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfileService profileService;
    private final CourseRepository courseRepository;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       ProfileService profileService,
                       CourseRepository courseRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.profileService = profileService;
        this.courseRepository = courseRepository;
    }

    public List<User> getSampleUsers(int limit) {
        return userRepository.findAllByOrderByIdAsc(PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public List<UserDtos.UserResponse> adminList(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return userRepository.findAllByOrderByIdAsc(PageRequest.of(0, safeLimit))
                .stream()
                .map(UserDtos.UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserDtos.UserResponse adminGet(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng #" + id));
        return UserDtos.UserResponse.fromEntity(user);
    }

    @Transactional
    public UserDtos.UserResponse adminCreate(UserDtos.UpsertRequest req) {
        if (req == null) throw new IllegalArgumentException("Thiếu dữ liệu người dùng");
        User user = new User();
        applyFields(user, req, true);
        return UserDtos.UserResponse.fromEntity(userRepository.save(user));
    }

    @Transactional
    public UserDtos.UserResponse adminUpdate(Long id, UserDtos.UpsertRequest req) {
        if (req == null) throw new IllegalArgumentException("Thiếu dữ liệu cập nhật");
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng #" + id));
        applyFields(user, req, false);
        return UserDtos.UserResponse.fromEntity(userRepository.save(user));
    }

    @Transactional
    public void adminDelete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng #" + id));
        courseRepository.findAllByCreatedBy(user).forEach(courseRepository::delete);
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public List<UserDtos.RoleInfo> availableRoles() {
        return roleRepository.findAll()
                .stream()
                .map(role -> new UserDtos.RoleInfo(role.getCode(), role.getName()))
                .collect(Collectors.toList());
    }

    private void applyFields(User user, UserDtos.UpsertRequest req, boolean creating) {
        LocalDateTime now = LocalDateTime.now();
        if (creating) {
            user.setCreatedAt(now);
        }
        user.setUpdatedAt(now);

        if (req.email != null || creating) {
            String email = trimToNull(req.email);
            if (email == null || !email.contains("@")) {
                throw new IllegalArgumentException("Email không hợp lệ");
            }
            boolean exists = creating
                    ? userRepository.existsByEmailIgnoreCase(email)
                    : userRepository.existsByEmailIgnoreCaseAndIdNot(email, user.getId());
            if (exists) throw new IllegalArgumentException("Email đã tồn tại");
            user.setEmail(email);
        }

        if (req.fullName != null || creating) {
            String name = trimToNull(req.fullName);
            if (name == null || name.length() < 2) {
                throw new IllegalArgumentException("Họ tên phải có ít nhất 2 ký tự");
            }
            user.setFullName(name);
        }

        if (req.username != null) {
            String desired = trimToNull(req.username);
            if (desired == null) throw new IllegalArgumentException("Username không được để trống");
            String available = profileService.ensureAvailableUsername(desired, creating ? null : user.getId());
            user.setUsername(available);
        } else if (creating) {
            user.setUsername(profileService.generateInitialUsername(user.getFullName(), user.getEmail()));
        }

        if (req.password != null) {
            String pwd = req.password.trim();
            if (pwd.length() < 6) throw new IllegalArgumentException("Mật khẩu phải từ 6 ký tự");
            user.setPasswordHash(passwordEncoder.encode(pwd));
        } else if (creating) {
            throw new IllegalArgumentException("Mật khẩu bắt buộc khi tạo mới");
        }

        if (req.avatarUrl != null || creating) {
            String avatar = trimToNull(req.avatarUrl);
            if (avatar != null && avatar.length() > 512) {
                throw new IllegalArgumentException("Đường dẫn avatar tối đa 512 ký tự");
            }
            user.setAvatarUrl(avatar);
        }

        if (req.locale != null || creating) {
            String locale = trimToNull(req.locale);
            user.setLocale(locale != null ? locale : "vi");
        }

        if (req.bio != null || creating) {
            String bio = trimToNull(req.bio);
            if (bio != null && bio.length() > 2000) {
                throw new IllegalArgumentException("Giới thiệu tối đa 2000 ký tự");
            }
            user.setBio(bio);
        }

        if (req.status != null || creating) {
            user.setStatus(validateStatus(req.status));
        }

        if (req.twoFactorEnabled != null || creating) {
            boolean enabled = req.twoFactorEnabled != null && req.twoFactorEnabled;
            user.setTwoFactorEnabled(enabled);
        }

        if (req.roles != null || creating) {
            Set<Role> roles = resolveRoles(req.roles);
            user.setRoles(roles);
        }
    }

    private Set<Role> resolveRoles(List<String> requested) {
        List<String> codes = (requested == null || requested.isEmpty())
                ? List.of("student")
                : requested.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(s -> s.trim().toLowerCase(Locale.ROOT))
                    .distinct()
                    .collect(Collectors.toList());
        if (codes.isEmpty()) {
            codes = List.of("student");
        }
        List<Role> roles = roleRepository.findByCodeIn(codes);
        if (roles.size() != codes.size()) {
            throw new IllegalArgumentException("Danh sách role không hợp lệ");
        }
        return new HashSet<>(roles);
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String validateStatus(String status) {
        String normalized = (status == null ? "" : status.trim().toLowerCase(Locale.ROOT));
        return switch (normalized) {
            case "active", "inactive", "suspended", "pending", "banned" -> normalized;
            default -> "active";
        };
    }
}
