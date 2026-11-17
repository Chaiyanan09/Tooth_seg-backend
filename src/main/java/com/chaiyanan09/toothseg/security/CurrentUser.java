package com.chaiyanan09.toothseg.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * อ่าน userId ของคนที่ล็อกอินอยู่ จาก Authentication ที่ JwtAuthFilter ใส่ไว้ (subject ของ JWT)
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static UUID id(Authentication auth) {
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Object principal = auth.getPrincipal();
        String raw = (principal instanceof String s) ? s : auth.getName();
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException | NullPointerException e) {
            // เช่น token เก่าสมัย MongoDB ที่ subject เป็น ObjectId -> ให้ล็อกอินใหม่
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid session. Please log in again.");
        }
    }
}
