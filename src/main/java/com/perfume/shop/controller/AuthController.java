package com.perfume.shop.controller;

import com.perfume.shop.dto.AuthResponse;
import com.perfume.shop.dto.LoginRequest;
import com.perfume.shop.dto.RegisterRequest;
import com.perfume.shop.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * Authentication controller handling user registration, login, and token refresh.
 * 
 * Security Features:
 * - Password policy enforcement on registration (8+ chars, uppercase, lowercase, digit, special char)
 * - JWT token-based authentication with refresh token support
 * - CORS-protected endpoints (configurable origins)
 * - Rate limiting recommendations (implement at API gateway level)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * Register a new user account.
     * 
     * @param request Registration details (email, password, firstName, lastName)
     * @return AuthResponse with access token, refresh token, and user info
     * @throws RuntimeException If email already exists or password policy violated
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }
    
    /**
     * Authenticate user and obtain tokens.
     * 
     * @param request Login credentials (email, password)
     * @return AuthResponse with access token, refresh token, and user info
     * @throws RuntimeException If credentials invalid or account disabled
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
    
    /**
     * Request password reset (send email with token).
     * 
     * @param email User email
     * @return Success message
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        authService.initiatePasswordReset(email);
        return ResponseEntity.ok(Map.of("message", "Password reset email sent (if user exists)"));
    }

    /**
     * Reset password using token.
     * 
     * @param request Reset token and new password
     * @return Success message
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");
        
        if (token == null || token.trim().isEmpty() || newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token and password are required"));
        }
        
        authService.resetPassword(token, newPassword);
        return ResponseEntity.ok(Map.of("message", "Password reset successful"));
    }

    /**
     * Refresh access token using refresh token.
     * 
     * Refresh tokens remain valid for 7 days. A 1-minute grace period allows
     * token refresh even if the access token is expired. The response includes
     * a new access token and optionally a new refresh token if near expiration.
     * 
     * @param refreshToken Refresh token from login/register response
     * @return AuthResponse with new access token and refresh token
     * @throws RuntimeException If refresh token invalid, expired, or not a refresh token
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@RequestParam String refreshToken) {
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }
    
    /**
     * Update user role (admin endpoint for initial setup).
     * 
     * WARNING: This is a helper endpoint to grant ADMIN role during initial setup.
     * Public for initial bootstrap - should be removed or secured in production.
     * 
     * @param email User email
     * @param role New role (CUSTOMER or ADMIN)
     * @return Success message
     */
    @PostMapping("/update-role")
    public ResponseEntity<java.util.Map<String, String>> updateUserRole(
            @RequestParam String email, 
            @RequestParam String role) {
        return ResponseEntity.ok(authService.updateUserRole(email, role));
    }
}
