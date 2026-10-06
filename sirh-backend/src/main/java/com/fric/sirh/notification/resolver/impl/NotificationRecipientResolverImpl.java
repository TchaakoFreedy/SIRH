package com.fric.sirh.notification.resolver.impl;

import com.fric.sirh.model.Employee;
import com.fric.sirh.model.Role;
import com.fric.sirh.model.User;
import com.fric.sirh.notification.enums.NotificationEvent;
import com.fric.sirh.notification.resolver.NotificationRecipientResolver;
import com.fric.sirh.repository.EmployeeRepository;
import com.fric.sirh.repository.RoleRepository;
import com.fric.sirh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRecipientResolverImpl implements NotificationRecipientResolver {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;

    private final Map<NotificationEvent, Function<Map<String, Object>, List<String>>> resolvers = new HashMap<>();

    @PostConstruct
    public void init() {
        // Événements existants
        resolvers.put(NotificationEvent.EMPLOYEE_CREATED, this::resolveForEmployeeCreated);
        resolvers.put(NotificationEvent.EMPLOYEE_UPDATED, this::resolveForEmployeeUpdated);
        resolvers.put(NotificationEvent.EMPLOYEE_SUSPENDED, this::resolveForEmployeeSuspended);
        resolvers.put(NotificationEvent.DOCUMENT_UPLOADED, this::resolveForDocumentUploaded);
        resolvers.put(NotificationEvent.COMPANY_CREATED, this::resolveForCompany);
        resolvers.put(NotificationEvent.COMPANY_UPDATED, this::resolveForCompany);
        resolvers.put(NotificationEvent.COMPANY_DELETED, this::resolveForCompany);
        resolvers.put(NotificationEvent.DEPARTMENT_CREATED, this::resolveForDepartment);
        resolvers.put(NotificationEvent.DEPARTMENT_UPDATED, this::resolveForDepartment);
        resolvers.put(NotificationEvent.DEPARTMENT_DELETED, this::resolveForDepartment);
        resolvers.put(NotificationEvent.POSITION_CREATED, this::resolveForPosition);
        resolvers.put(NotificationEvent.POSITION_UPDATED, this::resolveForPosition);
        resolvers.put(NotificationEvent.POSITION_DELETED, this::resolveForPosition);
        resolvers.put(NotificationEvent.LEAVE_REQUESTED, this::resolveForLeaveRequested);
        resolvers.put(NotificationEvent.LEAVE_APPROVED, this::resolveForLeaveApproved);
        resolvers.put(NotificationEvent.LEAVE_REJECTED, this::resolveForLeaveRejected);
        resolvers.put(NotificationEvent.LEAVE_CANCELLED, this::resolveForLeaveCancelled);
        resolvers.put(NotificationEvent.LEAVE_BALANCE_GLOBAL_UPDATED, this::resolveForLeaveBalanceGlobal);
        resolvers.put(NotificationEvent.LEAVE_BALANCE_INDIVIDUAL_UPDATED, this::resolveForLeaveBalanceIndividual);
        resolvers.put(NotificationEvent.ROLE_CREATED, this::resolveForRole);
        resolvers.put(NotificationEvent.ROLE_UPDATED, this::resolveForRole);
        resolvers.put(NotificationEvent.ROLE_DELETED, this::resolveForRole);
        resolvers.put(NotificationEvent.PERMISSION_UPDATED, this::resolveForPermission);
        resolvers.put(NotificationEvent.SYSTEM, this::resolveForSystem);

        // Nouveaux événements liés aux contrats
        resolvers.put(NotificationEvent.CONTRACT_EXPIRING_TWO_WEEKS, this::resolveForContractExpiration);
        resolvers.put(NotificationEvent.CONTRACT_EXPIRING_DAILY, this::resolveForContractExpiration);
        resolvers.put(NotificationEvent.CONTRACT_EXPIRED_TODAY, this::resolveForContractExpiration);

        // Ajout des événements de gestion des contrats
        resolvers.put(NotificationEvent.CONTRACT_CREATED, this::resolveForContractManagement);
        resolvers.put(NotificationEvent.CONTRACT_UPDATED, this::resolveForContractManagement);
        resolvers.put(NotificationEvent.CONTRACT_RENEWED, this::resolveForContractManagement);
        resolvers.put(NotificationEvent.CONTRACT_RESILIATED, this::resolveForContractManagement);
        resolvers.put(NotificationEvent.CONTRACT_ARCHIVED, this::resolveForContractManagement);
        resolvers.put(NotificationEvent.CONTRACT_EXPIRED, this::resolveForContractManagement);

        // Ajout de l'événement PAYSLIP_UPLOADED
        resolvers.put(NotificationEvent.PAYSLIP_UPLOADED, this::resolveForPaySlipUploaded);
    }

    @Override
    public List<String> resolve(NotificationEvent event, Map<String, Object> data) {
        Function<Map<String, Object>, List<String>> resolver = resolvers.get(event);
        if (resolver == null) {
            log.warn("No resolver defined for event: {}", event);
            return Collections.emptyList();
        }
        return resolver.apply(data);
    }

    // ========== MÉTHODES DE RÉSOLUTION EXISTANTES ==========

    private List<String> resolveForEmployeeCreated(Map<String, Object> data) {
        String companyId = (String) data.get("companyId");
        String departmentId = (String) data.get("departmentId");
        Set<String> recipients = new HashSet<>();

        recipients.addAll(getUserIdsByRoleName("TOP_MANAGER"));
        recipients.addAll(getUserIdsByRoleName("RH"));
        recipients.addAll(getUserIdsByCompanyAndRole(companyId, "DIRECTION"));
        recipients.addAll(getUserIdsByDepartmentAndRole(departmentId, "MANAGER"));

        return new ArrayList<>(recipients);
    }

    private List<String> resolveForEmployeeUpdated(Map<String, Object> data) {
        List<String> recipients = new ArrayList<>(resolveForEmployeeCreated(data));
        String employeeId = (String) data.get("employeeId");
        addEmployeeUser(employeeId, recipients);
        return recipients;
    }

    private List<String> resolveForEmployeeSuspended(Map<String, Object> data) {
        return resolveForEmployeeUpdated(data);
    }

    private List<String> resolveForDocumentUploaded(Map<String, Object> data) {
        List<String> recipients = new ArrayList<>(resolveForEmployeeCreated(data));
        String employeeId = (String) data.get("employeeId");
        addEmployeeUser(employeeId, recipients);
        String triggeredBy = (String) data.get("triggeredBy");
        if (triggeredBy != null) recipients.add(triggeredBy);
        return recipients;
    }

    private List<String> resolveForCompany(Map<String, Object> data) {
        Set<String> recipients = new HashSet<>();
        recipients.addAll(getUserIdsByRoleName("TOP_MANAGER"));
        recipients.addAll(getUserIdsByRoleName("RH"));
        String companyId = (String) data.get("companyId");
        if (companyId != null) {
            recipients.addAll(getUserIdsByCompanyAndRole(companyId, "DIRECTION"));
        }
        return new ArrayList<>(recipients);
    }

    private List<String> resolveForDepartment(Map<String, Object> data) {
        Set<String> recipients = new HashSet<>();
        recipients.addAll(getUserIdsByRoleName("TOP_MANAGER"));
        recipients.addAll(getUserIdsByRoleName("RH"));
        String companyId = (String) data.get("companyId");
        if (companyId != null) {
            recipients.addAll(getUserIdsByCompanyAndRole(companyId, "DIRECTION"));
        }
        String departmentId = (String) data.get("departmentId");
        if (departmentId != null) {
            recipients.addAll(getUserIdsByDepartmentAndRole(departmentId, "MANAGER"));
        }
        return new ArrayList<>(recipients);
    }

    private List<String> resolveForPosition(Map<String, Object> data) {
        return resolveForDepartment(data);
    }

    private List<String> resolveForLeaveRequested(Map<String, Object> data) {
        Set<String> recipients = new HashSet<>();
        recipients.addAll(getUserIdsByRoleName("TOP_MANAGER"));
        recipients.addAll(getUserIdsByRoleName("RH"));
        String companyId = (String) data.get("companyId");
        if (companyId != null) {
            recipients.addAll(getUserIdsByCompanyAndRole(companyId, "DIRECTION"));
        }
        String departmentId = (String) data.get("departmentId");
        if (departmentId != null) {
            recipients.addAll(getUserIdsByDepartmentAndRole(departmentId, "MANAGER"));
        }
        return new ArrayList<>(recipients);
    }

    private List<String> resolveForLeaveApproved(Map<String, Object> data) {
        List<String> recipients = new ArrayList<>(resolveForLeaveRequested(data));
        String employeeId = (String) data.get("employeeId");
        addEmployeeUser(employeeId, recipients);
        return recipients;
    }

    private List<String> resolveForLeaveRejected(Map<String, Object> data) {
        return resolveForLeaveApproved(data);
    }

    private List<String> resolveForLeaveCancelled(Map<String, Object> data) {
        return resolveForLeaveApproved(data);
    }

    private List<String> resolveForLeaveBalanceGlobal(Map<String, Object> data) {
        Set<String> recipients = new HashSet<>();
        recipients.addAll(getUserIdsByRoleName("TOP_MANAGER"));
        recipients.addAll(getUserIdsByRoleName("RH"));
        recipients.addAll(getUserIdsByRoleName("DIRECTION"));
        recipients.addAll(getUserIdsByRoleName("MANAGER"));
        recipients.addAll(getUserIdsByRoleName("EMPLOYEE"));
        return new ArrayList<>(recipients);
    }

    private List<String> resolveForLeaveBalanceIndividual(Map<String, Object> data) {
        Set<String> recipients = new HashSet<>();
        recipients.addAll(getUserIdsByRoleName("TOP_MANAGER"));
        recipients.addAll(getUserIdsByRoleName("RH"));
        String companyId = (String) data.get("companyId");
        if (companyId != null) {
            recipients.addAll(getUserIdsByCompanyAndRole(companyId, "DIRECTION"));
        }
        String departmentId = (String) data.get("departmentId");
        if (departmentId != null) {
            recipients.addAll(getUserIdsByDepartmentAndRole(departmentId, "MANAGER"));
        }
        String employeeId = (String) data.get("employeeId");
        addEmployeeUser(employeeId, new ArrayList<>(recipients));
        return new ArrayList<>(recipients);
    }

    private List<String> resolveForRole(Map<String, Object> data) {
        Set<String> recipients = new HashSet<>();
        recipients.addAll(getUserIdsByRoleName("TOP_MANAGER"));
        recipients.addAll(getUserIdsByRoleName("RH"));
        return new ArrayList<>(recipients);
    }

    private List<String> resolveForPermission(Map<String, Object> data) {
        return resolveForRole(data);
    }

    private List<String> resolveForSystem(Map<String, Object> data) {
        return userRepository.findAll().stream()
                .filter(User::getActive)
                .map(User::getId)
                .collect(Collectors.toList());
    }

    // ===== MÉTHODE DE RÉSOLUTION POUR LES ÉVÉNEMENTS D'EXPIRATION DES CONTRATS =====
    /**
     * Résout les destinataires pour les événements d'expiration de contrat.
     * Seuls les utilisateurs ayant le rôle RH (global et par entreprise) sont notifiés.
     */
    private List<String> resolveForContractExpiration(Map<String, Object> data) {
        Set<String> recipients = new HashSet<>();

        // 1. Récupérer tous les utilisateurs avec le rôle "RH" global
        recipients.addAll(getUserIdsByRoleName("RH"));

        // 2. Récupérer les RH spécifiques à l'entreprise du contrat (si companyId est présente)
        String companyId = (String) data.get("companyId");
        if (companyId != null) {
            recipients.addAll(getUserIdsByCompanyAndRole(companyId, "RH"));
        }

        // Ne pas ajouter l'employé ni les autres rôles, conformément au besoin.
        return new ArrayList<>(recipients);
    }

    // ===== NOUVELLE MÉTHODE : Résolveur générique pour les événements de gestion des contrats =====
    private List<String> resolveForContractManagement(Map<String, Object> data) {
        Set<String> recipients = new HashSet<>();

        // 1. Ajouter l'employé concerné (s'il existe)
        String employeeId = (String) data.get("employeeId");
        if (employeeId != null) {
            employeeRepository.findById(employeeId)
                    .map(Employee::getUser)
                    .map(User::getId)
                    .ifPresent(recipients::add);
        }

        // 2. Ajouter le déclencheur (l'utilisateur qui a fait l'action)
        String triggeredBy = (String) data.get("triggeredBy");
        if (triggeredBy != null) {
            recipients.add(triggeredBy);
        }

        // 3. Ajouter les RH de l'entreprise (pour information/suivi)
        String companyId = (String) data.get("companyId");
        if (companyId != null) {
            recipients.addAll(getUserIdsByCompanyAndRole(companyId, "RH"));
        }

        // 4. Ajouter les TOP_MANAGER
        recipients.addAll(getUserIdsByRoleName("TOP_MANAGER"));

        // 5. (Optionnel) Ajouter le manager du département pour information
        String departmentId = (String) data.get("departmentId");
        if (departmentId != null) {
            recipients.addAll(getUserIdsByDepartmentAndRole(departmentId, "MANAGER"));
        }

        return new ArrayList<>(recipients);
    }

    // ===== MÉTHODE POUR PAYSLIP =====
    private List<String> resolveForPaySlipUploaded(Map<String, Object> data) {
        Set<String> recipients = new HashSet<>();

        String employeeId = (String) data.get("employeeId");
        if (employeeId != null) {
            employeeRepository.findById(employeeId)
                    .map(Employee::getUser)
                    .map(User::getId)
                    .ifPresent(recipients::add);
        }

        String triggeredBy = (String) data.get("triggeredBy");
        if (triggeredBy != null) {
            recipients.add(triggeredBy);
        }

        String companyId = (String) data.get("companyId");
        if (companyId != null) {
            recipients.addAll(getUserIdsByCompanyAndRole(companyId, "RH"));
        }

        return new ArrayList<>(recipients);
    }

    // ========== MÉTHODES UTILITAIRES ==========

    private List<String> getUserIdsByRoleName(String roleName) {
        Optional<Role> roleOpt = roleRepository.findByName(roleName);
        if (roleOpt.isEmpty()) return Collections.emptyList();
        String roleId = roleOpt.get().getId();
        return userRepository.findByRoleId(roleId).stream()
                .map(User::getId)
                .collect(Collectors.toList());
    }

    private List<String> getUserIdsByCompanyAndRole(String companyId, String roleName) {
        if (companyId == null) return Collections.emptyList();
        Optional<Role> roleOpt = roleRepository.findByName(roleName);
        if (roleOpt.isEmpty()) return Collections.emptyList();
        String roleId = roleOpt.get().getId();

        List<Employee> employees = employeeRepository.findByEntrepriseId(companyId);
        List<String> userIds = employees.stream()
                .map(Employee::getUser)
                .filter(Objects::nonNull)
                .map(User::getId)
                .collect(Collectors.toList());
        if (userIds.isEmpty()) return Collections.emptyList();
        return userRepository.findAllById(userIds).stream()
                .filter(u -> roleId.equals(u.getRoleId()))
                .map(User::getId)
                .collect(Collectors.toList());
    }

    private List<String> getUserIdsByDepartmentAndRole(String departmentId, String roleName) {
        if (departmentId == null) return Collections.emptyList();
        Optional<Role> roleOpt = roleRepository.findByName(roleName);
        if (roleOpt.isEmpty()) return Collections.emptyList();
        String roleId = roleOpt.get().getId();

        List<Employee> employees = employeeRepository.findByDepartementId(departmentId);
        List<String> userIds = employees.stream()
                .map(Employee::getUser)
                .filter(Objects::nonNull)
                .map(User::getId)
                .collect(Collectors.toList());
        if (userIds.isEmpty()) return Collections.emptyList();
        return userRepository.findAllById(userIds).stream()
                .filter(u -> roleId.equals(u.getRoleId()))
                .map(User::getId)
                .collect(Collectors.toList());
    }

    private void addEmployeeUser(String employeeId, List<String> recipients) {
        if (employeeId == null) return;
        employeeRepository.findById(employeeId)
                .map(Employee::getUser)
                .map(User::getId)
                .ifPresent(recipients::add);
    }
}