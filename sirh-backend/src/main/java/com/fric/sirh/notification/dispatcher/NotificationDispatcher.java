// src/main/java/com/fric/sirh/notification/dispatcher/NotificationDispatcher.java
package com.fric.sirh.notification.dispatcher;

import com.fric.sirh.model.Employee;
import com.fric.sirh.model.Role;
import com.fric.sirh.model.User;
import com.fric.sirh.notification.entity.Notification;
import com.fric.sirh.notification.enums.NotificationEvent;
import com.fric.sirh.notification.enums.NotificationPriority;
import com.fric.sirh.notification.enums.NotificationType;
import com.fric.sirh.notification.event.NotificationDomainEvent;
import com.fric.sirh.notification.resolver.NotificationRecipientResolver;
import com.fric.sirh.notification.service.NotificationService;
import com.fric.sirh.notification.websocket.NotificationWebSocketHandler;
import com.fric.sirh.repository.EmployeeRepository;
import com.fric.sirh.repository.RoleRepository;
import com.fric.sirh.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final NotificationRecipientResolver recipientResolver;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationWebSocketHandler webSocketHandler;
    private final RoleRepository roleRepository;

    private final ConcurrentHashMap<String, Long> recentEvents = new ConcurrentHashMap<>();
    private static final long EXPIRE_DELAY_MS = 3000;

    @PostConstruct
    public void init() {
        log.info("NotificationDispatcher initialisé avec succès.");
    }

    @SuppressWarnings("unchecked")
    @EventListener
    public void handle(NotificationDomainEvent domainEvent) {
        log.info("=== NotificationDispatcher.handle() a été déclenché ===");
        NotificationEvent event = domainEvent.getEvent();
        Map<String, Object> data = domainEvent.getData();
        String triggeredBy = domainEvent.getTriggeredBy();

        cleanExpiredEntries();

        String entityId = (String) data.get("entityId");
        String eventKey = event.name() + ":" + (entityId != null ? entityId : data.hashCode()) + ":" + triggeredBy;

        if (recentEvents.containsKey(eventKey)) {
            log.warn("Evenement dupliqué IGNORÉ (clé: {})", eventKey);
            return;
        }
        recentEvents.put(eventKey, System.currentTimeMillis());

        String displayName = resolveUserDisplayName(triggeredBy);
        data.put("triggeredByDisplayName", displayName);
        log.info("ID déclencheur : {} -> Nom résolu : {}", triggeredBy, displayName);

        enrichDataWithNames(data);

        // Récupération des destinataires via le resolver
        List<String> recipientIds = recipientResolver.resolve(event, data);
        log.info("Destinataires résolus (bruts) : {}", recipientIds);

        // Si l'événement est une expiration, on ajoute systématiquement les rôles cibles (RH, DIRECTION, etc.)
        if (isExpirationEvent(event)) {
            List<String> roleUserIds = getExpirationRecipientUserIds();
            if (!roleUserIds.isEmpty()) {
                // Initialiser la liste si elle est null
                if (recipientIds == null) {
                    recipientIds = new ArrayList<>();
                }
                // Ajouter les IDs des rôles cibles en évitant les doublons
                for (String userId : roleUserIds) {
                    if (!recipientIds.contains(userId)) {
                        recipientIds.add(userId);
                    }
                }
                log.info("Ajout des utilisateurs des rôles cibles (RH, DIRECTION, ...) : {}", roleUserIds);
                log.info("Liste finale des destinataires (bruts) : {}", recipientIds);
            } else {
                log.warn("Aucun utilisateur trouvé pour les rôles cibles.");
            }
        }

        // Si après tout cela, il n'y a toujours pas de destinataires, on abandonne
        if (recipientIds == null || recipientIds.isEmpty()) {
            log.info("Aucun destinataire final pour l'événement {}", event);
            return;
        }

        // Résolution des identifiants en user IDs
        List<String> recipientUserIds = recipientIds.stream()
                .map(this::resolveRecipientUserId)
                .filter(id -> id != null && !id.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        log.info("User IDs finaux : {}", recipientUserIds);

        if (recipientUserIds.isEmpty()) {
            log.warn("Aucun ID utilisateur valide pour l'événement {}", event);
            return;
        }

        // Construction et sauvegarde des notifications
        List<Notification> notifications = recipientUserIds.stream()
                .map(userId -> buildNotification(userId, event, data))
                .collect(Collectors.toList());

        if (!notifications.isEmpty()) {
            List<Notification> savedNotifications = notificationService.saveAll(notifications);
            log.info("{} notifications sauvegardées pour {}", savedNotifications.size(), event);
            for (Notification notification : savedNotifications) {
                webSocketHandler.broadcastNotification(notification);
            }
            log.info("{} notifications diffusées via WebSocket", savedNotifications.size());
        } else {
            log.info("Aucune notification à sauvegarder pour {}", event);
        }
    }

    /**
     * Vérifie si l'événement est une alerte d'expiration de contrat.
     */
    private boolean isExpirationEvent(NotificationEvent event) {
        return event == NotificationEvent.CONTRACT_EXPIRING_SOON
                || event == NotificationEvent.CONTRACT_EXPIRING_DAILY
                || event == NotificationEvent.CONTRACT_EXPIRED_TODAY
                || event == NotificationEvent.CONTRACT_EXPIRING_TWO_WEEKS;
    }

    /**
     * Récupère les IDs des utilisateurs ayant l'un des rôles cibles pour les alertes d'expiration.
     * Vous pouvez adapter la liste selon vos besoins (RH, DIRECTION, TOP_MANAGER, MANAGER).
     */
    private List<String> getExpirationRecipientUserIds() {
        // Rôles cibles (vous pouvez ajouter ou retirer selon votre besoin)
        List<String> roleNames = List.of("RH", "DIRECTION", "TOP_MANAGER", "MANAGER");
        List<String> userIds = new ArrayList<>();
        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(roleName).orElse(null);
            if (role == null) {
                log.warn("Rôle {} non trouvé, ignoré.", roleName);
                continue;
            }
            List<User> users = userRepository.findByRoleId(role.getId());
            if (!users.isEmpty()) {
                log.info("Rôle {} : {} utilisateurs trouvés", roleName, users.size());
                userIds.addAll(users.stream().map(User::getId).collect(Collectors.toList()));
            } else {
                log.info("Aucun utilisateur pour le rôle {}", roleName);
            }
        }
        return userIds.stream().distinct().collect(Collectors.toList());
    }

    // ========== Les méthodes suivantes sont inchangées ==========

    private String resolveRecipientUserId(String recipientId) {
        if (recipientId == null || recipientId.trim().isEmpty()) return null;
        if (recipientId.matches("^[a-fA-F0-9]{24}$")) {
            if (userRepository.findById(recipientId).isPresent()) return recipientId;
            Employee employee = employeeRepository.findById(recipientId).orElse(null);
            if (employee != null && employee.getUserId() != null) {
                User user = userRepository.findById(employee.getUserId()).orElse(null);
                if (user != null) return user.getId();
            }
            log.warn("ID non résolu : {}", recipientId);
            return null;
        }
        if (recipientId.contains("@")) {
            User user = userRepository.findByEmail(recipientId).orElse(null);
            if (user != null) return user.getId();
            log.warn("Aucun user pour email : {}", recipientId);
            return null;
        }
        log.warn("Format non reconnu : {}", recipientId);
        return null;
    }

    private void enrichDataWithNames(Map<String, Object> data) {
        String employeeId = (String) data.get("employeeId");
        if (employeeId != null && !data.containsKey("employeeName")) {
            employeeRepository.findById(employeeId)
                    .map(this::getEmployeeFullName)
                    .ifPresent(name -> data.put("employeeName", name));
        }
        String entityType = (String) data.get("entityType");
        if ("EMPLOYEE".equals(entityType) && !data.containsKey("entityName")) {
            String empId = (String) data.get("entityId");
            if (empId != null) {
                employeeRepository.findById(empId)
                        .map(this::getEmployeeFullName)
                        .ifPresent(name -> data.put("entityName", name));
            }
        }
    }

    private String getEmployeeFullName(Employee employee) {
        if (employee == null) return "Inconnu";
        String prenom = employee.getPrenom() != null ? employee.getPrenom() : "";
        String nom = employee.getNom() != null ? employee.getNom() : "";
        String fullName = (prenom + " " + nom).trim();
        return fullName.isEmpty() ? "Employé" : fullName;
    }

    private Notification buildNotification(String recipientUserId, NotificationEvent event, Map<String, Object> data) {
        String title = buildTitle(event, data);
        String message = buildMessage(event, data);
        NotificationType type = determineType(event);
        NotificationPriority priority = determinePriority(event);

        String companyId = (String) data.get("companyId");
        String departmentId = (String) data.get("departmentId");
        String employeeId = (String) data.get("employeeId");
        String entityId = (String) data.get("entityId");
        String entityType = (String) data.get("entityType");
        String actionUrl = (String) data.get("actionUrl");
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) data.get("metadata");

        return Notification.builder()
                .recipientId(recipientUserId)
                .title(title)
                .message(message)
                .type(type)
                .priority(priority)
                .event(event)
                .read(false)
                .createdAt(LocalDateTime.now())
                .createdBy((String) data.get("triggeredByDisplayName"))
                .companyId(companyId)
                .departmentId(departmentId)
                .employeeId(employeeId)
                .entityId(entityId)
                .entityType(entityType)
                .actionUrl(actionUrl)
                .metadata(metadata)
                .build();
    }

    private String buildTitle(NotificationEvent event, Map<String, Object> data) {
        String employeeName = getEmployeeName(data);
        Map<String, Object> metadata = (Map<String, Object>) data.get("metadata");
        Long days = getDaysUntilExpiration(metadata);

        switch (event) {
            case CONTRACT_CREATED: return "Nouveau contrat pour " + employeeName;
            case CONTRACT_UPDATED: return "Contrat modifié pour " + employeeName;
            case CONTRACT_RENEWED: return "Contrat renouvelé pour " + employeeName;
            case CONTRACT_RESILIATED: return "Contrat résilié pour " + employeeName;
            case CONTRACT_ARCHIVED: return "Contrat archivé pour " + employeeName;
            case CONTRACT_EXTENDED: return "Contrat prolongé pour " + employeeName;
            case CONTRACT_EXPIRED: return "Contrat expiré pour " + employeeName;
            case CONTRACT_EXPIRING_TWO_WEEKS: return "Alerte : contrat de " + employeeName + " expire dans 2 semaines";
            case CONTRACT_EXPIRING_DAILY: return "Rappel : contrat de " + employeeName + " expire bientôt";
            case CONTRACT_EXPIRED_TODAY: return "URGENT : contrat de " + employeeName + " expire aujourd'hui";
            case CONTRACT_EXPIRING_SOON:
                if (days != null) return "Alerte : contrat de " + employeeName + " expire dans " + days + " jour" + (days > 1 ? "s" : "");
                return "Alerte : contrat de " + employeeName + " expire bientôt";
            case EMPLOYEE_CREATED: return "Nouvel employé " + employeeName;
            case EMPLOYEE_UPDATED: return "Informations de " + employeeName + " mises à jour";
            case EMPLOYEE_SUSPENDED: return "Employé " + employeeName + " suspendu";
            case EMPLOYEE_REACTIVATED: return "Employé " + employeeName + " réactivé";
            case DOCUMENT_UPLOADED: return "Document téléchargé pour " + employeeName;
            case PAYSLIP_UPLOADED: return "Bulletin de paie de " + employeeName + " disponible";
            default: return "Notification " + event.name();
        }
    }

    private String buildMessage(NotificationEvent event, Map<String, Object> data) {
        String employeeName = getEmployeeName(data);
        String triggeredBy = (String) data.get("triggeredByDisplayName");
        Map<String, Object> metadata = (Map<String, Object>) data.get("metadata");
        String contractType = getContractType(metadata);
        String startDate = getContractStart(metadata);
        String endDate = getContractEnd(metadata, data);
        Long days = getDaysUntilExpiration(metadata);

        switch (event) {
            case CONTRACT_CREATED:
                return String.format("Le contrat de type %s a été créé pour %s par %s. Début: %s, Fin: %s.",
                        contractType, employeeName, triggeredBy, startDate, endDate);
            case CONTRACT_UPDATED:
                return String.format("Le contrat de %s a été modifié par %s. Nouveau type: %s, Nouvelle fin: %s.",
                        employeeName, triggeredBy, contractType, endDate);
            case CONTRACT_RENEWED:
                return String.format("Le contrat de %s a été renouvelé par %s jusqu'au %s.",
                        employeeName, triggeredBy, endDate);
            case CONTRACT_RESILIATED:
                return String.format("Le contrat de %s a été résilié par %s.", employeeName, triggeredBy);
            case CONTRACT_ARCHIVED:
                return String.format("Le contrat de %s a été archivé par %s.", employeeName, triggeredBy);
            case CONTRACT_EXTENDED:
                return String.format("Le contrat de %s a été prolongé par %s jusqu'au %s.",
                        employeeName, triggeredBy, endDate);
            case CONTRACT_EXPIRED:
                return String.format("Le contrat de %s est expiré depuis le %s.", employeeName, endDate);
            case CONTRACT_EXPIRING_TWO_WEEKS:
                return String.format("Le contrat de %s expire dans 2 semaines (le %s).", employeeName, endDate);
            case CONTRACT_EXPIRING_DAILY:
                return String.format("Rappel : le contrat de %s expire le %s.", employeeName, endDate);
            case CONTRACT_EXPIRED_TODAY:
                return String.format("URGENT : Le contrat de %s expire AUJOURD'HUI (%s).", employeeName, endDate);
            case CONTRACT_EXPIRING_SOON:
                if (days != null) {
                    return String.format("Le contrat de %s expire dans %d jour%s (le %s).",
                            employeeName, days, (days > 1 ? "s" : ""), endDate);
                }
                return String.format("Le contrat de %s expire bientôt (le %s).", employeeName, endDate);
            case EMPLOYEE_CREATED:
                return String.format("L'employé %s a été créé par %s.", employeeName, triggeredBy);
            case EMPLOYEE_UPDATED:
                return String.format("Les informations de l'employé %s ont été modifiées par %s.", employeeName, triggeredBy);
            case EMPLOYEE_SUSPENDED:
                return String.format("L'employé %s a été suspendu par %s.", employeeName, triggeredBy);
            case EMPLOYEE_REACTIVATED:
                return String.format("L'employé %s a été réactivé par %s.", employeeName, triggeredBy);
            case DOCUMENT_UPLOADED:
                return String.format("Un document a été téléchargé pour %s par %s.", employeeName, triggeredBy);
            case PAYSLIP_UPLOADED:
                return String.format("Un bulletin de paie de %s a été téléchargé par %s.", employeeName, triggeredBy);
            default: return "Notification système";
        }
    }

    private String getEmployeeName(Map<String, Object> data) {
        String name = (String) data.get("employeeName");
        return (name != null && !name.isEmpty()) ? name : "un employé";
    }

    private String getContractType(Map<String, Object> metadata) {
        if (metadata != null && metadata.containsKey("typeContrat")) return (String) metadata.get("typeContrat");
        return "contrat";
    }

    private String getContractStart(Map<String, Object> metadata) {
        if (metadata != null && metadata.containsKey("dateDebut")) return (String) metadata.get("dateDebut");
        return "date non spécifiée";
    }

    private String getContractEnd(Map<String, Object> metadata, Map<String, Object> data) {
        if (metadata != null && metadata.containsKey("dateFin")) return (String) metadata.get("dateFin");
        String end = (String) data.get("endDate");
        return end != null ? end : "date non spécifiée";
    }

    private Long getDaysUntilExpiration(Map<String, Object> metadata) {
        if (metadata != null && metadata.containsKey("daysUntilExpiration")) {
            Object daysObj = metadata.get("daysUntilExpiration");
            if (daysObj instanceof Number) return ((Number) daysObj).longValue();
        }
        return null;
    }

    private NotificationType determineType(NotificationEvent event) {
        switch (event) {
            case CONTRACT_CREATED: case CONTRACT_RENEWED: case CONTRACT_EXTENDED:
            case EMPLOYEE_CREATED: case EMPLOYEE_REACTIVATED:
                return NotificationType.SUCCESS;
            case CONTRACT_EXPIRING_TWO_WEEKS: case CONTRACT_EXPIRING_DAILY:
            case CONTRACT_EXPIRED_TODAY: case CONTRACT_EXPIRED: case CONTRACT_RESILIATED:
            case EMPLOYEE_SUSPENDED: case CONTRACT_EXPIRING_SOON:
                return NotificationType.WARNING;
            default: return NotificationType.INFO;
        }
    }

    private NotificationPriority determinePriority(NotificationEvent event) {
        switch (event) {
            case CONTRACT_EXPIRED_TODAY: case EMPLOYEE_SUSPENDED:
                return NotificationPriority.HIGH;
            case CONTRACT_EXPIRING_TWO_WEEKS: case CONTRACT_EXPIRING_DAILY:
            case CONTRACT_EXPIRING_SOON: case CONTRACT_CREATED: case CONTRACT_RENEWED:
                return NotificationPriority.NORMAL;
            default: return NotificationPriority.LOW;
        }
    }

    private String resolveUserDisplayName(String userId) {
        if (userId == null || "SYSTEM".equals(userId)) return "SYSTEM";
        if (userId.contains("@")) return userId;
        if (userId.matches("^[a-fA-F0-9]{24}$")) {
            return userRepository.findById(userId)
                    .map(this::getUserFullName)
                    .orElse(userId);
        }
        return userId;
    }

    private String getUserFullName(User user) {
        if (user == null) return "Utilisateur inconnu";
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? user.getEmail() : fullName;
    }

    private void cleanExpiredEntries() {
        long now = System.currentTimeMillis();
        recentEvents.entrySet().removeIf(entry -> (now - entry.getValue()) > EXPIRE_DELAY_MS);
    }
}