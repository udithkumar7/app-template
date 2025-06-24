package com.template.config;

import com.template.entity.Role;
import com.template.entity.User;
import com.template.repository.RoleRepository;
import com.template.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Scanner;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile({"prod"}) // Only run in production profile
public class ProductionDataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${auth.account-expiry-years:1}")
    private int accountExpiryYears;

    @Value("${app.production.setup-mode:false}")
    private boolean setupMode;

    @Override
    public void run(String... args) throws Exception {
        log.info("Production data initialization starting...");
        
        // Initialize roles first
        initializeRoles();
        
        // Check if any superadmin exists
        boolean hasSuperAdmin = userRepository.findAll().stream()
            .anyMatch(user -> user.getRoles().stream()
                .anyMatch(role -> "SUPERADMIN".equals(role.getName())));
        
        if (!hasSuperAdmin) {
            if (setupMode) {
                log.warn("PRODUCTION SETUP MODE: No superadmin found. Interactive setup required.");
                createSuperAdminInteractively();
            } else {
                log.error("SECURITY ALERT: No superadmin user found in production!");
                log.error("Enable setup mode with: app.production.setup-mode=true");
                log.error("Then restart the application to create the initial admin user.");
                throw new IllegalStateException("No superadmin user found. Enable setup mode to create one.");
            }
        } else {
            log.info("Production initialization completed. Superadmin user exists.");
        }
    }

    private void initializeRoles() {
        log.info("Initializing production roles...");
        
        createRoleIfNotExists("SUPERADMIN", "Super Administrator with full system access");
        createRoleIfNotExists("ADMIN", "Administrator with management access");
        createRoleIfNotExists("USER", "Regular user with basic access");
        
        log.info("Production roles initialization completed");
    }

    private void createRoleIfNotExists(String roleName, String description) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            Role role = Role.builder()
                    .name(roleName)
                    .description(description)
                    .build();
            
            roleRepository.save(role);
            log.info("Created role: {} - {}", roleName, description);
        }
    }

    private void createSuperAdminInteractively() {
        log.warn("========================================");
        log.warn("PRODUCTION SUPERADMIN SETUP");
        log.warn("========================================");
        log.warn("This is a ONE-TIME setup for production.");
        log.warn("Please provide secure credentials for the superadmin user.");
        log.warn("========================================");

        Scanner scanner = new Scanner(System.in);
        
        // Get username
        System.out.print("Enter superadmin username: ");
        String username = scanner.nextLine().trim();
        
        if (username.isEmpty() || username.length() < 3) {
            throw new IllegalStateException("Username must be at least 3 characters");
        }
        
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalStateException("User already exists: " + username);
        }
        
        // Get email
        System.out.print("Enter superadmin email: ");
        String email = scanner.nextLine().trim();
        
        if (email.isEmpty() || !email.contains("@")) {
            throw new IllegalStateException("Valid email is required");
        }
        
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("Email already exists: " + email);
        }
        
        // Get password (Note: In real production, use more secure input method)
        System.out.print("Enter superadmin password (min 12 chars, mixed case, numbers, special chars): ");
        String password = scanner.nextLine().trim();
        
        if (!isPasswordSecure(password)) {
            throw new IllegalStateException("Password does not meet security requirements: " +
                "minimum 12 characters with uppercase, lowercase, numbers, and special characters");
        }
        
        // Create user
        Role superadminRole = roleRepository.findByName("SUPERADMIN")
                .orElseThrow(() -> new RuntimeException("SUPERADMIN role not found"));

        User superadmin = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .roles(Set.of(superadminRole))
                .accountNonLocked(true)
                .enabled(true)
                .failedLoginAttempts(0)
                .accountExpiryDate(LocalDateTime.now().plusYears(accountExpiryYears))
                .build();

        userRepository.save(superadmin);
        
        log.warn("========================================");
        log.warn("SUPERADMIN USER CREATED SUCCESSFULLY");
        log.warn("Username: {}", username);
        log.warn("Email: {}", email);
        log.warn("========================================");
        log.warn("IMPORTANT SECURITY NOTES:");
        log.warn("1. Change password immediately after first login");
        log.warn("2. Disable setup mode: app.production.setup-mode=false");
        log.warn("3. Restart application to disable setup mode");
        log.warn("4. Monitor login attempts and access logs");
        log.warn("========================================");
    }
    
    private boolean isPasswordSecure(String password) {
        if (password.length() < 12) return false;
        
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0);
        
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
} 