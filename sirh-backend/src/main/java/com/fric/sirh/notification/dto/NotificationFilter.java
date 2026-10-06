package com.fric.sirh.notification.dto;

import com.fric.sirh.notification.enums.NotificationEvent;
import com.fric.sirh.notification.enums.NotificationPriority;
import com.fric.sirh.notification.enums.NotificationType;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class NotificationFilter {
    private String recipientId;
    private String companyId;
    private String departmentId;
    private Boolean read;
    private NotificationEvent event;
    private NotificationType type;
    private NotificationPriority priority;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime toDate;
}