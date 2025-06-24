package com.template.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.host:localhost}")
    private String mailHost;
    
    @Value("${app.email.console-only:false}")
    private boolean consoleOnlyMode;

    public void sendOtpEmail(String to, String otp) {
        String subject = "OTP Verification";
        String text = "Your OTP is: " + otp + "\n\nThis OTP will expire in 10 minutes.";
        
        // If console-only mode or localhost, just log to console
        if (consoleOnlyMode || "localhost".equals(mailHost)) {
            log.info("=== EMAIL WOULD BE SENT ===");
            log.info("To: {}", to);
            log.info("Subject: {}", subject);
            log.info("Body: {}", text);
            log.info("=========================");
            return;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            
            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}. Error: {}", to, e.getMessage());
            // For development, log the OTP so testing can continue
            log.warn("For development - OTP was: {}", otp);
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }
}