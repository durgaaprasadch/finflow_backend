package com.finflow.auth.controller;

import com.finflow.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/internal")
@Hidden
public class InternalAdminController {

    private final AuthService authService;

    public InternalAdminController(AuthService authService) {
        this.authService = authService;
    }

    @PatchMapping("/users/{userId}/status")
    public void updateUserStatus(@PathVariable String userId, @RequestParam String status) {
        authService.updateUserStatus(userId, status);
    }

    @GetMapping("/users/active")
    public Object getActiveAdmins() {
        return authService.getActiveAdminUsers();
    }

    @GetMapping("/users/all")
    public Object getAllAdmins() {
        return authService.getAllAdminUsers();
    }

    @GetMapping("/users/pending")
    public Object getPendingAdmins() {
        return authService.getPendingAdminUsers();
    }

    @GetMapping("/users/everybody")
    public Object getAllUsers() {
        return authService.getAllUsers();
    }
}
