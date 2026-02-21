package com.chaiyanan09.toothseg.user;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("users")
public class User {
    @Id
    public String id;

    public String fullName;

    @Indexed(unique = true)
    public String email;

    public String passwordHash;

    @Indexed
    public String resetTokenHash;
    public Instant resetTokenExpiresAt;

    public Instant createdAt = Instant.now();
}