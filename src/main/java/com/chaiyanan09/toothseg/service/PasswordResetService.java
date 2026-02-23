package com.chaiyanan09.toothseg.service;

import com.chaiyanan09.toothseg.repository.UserRepository;
import com.chaiyanan09.toothseg.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class PasswordResetService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final MailService mailService;

    private final String frontendBaseUrl;
    private final long expiresMinutes;

    public PasswordResetService(
            UserRepository users,
            PasswordEncoder encoder,
            MailService mailService,
            @Value("${app.frontend.baseUrl:http://localhost:3000}") String frontendBaseUrl,
            @Value("${app.reset.expiresMinutes:15}") long expiresMinutes
    ) {
        this.users = users;
        this.encoder = encoder;
        this.mailService = mailService;
        this.frontendBaseUrl = trimRightSlash(frontendBaseUrl);
        this.expiresMinutes = Math.max(1, expiresMinutes); // กันค่า 0/ติดลบ
    }

    /**
     * ขอ reset: generate token, เก็บ hash+expiry, แล้วส่งลิงก์ไปอีเมล
     * NOTE: ไม่ควร throw error ออกไปหา client เพื่อไม่ให้ endpoint 500
     */
    public void requestReset(String email) {
        String em = normalizeEmail(email);
        if (em.isBlank()) return;

        var opt = users.findByEmail(em);
        if (opt.isEmpty()) {
            // ✅ ไม่บอกว่า email มี/ไม่มี เพื่อความปลอดภัย
            return;
        }

        User u = opt.get();

        String token = generateToken();
        u.resetTokenHash = sha256(token);
        u.resetTokenExpiresAt = Instant.now().plusSeconds(expiresMinutes * 60);
        users.save(u);

        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        String link = frontendBaseUrl + "/reset-password?token=" + encodedToken;

        // ✅ สำคัญ: ห้ามให้ส่งเมลล้มแล้วทำ API 500
        try {
            mailService.sendResetLink(u.email, link);
        } catch (Exception ex) {
            // log ได้ แต่ไม่ throw
            System.err.println("[MAIL ERROR] " + ex.getMessage());
            // fallback: แสดงลิงก์ใน log เผื่อ debug
            System.out.println("[RESET LINK] " + link);
        }
    }

    public void resetPassword(String token, String newPassword, String confirmPassword) {
        if (token == null || token.isBlank()) throw new IllegalArgumentException("Invalid or expired token.");
        if (newPassword == null || newPassword.isBlank()) throw new IllegalArgumentException("Password is required.");
        if (newPassword.length() < 6) throw new IllegalArgumentException("Password must be at least 6 characters.");
        if (!newPassword.equals(confirmPassword)) throw new IllegalArgumentException("Passwords do not match.");

        String tokenHash = sha256(token);

        User u = users.findByResetTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token."));

        // ถ้า expired -> เคลียร์ token ทิ้งแล้วตอบ error
        if (u.resetTokenExpiresAt == null || Instant.now().isAfter(u.resetTokenExpiresAt)) {
            u.resetTokenHash = null;
            u.resetTokenExpiresAt = null;
            users.save(u);
            throw new IllegalArgumentException("Invalid or expired token.");
        }

        u.passwordHash = encoder.encode(newPassword);
        u.resetTokenHash = null;
        u.resetTokenExpiresAt = null;
        users.save(u);
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static String trimRightSlash(String s) {
        if (s == null) return "";
        String t = s.trim();
        while (t.endsWith("/")) t = t.substring(0, t.length() - 1);
        return t;
    }

    private static String generateToken() {
        byte[] b = new byte[32];
        new SecureRandom().nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(dig);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}