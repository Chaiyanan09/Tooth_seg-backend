package com.chaiyanan09.toothseg.controller;

import com.chaiyanan09.toothseg.dto.ForgotPasswordRequest;
import com.chaiyanan09.toothseg.dto.ResetPasswordRequest;
import com.chaiyanan09.toothseg.service.PasswordResetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

    private final PasswordResetService reset;

    public PasswordResetController(PasswordResetService reset) {
        this.reset = reset;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgot(@RequestBody ForgotPasswordRequest req) {
        reset.requestReset(req.email);
        // ✅ ไม่เปิดเผยว่า email มี/ไม่มี
        return ResponseEntity.ok(Map.of("message", "If the account exists, a reset link will be sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody ResetPasswordRequest req) {
        reset.resetPassword(req.token, req.newPassword, req.confirmPassword);
        return ResponseEntity.ok(Map.of("message", "Password updated."));
    }
}