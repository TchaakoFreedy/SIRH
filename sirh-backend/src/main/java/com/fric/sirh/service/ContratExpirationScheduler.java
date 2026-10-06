// src/main/java/com/fric/sirh/service/ContratExpirationScheduler.java
package com.fric.sirh.service;

import com.fric.sirh.model.Contrat;
import com.fric.sirh.model.Employee;
import com.fric.sirh.model.ContractAlertConfig;
import com.fric.sirh.notification.enums.NotificationEvent;
import com.fric.sirh.notification.publisher.NotificationPublisher;
import com.fric.sirh.repository.ContratRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContratExpirationScheduler {

    private final ContratRepository contratRepository;
    private final NotificationPublisher notificationPublisher;
    private final ContractAlertConfigService configService;
    private final EmailService emailService;

    private final Map<String, Long> notificationCache = new ConcurrentHashMap<>();
    private static LocalDate lastRunDate = null;

    /**
     * Exécute la vérification au démarrage, après que l'application soit prête.
     * Utilise ApplicationReadyEvent pour garantir que tous les beans sont initialisés.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void runAtStartup() {
        LocalDate today = LocalDate.now();
        if (lastRunDate == null || !lastRunDate.equals(today)) {
            log.info("Exécution de la vérification des contrats au démarrage (dernière exécution : {})", lastRunDate);
            checkExpiringContracts();
        } else {
            log.debug("Vérification déjà effectuée aujourd'hui, aucune exécution au démarrage.");
        }
    }

    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void checkExpiringContracts() {
        lastRunDate = LocalDate.now();
        log.info("Vérification quotidienne des contrats proches de l'expiration...");

        ContractAlertConfig config = configService.getConfig();
        if (!config.isEnabled()) {
            log.info("Alertes désactivées.");
            return;
        }

        int daysThreshold = config.getDaysBefore();
        LocalDate today = LocalDate.now();
        List<Contrat> activeContracts = contratRepository.findByStatut("ACTIF");

        int urgentCount = 0, dailyCount = 0, thresholdCount = 0, expiredCount = 0;

        for (Contrat contrat : activeContracts) {
            if (contrat.getDateFin() == null) continue;
            Employee employee = contrat.getEmployee();
            if (employee == null) continue;

            long daysUntilExpiration = ChronoUnit.DAYS.between(today, contrat.getDateFin());

            if (daysUntilExpiration < 0) {
                contrat.setStatut("EXPIRE");
                contratRepository.save(contrat);
                expiredCount++;
                log.warn("Contrat expiré - Employé: {}", employee.getMatriculeInterne());
                continue;
            }

            if (daysUntilExpiration == 0) {
                sendExpirationAlert(contrat, "TODAY", daysUntilExpiration);
                urgentCount++;
                continue;
            }

            if (daysUntilExpiration <= daysThreshold && daysUntilExpiration > 0) {
                String cacheKey = contrat.getId() + "_" + today.toString();
                if (!notificationCache.containsKey(cacheKey)) {
                    sendExpirationAlert(contrat, "DAILY", daysUntilExpiration);
                    notificationCache.put(cacheKey, System.currentTimeMillis());
                    dailyCount++;
                }
                continue;
            }

            if (daysUntilExpiration == daysThreshold) {
                sendExpirationAlert(contrat, "THRESHOLD", daysUntilExpiration);
                thresholdCount++;
            }
        }

        cleanCache();
        log.info("Vérification terminée: {} expirés, {} urgents, {} quotidiens, {} seuil {} jours",
                expiredCount, urgentCount, dailyCount, thresholdCount, daysThreshold);
    }

    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void checkExpiredContracts() {
        log.debug("Vérification des contrats expirés...");
        List<Contrat> activeContracts = contratRepository.findByStatut("ACTIF");
        LocalDate today = LocalDate.now();
        int expiredCount = 0;
        for (Contrat contrat : activeContracts) {
            if (contrat.getDateFin() != null && contrat.getDateFin().isBefore(today)) {
                contrat.setStatut("EXPIRE");
                contratRepository.save(contrat);
                expiredCount++;
                log.warn("Contrat marqué expiré - ID: {}", contrat.getId());
            }
        }
        if (expiredCount > 0) log.info("{} contrats marqués expirés", expiredCount);
    }

    private void sendExpirationAlert(Contrat contrat, String alertType, long daysUntilExpiration) {
        Employee employee = contrat.getEmployee();
        String employeeName = employee.getPrenom() + " " + employee.getNom();

        Map<String, Object> data = new HashMap<>();
        data.put("employeeId", employee.getId());
        data.put("companyId", employee.getEntrepriseId());
        data.put("departmentId", employee.getDepartementId());
        data.put("entityId", contrat.getId());
        data.put("entityType", "CONTRAT");
        data.put("actionUrl", "/contrats/" + contrat.getId());
        data.put("employeeName", employeeName);
        data.put("employeeMatricule", employee.getMatriculeInterne());
        data.put("contractType", contrat.getTypeContrat());
        data.put("startDate", contrat.getDateDebut().toString());
        data.put("endDate", contrat.getDateFin().toString());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("daysUntilExpiration", daysUntilExpiration);
        metadata.put("alertType", alertType);
        metadata.put("employeeName", employeeName);
        metadata.put("employeeMatricule", employee.getMatriculeInterne());
        metadata.put("contractType", contrat.getTypeContrat());
        metadata.put("dateFin", contrat.getDateFin().toString());
        data.put("metadata", metadata);

        NotificationEvent event;
        switch (alertType) {
            case "TODAY": event = NotificationEvent.CONTRACT_EXPIRED_TODAY; break;
            case "DAILY": event = NotificationEvent.CONTRACT_EXPIRING_DAILY; break;
            default: event = NotificationEvent.CONTRACT_EXPIRING_SOON; break;
        }

        notificationPublisher.publish(event, data, "SYSTEM");
        log.info("Notification publiée pour événement {} (SYSTEM)", event);

        ContractAlertConfig config = configService.getConfig();
        if (config.isEnabled() && config.getEmailRecipients() != null && !config.getEmailRecipients().isEmpty()) {
            try {
                emailService.sendContractExpirationAlert(
                        contrat,
                        config.getEmailRecipients(),
                        config.getEmailCc(),
                        config.getEmailSubject(),
                        config.getEmailBodyTemplate()
                );
                log.info("Email envoyé pour contrat {}", contrat.getId());
            } catch (Exception e) {
                log.error("Erreur email contrat {}: {}", contrat.getId(), e.getMessage());
            }
        }

        if (employee.getUserId() != null && !employee.getUserId().isEmpty()) {
            notificationPublisher.publish(event, data, employee.getUserId());
            log.info("Notification envoyée à l'employé (userId={})", employee.getUserId());
        }
    }

    private void cleanCache() {
        String today = LocalDate.now().toString();
        notificationCache.entrySet().removeIf(e -> !e.getKey().endsWith(today));
        if (notificationCache.size() > 1000) notificationCache.clear();
    }

    @Transactional
    public Map<String, Object> manualCheck() {
        log.info("Déclenchement manuel de la vérification...");
        checkExpiringContracts();
        Map<String, Object> result = new HashMap<>();
        result.put("status", "SUCCESS");
        result.put("message", "Vérification effectuée");
        result.put("timestamp", LocalDate.now().toString());
        result.put("notificationCacheSize", notificationCache.size());
        return result;
    }
}