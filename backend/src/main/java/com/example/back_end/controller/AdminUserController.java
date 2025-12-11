package com.example.back_end.controller;

import com.example.back_end.dto.UserDtos;
import com.example.back_end.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('MANAGER')")
public class AdminUserController {
    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserDtos.UserResponse>> list(@RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(userService.adminList(limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDtos.UserResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(userService.adminGet(id));
    }

    @PostMapping
    public ResponseEntity<UserDtos.UserResponse> create(@RequestBody UserDtos.UpsertRequest request) {
        return ResponseEntity.ok(userService.adminCreate(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDtos.UserResponse> update(@PathVariable Long id,
                                                        @RequestBody UserDtos.UpsertRequest request) {
        return ResponseEntity.ok(userService.adminUpdate(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.adminDelete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/roles")
    public ResponseEntity<List<UserDtos.RoleInfo>> roles() {
        return ResponseEntity.ok(userService.availableRoles());
    }
}
