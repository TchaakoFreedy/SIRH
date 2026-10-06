package com.fric.sirh.repository;

import com.fric.sirh.model.PasswordResetToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {

    Optional<PasswordResetToken> findByEmailAndCode(String email, String code);

    void deleteByEmail(String email);
}