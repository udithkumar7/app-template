package com.template.controller;

import com.template.entity.Role;
import com.template.entity.User;
import com.template.repository.RoleRepository;
import com.template.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/init")
@RequiredArgsConstructor
public class InitializationController {
    
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getInitializationStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // Check roles
        List<Role> roles = roleRepository.findAll();
        Map<String, Object> roleStatus = new HashMap<>();
        roleStatus.put("totalRoles", roles.size());
        roleStatus.put("roles", roles);
        roleStatus.put("hasSuperAdmin", roleRepository.findByName("SUPERADMIN").isPresent());
        roleStatus.put("hasAdmin", roleRepository.findByName("ADMIN").isPresent());
        roleStatus.put("hasUser", roleRepository.findByName("USER").isPresent());
        
        // Check users
        List<User> users = userRepository.findAll();
        Map<String, Object> userStatus = new HashMap<>();
        userStatus.put("totalUsers", users.size());
        userStatus.put("hasSuperAdminUser", users.stream()
            .anyMatch(user -> user.getRoles().stream()
                .anyMatch(role -> "SUPERADMIN".equals(role.getName()))));
        
        status.put("roles", roleStatus);
        status.put("users", userStatus);
        status.put("initialized", roleStatus.get("hasSuperAdmin").equals(true) && 
                                  roleStatus.get("hasAdmin").equals(true) && 
                                  roleStatus.get("hasUser").equals(true) &&
                                  userStatus.get("hasSuperAdminUser").equals(true));
        
        return ResponseEntity.ok(status);
    }

    @GetMapping("/default-credentials")
    public ResponseEntity<Map<String, String>> getDefaultCredentials() {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", "superadmin");
        credentials.put("email", "superadmin@template.com");
        credentials.put("note", "Password is set via configuration. Check application logs for details.");
        credentials.put("warning", "Change default credentials after first login!");
        
        return ResponseEntity.ok(credentials);
    }
} 