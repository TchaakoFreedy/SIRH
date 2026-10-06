package com.fric.sirh.controller;

import com.fric.sirh.dto.UserPermissionsResponse;
import com.fric.sirh.dto.UserPermissionsUpdateRequest;
import com.fric.sirh.service.UserPermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor

public class UserPermissionController {

    private final UserPermissionService userPermissionService;

    @GetMapping("/{userId}/permissions")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<UserPermissionsResponse> getUserPermissions(@PathVariable String userId) {
        return ResponseEntity.ok(userPermissionService.getUserPermissions(userId));
    }

    @PutMapping("/{userId}/permissions")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<UserPermissionsResponse> updateUserPermissions(
            @PathVariable String userId,
            @Valid @RequestBody UserPermissionsUpdateRequest request) {
        return ResponseEntity.ok(userPermissionService.updateUserPermissions(userId, request));
    }
}