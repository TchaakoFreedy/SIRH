package com.fric.sirh.controller;

import com.fric.sirh.dto.CreateRoleRequest;
import com.fric.sirh.dto.RoleDto;
import com.fric.sirh.dto.RolePermissionsUpdateRequest;
import com.fric.sirh.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@CrossOrigin("*")
public class RoleController {

    private final RoleService roleService;

    // ✅ Modifier la permission pour permettre aux RH de voir les rôles
    @GetMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('ROLE_VIEW') or hasAuthority('ROLE_VIEW_ALL')")
    public ResponseEntity<List<RoleDto>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('ROLE_VIEW')")
    public ResponseEntity<RoleDto> getRoleById(@PathVariable String id) {
        return ResponseEntity.ok(roleService.getRoleById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('ROLE_CREATE')")
    public ResponseEntity<RoleDto> createRole(@Valid @RequestBody CreateRoleRequest request) {
        return new ResponseEntity<>(roleService.createRole(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<RoleDto> updateRole(
            @PathVariable String id,
            @Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.ok(roleService.updateRole(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('ROLE_DELETE')")
    public ResponseEntity<Void> deleteRole(@PathVariable String id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('PERMISSION_UPDATE')")
    public ResponseEntity<RoleDto> updateRolePermissions(
            @PathVariable String id,
            @Valid @RequestBody RolePermissionsUpdateRequest request) {
        return ResponseEntity.ok(roleService.updateRolePermissions(id, request));
    }
}