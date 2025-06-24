package com.template.controller;

import com.template.dto.LoginRequest;
import com.template.dto.LoginResponse;
import com.template.dto.RefreshTokenRequest;
import com.template.dto.RefreshTokenResponse;
import com.template.entity.RefreshToken;
import com.template.entity.User;
import com.template.service.AuthenticationService;
import com.template.service.RefreshTokenService;
import com.template.service.TokenBlacklistService;
import com.template.service.UserSessionService;
import com.template.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserSessionService userSessionService;
    private final AuthenticationService authenticationService;
    private final RefreshTokenService refreshTokenService;
    
    @Value("${jwt.expiration}")
    private Long accessTokenExpiration;
    
    @Value("${jwt.refresh-expiration:604800000}")
    private Long refreshTokenExpiration;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest, 
                                             HttpServletRequest request) {
        AuthenticationService.AuthenticationResult result = 
            authenticationService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());
        
        if (result.isSuccess()) {
            User user = result.getUser();
            Set<String> authorities = user.getRoles().stream()
                .map(role -> role.getName())
                .collect(java.util.stream.Collectors.toSet());
            
            // Generate access token
            String accessToken = jwtUtil.generateToken(user.getUsername(), authorities);
            Instant accessExpiry = jwtUtil.extractExpiration(accessToken).toInstant();
            userSessionService.registerToken(user.getUsername(), accessToken, accessExpiry);
            
            // Generate refresh token
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUsername(), request);
            
            LoginResponse response = new LoginResponse();
            response.setAccessToken(accessToken);
            response.setRefreshToken(refreshToken.getToken());
            response.setUsername(user.getUsername());
            response.setMessage("Login successful");
            response.setAccessTokenExpiresIn(accessTokenExpiration);
            response.setRefreshTokenExpiresIn(refreshTokenExpiration);
            
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(new LoginResponse(null, null, result.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request, 
                                                           HttpServletRequest httpRequest) {
        try {
            String requestRefreshToken = request.getRefreshToken();
            
            RefreshToken refreshToken = refreshTokenService.findByToken(requestRefreshToken)
                    .map(refreshTokenService::verifyExpiration)
                    .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
            
            String username = refreshToken.getUsername();
            
            // Get user to generate new access token
            User user = userSessionService.findUserByUsername(username);
            if (user == null) {
                throw new RuntimeException("User not found for refresh token");
            }
            
            Set<String> authorities = user.getRoles().stream()
                    .map(role -> role.getName())
                    .collect(java.util.stream.Collectors.toSet());
            
            // Generate new access token
            String newAccessToken = jwtUtil.generateToken(username, authorities);
            Instant accessExpiry = jwtUtil.extractExpiration(newAccessToken).toInstant();
            userSessionService.registerToken(username, newAccessToken, accessExpiry);
            
            // Optionally generate new refresh token (rotation)
            RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(username, httpRequest);
            // Revoke old refresh token
            refreshTokenService.revokeToken(requestRefreshToken);
            
            RefreshTokenResponse response = RefreshTokenResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken.getToken())
                    .username(username)
                    .message("Token refreshed successfully")
                    .accessTokenExpiresIn(accessTokenExpiration)
                    .refreshTokenExpiresIn(refreshTokenExpiration)
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(RefreshTokenResponse.builder()
                    .message("Refresh token failed: " + e.getMessage())
                    .build());
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") @NotBlank(message = "Authorization header is required") String authHeader,
                                  @RequestBody(required = false) RefreshTokenRequest refreshTokenRequest) {
        String token = authHeader.replace("Bearer ", "");
        Instant expiry = jwtUtil.extractExpiration(token).toInstant();
        tokenBlacklistService.blacklistToken(token, expiry);
        
        // Remove from session map
        String username = jwtUtil.extractUsername(token);
        userSessionService.removeToken(username, token);
        
        // Revoke refresh token if provided
        if (refreshTokenRequest != null && refreshTokenRequest.getRefreshToken() != null) {
            refreshTokenService.revokeToken(refreshTokenRequest.getRefreshToken());
        }
        
        return ResponseEntity.ok("Logged out successfully");
    }
    
    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAll(@RequestHeader("Authorization") @NotBlank(message = "Authorization header is required") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.extractUsername(token);
        
        // Blacklist current access token
        Instant expiry = jwtUtil.extractExpiration(token).toInstant();
        tokenBlacklistService.blacklistToken(token, expiry);
        
        // Remove all sessions for user
        userSessionService.removeAllTokensForUser(username);
        
        // Revoke all refresh tokens for user
        refreshTokenService.revokeAllUserTokens(username);
        
        return ResponseEntity.ok("Logged out from all devices successfully");
    }
}