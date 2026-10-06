package com.fric.sirh.repository;

import com.fric.sirh.model.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {

    Optional<RefreshToken> findByUserIdAndToken(String userId, String token);

    Optional<RefreshToken> findByToken(String token);

    void deleteByUserId(String userId);

    void deleteByToken(String token);

    long countByUserId(String userId);
}