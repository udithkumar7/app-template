package com.template.controller;

import com.template.entity.Role;
import com.template.entity.User;
import com.template.repository.RoleRepository;
import com.template.repository.UserRepository;
import com.template.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/audit-test")
@RequiredArgsConstructor
public class AuditTestController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/create-test-data")
    public ResponseEntity<Map<String, Object>> createTestData() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Create a test role
            Role role = Role.builder()
                    .name("TEST_ROLE")
                    .build();
            Role savedRole = roleRepository.save(role);
            
            // Create a test user
            User user = User.builder()
                    .username("testuser")
                    .email("test@example.com")
                    .password(passwordEncoder.encode("password"))
                    .build();
            User savedUser = userRepository.save(user);
            
            // Get audit info
            String roleAuditInfo = auditService.getAuditInfo(savedRole);
            String userAuditInfo = auditService.getAuditInfo(savedUser);
            
            response.put("success", true);
            response.put("role", savedRole);
            response.put("user", savedUser);
            response.put("roleAuditInfo", roleAuditInfo);
            response.put("userAuditInfo", userAuditInfo);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/check-audit-fields")
    public ResponseEntity<Map<String, Object>> checkAuditFields() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Check if audit fields are populated
            User user = userRepository.findByUsername("testuser").orElse(null);
            Role role = roleRepository.findByName("TEST_ROLE").orElse(null);
            
            if (user != null) {
                Map<String, Object> userAudit = new HashMap<>();
                userAudit.put("createdBy", user.getCreatedBy());
                userAudit.put("createdAt", user.getCreatedAt());
                userAudit.put("updatedBy", user.getUpdatedBy());
                userAudit.put("updatedAt", user.getUpdatedAt());
                userAudit.put("version", user.getVersion());
                response.put("userAudit", userAudit);
            }
            
            if (role != null) {
                Map<String, Object> roleAudit = new HashMap<>();
                roleAudit.put("createdBy", role.getCreatedBy());
                roleAudit.put("createdAt", role.getCreatedAt());
                roleAudit.put("updatedBy", role.getUpdatedBy());
                roleAudit.put("updatedAt", role.getUpdatedAt());
                roleAudit.put("version", role.getVersion());
                response.put("roleAudit", roleAudit);
            }
            
            response.put("success", true);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
} 