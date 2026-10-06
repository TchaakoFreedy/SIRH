package com.fric.sirh.service;

import com.fric.sirh.model.Contrat;
import com.fric.sirh.model.Employee;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    // EXISTANT
    public void sendDemandeExplication(String to, String motif, String description) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Demande d'explication - SIRH");
        message.setText(
                "Bonjour,\n\n" +
                        "Motif : " + motif + "\n" +
                        "Description : " + description
        );
        mailSender.send(message);
    }

    // EXISTANT : RESET PASSWORD CODE
    public void sendResetPasswordCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Code de réinitialisation - SIRH");
        message.setText(
                "Bonjour,\n\n" +
                        "Votre code de réinitialisation est : " + code + "\n\n" +
                        "Ce code expire dans 10 minutes.\n\n" +
                        "Si ce n'était pas vous, ignorez ce message."
        );
        mailSender.send(message);
    }

    // NOUVEAU : Envoi d'email d'alerte d'expiration de contrat avec template personnalisable
    public void sendContractExpirationAlert(Contrat contrat,
                                            List<String> recipients,
                                            List<String> cc,
                                            String subjectTemplate,
                                            String bodyTemplate) {

        if (recipients == null || recipients.isEmpty()) {
            log.warn("Aucun destinataire configuré pour l'alerte du contrat {}", contrat.getId());
            return;
        }

        Employee employee = contrat.getEmployee();
        if (employee == null) {
            log.warn("L'employé est null pour le contrat {}", contrat.getId());
            return;
        }

        String employeeName = (employee.getPrenom() != null ? employee.getPrenom() : "")
                + " " + (employee.getNom() != null ? employee.getNom() : "");
        String endDate = contrat.getDateFin() != null
                ? contrat.getDateFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "N/A";
        String startDate = contrat.getDateDebut() != null
                ? contrat.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "N/A";
        String typeContrat = contrat.getTypeContrat() != null ? contrat.getTypeContrat() : "N/A";

        // Remplacer les variables du template
        String subject = subjectTemplate != null ? subjectTemplate : "Alerte expiration de contrat";
        String body = bodyTemplate
                .replace("{employeeName}", employeeName)
                .replace("{endDate}", endDate)
                .replace("{startDate}", startDate)
                .replace("{typeContrat}", typeContrat)
                .replace("{employeeId}", employee.getId())
                .replace("{contratId}", contrat.getId());

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(recipients.toArray(new String[0]));
            if (cc != null && !cc.isEmpty()) {
                helper.setCc(cc.toArray(new String[0]));
            }
            helper.setSubject(subject);
            helper.setText(body, false); // false pour texte brut, true pour HTML

            mailSender.send(message);
            log.info("Email d'alerte envoyé pour le contrat {} à {}", contrat.getId(), recipients);
        } catch (MessagingException e) {
            log.error("Échec de l'envoi de l'email pour le contrat {}: {}", contrat.getId(), e.getMessage(), e);
            throw new RuntimeException("Échec de l'envoi de l'email", e);
        }
    }
}