package com.fric.sirh.controller;

import com.fric.sirh.dto.EmployeeSelfUpdateRequest;
import com.fric.sirh.model.Employee;
import com.fric.sirh.model.User;
import com.fric.sirh.service.EmployeeService;
import com.fric.sirh.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final EmployeeService employeeService;
    private final UserService userService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Employee> getMyProfile(Authentication authentication) {
        String email = authentication.getName();
        log.info("Recuperation du profil pour email: {}", email);

        User user = userService.findByEmail(email);
        if (user == null) {
            log.warn("Aucun utilisateur trouve avec l'email: {}", email);
            return ResponseEntity.notFound().build();
        }

        String employeeId = user.getEmployeeId();
        if (employeeId == null || employeeId.isEmpty()) {
            log.warn("L'utilisateur {} n'a pas d'employeeId associe", email);
            return ResponseEntity.notFound().build();
        }

        Employee employee = employeeService.getById(employeeId);
        if (employee == null) {
            log.warn("Aucun employe trouve avec l'ID: {}", employeeId);
            return ResponseEntity.notFound().build();
        }

        log.info("Profil recupere pour: {} {}", employee.getPrenom(), employee.getNom());
        return ResponseEntity.ok(employee);
    }

    @PatchMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Employee> updateMyProfile(
            @RequestBody EmployeeSelfUpdateRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        log.info("Mise a jour du profil pour email: {}", email);

        // Log du contenu de la requete
        log.info("Requete recue - Telephone: {}, Adresse: {}",
                request.getTelephone(), request.getAddresse());

        User user = userService.findByEmail(email);
        if (user == null) {
            log.warn("Aucun utilisateur trouve avec l'email: {}", email);
            return ResponseEntity.notFound().build();
        }

        String employeeId = user.getEmployeeId();
        if (employeeId == null || employeeId.isEmpty()) {
            log.warn("L'utilisateur {} n'a pas d'employeeId associe", email);
            return ResponseEntity.notFound().build();
        }

        log.info("EmployeeId trouve: {}", employeeId);

        Employee employee = employeeService.getById(employeeId);
        if (employee == null) {
            log.warn("Aucun employe trouve avec l'ID: {}", employeeId);
            return ResponseEntity.notFound().build();
        }

        log.info("Employe avant mise a jour - Telephone: {}, Adresse: {}",
                employee.getTelephone(), employee.getAddresse());

        Employee updatedEmployee = employeeService.updateSelfProfile(employee.getId(), request);

        log.info("Employe apres mise a jour - Telephone: {}, Adresse: {}",
                updatedEmployee.getTelephone(), updatedEmployee.getAddresse());
        log.info("Profil mis a jour pour: {} {}", updatedEmployee.getPrenom(), updatedEmployee.getNom());

        return ResponseEntity.ok(updatedEmployee);
    }
}