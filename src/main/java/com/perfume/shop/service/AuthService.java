package com.perfume.shop.service;

import com.perfume.shop.dto.AuthResponse;
import com.perfume.shop.dto.LoginRequest;
import com.perfume.shop.dto.RegisterRequest;
import com.perfume.shop.entity.Cart;
import com.perfume.shop.entity.User;
import com.perfume.shop.exception.AuthenticationException;
import com.perfume.shop.exception.EmailAlreadyExistsException;
import com.perfume.shop.exception.PasswordPolicyException;
import com.perfume.shop.exception.UserNotFoundException;
import com.perfume.shop.repository.CartRepository;
import com.perfume.shop.repository.UserRepository;
import com.perfume.shop.security.JwtService;
import com.perfume.shop.security.PasswordPolicyValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication Service with hardened security
 * 
 * Features:
 * - Password policy enforcement
 * - Strong password validation
 * - Access and refresh token generation
 * - Secure login/registration
 * - Account status validation
 * - Custom exception handling for better error reporting
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements UserDetailsService {
    
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ObjectProvider<AuthenticationManager> authenticationManagerProvider;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final EmailService emailService;
    
    // === Password Reset ===
    @Transactional
    public void initiatePasswordReset(String email) {
        try {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                // Do not reveal user existence
                log.info("Password reset requested for non-existent user: {}", email);
                return;
            }
            // Generate token
            String token = java.util.UUID.randomUUID().toString();
            java.time.Instant expiry = java.time.Instant.now().plusSeconds(60 * 30); // 30 min
            user.setResetToken(token);
            user.setResetTokenExpiry(expiry);
            userRepository.save(user);
            
            // Send email
            String resetLink = "http://localhost:3000/reset-password?token=" + token;
            String subject = "Password Reset Request";
            String body = "<html><body>" +
                    "<h2>Password Reset Request</h2>" +
                    "<p>Click the link below to reset your password:</p>" +
                    "<a href=\"" + resetLink + "\">Reset Password</a>" +
                    "<p>This link will expire in 30 minutes.</p>" +
                    "</body></html>";
            try {
                emailService.sendEmail(user.getEmail(), subject, body);
                log.info("Password reset email sent successfully to: {}", user.getEmail());
            } catch (Exception emailException) {
                log.warn("Failed to send password reset email to {}: {}", user.getEmail(), emailException.getMessage());
                // Don't fail the request if email fails - token is still stored
            }
        } catch (Exception e) {
            log.error("Error in initiatePasswordReset: {}", e.getMessage(), e);
            // Still return successfully to not reveal if user exists
        }
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token).orElseThrow(() -> new UserNotFoundException("Invalid or expired token"));
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(java.time.Instant.now())) {
            throw new RuntimeException("Reset token expired");
        }
        // Validate password
        passwordPolicyValidator.validate(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }
    
    @Override
    public UserDetails loadUserByUsername(String username) throws org.springframework.security.core.userdetails.UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException(
                        "User not found with email: " + username
                ));
    }
    
    /**
     * Register new user with password policy validation
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registration request for email: {}", request.getEmail());
        
        // Validate email uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration attempt with existing email: {}", request.getEmail());
            throw new EmailAlreadyExistsException(request.getEmail());
        }
        
        // Validate password meets security policy
        try {
            passwordPolicyValidator.validate(request.getPassword());
        } catch (com.perfume.shop.security.PasswordPolicyValidator.PasswordPolicyException e) {
            log.warn("Password policy violation during registration: {}", e.getMessage());
            throw new PasswordPolicyException(
                    "Password does not meet security requirements",
                    e.getMessage()
            );
        }
        
        // Validate input
        if (request.getFirstName() == null || request.getFirstName().isBlank()) {
            log.warn("Registration with empty first name");
            throw new IllegalArgumentException("First name is required and cannot be empty");
        }
        if (request.getLastName() == null || request.getLastName().isBlank()) {
            log.warn("Registration with empty last name");
            throw new IllegalArgumentException("Last name is required and cannot be empty");
        }
        
        // Create user with encoded password
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .phoneNumber(request.getPhoneNumber())
                .role(User.Role.CUSTOMER)
                .active(true)
                .build();
        
        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getEmail());
        
        // Create cart for new user
        Cart cart = Cart.builder()
                .user(user)
                .build();
        cartRepository.save(cart);
        
        // Generate tokens
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        
        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .expiresIn(jwtService.getTimeUntilExpiration(accessToken) / 1000)
                .build();
    }
    
    /**
     * Login user and return access/refresh tokens
     */
    public AuthResponse login(LoginRequest request) {
        log.info("Login request for email: {}", request.getEmail());
        
        try {
            // Authenticate user
            authenticationManagerProvider.getObject().authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (org.springframework.security.core.AuthenticationException e) {
            log.warn("Failed login attempt for email: {}", request.getEmail());
            throw AuthenticationException.invalidCredentials();
        }
        
        // Load user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException(request.getEmail()));
        
        // Verify account is active
        if (!user.isEnabled()) {
            log.warn("Login attempt for disabled account: {}", user.getEmail());
            throw new AuthenticationException("Your account has been disabled. Please contact support.");
        }
        
        log.info("User logged in successfully: {}", user.getEmail());
        
        // Generate tokens
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        
        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .expiresIn(jwtService.getTimeUntilExpiration(accessToken) / 1000)
                .build();
    }
    
    /**
     * Refresh access token using refresh token
     */
    public AuthResponse refreshToken(String refreshToken) {
        log.debug("Token refresh request");
        
        try {
            // Validate refresh token type
            if (!jwtService.isRefreshToken(refreshToken)) {
                log.warn("Refresh attempted with non-refresh token");
                throw AuthenticationException.invalidToken();
            }
            
            // Extract username
            String userEmail = jwtService.extractUsername(refreshToken);
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new UserNotFoundException(userEmail));
            
            // Check if token is still valid (not beyond grace period)
            if (!jwtService.isTokenValid(refreshToken, user)) {
                log.warn("Refresh token expired for user: {}", userEmail);
                throw AuthenticationException.tokenExpired();
            }
            
            log.debug("Token refreshed for user: {}", userEmail);
            
            // Generate new access token
            String newAccessToken = jwtService.generateToken(user);
            String newRefreshToken = jwtService.generateRefreshToken(user);
            
            return AuthResponse.builder()
                    .token(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .id(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .role(user.getRole().name())
                    .expiresIn(jwtService.getTimeUntilExpiration(newAccessToken) / 1000)
                    .build();
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            throw AuthenticationException.tokenRefreshFailed();
        }
    }
    
    /**
     * Update user role (for admin setup purposes)
     */
    @Transactional
    public java.util.Map<String, String> updateUserRole(String email, String role) {
        log.info("Updating role for user: {} to {}", email, role);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        
        try {
            User.Role newRole = User.Role.valueOf(role.toUpperCase());
            user.setRole(newRole);
            userRepository.save(user);
            log.info("User role updated: {} -> {}", email, role);
            
            return java.util.Map.of("message", "Role updated successfully", "email", email, "role", role);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid role: {}", role);
            throw new IllegalArgumentException("Invalid role: " + role);
        }
    }
}