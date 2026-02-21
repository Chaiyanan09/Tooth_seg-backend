package com.chaiyanan09.toothseg.controller;

import com.chaiyanan09.toothseg.dto.MeResponse;
import com.chaiyanan09.toothseg.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class MeController {

    private final UserRepository users;

    public MeController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/me")
    public MeResponse me(Authentication auth) {
        String userId = (String) auth.getPrincipal(); // JwtAuthFilter set principal = userId
        var u = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found."));
        return new MeResponse(u.fullName, u.email);
    }
}