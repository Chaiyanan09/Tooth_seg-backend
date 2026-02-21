package com.chaiyanan09.toothseg.controller;

import com.chaiyanan09.toothseg.dto.MeResponse;
import com.chaiyanan09.toothseg.dto.UpdateMeRequest;
import com.chaiyanan09.toothseg.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class MeController {
    private final UserRepository users;

    public MeController(UserRepository users) { this.users = users; }

    @GetMapping("/me")
    public MeResponse me(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        var u = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found."));
        return new MeResponse(u.fullName, u.email);
    }

    @PutMapping("/me")
    public MeResponse updateMe(Authentication auth, @RequestBody UpdateMeRequest req) {
        String userId = (String) auth.getPrincipal();
        var u = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found."));

        String name = req.fullName == null ? "" : req.fullName.trim();
        if (name.isBlank()) throw new IllegalArgumentException("Full name is required.");

        u.fullName = name;
        users.save(u);
        return new MeResponse(u.fullName, u.email);
    }
}