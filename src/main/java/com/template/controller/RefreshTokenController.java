package com.template.controller;

import com.template.entity.RefreshToken;
import com.template.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/refresh-tokens")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPERADMIN')")
public class RefreshTokenController {
    
    private final RefreshTokenService refreshTokenService;
    
    @GetMapping("/user/{username}")
    public ResponseEntity<List<RefreshToken>> getUserRefreshTokens(@PathVariable String username) {
        // This would require adding a method to get tokens by username
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/revoke/{username}")
    public ResponseEntity<Map<String, String>> revokeUserTokens(@PathVariable String username) {
        refreshTokenService.revokeAllUserTokens(username);
        return ResponseEntity.ok(Map.of("message", "All refresh tokens revoked for user: " + username));
    }
    
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, String>> cleanupExpiredTokens() {
        refreshTokenService.cleanupExpiredTokens();
        return ResponseEntity.ok(Map.of("message", "Expired refresh tokens cleaned up"));
    }
} 