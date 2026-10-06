// com.fric.sirh.service.BulletinNotificationService
package com.fric.sirh.service;

import com.fric.sirh.model.BulletinPaie;
import com.fric.sirh.model.Employee;
import com.fric.sirh.model.User;
import com.fric.sirh.notification.enums.NotificationEvent;
import com.fric.sirh.notification.publisher.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class BulletinNotificationService {

    private final NotificationPublisher notificationPublisher;

    public void notifyEmployee(Employee employee, BulletinPaie bulletin) {
        User user = employee.getUser();
        if (user == null) {
            log.warn("Employé {} n'a pas d'utilisateur, notification non envoyée", employee.getId());
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("employeeId", employee.getId());
        data.put("entityId", bulletin.getId());
        data.put("entityType", "PAYSLIP");
        data.put("actionUrl", "/pay-slips/" + bulletin.getId());
        data.put("metadata", Map.of(
                "month", bulletin.getMonth(),
                "year", bulletin.getYear(),
                "netSalary", bulletin.getNetSalary()
        ));
        notificationPublisher.publish(NotificationEvent.PAYSLIP_UPLOADED, data, user.getId());
        log.info("Notification envoyée à l'utilisateur {} pour le bulletin {}", user.getId(), bulletin.getId());
    }
}