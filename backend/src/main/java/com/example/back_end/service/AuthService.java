package com.example.back_end.service;

import com.example.back_end.dto.AuthDtos;
import com.example.back_end.dto.ProfileDtos;
import com.example.back_end.model.Role;
import com.example.back_end.model.User;
import com.example.back_end.repository.RoleRepository;
import com.example.back_end.repository.UserRepository;
import com.example.back_end.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {
    private static final String GOOGLE_TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token={idToken}";
    private static final String FACEBOOK_DEBUG_TOKEN_URL = "https://graph.facebook.com/debug_token?input_token={inputToken}&access_token={accessToken}";
    private static final String FACEBOOK_PROFILE_URL = "https://graph.facebook.com/{userId}?fields=id,name,email,picture.width(512).height(512)&access_token={accessToken}";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ProfileService profileService;
    private final RestTemplate restTemplate;
    private final Set<String> googleClientIds;
    private final String facebookAppId;
    private final String facebookAppSecret;
    private final String facebookAppAccessToken;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            ProfileService profileService,
            RestTemplate restTemplate,
            @Value("${app.oauth.google.client-id:}") String googleClientId,
            @Value("${app.oauth.facebook.app-id:}") String facebookAppId,
            @Value("${app.oauth.facebook.app-secret:}") String facebookAppSecret
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.profileService = profileService;
        this.restTemplate = restTemplate;
        this.googleClientIds = parseClientIds(googleClientId);
        this.facebookAppId = safeTrim(facebookAppId);
        this.facebookAppSecret = safeTrim(facebookAppSecret);
        this.facebookAppAccessToken = (this.facebookAppId != null && this.facebookAppSecret != null)
                ? this.facebookAppId + "|" + this.facebookAppSecret
                : null;
    }

    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest req) {
        String roleCode = (req.role == null || req.role.isBlank()) ? "student" : req.role.trim().toLowerCase();
        if (roleCode.equals("manager")) {
            throw new IllegalArgumentException("Không thể tự đăng ký quyền manager");
        }
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new IllegalArgumentException("Role không hợp lệ: " + roleCode));

        User u = new User();
        u.setEmail(req.email);
        u.setPasswordHash(passwordEncoder.encode(req.password));
        u.setFullName(req.fullName);
        u.setUsername(profileService.generateInitialUsername(req.fullName, req.email));
        u.setLocale("vi");
        u.setBio(null);
        u.setTwoFactorEnabled(false);
        u.setStatus("active");
        u.setCreatedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());
        u.getRoles().add(role);

        try {
            u = userRepository.save(u);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Email đã tồn tại trên hệ thống");
        }

        return toAuthResponse(u);
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest req) {
        User u = userRepository.findByEmailIgnoreCase(req.email)
                .orElseThrow(() -> new IllegalArgumentException("Sai email hoặc mật khẩu"));
        if (!passwordEncoder.matches(req.password, u.getPasswordHash())) {
            throw new IllegalArgumentException("Sai email hoặc mật khẩu");
        }
        return toAuthResponse(u);
    }

    public AuthDtos.AuthResponse loginWithGoogle(AuthDtos.GoogleLoginRequest req) {
        if (req == null || req.credential == null || req.credential.isBlank()) {
            throw new IllegalArgumentException("Thiếu credential Google");
        }
        if (googleClientIds.isEmpty()) {
            throw new IllegalArgumentException("Google login chưa được cấu hình");
        }
        SocialProfile profile = fetchGoogleProfile(req.credential.trim());
        return handleSocialProfile(profile);
    }

    public AuthDtos.AuthResponse loginWithFacebook(AuthDtos.FacebookLoginRequest req) {
        if (req == null || req.accessToken == null || req.accessToken.isBlank()) {
            throw new IllegalArgumentException("Thiếu access token Facebook");
        }
        if (facebookAppAccessToken == null) {
            throw new IllegalArgumentException("Facebook login chưa được cấu hình");
        }
        SocialProfile profile = fetchFacebookProfile(req.accessToken.trim());
        return handleSocialProfile(profile);
    }

    private AuthDtos.AuthResponse toAuthResponse(User u) {
        List<String> roleCodes = u.getRoles().stream().map(Role::getCode).collect(Collectors.toList());
        String token = jwtService.generate(u.getEmail(), Map.of("uid", u.getId(), "roles", roleCodes));
        AuthDtos.AuthResponse res = new AuthDtos.AuthResponse();
        res.accessToken = token;
        res.userId = u.getId();
        res.email = u.getEmail();
        res.fullName = u.getFullName();
        res.username = u.getUsername();
        res.bio = u.getBio();
        res.avatarUrl = u.getAvatarUrl();
        res.twoFactorEnabled = u.isTwoFactorEnabled();
        res.hasPassword = u.getPasswordHash() != null && !u.getPasswordHash().isBlank();
        res.roles = new AuthDtos.ListRole(roleCodes);
        return res;
    }

    public ProfileDtos.ProfileResponse me(String email) {
        return profileService.getProfile(email);
    }

    private AuthDtos.AuthResponse handleSocialProfile(SocialProfile profile) {
        if (profile == null || profile.email == null || profile.email.isBlank()) {
            throw new IllegalArgumentException("Không lấy được email từ tài khoản mạng xã hội");
        }
        User user = userRepository.findByEmailIgnoreCase(profile.email).orElse(null);
        if (user == null) {
            user = createUserFromSocialProfile(profile);
        } else {
            boolean changed = false;
            if ((user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) && profile.avatarUrl != null && !profile.avatarUrl.isBlank()) {
                user.setAvatarUrl(profile.avatarUrl);
                changed = true;
            }
            if ((user.getFullName() == null || user.getFullName().isBlank()) && profile.fullName != null && !profile.fullName.isBlank()) {
                user.setFullName(profile.fullName.trim());
                changed = true;
            }
            if (changed) {
                user.setUpdatedAt(LocalDateTime.now());
                user = userRepository.save(user);
            }
        }
        return toAuthResponse(user);
    }

    private User createUserFromSocialProfile(SocialProfile profile) {
        Role role = roleRepository.findByCode("student")
                .orElseThrow(() -> new IllegalArgumentException("KhA'ng tA�m thA?y role student"));
        User user = new User();
        user.setEmail(profile.email.trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode("social-login-" + profile.provider + "-" + UUID.randomUUID()));
        String fullName = (profile.fullName != null && !profile.fullName.isBlank()) ? profile.fullName.trim() : profile.email;
        user.setFullName(fullName);
        user.setUsername(profileService.generateInitialUsername(fullName, profile.email));
        user.setAvatarUrl(profile.avatarUrl);
        user.setLocale("vi");
        user.setBio(null);
        user.setTwoFactorEnabled(false);
        user.setStatus("active");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.getRoles().add(role);
        return userRepository.save(user);
    }

    private SocialProfile fetchGoogleProfile(String credential) {
        Map<String, Object> response;
        try {
            response = restTemplate.getForObject(GOOGLE_TOKEN_INFO_URL, Map.class, credential);
        } catch (RestClientException ex) {
            throw new IllegalArgumentException("Google credential không hợp lệ");
        }
        if (response == null) {
            throw new IllegalArgumentException("Không xác thực được Google credential");
        }
        String audience = asString(response.get("aud"));
        if (audience == null || !googleClientIds.contains(audience)) {
            throw new IllegalArgumentException("Google token không thuộc ứng dụng này");
        }
        String emailVerified = asString(response.get("email_verified"));
        if (emailVerified != null && !"true".equalsIgnoreCase(emailVerified)) {
            throw new IllegalArgumentException("Email Google chưa được xác minh");
        }
        String email = asString(response.get("email"));
        String name = asString(response.get("name"));
        String picture = asString(response.get("picture"));
        String userId = asString(response.get("sub"));
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Không nhận được email từ Google");
        }
        return new SocialProfile("google", userId, email, name, picture);
    }

    private SocialProfile fetchFacebookProfile(String accessToken) {
        Map<String, Object> debug;
        try {
            debug = restTemplate.getForObject(FACEBOOK_DEBUG_TOKEN_URL, Map.class, accessToken, facebookAppAccessToken);
        } catch (RestClientException ex) {
            throw new IllegalArgumentException("Facebook token không hợp lệ");
        }
        Map<String, Object> data = asMap(debug != null ? debug.get("data") : null);
        if (data == null || !"true".equalsIgnoreCase(asString(data.get("is_valid")))) {
            throw new IllegalArgumentException("Facebook token không còn hiệu lực");
        }
        String appId = asString(data.get("app_id"));
        if (appId == null || !appId.equals(facebookAppId)) {
            throw new IllegalArgumentException("Facebook token không thuộc ứng dụng");
        }
        String userId = asString(data.get("user_id"));
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Facebook không trả về user id");
        }
        Map<String, Object> profile;
        try {
            profile = restTemplate.getForObject(FACEBOOK_PROFILE_URL, Map.class, userId, accessToken);
        } catch (RestClientException ex) {
            throw new IllegalArgumentException("KhA'ng lA?y �?�?c thA'ng tin Facebook");
        }
        if (profile == null) {
            throw new IllegalArgumentException("KhA'ng nhA?n �?�?c thA'ng tin Facebook");
        }
        String email = asString(profile.get("email"));
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Facebook chưa cấp quyền email, vui lòng thử lại");
        }
        String name = asString(profile.get("name"));
        String avatar = extractFacebookAvatar(profile.get("picture"));
        return new SocialProfile("facebook", userId, email, name, avatar);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    private String extractFacebookAvatar(Object pictureRoot) {
        Map<String, Object> picture = asMap(pictureRoot);
        if (picture == null) return null;
        Map<String, Object> data = asMap(picture.get("data"));
        if (data == null) return null;
        Object url = data.get("url");
        return url != null ? url.toString() : null;
    }

    private String asString(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private Set<String> parseClientIds(String raw) {
        if (raw == null) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(this::safeTrim)
                .filter(v -> v != null && !v.isBlank())
                .collect(Collectors.toSet());
    }

    private String safeTrim(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record SocialProfile(String provider, String providerUserId, String email, String fullName, String avatarUrl) {}
}
