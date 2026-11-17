package com.chaiyanan09.toothseg.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Table "users" (created by Flyway: db/migration/V1__init_schema.sql).
 */
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "full_name", nullable = false)
    public String fullName;

    // UNIQUE + index are defined in the migration
    @Column(name = "email", nullable = false, unique = true)
    public String email;

    @Column(name = "password_hash", nullable = false)
    public String passwordHash;

    @Column(name = "reset_token_hash")
    public String resetTokenHash;

    @Column(name = "reset_token_expires_at")
    public Instant resetTokenExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt = Instant.now();
}
