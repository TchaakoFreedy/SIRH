package com.fric.sirh.controller;

import com.fric.sirh.dto.CreatePermissionRequest;
import com.fric.sirh.dto.PermissionDto;
import com.fric.sirh.dto.UpdatePermissionRequest;
import com.fric.sirh.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@CrossOrigin("*")
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<List<PermissionDto>> getAllPermissions() {
        return ResponseEntity.ok(permissionService.getAllPermissions());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<PermissionDto> getPermissionById(@PathVariable String id) {
        return ResponseEntity.ok(permissionService.getPermissionById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<PermissionDto> createPermission(
            @Valid @RequestBody CreatePermissionRequest request) {
        return new ResponseEntity<>(permissionService.createPermission(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<PermissionDto> updatePermission(
            @PathVariable String id,
            @Valid @RequestBody UpdatePermissionRequest request) {
        return ResponseEntity.ok(permissionService.updatePermission(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<Void> deletePermission(@PathVariable String id) {
        permissionService.deletePermission(id);
        return ResponseEntity.noContent().build();
    }
}