package com.template.config;

import com.template.service.CustomUserDetailsService;
import com.template.service.TokenBlacklistService;
import com.template.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private TokenBlacklistService tokenBlacklistService;
    @Autowired
    private CustomUserDetailsService userDetailsService;
    @Autowired
    private CorsConfig corsConfig;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - no authentication required
                .requestMatchers("/api/auth/login", "/api/auth/refresh").permitAll()
                .requestMatchers("/api/init/**").permitAll()
                .requestMatchers("/api/audit-test/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                
                // Authentication endpoints - require authentication
                .requestMatchers("/api/auth/logout", "/api/auth/logout-all").authenticated()
                
                // OTP endpoints - public for password reset and account unlock
                .requestMatchers("/api/otp/**").permitAll()
                
                // User Management - CRUD operations
                .requestMatchers(HttpMethod.GET, "/api/users/crud/**").hasAnyRole("SUPERADMIN", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/users/crud/**").permitAll()
                .requestMatchers(HttpMethod.PUT, "/api/users/crud/**").hasAnyRole("SUPERADMIN", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/users/crud/**").hasRole("SUPERADMIN")
                
                // User Role Management
                .requestMatchers("/api/users/*/roles").hasRole("SUPERADMIN")
                
                // Role Management - Superadmin only
                .requestMatchers("/api/roles/**").hasRole("SUPERADMIN")
                
                // Menu Management - All authenticated users can access
                .requestMatchers("/api/menus/**").hasAnyRole("SUPERADMIN", "ADMIN", "USER")
                
                // Location Code Management - Superadmin only
                .requestMatchers("/api/location-codes/**").hasRole("SUPERADMIN")
                
                // Refresh Token Management - Superadmin only
                .requestMatchers("/api/admin/refresh-tokens/**").hasRole("SUPERADMIN")
                
                // Product Management - Different access levels
                .requestMatchers(HttpMethod.GET, "/api/jquery/products/**").hasAnyRole("SUPERADMIN", "ADMIN", "USER")
                .requestMatchers(HttpMethod.POST, "/api/jquery/products/**").hasAnyRole("SUPERADMIN", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/jquery/products/**").hasAnyRole("SUPERADMIN", "ADMIN")
                //.requestMatchers(HttpMethod.DELETE, "/api/jquery/products/**").hasRole("SUPERADMIN", "ADMIN")
                
                // Any other request requires authentication
                .anyRequest().authenticated()
            )
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(new JwtAuthFilter(jwtUtil, tokenBlacklistService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
