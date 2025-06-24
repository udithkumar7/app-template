package com.template.config;

import com.template.entity.Code;
import com.template.entity.Menu;
import com.template.entity.Role;
import com.template.entity.User;
import com.template.repository.CodeRepository;
import com.template.repository.MenuRepository;
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
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile({"dev"}) // SECURITY FIX: Only run in dev profile, NOT prod
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final MenuRepository menuRepository;
    private final CodeRepository codeRepository;
    private final PasswordEncoder passwordEncoder;

    // SECURITY FIX: Make credentials mandatory in environment variables
    @Value("${app.init.superadmin.username:#{null}}")
    private String superadminUsername;

    @Value("${app.init.superadmin.password:#{null}}")
    private String superadminPassword;

    @Value("${app.init.superadmin.email:#{null}}")
    private String superadminEmail;

    @Value("${auth.account-expiry-years:3}")
    private int accountExpiryYears;

    @Value("${app.init.force-superadmin-creation:false}")
    private boolean forceSuperadminCreation;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting data initialization...");
        
        initializeRoles();
        
        // SECURITY FIX: Only create superadmin if explicitly enabled and configured
        if (forceSuperadminCreation) {
            initializeSuperAdminUser();
        } else {
            log.info("Superadmin creation disabled. Use app.init.force-superadmin-creation=true to enable.");
        }
        
        initializeDefaultMenus();
        initializeDefaultCodes();
        
        log.info("Data initialization completed successfully!");
    }

    private void initializeRoles() {
        log.info("Initializing default roles...");
        
        // Create SUPERADMIN role
        createRoleIfNotExists("SUPERADMIN", "Super Administrator with full system access");
        
        // Create ADMIN role  
        createRoleIfNotExists("ADMIN", "Administrator with management access");
        
        // Create USER role
        createRoleIfNotExists("USER", "Regular user with basic access");
        
        log.info("Default roles initialization completed");
    }

    private void createRoleIfNotExists(String roleName, String description) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            Role role = Role.builder()
                    .name(roleName)
                    .description(description)
                    .build();
            
            roleRepository.save(role);
            log.info("Created role: {} - {}", roleName, description);
        } else {
            log.debug("Role already exists: {}", roleName);
        }
    }

    private void initializeSuperAdminUser() {
        log.info("Initializing superadmin user...");
        
        // SECURITY FIX: Validate all required fields are provided
        if (superadminUsername == null || superadminUsername.trim().isEmpty()) {
            log.error("SECURITY: Superadmin username not provided. Set app.init.superadmin.username");
            throw new IllegalStateException("Superadmin username is required but not configured");
        }
        
        if (superadminPassword == null || superadminPassword.trim().isEmpty()) {
            log.error("SECURITY: Superadmin password not provided. Set app.init.superadmin.password");
            throw new IllegalStateException("Superadmin password is required but not configured");
        }
        
        if (superadminEmail == null || superadminEmail.trim().isEmpty()) {
            log.error("SECURITY: Superadmin email not provided. Set app.init.superadmin.email");
            throw new IllegalStateException("Superadmin email is required but not configured");
        }
        
        // SECURITY FIX: Validate password strength
        if (!isPasswordSecure(superadminPassword)) {
            log.error("SECURITY: Superadmin password does not meet security requirements");
            throw new IllegalStateException("Superadmin password must be at least 12 characters with mixed case, numbers, and special characters");
        }

        // Check if superadmin user already exists
        if (userRepository.findByUsername(superadminUsername).isPresent()) {
            log.info("Superadmin user already exists: {}", superadminUsername);
            return;
        }

        // Check if email already exists
        if (userRepository.findByEmail(superadminEmail).isPresent()) {
            log.warn("Email already exists for superadmin: {}. Skipping superadmin creation.", superadminEmail);
            return;
        }

        // Get SUPERADMIN role
        Role superadminRole = roleRepository.findByName("SUPERADMIN")
                .orElseThrow(() -> new RuntimeException("SUPERADMIN role not found. Roles must be created first."));

        // Create superadmin user
        User superadmin = User.builder()
                .username(superadminUsername)
                .password(passwordEncoder.encode(superadminPassword))
                .email(superadminEmail)
                .roles(Set.of(superadminRole))
                .accountNonLocked(true)
                .enabled(true)
                .failedLoginAttempts(0)
                .accountExpiryDate(LocalDateTime.now().plusYears(accountExpiryYears))
                .build();

        userRepository.save(superadmin);
        
        log.info("Created superadmin user: {} with email: {}", superadminUsername, superadminEmail);
        // SECURITY FIX: Never log the actual password
        log.warn("IMPORTANT: Please change the superadmin password after first login!");
        log.warn("SECURITY NOTICE: The superadmin account has been created. Consider disabling this initializer in production.");
    }
    
    // SECURITY FIX: Add password strength validation
    private boolean isPasswordSecure(String password) {
        if (password.length() < 12) return false;
        
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0);
        
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    private void initializeDefaultMenus() {
        log.info("Initializing default menus...");
        
        // Only create menus if none exist
        if (menuRepository.count() > 0) {
            log.info("Menus already exist, skipping menu initialization");
            return;
        }

        // Create main menu items with proper ordering
        createMenuIfNotExists("Dashboard", "/dashboard", 1, "dashboard", "Main dashboard view");
        createMenuIfNotExists("User Management", "/users", 2, "users", "Manage system users");
        createMenuIfNotExists("Role Management", "/roles", 3, "shield", "Manage user roles and permissions");
        createMenuIfNotExists("Menu Management", "/menus", 4, "menu", "Configure system menus");
        createMenuIfNotExists("Reports", "/reports", 5, "chart-bar", "View system reports");
        createMenuIfNotExists("Settings", "/settings", 6, "cog", "System configuration");
        
        log.info("Default menus initialization completed");
    }

    private void createMenuIfNotExists(String name, String path, Integer displayOrder, String icon, String description) {
        if (menuRepository.findByName(name).isEmpty()) {
            Menu menu = Menu.builder()
                    .name(name)
                    .path(path)
                    .displayOrder(displayOrder)
                    .icon(icon)
                    .description(description)
                    .isActive(true)
                    .build();
            
            menuRepository.save(menu);
            log.info("Created menu: {} at position {}", name, displayOrder);
        } else {
            log.debug("Menu already exists: {}", name);
        }
    }

    private void initializeDefaultCodes() {
        log.info("Initializing default location codes...");
        
        // Only create codes if none exist
        if (codeRepository.count() > 0) {
            log.info("Location codes already exist, skipping code initialization");
            return;
        }

        // Create countries
        Code india = createCodeIfNotExists("IN", "India", "COUNTRY", null, 1, "Republic of India");
        Code usa = createCodeIfNotExists("US", "United States", "COUNTRY", null, 2, "United States of America");
        Code uk = createCodeIfNotExists("UK", "United Kingdom", "COUNTRY", null, 3, "United Kingdom of Great Britain");
        
        // Create states for India
        Code karnataka = createCodeIfNotExists("KA", "Karnataka", "STATE", india, 1, "Karnataka State, India");
        Code maharashtra = createCodeIfNotExists("MH", "Maharashtra", "STATE", india, 2, "Maharashtra State, India");
        Code tamilnadu = createCodeIfNotExists("TN", "Tamil Nadu", "STATE", india, 3, "Tamil Nadu State, India");
        
        // Create cities for Karnataka
        createCodeIfNotExists("BLR", "Bangalore", "CITY", karnataka, 1, "Bangalore (Bengaluru), Karnataka");
        createCodeIfNotExists("MYS", "Mysore", "CITY", karnataka, 2, "Mysore, Karnataka");
        createCodeIfNotExists("MNG", "Mangalore", "CITY", karnataka, 3, "Mangalore, Karnataka");
        
        // Create cities for Maharashtra
        createCodeIfNotExists("MUM", "Mumbai", "CITY", maharashtra, 1, "Mumbai, Maharashtra");
        createCodeIfNotExists("PUN", "Pune", "CITY", maharashtra, 2, "Pune, Maharashtra");
        createCodeIfNotExists("NGP", "Nagpur", "CITY", maharashtra, 3, "Nagpur, Maharashtra");
        
        log.info("Default location codes initialization completed");
    }

    private Code createCodeIfNotExists(String keycode, String valuekey, String category, Code parentCode, 
                                      Integer displayOrder, String description) {
        if (codeRepository.findBykeycode(keycode).isEmpty()) {
            Code code = Code.builder()
                    .keycode(keycode)
                    .valuekey(valuekey)
                    .category(category)
                    .parentCode(parentCode)
                    .displayOrder(displayOrder)
                    .description(description)
                    .isActive(true)
                    .build();
            
            Code savedCode = codeRepository.save(code);
            log.info("Created {} code: {} ({}) at position {}", category.toLowerCase(), keycode, valuekey, displayOrder);
            return savedCode;
        } else {
            log.debug("Code already exists: {}", keycode);
            return codeRepository.findBykeycode(keycode).get();
        }
    }
} 