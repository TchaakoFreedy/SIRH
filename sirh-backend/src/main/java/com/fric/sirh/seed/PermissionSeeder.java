package com.fric.sirh.seed;

import com.fric.sirh.model.Permission;
import com.fric.sirh.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class PermissionSeeder implements CommandLineRunner {

    private final PermissionRepository permissionRepository;

    @Override
    public void run(String... args) {

        if (permissionRepository.count() > 0) {
            log.info("ℹ️ Permissions already exist");
            return;
        }

        LocalDate now = LocalDate.now();

        List<Permission> permissions = List.of(

                // ============================================
                // 1. UTILISATEURS (USER)
                // ============================================
                p("USER_VIEW_ALL", "Voir tous les utilisateurs", "USER", 1),
                p("USER_VIEW", "Voir un utilisateur", "USER", 1),
                p("USER_CREATE", "Créer un utilisateur", "USER", 2),
                p("USER_UPDATE", "Modifier un utilisateur", "USER", 2),
                p("USER_DELETE", "Supprimer un utilisateur", "USER", 3),

                // ============================================
                // 2. EMPLOYÉS (EMPLOYEE)
                // ============================================
                p("EMPLOYEE_VIEW_ALL", "Voir tous les employés", "EMPLOYEE", 1),
                p("EMPLOYEE_VIEW", "Voir un employé", "EMPLOYEE", 1),
                p("EMPLOYEE_CREATE", "Créer un employé", "EMPLOYEE", 2),
                p("EMPLOYEE_UPDATE", "Modifier un employé", "EMPLOYEE", 2),
                p("EMPLOYEE_DELETE", "Supprimer un employé", "EMPLOYEE", 3),
                p("EMPLOYEE_SUSPEND", "Suspendre un employé", "EMPLOYEE", 3),
                p("EMPLOYEE_REACTIVATE", "Réactiver un employé", "EMPLOYEE", 3),

                // ============================================
                // 3. ENTREPRISES (COMPANY)
                // ============================================
                p("COMPANY_VIEW", "Voir une entreprise", "COMPANY", 1),
                p("COMPANY_VIEW_ALL", "Voir toutes les entreprises", "COMPANY", 1),
                p("COMPANY_CREATE", "Créer une entreprise", "COMPANY", 2),
                p("COMPANY_UPDATE", "Modifier une entreprise", "COMPANY", 2),
                p("COMPANY_DELETE", "Supprimer une entreprise", "COMPANY", 3),
                p("COMPANY_SUSPEND", "Suspendre une entreprise", "COMPANY", 3),
                p("COMPANY_REACTIVATE", "Réactiver une entreprise", "COMPANY", 3),

                // ============================================
                // 4. DÉPARTEMENTS (DEPARTMENT)
                // ============================================
                p("DEPARTMENT_VIEW", "Voir un département", "DEPARTMENT", 1),
                p("DEPARTMENT_VIEW_ALL", "Voir tous les départements", "DEPARTMENT", 1),
                p("DEPARTMENT_CREATE", "Créer un département", "DEPARTMENT", 2),
                p("DEPARTMENT_UPDATE", "Modifier un département", "DEPARTMENT", 2),
                p("DEPARTMENT_DELETE", "Supprimer un département", "DEPARTMENT", 3),
                p("DEPARTMENT_SUSPEND", "Suspendre un département", "DEPARTMENT", 3),
                p("DEPARTMENT_REACTIVATE", "Réactiver un département", "DEPARTMENT", 3),

                // ============================================
                // 5. POSTES (POSITION)
                // ============================================
                p("POSITION_VIEW", "Voir un poste", "POSITION", 1),
                p("POSITION_VIEW_ALL", "Voir tous les postes", "POSITION", 1),
                p("POSITION_CREATE", "Créer un poste", "POSITION", 2),
                p("POSITION_UPDATE", "Modifier un poste", "POSITION", 2),
                p("POSITION_DELETE", "Supprimer un poste", "POSITION", 3),
                p("POSITION_TOGGLE", "Activer/Désactiver un poste", "POSITION", 2),

                // ============================================
                // 6. PROFIL (PROFILE)
                // ============================================
                p("PROFILE_VIEW", "Voir son profil", "PROFILE", 1),
                p("PROFILE_UPDATE", "Modifier son profil", "PROFILE", 1),
                p("PROFILE_VIEW_ANY", "Voir le profil de n'importe qui", "PROFILE", 2),
                p("PROFILE_UPDATE_ANY", "Modifier le profil de n'importe qui", "PROFILE", 3),

                // ============================================
                // 7. CONGÉS (LEAVE)
                // ============================================
                p("LEAVE_CREATE", "Créer une demande de congé", "LEAVE", 1),
                p("LEAVE_VIEW_OWN", "Voir ses propres congés", "LEAVE", 1),
                p("LEAVE_VIEW_TEAM", "Voir les congés de son équipe", "LEAVE", 2),
                p("LEAVE_VIEW_ALL", "Voir tous les congés", "LEAVE", 2),
                p("LEAVE_APPROVE", "Approuver un congé", "LEAVE", 2),
                p("LEAVE_REJECT", "Rejeter un congé", "LEAVE", 2),
                p("LEAVE_CANCEL", "Annuler un congé", "LEAVE", 2),
                p("LEAVE_DELETE", "Supprimer un congé", "LEAVE", 3),

                // ============================================
                // 8. FICHES DE PAIE (PAYROLL)
                // ============================================
                p("PAYSLIP_VIEW", "Voir sa fiche de paie", "PAYROLL", 1),
                p("PAYSLIP_VIEW_ALL", "Voir toutes les fiches de paie", "PAYROLL", 2),
                p("PAYSLIP_CREATE", "Créer une fiche de paie", "PAYROLL", 3),
                p("PAYSLIP_UPDATE", "Modifier une fiche de paie", "PAYROLL", 3),
                p("PAYSLIP_DELETE", "Supprimer une fiche de paie", "PAYROLL", 3),
                p("PAYSLIP_DOWNLOAD", "Télécharger une fiche de paie", "PAYROLL", 1),

                // ============================================
                // 9. DOCUMENTS (DOC)
                // ============================================
                p("DOC_VIEW", "Voir ses documents", "DOCUMENT", 1),
                p("DOC_VIEW_ALL", "Voir tous les documents", "DOCUMENT", 2),
                p("DOC_UPLOAD", "Uploader des documents", "DOCUMENT", 1),
                p("DOC_DELETE", "Supprimer des documents", "DOCUMENT", 2),
                p("DOC_DOWNLOAD", "Télécharger des documents", "DOCUMENT", 1),
                p("DOC_SHARE", "Partager des documents", "DOCUMENT", 2),

                // ============================================
                // 10. CONTRATS (CONTRACT)
                // ============================================
                p("CONTRACT_VIEW", "Voir un contrat", "CONTRACT", 1),
                p("CONTRACT_VIEW_ALL", "Voir tous les contrats", "CONTRACT", 2),
                p("CONTRACT_CREATE", "Créer un contrat", "CONTRACT", 2),
                p("CONTRACT_UPDATE", "Modifier un contrat", "CONTRACT", 2),
                p("CONTRACT_DELETE", "Supprimer un contrat", "CONTRACT", 3),
                p("CONTRACT_SIGN", "Signer un contrat", "CONTRACT", 2),
                p("CONTRACT_TERMINATE", "Résilier un contrat", "CONTRACT", 3),

                // ============================================
                // 11. SANCTIONS (SANCTION)
                // ============================================
                p("SANCTION_VIEW_OWN", "Voir ses propres sanctions", "SANCTION", 1),
                p("SANCTION_VIEW", "Voir une sanction", "SANCTION", 1),
                p("SANCTION_VIEW_ALL", "Voir toutes les sanctions", "SANCTION", 2),
                p("SANCTION_CREATE", "Créer une sanction", "SANCTION", 2),
                p("SANCTION_UPDATE", "Modifier une sanction", "SANCTION", 2),
                p("SANCTION_DELETE", "Supprimer une sanction", "SANCTION", 3),

                // ============================================
                // 12. DEMANDES RH (HR_REQUEST)
                // ============================================
                p("HR_REQUEST_CREATE", "Créer une demande RH", "HR_REQUEST", 1),
                p("HR_REQUEST_VIEW_OWN", "Voir ses demandes RH", "HR_REQUEST", 1),
                p("HR_REQUEST_VIEW_ALL", "Voir toutes les demandes RH", "HR_REQUEST", 2),
                p("HR_REQUEST_APPROVE", "Approuver une demande RH", "HR_REQUEST", 2),
                p("HR_REQUEST_REJECT", "Rejeter une demande RH", "HR_REQUEST", 2),

                // ============================================
                // 13. DEMANDES D'EXPLICATION (EXPLANATION)
                // ============================================
                p("EXPLANATION_REQUEST_VIEW", "Voir les demandes d'explication", "EXPLANATION", 1),
                p("EXPLANATION_REQUEST_VIEW_OWN", "Voir ses propres demandes d'explication", "EXPLANATION", 1),
                p("EXPLANATION_REQUEST_RESPOND", "Répondre à une demande d'explication", "EXPLANATION", 1),
                p("EXPLANATION_REQUEST_CREATE", "Créer une demande d'explication", "EXPLANATION", 2),
                p("EXPLANATION_REQUEST_UPDATE", "Modifier une demande d'explication", "EXPLANATION", 2),
                p("EXPLANATION_REQUEST_DELETE", "Supprimer une demande d'explication", "EXPLANATION", 3),
                p("DISCIPLINE_CREATE", "Créer une mesure disciplinaire", "DISCIPLINE", 2),
                p("DISCIPLINE_UPDATE", "Modifier une mesure disciplinaire", "DISCIPLINE", 2),
                p("DISCIPLINE_DELETE", "Supprimer une mesure disciplinaire", "DISCIPLINE", 3),
                p("DISCIPLINE_VIEW", "Voir les mesures disciplinaires", "DISCIPLINE", 1),
                p("DISCIPLINE_RESPOND", "Répondre à une mesure disciplinaire", "DISCIPLINE", 1),
                p("DISCIPLINE_VALIDATE", "Valider une mesure disciplinaire", "DISCIPLINE", 2),

                // ============================================
                // 14. ÉQUIPES (TEAM)
                // ============================================
                p("TEAM_VIEW", "Voir son équipe", "TEAM", 1),
                p("TEAM_VIEW_ALL", "Voir toutes les équipes", "TEAM", 2),
                p("TEAM_CREATE", "Créer une équipe", "TEAM", 2),
                p("TEAM_UPDATE", "Modifier une équipe", "TEAM", 2),
                p("TEAM_DELETE", "Supprimer une équipe", "TEAM", 3),

                // ============================================
                // 15. RAPPORTS (REPORT)
                // ============================================
                p("REPORT_VIEW", "Voir les rapports", "REPORT", 2),
                p("REPORT_EXPORT", "Exporter les rapports", "REPORT", 2),
                p("REPORT_CREATE", "Créer des rapports", "REPORT", 3),

                // ============================================
                // 16. ADMINISTRATION (ADMIN)
                // ============================================
                p("ROLE_VIEW", "Voir les rôles", "ADMIN", 2),
                p("ROLE_VIEW_ALL", "Voir tous les rôles", "ADMIN", 2),
                p("ROLE_CREATE", "Créer un rôle", "ADMIN", 3),
                p("ROLE_UPDATE", "Modifier un rôle", "ADMIN", 3),
                p("ROLE_DELETE", "Supprimer un rôle", "ADMIN", 3),

                p("PERMISSION_VIEW", "Voir les permissions", "ADMIN", 2),
                p("PERMISSION_UPDATE", "Modifier les permissions", "ADMIN", 3),

                p("USER_PERMISSION_VIEW", "Voir les permissions des utilisateurs", "ADMIN", 2),
                p("USER_PERMISSION_EDIT", "Modifier les permissions des utilisateurs", "ADMIN", 3),

                // ============================================
                // 17. SYSTÈME (SYSTEM)
                // ============================================
                p("SYSTEM_ADMIN", "Accès système total", "SYSTEM", 5),
                p("SYSTEM_CONFIG", "Configurer le système", "SYSTEM", 4),
                p("SYSTEM_LOGS", "Voir les logs système", "SYSTEM", 4),
                p("SYSTEM_BACKUP", "Gérer les sauvegardes", "SYSTEM", 4),

                // ============================================
                // 18. NOTIFICATIONS
                // ============================================
                p("NOTIFICATION_VIEW", "Voir ses notifications", "NOTIFICATION", 1),
                p("NOTIFICATION_SEND", "Envoyer des notifications", "NOTIFICATION", 2),
                p("NOTIFICATION_VIEW_ALL", "Voir toutes les notifications", "NOTIFICATION", 2),

                // ============================================
                // 19. CALENDRIER
                // ============================================
                p("CALENDAR_VIEW", "Voir le calendrier", "CALENDAR", 1),
                p("CALENDAR_EDIT", "Modifier le calendrier", "CALENDAR", 2),
                p("CALENDAR_VIEW_ALL", "Voir tous les calendriers", "CALENDAR", 2),

                // ============================================
                // 20. PERFORMANCE (PERFORMANCE) ✅ AJOUTÉ
                // ============================================
                p("PERFORMANCE_VIEW", "Voir les performances", "PERFORMANCE", 1),
                p("PERFORMANCE_CREATE", "Créer une évaluation de performance", "PERFORMANCE", 2),
                p("PERFORMANCE_UPDATE", "Modifier une évaluation de performance", "PERFORMANCE", 2),
                p("PERFORMANCE_DELETE", "Supprimer une évaluation de performance", "PERFORMANCE", 3),
                p("PERFORMANCE_EXPORT", "Exporter les données de performance", "PERFORMANCE", 2),
                p("PERFORMANCE_VIEW_ALL", "Voir toutes les performances", "PERFORMANCE", 2),

                // ============================================
                // 21. CRITÈRES DE PERFORMANCE (PERFORMANCE_CRITERIA) ✅ AJOUTÉ
                // ============================================
                p("PERFORMANCE_CRITERIA_VIEW", "Voir les critères de performance", "PERFORMANCE_CRITERIA", 1),
                p("PERFORMANCE_CRITERIA_CREATE", "Créer un critère de performance", "PERFORMANCE_CRITERIA", 2),
                p("PERFORMANCE_CRITERIA_UPDATE", "Modifier un critère de performance", "PERFORMANCE_CRITERIA", 2),
                p("PERFORMANCE_CRITERIA_DELETE", "Supprimer un critère de performance", "PERFORMANCE_CRITERIA", 3),

                // ============================================
                // 22. CLASSEMENT (RANKING) ✅ AJOUTÉ
                // ============================================
                p("RANKING_VIEW", "Voir le classement des employés", "RANKING", 1),
                p("RANKING_VIEW_ALL", "Voir tous les classements", "RANKING", 2),
                p("RANKING_EXPORT", "Exporter le classement", "RANKING", 2)
        );

        permissionRepository.saveAll(permissions);
        log.info("✅ Permissions seeded: {}", permissions.size());
    }

    private Permission p(String name, String description, String category, int requiredLevel) {
        return Permission.builder()
                .name(name)
                .description(description)
                .category(category)
                .requiredLevel(requiredLevel)
                .active(true)
                .createdAt(LocalDate.now())
                .createdBy("SYSTEM")
                .build();
    }
}