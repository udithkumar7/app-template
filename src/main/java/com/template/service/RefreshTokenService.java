package com.template.service;

import com.template.entity.RefreshToken;
import com.template.entity.User;
import com.template.repository.RefreshTokenRepository;
import com.template.repository.UserRepository;
import com.template.util.IpAddressUtil;
import com.template.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {
    
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final IpAddressUtil ipAddressUtil;
    
    @Value("${jwt.refresh-expiration:604800000}") // 7 days default
    private Long refreshTokenDurationMs;
    
    @Value("${auth.max-refresh-tokens-per-user:5}")
    private int maxRefreshTokensPerUser;
    
    public RefreshToken createRefreshToken(String username, HttpServletRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        // Clean up old tokens if user has too many active tokens
        cleanupExcessiveTokensForUser(username);
        
        String token = jwtUtil.generateRefreshToken(username);
        String deviceInfo = extractDeviceInfo(request);
        String ipAddress = ipAddressUtil.getClientIpAddress(request);
        
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .username(username)
                .expiryDate(LocalDateTime.now().plusSeconds(refreshTokenDurationMs / 1000))
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .revoked(false)
                .build();
        
        return refreshTokenRepository.save(refreshToken);
    }
    
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByTokenAndRevokedFalse(token);
    }
    
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token was expired. Please make a new signin request");
        }
        return token;
    }
    
    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(refreshToken -> {
                    refreshToken.setRevoked(true);
                    refreshTokenRepository.save(refreshToken);
                });
    }
    
    @Transactional
    public void revokeAllUserTokens(String username) {
        refreshTokenRepository.revokeAllByUsername(username);
        log.info("Revoked all refresh tokens for user: {}", username);
    }
    
    @Transactional
    public void revokeAllUserTokensExceptCurrent(String username, String currentToken) {
        refreshTokenRepository.revokeAllByUsernameExceptCurrent(username, currentToken);
        log.info("Revoked all refresh tokens for user: {} except current", username);
    }
    
    private void cleanupExcessiveTokensForUser(String username) {
        long activeTokens = refreshTokenRepository.countActiveTokensByUsername(username, LocalDateTime.now());
        
        if (activeTokens >= maxRefreshTokensPerUser) {
            // Revoke oldest tokens to make room
            var oldTokens = refreshTokenRepository.findByUsernameAndRevokedFalse(username);
            oldTokens.stream()
                    .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                    .limit(activeTokens - maxRefreshTokensPerUser + 1)
                    .forEach(token -> {
                        token.setRevoked(true);
                        refreshTokenRepository.save(token);
                    });
            
            log.info("Cleaned up {} old refresh tokens for user: {}", 
                    Math.min(oldTokens.size(), (int)(activeTokens - maxRefreshTokensPerUser + 1)), username);
        }
    }
    
    private String extractDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) return "Unknown Device";
        
        // Simple device detection - you can enhance this
        if (userAgent.contains("Mobile")) return "Mobile Device";
        if (userAgent.contains("Tablet")) return "Tablet";
        if (userAgent.contains("Windows")) return "Windows PC";
        if (userAgent.contains("Mac")) return "Mac";
        if (userAgent.contains("Linux")) return "Linux PC";
        
        return "Unknown Device";
    }
    
    // Scheduled cleanup of expired and revoked tokens
    @Scheduled(fixedRate = 3600000) // Run every hour
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredAndRevoked(LocalDateTime.now());
        log.debug("Cleaned up expired and revoked refresh tokens");
    }
} 