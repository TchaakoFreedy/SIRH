package com.fric.sirh.scheduler;

import com.fric.sirh.model.Contrat;
import com.fric.sirh.model.Employee;
import com.fric.sirh.notification.enums.NotificationEvent;
import com.fric.sirh.notification.publisher.NotificationPublisher;
import com.fric.sirh.repository.ContratRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scheduler chargé d'envoyer quotidiennement des notifications aux RH
 * pour les contrats arrivant à expiration dans les 14 prochains jours.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContractExpirationNotificationScheduler {

    private final ContratRepository contratRepository;
    private final NotificationPublisher notificationPublisher;

    /**
     * Exécution quotidienne à 8h00.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void sendExpirationNotifications() {
        log.info("Début du job de notification d'expiration des contrats");

        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(14);

        // Récupérer les contrats actifs dont la date de fin est comprise entre aujourd'hui et aujourd'hui+14 jours
        List<Contrat> contrats = contratRepository.findByStatutAndDateFinBetween("ACTIF", today, threshold);

        if (contrats.isEmpty()) {
            log.info("Aucun contrat actif arrivant à expiration dans les 14 jours.");
            return;
        }

        log.info("{} contrat(s) trouvé(s) pour notification d'expiration.", contrats.size());

        for (Contrat contrat : contrats) {
            try {
                processContrat(contrat, today);
            } catch (Exception e) {
                log.error("Erreur lors du traitement du contrat ID {} : {}", contrat.getId(), e.getMessage(), e);
            }
        }

        log.info("Job de notification d'expiration terminé.");
    }

    private void processContrat(Contrat contrat, LocalDate today) {
        LocalDate dateFin = contrat.getDateFin();
        if (dateFin == null) {
            log.warn("Contrat ID {} n'a pas de date de fin, ignoré.", contrat.getId());
            return;
        }

        // Ne pas traiter si la date de fin est avant aujourd'hui (déjà expiré)
        if (dateFin.isBefore(today)) {
            log.debug("Contrat ID {} déjà expiré, ignoré.", contrat.getId());
            return;
        }

        long daysUntilExpiration = ChronoUnit.DAYS.between(today, dateFin);
        // daysUntilExpiration est compris entre 0 et 14 (inclus) grâce au filtre de la requête

        boolean needSave = false;

        // Cas J-14 : envoi du rappel initial
        if (daysUntilExpiration == 14) {
            if (contrat.getReminder14Sent() == null || !contrat.getReminder14Sent()) {
                log.info("Contrat ID {} : envoi du rappel J-14.", contrat.getId());
                publishExpirationEvent(contrat, NotificationEvent.CONTRACT_EXPIRING_TWO_WEEKS, daysUntilExpiration);
                contrat.setReminder14Sent(true);
                needSave = true;
            } else {
                log.debug("Contrat ID {} : rappel J-14 déjà envoyé, ignoré.", contrat.getId());
            }
        }
        // Cas J-13 à J-1 : rappels quotidiens
        else if (daysUntilExpiration > 0 && daysUntilExpiration < 14) {
            LocalDate lastDaily = contrat.getLastDailyReminderDate();
            if (lastDaily == null || !lastDaily.isEqual(today)) {
                log.info("Contrat ID {} : envoi du rappel quotidien (J-{}).", contrat.getId(), daysUntilExpiration);
                publishExpirationEvent(contrat, NotificationEvent.CONTRACT_EXPIRING_DAILY, daysUntilExpiration);
                contrat.setLastDailyReminderDate(today);
                needSave = true;
            } else {
                log.debug("Contrat ID {} : rappel quotidien déjà envoyé aujourd'hui, ignoré.", contrat.getId());
            }
        }
        // Cas J-0 : expiration aujourd'hui
        else if (daysUntilExpiration == 0) {
            // On envoie une seule fois le jour J ; on peut utiliser lastDailyReminderDate pour éviter les doublons
            LocalDate lastDaily = contrat.getLastDailyReminderDate();
            if (lastDaily == null || !lastDaily.isEqual(today)) {
                log.info("Contrat ID {} : envoi du rappel d'expiration aujourd'hui.", contrat.getId());
                publishExpirationEvent(contrat, NotificationEvent.CONTRACT_EXPIRED_TODAY, daysUntilExpiration);
                contrat.setLastDailyReminderDate(today);
                needSave = true;
            } else {
                log.debug("Contrat ID {} : rappel d'expiration aujourd'hui déjà envoyé, ignoré.", contrat.getId());
            }
        }

        // Sauvegarder les modifications si nécessaire
        if (needSave) {
            contratRepository.save(contrat);
        }
    }

    private void publishExpirationEvent(Contrat contrat, NotificationEvent event, long daysUntilExpiration) {
        Employee employee = contrat.getEmployee();
        if (employee == null) {
            log.warn("Le contrat ID {} n'a pas d'employé associé, impossible de publier la notification.", contrat.getId());
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("employeeId", employee.getId());
        data.put("companyId", employee.getEntrepriseId());
        data.put("departmentId", employee.getDepartementId());
        data.put("entityId", contrat.getId());
        data.put("entityType", "CONTRAT");
        data.put("actionUrl", "/contrats/" + contrat.getId());
        data.put("endDate", contrat.getDateFin().toString());
        data.put("daysUntilExpiration", daysUntilExpiration);

        // Nom de l'employé pour affichage
        String fullName = (employee.getPrenom() != null ? employee.getPrenom() : "")
                + " " + (employee.getNom() != null ? employee.getNom() : "");
        data.put("employeeName", fullName.trim().isEmpty() ? "Employé" : fullName.trim());

        // Métadonnées
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("typeContrat", contrat.getTypeContrat());
        metadata.put("statut", contrat.getStatut());
        metadata.put("dateDebut", contrat.getDateDebut() != null ? contrat.getDateDebut().toString() : null);
        metadata.put("dateFin", contrat.getDateFin().toString());
        metadata.put("daysUntilExpiration", daysUntilExpiration);
        data.put("metadata", metadata);

        // Déclencheur = "SYSTEM" pour les notifications automatiques
        notificationPublisher.publish(event, data, "SYSTEM");
        log.info("Notification {} publiée pour le contrat ID {} (employé {})", event, contrat.getId(), employee.getId());
    }
}