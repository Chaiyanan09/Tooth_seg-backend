package com.chaiyanan09.toothseg.service;

import com.chaiyanan09.toothseg.dto.LoginRequest;
import com.chaiyanan09.toothseg.dto.RegisterRequest;
import com.chaiyanan09.toothseg.repository.UserRepository;
import com.chaiyanan09.toothseg.user.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public void register(RegisterRequest req) {
        String fullName = req.fullName == null ? "" : req.fullName.trim();
        String email = req.email == null ? "" : req.email.trim().toLowerCase();
        String password = req.password == null ? "" : req.password;
        String confirm = req.confirmPassword == null ? "" : req.confirmPassword;

        if (fullName.isBlank()) throw new IllegalArgumentException("Full name is required.");
        if (email.isBlank()) throw new IllegalArgumentException("Email is required.");
        if (password.isBlank()) throw new IllegalArgumentException("Password is required.");
        if (password.length() < 6) throw new IllegalArgumentException("Password must be at least 6 characters.");
        if (!password.equals(confirm)) throw new IllegalArgumentException("Passwords do not match.");
        if (users.existsByEmail(email)) throw new IllegalArgumentException("Email already exists.");

        User u = new User();
        u.fullName = fullName;
        u.email = email;
        u.passwordHash = encoder.encode(password);
        users.save(u);
    }

    public String login(LoginRequest req) {
        String email = (req.email == null ? "" : req.email.trim().toLowerCase());
        String password = (req.password == null ? "" : req.password);

        var u = users.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

        if (!encoder.matches(password, u.passwordHash)) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        return jwt.createToken(u.id, u.email);
    }
}