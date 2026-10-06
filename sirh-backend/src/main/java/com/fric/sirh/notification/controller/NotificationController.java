package com.fric.sirh.notification.controller;

import com.fric.sirh.model.Employee;
import com.fric.sirh.model.Role;
import com.fric.sirh.model.User;
import com.fric.sirh.notification.dto.NotificationFilter;
import com.fric.sirh.notification.dto.NotificationPageResponse;
import com.fric.sirh.notification.security.SecurityUtils;
import com.fric.sirh.notification.service.NotificationService;
import com.fric.sirh.repository.EmployeeRepository;
import com.fric.sirh.repository.RoleRepository;
import com.fric.sirh.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;

    // ==========================================
    // 1. Récupérer toutes les notifications (filtrées)
    // ==========================================
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationPageResponse> getNotifications(
            @Valid NotificationFilter filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String userId = securityUtils.getCurrentUserId();
        NotificationFilter securedFilter = applySecurityFilters(filter, userId);
        NotificationPageResponse response = notificationService.getNotifications(securedFilter, pageable, userId);
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 2. Compter les notifications non lues
    // ==========================================
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Long> getUnreadCount() {
        String userId = securityUtils.getCurrentUserId();
        long count = notificationService.countUnread(userId);
        return ResponseEntity.ok(count);
    }

    // ==========================================
    // 3. Récupérer les notifications non lues
    // ==========================================
    @GetMapping("/unread")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationPageResponse> getUnread(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String userId = securityUtils.getCurrentUserId();
        NotificationPageResponse response = notificationService.getUnread(userId, pageable);
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 4. Marquer une notification comme lue
    // ==========================================
    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAsRead(@PathVariable String id) {
        String userId = securityUtils.getCurrentUserId();
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok().build();
    }

    // ==========================================
    // 5. Marquer toutes les notifications comme lues
    // ==========================================
    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAllAsRead() {
        String userId = securityUtils.getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    // ==========================================
    // 6. Supprimer une notification
    // ==========================================
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        String userId = securityUtils.getCurrentUserId();
        notificationService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // 7. Supprimer toutes les notifications de l'utilisateur
    // ==========================================
    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteAll() {
        String userId = securityUtils.getCurrentUserId();
        notificationService.deleteAll(userId);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // 8. Recherche avancée (alias de /)
    // ==========================================
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationPageResponse> search(
            @Valid NotificationFilter filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String userId = securityUtils.getCurrentUserId();
        NotificationFilter securedFilter = applySecurityFilters(filter, userId);
        NotificationPageResponse response = notificationService.search(securedFilter, pageable, userId);
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 9. Historique des notifications de l'utilisateur
    // ==========================================
    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationPageResponse> getHistory(
            @Valid NotificationFilter filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String userId = securityUtils.getCurrentUserId();
        NotificationPageResponse response = notificationService.getHistory(userId, filter, pageable);
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // FILTRE : FORCER LE recipientId AVEC L'ID UTILISATEUR
    // ==========================================
    private NotificationFilter applySecurityFilters(NotificationFilter filter, String userId) {
        if (filter == null) {
            filter = new NotificationFilter();
        }
        filter.setRecipientId(userId);
        return filter;
    }
}