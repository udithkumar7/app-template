package com.template.service;

import com.template.entity.Otp;
import com.template.entity.User;
import com.template.repository.OtpRepository;
import com.template.repository.UserRepository;
import com.template.util.IpAddressUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {
    private final OtpRepository otpRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final RateLimitService rateLimitService;
    private final IpAddressUtil ipAddressUtil;

    @Value("${app.otp.length:6}")
    private int otpLength;

    @Value("${app.otp.expiry-minutes:10}")
    private int otpExpiryMinutes;

    @Value("${auth.account-expiry-years:1}")
    private int accountExpiryYears;

    public String generateOtp() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    public OtpSendResult sendOtp(String email, HttpServletRequest request) {
        // Check rate limiting
        String rateLimitKey = ipAddressUtil.getRateLimitKey(request, "otp");
        if (!rateLimitService.isAllowed(rateLimitKey)) {
            long remainingTime = rateLimitService.getTimeUntilReset(rateLimitKey);
            log.warn("Rate limit exceeded for IP: {}, email: {}",
                    ipAddressUtil.getClientIpAddress(request), email);
            return new OtpSendResult(false, "Rate limit exceeded. Please try again in " +
                    remainingTime + " seconds.", remainingTime);
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.warn("OTP requested for non-existent email: {}", email);
            return new OtpSendResult(false, "Email not found.", 0);
        }

        String otpCode = generateOtp();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(otpExpiryMinutes);

        Otp otp = Otp.builder()
                .email(email)
                .otpCode(otpCode)
                .expiryTime(expiryTime)
                .used(false)
                .build();
        Otp savedOtp = otpRepository.save(otp);

        try {
            emailService.sendOtpEmail(email, otpCode);
            log.info("OTP sent successfully to email: {}", email);
            return new OtpSendResult(true, "OTP sent successfully.", 0);
        } catch (Exception e) {
            otpRepository.delete(savedOtp);
            log.error("Failed to send OTP to email: {}", email, e);
            return new OtpSendResult(false, "Failed to send OTP. Please try again.", 0);
        }
    }

    // Legacy method for backward compatibility
    public boolean sendOtp(String email) {
        log.warn("Using legacy sendOtp method without rate limiting for email: {}", email);
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty())
            return false;

        String otpCode = generateOtp();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(otpExpiryMinutes);

        Otp otp = Otp.builder()
                .email(email)
                .otpCode(otpCode)
                .expiryTime(expiryTime)
                .used(false)
                .build();
        Otp savedOtp = otpRepository.save(otp);

        try {
            emailService.sendOtpEmail(email, otpCode);
            return true;
        } catch (Exception e) {
            otpRepository.delete(savedOtp);
            return false;
        }
    }

    public boolean verifyOtp(String email, String otpCode) {
        Optional<Otp> otpOpt = otpRepository.findByEmailAndOtpCodeAndUsedFalse(email, otpCode);
        if (otpOpt.isPresent()) {
            Otp otp = otpOpt.get();
            if (LocalDateTime.now().isAfter(otp.getExpiryTime())) {
                log.warn("Expired OTP used for email: {}", email);
                return false;
            }
            otp.setUsed(true);
            otpRepository.save(otp);
            log.info("OTP verified successfully for email: {}", email);
            return true;
        }
        log.warn("Invalid OTP used for email: {}", email);
        return false;
    }

    // Unlock account
    public boolean unlockAccount(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setAccountNonLocked(true);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
            log.info("Account unlocked for email: {}", email);
            return true;
        }
        return false;
    }

    // Extend expiry
    public boolean extendExpiry(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setAccountExpiryDate(LocalDateTime.now().plusYears(accountExpiryYears));
            userRepository.save(user);
            log.info("Account expiry extended for email: {}", email);
            return true;
        }
        return false;
    }

    // Update password
    public boolean updatePassword(String email, String newPassword) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Shift old passwords
            user.setPassword5(user.getPassword4());
            user.setPassword4(user.getPassword3());
            user.setPassword3(user.getPassword2());
            user.setPassword2(user.getPassword1());
            user.setPassword1(user.getPassword());
            // Set new password
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            log.info("Password updated for email: {}", email);
            return true;
        }
        return false;
    }

    // Helper methods for controller
    public boolean isAccountLocked(String email) {
        return userRepository.findByEmail(email)
                .map(u -> !u.getAccountNonLocked())
                .orElse(false);
    }

    public boolean isAccountExpired(String email) {
        return userRepository.findByEmail(email)
                .map(u -> u.getAccountExpiryDate() != null && u.getAccountExpiryDate().isBefore(LocalDateTime.now()))
                .orElse(false);
    }

    // Result class for OTP send operations
    public static class OtpSendResult {
        private final boolean success;
        private final String message;
        private final long timeUntilReset;

        public OtpSendResult(boolean success, String message, long timeUntilReset) {
            this.success = success;
            this.message = message;
            this.timeUntilReset = timeUntilReset;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public long getTimeUntilReset() {
            return timeUntilReset;
        }
    }
}