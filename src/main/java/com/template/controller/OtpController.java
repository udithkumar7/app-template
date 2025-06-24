package com.template.controller;

import com.template.dto.OtpRequest;
import com.template.dto.OtpVerificationRequest;
import com.template.dto.OtpForgotRequest;
import com.template.service.OtpService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/otp")
@RequiredArgsConstructor
@Slf4j
public class OtpController {
    private final OtpService otpService;

    // Send OTP with rate limiting
    @PostMapping("/send")
    public ResponseEntity<?> sendOtp(@RequestBody OtpRequest req, HttpServletRequest request) {
        OtpService.OtpSendResult result = otpService.sendOtp(req.getEmail(), request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("message", result.getMessage());

        if (!result.isSuccess() && result.getTimeUntilReset() > 0) {
            response.put("timeUntilReset", result.getTimeUntilReset());
            return ResponseEntity.status(429).body(response); // 429 Too Many Requests
        }

        return result.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpVerificationRequest req) {
        if (otpService.verifyOtp(req.getEmail(), req.getOtpCode())) {
            return ResponseEntity.ok("OTP verified successfully.");
        }
        return ResponseEntity.badRequest().body("Invalid or expired OTP.");
    }

    // Send OTP only if account is locked
    @PostMapping("/send-if-locked")
    public ResponseEntity<?> sendOtpIfLocked(@RequestBody OtpRequest req, HttpServletRequest request) {
        if (!otpService.isAccountLocked(req.getEmail())) {
            return ResponseEntity.badRequest().body("Account is not locked.");
        }

        OtpService.OtpSendResult result = otpService.sendOtp(req.getEmail(), request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("message", result.getMessage());

        if (!result.isSuccess() && result.getTimeUntilReset() > 0) {
            response.put("timeUntilReset", result.getTimeUntilReset());
            return ResponseEntity.status(429).body(response);
        }

        return result.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/verify-unlock")
    public ResponseEntity<?> verifyUnlock(@RequestBody OtpVerificationRequest req) {
        if (otpService.verifyOtp(req.getEmail(), req.getOtpCode())) {
            otpService.unlockAccount(req.getEmail());
            return ResponseEntity.ok("Account unlocked.");
        }
        return ResponseEntity.badRequest().body("Invalid or expired OTP.");
    }

    // Activate/extend account
    @PostMapping("/send-activate")
    public ResponseEntity<?> sendActivateOtp(@RequestBody OtpRequest req, HttpServletRequest request) {
        if (!otpService.isAccountExpired(req.getEmail())) {
            return ResponseEntity.badRequest().body("Account is not expired.");
        }

        OtpService.OtpSendResult result = otpService.sendOtp(req.getEmail(), request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("message", result.getMessage());

        if (!result.isSuccess() && result.getTimeUntilReset() > 0) {
            response.put("timeUntilReset", result.getTimeUntilReset());
            return ResponseEntity.status(429).body(response);
        }

        return result.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/verify-activate")
    public ResponseEntity<?> verifyActivate(@RequestBody OtpVerificationRequest req) {
        if (otpService.verifyOtp(req.getEmail(), req.getOtpCode())) {
            otpService.extendExpiry(req.getEmail());
            return ResponseEntity.ok("Account activated.");
        }
        return ResponseEntity.badRequest().body("Invalid or expired OTP.");
    }

    // Forgot password
    @PostMapping("/send-forgot")
    public ResponseEntity<?> sendForgotOtp(@RequestBody OtpRequest req, HttpServletRequest request) {
        OtpService.OtpSendResult result = otpService.sendOtp(req.getEmail(), request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("message", result.getMessage());

        if (!result.isSuccess() && result.getTimeUntilReset() > 0) {
            response.put("timeUntilReset", result.getTimeUntilReset());
            return ResponseEntity.status(429).body(response);
        }

        return result.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/verify-forgot")
    public ResponseEntity<?> verifyForgot(@RequestBody OtpForgotRequest req) {
        if (otpService.verifyOtp(req.getEmail(), req.getOtpCode())) {
            otpService.updatePassword(req.getEmail(), req.getNewPassword());
            return ResponseEntity.ok("Password updated.");
        }
        return ResponseEntity.badRequest().body("Invalid or expired OTP.");
    }
}