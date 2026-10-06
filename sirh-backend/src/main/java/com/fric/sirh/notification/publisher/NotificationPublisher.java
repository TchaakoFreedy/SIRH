package com.fric.sirh.notification.publisher;

import com.fric.sirh.notification.enums.NotificationEvent;
import com.fric.sirh.notification.event.NotificationDomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(NotificationEvent event, Map<String, Object> data, String triggeredBy) {
        NotificationDomainEvent domainEvent = new NotificationDomainEvent(this, event, data, triggeredBy);
        applicationEventPublisher.publishEvent(domainEvent);
    }
}