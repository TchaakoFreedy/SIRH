package com.fric.sirh.notification.entity;

import com.fric.sirh.notification.enums.NotificationEvent;
import com.fric.sirh.notification.enums.NotificationPriority;
import com.fric.sirh.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "idx_recipient_created", def = "{'recipientId': 1, 'createdAt': -1}")
@CompoundIndex(name = "idx_company_created", def = "{'companyId': 1, 'createdAt': -1}")
@CompoundIndex(name = "idx_department_created", def = "{'departmentId': 1, 'createdAt': -1}")
public class Notification {
    @Id
    private String id;

    @Indexed
    private String recipientId;

    private String title;
    private String message;
    private NotificationType type;
    private NotificationPriority priority;
    private NotificationEvent event;

    @Indexed
    private boolean read;

    private LocalDateTime createdAt;
    private String createdBy;

    @Indexed
    private String companyId;

    @Indexed
    private String departmentId;

    private String employeeId;
    private String entityId;
    private String entityType;
    private String actionUrl;
    private Map<String, Object> metadata;
}