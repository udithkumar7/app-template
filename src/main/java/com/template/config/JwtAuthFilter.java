package com.template.config;

import com.template.service.TokenBlacklistService;
import com.template.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

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
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            token = header.substring(7);
        }

        if (token != null && !tokenBlacklistService.isTokenBlacklisted(token)) {
            String username = jwtUtil.extractUsername(token);
            // Only allow access tokens for authentication (not refresh tokens)
            if (username != null && jwtUtil.validateToken(token, username) && jwtUtil.isAccessToken(token)) {
                // Extract authorities from JWT
                var claims = jwtUtil.extractAllClaims(token);
                var authoritiesObj = claims.get("authorities");
                java.util.List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
                if (authoritiesObj instanceof java.util.Collection<?>) {
                    for (Object authority : (java.util.Collection<?>) authoritiesObj) {
                        authorities.add(new SimpleGrantedAuthority(authority.toString()));
                    }
                }
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                new User(username, "", authorities),
                                null,
                                authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
