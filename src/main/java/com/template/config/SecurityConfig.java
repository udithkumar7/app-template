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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private TokenBlacklistService tokenBlacklistService;
    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/logout", "/api/auth/logout-all", "/api/auth/refresh", "/h2-console/**").permitAll()
                .requestMatchers("/api/audit-test/**").permitAll()
                .requestMatchers("/api/init/**").permitAll() // Allow access to initialization status
                .requestMatchers("/api/jquery/**").permitAll() // Allow access to jQuery backend endpoints
                //.requestMatchers(HttpMethod.GET, "/api/roles").permitAll() // Allow viewing roles
                //.requestMatchers(HttpMethod.GET, "/api/menus/**").permitAll() // Allow viewing menus for testing
                //.requestMatchers(HttpMethod.PUT, "/api/menus/**").permitAll() // Allow menu ordering for testing
                //.requestMatchers(HttpMethod.POST, "/api/menus").permitAll() // Allow menu creation for testing
                //.requestMatchers(HttpMethod.GET, "/api/location-codes/**").permitAll() // Allow viewing location codes for testing
                //.requestMatchers(HttpMethod.PUT, "/api/location-codes/**").permitAll() // Allow location code ordering for testing
                //.requestMatchers(HttpMethod.POST, "/api/location-codes").permitAll() // Allow location code creation for testing
                //.requestMatchers(HttpMethod.POST, "/api/roles").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/menus/role/*").permitAll() // Allow menu role assignment for testing
                //.requestMatchers(HttpMethod.POST, "/api/menus").permitAll() // Allow menu creation for testing
                .requestMatchers("/api/users/crud/**").permitAll()
                //.requestMatchers("/api/users/*/roles").permitAll()
                .requestMatchers("/api/otp/**").permitAll()
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
