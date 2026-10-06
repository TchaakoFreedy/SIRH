// src/main/java/com/fric/sirh/notification/event/NotificationDomainEvent.java
package com.fric.sirh.notification.event;

import com.fric.sirh.notification.enums.NotificationEvent;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

public class NotificationDomainEvent extends ApplicationEvent {
    private final NotificationEvent event;
    private final Map<String, Object> data;
    private final String triggeredBy;

    public NotificationDomainEvent(Object source, NotificationEvent event, Map<String, Object> data, String triggeredBy) {
        super(source);
        this.event = event;
        this.data = data;
        this.triggeredBy = triggeredBy;
    }

    public NotificationEvent getEvent() { return event; }
    public Map<String, Object> getData() { return data; }
    public String getTriggeredBy() { return triggeredBy; }
}