package com.template.controller;

import com.template.entity.Role;
import com.template.entity.User;
import com.template.repository.RoleRepository;
import com.template.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        credentials.put("email", "udichick@gmail.com");
        credentials.put("note", "Password is set via configuration. Check application logs for details.");
        credentials.put("warning", "Change default credentials after first login!");
        
        return ResponseEntity.ok(credentials);
    }

    @GetMapping("/test-security")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<Map<String, Object>> testSecurity() {
        Map<String, Object> response = new HashMap<>();
        
        // Get current authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        response.put("message", "SUPERADMIN access granted!");
        response.put("username", authentication.getName());
        response.put("authorities", authentication.getAuthorities());
        response.put("principal", authentication.getPrincipal().getClass().getSimpleName());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test-admin")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> testAdminAccess() {
        Map<String, Object> response = new HashMap<>();
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        response.put("message", "ADMIN or SUPERADMIN access granted!");
        response.put("username", authentication.getName());
        response.put("authorities", authentication.getAuthorities());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test-user")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'USER')")
    public ResponseEntity<Map<String, Object>> testUserAccess() {
        Map<String, Object> response = new HashMap<>();
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        response.put("message", "Any authenticated user access granted!");
        response.put("username", authentication.getName());
        response.put("authorities", authentication.getAuthorities());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/debug-auth")
    public ResponseEntity<Map<String, Object>> debugAuthentication() {
        Map<String, Object> response = new HashMap<>();
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        response.put("authenticated", authentication != null && authentication.isAuthenticated());
        response.put("username", authentication != null ? authentication.getName() : "null");
        response.put("authorities", authentication != null ? authentication.getAuthorities() : "null");
        response.put("principal", authentication != null ? authentication.getPrincipal().getClass().getSimpleName() : "null");
        response.put("details", authentication != null ? authentication.getDetails() : "null");
        
        return ResponseEntity.ok(response);
    }
} 