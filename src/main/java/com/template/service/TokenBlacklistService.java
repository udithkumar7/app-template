package com.template.service;

import com.template.entity.BlacklistedToken;
import com.template.repository.BlacklistedTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
    private final BlacklistedTokenRepository repository;

    public void blacklistToken(String token, Instant expiry) {
        repository.save(BlacklistedToken.builder()
            .token(token)
            .expiry(expiry)
            .build());
    }

    public boolean isTokenBlacklisted(String token) {
        return repository.existsByToken(token);
    }
}
