package com.fric.sirh.notification.repository;

import com.fric.sirh.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    Page<Notification> findByRecipientId(String recipientId, Pageable pageable);

    @Query("{ 'recipientId': ?0, 'read': false }")
    Page<Notification> findUnreadByRecipientId(String recipientId, Pageable pageable);

    @Query(value = "{ 'recipientId': ?0, 'read': false }", count = true)
    long countUnreadByRecipientId(String recipientId);

    Page<Notification> findByRecipientIdAndCreatedAtBetween(String recipientId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    // Méthodes supplémentaires si besoin
}