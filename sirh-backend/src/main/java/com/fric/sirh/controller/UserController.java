package com.fric.sirh.controller;

import com.fric.sirh.dto.*;
import com.fric.sirh.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Required for @Slf4j
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j // Add this annotation to enable logging
@RestController
@RequestMapping("/api/users")

@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW_ALL')")
    public List<UserDTO> getAll() {
        return userService.getAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public UserDTO getById(@PathVariable String id) {
        return userService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public UserDTO create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public UserDTO update(@PathVariable String id, @RequestBody CreateUserRequest request) {
        log.info("Request received for update ID: {}", id);
        return userService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public ResponseEntity<String> delete(@PathVariable String id) {
        userService.delete(id);
        return ResponseEntity.ok("Utilisateur supprimé avec succès");
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public UserDTO toggle(@PathVariable String id) {
        return userService.toggleStatus(id);
    }
}