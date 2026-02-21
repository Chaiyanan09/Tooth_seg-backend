package com.chaiyanan09.toothseg.controller;

import com.chaiyanan09.toothseg.dto.AuthResponse;
import com.chaiyanan09.toothseg.dto.LoginRequest;
import com.chaiyanan09.toothseg.dto.RegisterRequest;
import com.chaiyanan09.toothseg.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest req) {
        String token = authService.login(req);
        return new AuthResponse(token);
    }
}