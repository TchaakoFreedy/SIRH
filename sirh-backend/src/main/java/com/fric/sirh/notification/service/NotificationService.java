package com.fric.sirh.notification.service;

import com.fric.sirh.notification.dto.NotificationDto;
import com.fric.sirh.notification.dto.NotificationFilter;
import com.fric.sirh.notification.dto.NotificationPageResponse;
import com.fric.sirh.notification.entity.Notification;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {

    Notification save(Notification notification);

    List<Notification> saveAll(List<Notification> notifications);

    NotificationPageResponse getNotifications(NotificationFilter filter, Pageable pageable, String currentUserId);

    long countUnread(String userId);

    NotificationPageResponse getUnread(String userId, Pageable pageable);

    void markAsRead(String notificationId, String currentUserId);

    void markAllAsRead(String userId);

    void delete(String notificationId, String currentUserId);

    void deleteAll(String userId);

    NotificationPageResponse search(NotificationFilter filter, Pageable pageable, String currentUserId);

    NotificationPageResponse getHistory(String userId, NotificationFilter filter, Pageable pageable);
}