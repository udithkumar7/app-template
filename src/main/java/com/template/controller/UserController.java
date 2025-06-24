package com.template.controller;

import com.template.entity.User;
import com.template.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserController {
    private final UserService userService;

    @PostMapping("/{userId}/roles")
    public ResponseEntity<User> assignRoles(
            @PathVariable @Positive(message = "User ID must be positive") Long userId,
            @RequestBody @NotEmpty(message = "Role names set cannot be empty") Set<String> roleNames) {
        return ResponseEntity.ok(userService.assignRoles(userId, roleNames));
    }

    @DeleteMapping("/{userId}/roles")
    public ResponseEntity<User> removeRoles(
            @PathVariable @Positive(message = "User ID must be positive") Long userId,
            @RequestBody @NotEmpty(message = "Role names set cannot be empty") Set<String> roleNames) {
        return ResponseEntity.ok(userService.removeRoles(userId, roleNames));
    }
} 