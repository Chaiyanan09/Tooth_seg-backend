package com.chaiyanan09.toothseg.repository;

import com.chaiyanan09.toothseg.user.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // ✅ Add this for password reset
    Optional<User> findByResetTokenHash(String resetTokenHash);
}