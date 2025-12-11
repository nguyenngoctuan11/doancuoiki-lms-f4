package com.example.back_end.controller;

import com.example.back_end.model.User;
import com.example.back_end.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<User>> list(@RequestParam(name = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(userService.getSampleUsers(Math.max(1, Math.min(limit, 100))));
    }
}

