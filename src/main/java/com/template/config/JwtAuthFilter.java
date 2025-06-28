package com.template.config;

import com.template.service.TokenBlacklistService;
import com.template.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtAuthFilter(JwtUtil jwtUtil, TokenBlacklistService tokenBlacklistService) {
        this.jwtUtil = jwtUtil;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        String token = null;
        
        log.debug("Processing request: {} {}", request.getMethod(), request.getRequestURI());
        
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            token = header.substring(7);
            log.debug("JWT token found: {}", token.substring(0, Math.min(20, token.length())) + "...");
        } else {
            log.debug("No valid Authorization header found");
        }

        if (token != null && !tokenBlacklistService.isTokenBlacklisted(token)) {
            try {
                String username = jwtUtil.extractUsername(token);
                log.debug("Extracted username from token: {}", username);
                
                // Only allow access tokens for authentication (not refresh tokens)
                if (username != null && jwtUtil.validateToken(token, username) && jwtUtil.isAccessToken(token)) {
                    log.debug("Token validation successful for user: {}", username);
                    
                    // Extract authorities from JWT
                    var claims = jwtUtil.extractAllClaims(token);
                    var authoritiesObj = claims.get("authorities");
                    java.util.List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
                    
                    if (authoritiesObj instanceof java.util.Collection<?>) {
                        for (Object authority : (java.util.Collection<?>) authoritiesObj) {
                            authorities.add(new SimpleGrantedAuthority(authority.toString()));
                        }
                    }
                    
                    log.debug("Extracted authorities: {}", authorities);
                    
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    new User(username, "", authorities),
                                    null,
                                    authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.debug("Authentication set in SecurityContext for user: {} with authorities: {}", username, authorities);
                } else {
                    log.debug("Token validation failed for user: {}", username);
                    if (username == null) {
                        log.debug("Username is null");
                    }
                    if (username != null && !jwtUtil.validateToken(token, username)) {
                        log.debug("Token validation failed");
                    }
                    if (username != null && !jwtUtil.isAccessToken(token)) {
                        log.debug("Token is not an access token");
                    }
                }
            } catch (Exception e) {
                log.error("Error processing JWT token: {}", e.getMessage(), e);
            }
        } else {
            if (token != null) {
                log.debug("Token is blacklisted");
            }
        }

        filterChain.doFilter(request, response);
    }
}
