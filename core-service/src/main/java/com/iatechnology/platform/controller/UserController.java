package com.iatechnology.platform.controller;

import com.iatechnology.platform.dto.UserDTO;
import com.iatechnology.platform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ─── USER PROFILE ───────────────────────────────────────────────────────

    @GetMapping("/user/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> getProfile(Authentication authentication) {
        com.iatechnology.platform.entity.User user = (com.iatechnology.platform.entity.User) authentication.getPrincipal();
        return ResponseEntity.ok(userService.getUserProfile(user.getEmail()));
    }

    @PutMapping("/user/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> updateProfile(Authentication authentication, @RequestBody UserDTO dto) {
        com.iatechnology.platform.entity.User user = (com.iatechnology.platform.entity.User) authentication.getPrincipal();
        return ResponseEntity.ok(userService.updateProfile(user.getEmail(), dto));
    }

    // ─── ADMIN ──────────────────────────────────────────────────────────────

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/admin/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> updateUserRole(@PathVariable("id") Long id, @RequestParam("role") String role) {
        return ResponseEntity.ok(userService.updateUserRole(id, role));
    }

    @DeleteMapping("/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
