package com.template.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

// Import your hybrid encryption utils
import com.template.util.RSAUtil;
import com.template.util.RSAPublicKeyUtil;
import com.template.util.RSAPrivateKeyUtil;
import com.template.util.AESUtil;

@RestController
@RequestMapping("/api/crypto")
public class CryptoController {
    private static final String SESSION_PRIVATE_KEY = "PRIVATE_KEY";
    private static final String SESSION_AES_KEY = "SESSION_AES_KEY";

    @GetMapping("/public-key")
    public ResponseEntity<String> getPublicKey(HttpSession session) throws Exception {
        KeyPair keyPair = RSAUtil.generateKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();
        session.setAttribute(SESSION_PRIVATE_KEY, privateKey);
        String publicKeyStr = RSAPublicKeyUtil.toString(publicKey);
        return ResponseEntity.ok(publicKeyStr);
    }

    @PostMapping("/session-key")
    public ResponseEntity<String> receiveSessionKey(@RequestBody String encryptedSessionKey, HttpSession session) throws Exception {
        PrivateKey privateKey = (PrivateKey) session.getAttribute(SESSION_PRIVATE_KEY);
        if (privateKey == null) {
            return ResponseEntity.badRequest().body("No private key in session");
        }
        // Decrypt AES key with private key
        String aesKey = RSAPrivateKeyUtil.decrypt(encryptedSessionKey, privateKey);
        session.setAttribute(SESSION_AES_KEY, aesKey);
        return ResponseEntity.ok("Session key received and stored");
    }

    @PostMapping("/secure-data")
    public ResponseEntity<String> receiveSecureData(@RequestBody String encryptedData, HttpSession session) throws Exception {
        String aesKey = (String) session.getAttribute(SESSION_AES_KEY);
        if (aesKey == null) {
            return ResponseEntity.badRequest().body("No session key in session");
        }
        // Decrypt data with AES key using JS-compatible method
        String decrypted = AESUtil.decryptJS(encryptedData, aesKey);
        System.out.println("Decrypted data: " + decrypted);
        
        // Encrypt "success" response with the same AES key
        String encryptedResponse = AESUtil.encryptJS("success", aesKey);
        return ResponseEntity.ok(encryptedResponse);
    }
} 