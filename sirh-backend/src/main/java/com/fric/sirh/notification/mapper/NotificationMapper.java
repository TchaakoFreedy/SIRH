package com.fric.sirh.notification.mapper;

import com.fric.sirh.notification.dto.NotificationDto;
import com.fric.sirh.notification.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationDto toDto(Notification notification) {
        if (notification == null) return null;
        return NotificationDto.builder()
                .id(notification.getId())
                .recipientId(notification.getRecipientId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .priority(notification.getPriority())
                .event(notification.getEvent())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .createdBy(notification.getCreatedBy())
                .companyId(notification.getCompanyId())
                .departmentId(notification.getDepartmentId())
                .employeeId(notification.getEmployeeId())
                .entityId(notification.getEntityId())
                .entityType(notification.getEntityType())
                .actionUrl(notification.getActionUrl())
                .metadata(notification.getMetadata())
                .build();
    }
}