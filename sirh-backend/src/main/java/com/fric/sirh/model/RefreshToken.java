package com.fric.sirh.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "refresh_tokens")
public class RefreshToken {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    @Indexed(unique = true)
    private String token;

    private LocalDateTime createdAt;

    @Indexed(expireAfterSeconds = 0)  // Expiration automatique MongoDB
    private LocalDateTime expiresAt;

    private String userAgent;
    private String ipAddress;
    private boolean revoked;
}