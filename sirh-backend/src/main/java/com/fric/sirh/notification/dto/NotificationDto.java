package com.fric.sirh.notification.dto;

import com.fric.sirh.notification.enums.NotificationEvent;
import com.fric.sirh.notification.enums.NotificationPriority;
import com.fric.sirh.notification.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class NotificationDto {
    private String id;
    private String recipientId;
    private String title;
    private String message;
    private NotificationType type;
    private NotificationPriority priority;
    private NotificationEvent event;
    private boolean read;
    private LocalDateTime createdAt;
    private String createdBy;
    private String companyId;
    private String departmentId;
    private String employeeId;
    private String entityId;
    private String entityType;
    private String actionUrl;
    private Map<String, Object> metadata;
}