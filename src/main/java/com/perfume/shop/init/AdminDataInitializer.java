package com.perfume.shop.init;

import com.perfume.shop.entity.User;
import com.perfume.shop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * DEMO ADMIN INITIALIZATION
 * 
 * This initializer ONLY runs in demo/dev profiles with explicit flag.
 * Creates demo admin and customer accounts for testing.
 * 
 * NOT USED in production - see ProductionDataInitializer for production setup.
 * 
 * To create admin in production:
 * 1. Set environment variables: ADMIN_EMAIL and ADMIN_PASSWORD
 * 2. Or use secure CLI tool: java -jar app.jar --create-admin=email@domain.com --password=<secure-password>
 * 3. Never hardcode credentials
 * 4. Use environment-based password rotation
 */
@Component
@Profile("!production")  // Only run when NOT in production profile
@ConditionalOnProperty(
    name = "app.init.create-demo-admin",
    havingValue = "true"
)
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class AdminDataInitializer implements CommandLineRunner {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        try {
            // Check if admin user already exists
            String adminEmail = "admin@perfumeshop.local";
            String customerEmail = "mohammed@example.com";
            
            if (userRepository.existsByEmail(adminEmail)) {
                log.info("✓ Default admin user already exists");
            } else {
                // Create default admin user (DEMO ONLY)
                User admin = User.builder()
                        .email(adminEmail)
                        .password(passwordEncoder.encode("admin123456"))
                        .firstName("Admin")
                        .lastName("User")
                        .phoneNumber("+1-555-0000")
                        .address("123 Admin Street")
                        .city("Admin City")
                        .country("Admin Country")
                        .zipCode("00000")
                        .role(User.Role.ADMIN)
                        .active(true)
                        .build();
                
                userRepository.save(admin);
                log.warn("═══════════════════════════════════════════════════════════");
                log.warn("⚠️  DEMO ADMIN CREATED - FOR TESTING ONLY!");
                log.warn("═══════════════════════════════════════════════════════════");
                log.warn("Email: {}", adminEmail);
                log.warn("Password: admin123456");
                log.warn("⚠️  DO NOT USE THIS ACCOUNT IN PRODUCTION!");
                log.warn("═══════════════════════════════════════════════════════════");
            }
            
            // Also create a test customer user
            if (!userRepository.existsByEmail(customerEmail)) {
                User customer = User.builder()
                        .email(customerEmail)
                        .password(passwordEncoder.encode("password123"))
                        .firstName("Mohammed")
                        .lastName("User")
                        .phoneNumber("9999999999")
                        .address("123 Customer St")
                        .city("Mumbai")
                        .country("India")
                        .zipCode("400001")
                        .role(User.Role.CUSTOMER)
                        .active(true)
                        .build();
                
                userRepository.save(customer);
                log.warn("✓ DEMO CUSTOMER CREATED - Email: {}, Password: password123", customerEmail);
            }
            
        } catch (Exception e) {
            log.error("Error initializing admin data", e);
        }
    }
}
