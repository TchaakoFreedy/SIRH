package com.fric.sirh.notification.resolver;

import com.fric.sirh.notification.enums.NotificationEvent;

import java.util.List;
import java.util.Map;

public interface NotificationRecipientResolver {
    List<String> resolve(NotificationEvent event, Map<String, Object> data);
}