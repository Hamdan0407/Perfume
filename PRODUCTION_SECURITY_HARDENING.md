# Production Security Hardening Guide

**Version:** 1.0  
**Last Updated:** February 6, 2026  
**Compliance:** OWASP Top 10, PCI DSS (Payment Card Security)

---

## Table of Contents
1. [Database Security](#database-security)
2. [Application Security](#application-security)
3. [Server Security](#server-security)
4. [Network Security](#network-security)
5. [Data Protection](#data-protection)
6. [Secrets Management](#secrets-management)
7. [Monitoring & Audit](#monitoring--audit)
8. [Compliance](#compliance)

---

## DATABASE SECURITY

### PostgreSQL User Permissions

✅ **CORRECT: Limited User for Application**
```sql
-- DO NOT use postgres superuser!
CREATE USER perfume_prod_user WITH PASSWORD 'strong_random_password_here';

-- Grant only necessary permissions
GRANT CONNECT ON DATABASE perfume_shop_prod TO perfume_prod_user;
GRANT USAGE ON SCHEMA public TO perfume_prod_user;

-- Grant table permissions (NOT superuser)
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO perfume_prod_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE ON SEQUENCES TO perfume_prod_user;

-- Specifically grant what's needed
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO perfume_prod_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO perfume_prod_user;

-- View permissions
\du  -- List users
\dp  -- List permissions
```

❌ **WRONG: DO NOT DO THIS**
```sql
-- ❌ Never make application user superuser
-- GRANT SUPERUSER ON perfume_prod_user;

-- ❌ Never allow PUBLIC access
-- GRANT ALL ON ALL TABLES IN SCHEMA public TO PUBLIC;

-- ❌ Never use default password
-- CREATE USER perfume_prod_user WITH PASSWORD 'password123';
```

### Connection Security

```yaml
# In PostgreSQL configuration (postgresql.conf)

# Listen only on localhost (not public internet)
listen_addresses = 'localhost'

# SSL/TLS for remote connections (if needed)
ssl = on
ssl_cert_file = '/path/to/server.crt'
ssl_key_file = '/path/to/server.key'

# Connection timeout (prevent DoS)
statement_timeout = '30s'

# Log all failed login attempts
log_connections = on
log_disconnections = on
log_statement = 'ddl'
```

### Prepared Statements (Prevent SQL Injection)

✅ **CORRECT: Using JPA (Automatic)**
```java
// Spring Data JPA automatically uses prepared statements
@Query("SELECT u FROM User u WHERE u.email = :email")
User findByEmail(@Param("email") String email);
```

❌ **WRONG: String Concatenation**
```java
// ❌ Vulnerable to SQL injection!
String query = "SELECT * FROM user WHERE email = '" + email + "'";
```

### Data Encryption in Transit

```yaml
# In application-production.yml

spring:
  datasource:
    # SSL mode for PostgreSQL connection
    url: jdbc:postgresql://localhost:5432/perfume_shop_prod?sslmode=require&ssl=true
    
# In PostgreSQL (if using remote host)
postgresql.conf:
  ssl = on
  ssl_cert_file = '/etc/ssl/certs/server.crt'
  ssl_key_file = '/etc/ssl/private/server.key'
```

---

## APPLICATION SECURITY

### Authentication & Authorization

#### Password Hashing (BCrypt)
```java
// In SecurityConfig.java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Strength 12 = 2^12 iterations (~290ms per hash)
        // Production should use strength 12+
        return new BCryptPasswordEncoder(12);
    }
}
```

✅ **Correct Password Requirements:**
- [ ] Minimum 12 characters
- [ ] Mix of uppercase and lowercase letters
- [ ] At least one number
- [ ] At least one special character (!@#$%^&*)
- [ ] Not in common password list

❌ **Wrong Passwords:**
```powershell
# ❌ Too short
password: "admin"

# ❌ Only numbers
password: "123456789"

# ❌ Dictionary words
password: "password123"

# ❌ Sequential
password: "12345678"

# ❌ Same as username
password = email
```

#### JWT Token Security
```yaml
# In .env.production

# Generate secure JWT secret
# openssl rand -base64 32
JWT_SECRET=YOUR_GENERATED_256_BIT_SECRET_HERE

# Short expiration (1 hour for access token)
JWT_EXPIRATION=3600000  # ms = 1 hour

# Longer refresh token (7 days)
JWT_REFRESH_EXPIRATION=604800000  # ms = 7 days
```

#### Role-Based Access Control (RBAC)
```java
// In endpoints - ensure RBAC
@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/api/admin/products")
public ResponseEntity<Product> createProduct(@RequestBody Product product) {
    // Only ADMIN can create products
    return productService.create(product);
}

@PreAuthorize("hasRole('CUSTOMER')")
@PostMapping("/api/orders")
public ResponseEntity<Order> createOrder(@RequestBody OrderRequest request) {
    // Only CUSTOMER can place orders
    return orderService.create(request);
}
```

### Input Validation

**All Input Must Be Validated:**
```java
public class UserRegistrationRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255)
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 12, max = 255, message = "Password must be 12+ characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", 
             message = "Password must contain uppercase, lowercase, number")
    private String password;
    
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100)
    private String firstName;
}
```

### Output Encoding (XSS Prevention)

```java
// Spring automatically encodes JSON output
@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    @GetMapping
    public ResponseEntity<List<ProductDTO>> getProducts() {
        // Spring automatically escapes JSON values
        // XSS prevented: <script> becomes \\u003cscript\\u003e
        return ResponseEntity.ok(productService.getAll());
    }
}
```

### HTTPS/TLS Configuration

```yaml
# In application-production.yml

server:
  ssl:
    enabled: true
    key-store: /path/to/keystore.p12
    key-store-password: ${KEY_STORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: perfume-shop
  compression:
    enabled: true

# Add HSTS header (enforce HTTPS)
spring:
  http:
    encoding:
      charset: UTF-8
```

### Security Headers

```java
// In SecurityConfig.java

@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .headers()
            .contentSecurityPolicy("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'")
            .and()
            .xssProtection()
            .and()
            .frameOptions().deny()
            .and()
            .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_NO_REFERRER)
        .and()
        .https()
            .requiresSecure();
    return http.build();
}
```

---

## SERVER SECURITY

### Firewall Configuration (Windows)

```powershell
# Allow only necessary ports
# Block everything else

# Port 8080 - Application (only from load balancer)
New-NetFirewallRule -DisplayName "Allow App Port 8080" `
    -Direction Inbound -LocalPort 8080 -Protocol TCP `
    -RemoteAddress 10.0.0.0/8  # Only from internal network

# Port 5432 - PostgreSQL (only from app server)
New-NetFirewallRule -DisplayName "Allow DB Port 5432" `
    -Direction Inbound -LocalPort 5432 -Protocol TCP `
    -RemoteAddress 127.0.0.1  # Only localhost

# Port 22 - SSH (only from admin)
New-NetFirewallRule -DisplayName "Allow SSH Port 22" `
    -Direction Inbound -LocalPort 22 -Protocol TCP `
    -RemoteAddress YOUR.ADMIN.IP.ADDRESS

# Block all other inbound
Set-NetFirewallProfile -DefaultInboundAction Block -DefaultOutboundAction Allow
```

### Application Server Hardening

```powershell
# 1. Disable unnecessary services
Get-Service | Where-Object {$_.StartType -eq 'Automatic'} | 
    Select-Object Name, DisplayName | 
    Out-GridView  # Review and disable unnecessary services

# 2. Update Windows
Install-Module PSWindowsUpdate
Get-WindowsUpdate -Install -AcceptAll

# 3. Run Windows Defender
Start-MpScan -ScanType FullScan

# 4. Enable Windows Firewall
Set-NetFirewallProfile -Enabled True

# 5. Audit and logging
auditpol /set /subcategory:"Logon/Logoff" /success:enable /failure:enable
auditpol /set /subcategory:"Object Access" /success:enable /failure:enable
```

### Java/Application Security

```powershell
# 1. Keep Java updated frequently
# Check: java -version

# 2. Run with minimal privileges (NOT as system/admin)
# Create service account: perfume_shop_user
# Grant only necessary permissions

# 3. Run with security manager (optional)
java -Djava.security.manager -jar target/perfume-shop-1.0.0.jar

# 4. Disable dangerous features
# -XX:-UseBiasedLocking
# -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints
```

---

## NETWORK SECURITY

### Load Balancer Configuration

```
Internet
   ↓
   [HTTPS/TLS Termination]
   ↓
   [Load Balancer: nginx/HAProxy]
   ↓
   [Application: :8080]
   ↓
   [Database: :5432]
```

### nginx Configuration (Reverse Proxy)

```nginx
# /etc/nginx/nginx.conf

upstream perfume_shop {
    server localhost:8080;
    keepalive 32;
}

server {
    listen 443 ssl http2;
    server_name yourdomain.com;
    
    # SSL Configuration
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    
    # Security Headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-Frame-Options "DENY" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-no-referrer" always;
    
    # Rate limiting
    limit_req_zone $binary_remote_addr zone=general:10m rate=10r/s;
    limit_req_zone $binary_remote_addr zone=api:10m rate=100r/m;
    limit_req zone=general burst=20 nodelay;
    
    location /api/ {
        limit_req zone=api burst=50 nodelay;
        proxy_pass http://perfume_shop;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
    
    location / {
        proxy_pass http://perfume_shop;
    }
}

# Redirect HTTP to HTTPS
server {
    listen 80;
    server_name yourdomain.com;
    return 301 https://$server_name$request_uri;
}
```

---

## DATA PROTECTION

### Sensitive Data NOT Logged

```java
public class AuditLogger {
    
    @Around("@annotation(com.perfume.shop.audit.Auditable)")
    public Object auditOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        // ✅ Log what happened
        log.info("User {} performed action {}", userId, action);
        
        // ❌ NEVER log these:
        // - Passwords
        // - JWT tokens
        // - Credit card numbers
        // - SSN or ID numbers
        // - Email addresses (in sensitive contexts)
        
        return joinPoint.proceed();
    }
}
```

### Data Retention & Deletion

```sql
-- Anonymize old user data (GDPR compliance)
UPDATE user 
SET email = 'deleted_' || id || '@example.com',
    password = NULL,
    phone_number = NULL,
    address = NULL
WHERE last_login < NOW() - INTERVAL '1 year'
  AND deleted_at IS NULL;

-- Delete old orders (after regulatory period)
DELETE FROM order 
WHERE created_at < NOW() - INTERVAL '7 years'
  AND status = 'DELIVERED';

-- Delete old email logs
DELETE FROM email_event
WHERE created_at < NOW() - INTERVAL '90 days';
```

### Payment Card Data

✅ **CORRECT: Never store card data**
```java
// Payment handled by Razorpay
// We never see or store card numbers
// Razorpay returns: payment_id, customer_id
// We store: payment_id (not card data)

public class Payment {
    private String razorpayPaymentId;  // ✅ Safe to store
    private String razorpayOrderId;    // ✅ Safe to store
    
    // ❌ NEVER add these fields:
    // private String cardNumber;  // ❌ ILLEGAL
    // private String cvv;         // ❌ ILLEGAL
    // private String expiryDate;  // ❌ ILLEGAL
}
```

---

## SECRETS MANAGEMENT

### Environment Variables (NEVER Commit)

```dotenv
# .env.production (NEVER commit to Git!)
# File permissions: chmod 600

# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/perfume_shop_prod
DATABASE_USERNAME=perfume_prod_user
DATABASE_PASSWORD=YOUR_SECURE_PASSWORD_HERE

# JWT
JWT_SECRET=YOUR_256_BIT_RANDOM_SECRET_HERE

# Email
MAIL_PASSWORD=YOUR_GMAIL_APP_PASSWORD

# Payment
RAZORPAY_KEY_SECRET=YOUR_RAZORPAY_SECRET_KEY

# Redis
REDIS_PASSWORD=YOUR_REDIS_PASSWORD
```

### .gitignore (Prevent Accidental Commit)

```gitignore
# Environment files
.env
.env.*
!.env.example

# Secrets
secrets/
*.key
*.pem
keystore.p12

# IDE
.idea/
.vscode/
*.swp
*.swo

# Build
target/
node_modules/
*.log

# Database backups
*.sql
*.dump
```

### Secrets Rotation Policy

Every 90 days, rotate:
- [ ] Database password
- [ ] JWT secret
- [ ] Email password
- [ ] API keys (Razorpay, Stripe)
- [ ] Redis password

```powershell
# Rotation Policy:
# 1. Generate new secret: openssl rand -base64 32
# 2. Update .env.production
# 3. Restart application
# 4. Change in actual service (email, payment gateway)
# 5. Delete old secret from memory
# 6. Document date and who performed rotation
```

---

## MONITORING & AUDIT

### Audit Logging

```java
@Component
@Aspect
@Slf4j
public class AuditAspect {
    
    @Around("@annotation(com.perfume.shop.audit.Auditable)")
    public Object auditLog(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().getName();
        String user = SecurityContextHolder.getContext().getAuthentication().getName();
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            log.info("AUDIT: User={} Action={} Status=SUCCESS Duration={}ms",
                    user, method, System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            log.warn("AUDIT: User={} Action={} Status=FAILED Error={} Duration={}ms",
                    user, method, e.getMessage(), System.currentTimeMillis() - startTime);
            throw e;
        }
    }
}
```

### Log Monitoring

```yaml
# Monitor these critical events:

# ✅ Successful logins
# 2026-02-06 10:15:22 [INFO] [http-nio-8080-exec-1] User logged in successfully: user@example.com

# ⚠️ Failed logins (potential brute force)
# 2026-02-06 10:15:30 [WARN] [http-nio-8080-exec-2] Login failed for user@example.com (attempt 3/5)

# ✅ Order creation
# 2026-02-06 10:20:15 [INFO] [pool-123-thread-1] Order created: ORD-12345

# ⚠️ Payment failures
# 2026-02-06 10:20:30 [ERROR] [pool-123-thread-2] Payment failed for order ORD-12345

# ✅ Admin actions
# 2026-02-06 10:25:00 [INFO] [http-nio-8080-exec-5] Admin user@admin.com created product: Chanel No. 5
```

### Alert Conditions

Set alerts for:
- [ ] Error rate > 1% per minute
- [ ] Response time > 3 seconds (95th percentile)
- [ ] Database connection pool > 90%
- [ ] Failed login attempts > 5 in 5 minutes (same IP)
- [ ] Unusual query patterns
- [ ] Disk usage > 80%
- [ ] Memory usage > 85%

---

## COMPLIANCE

### GDPR Compliance

- [ ] **Data Privacy Policy** - Published
- [ ] **User Consent** - Collect before processing
- [ ] **Right to Access** - Users can download their data
- [ ] **Right to Delete** - "Delete my account" functionality
- [ ] **Data Breach Notification** - Within 72 hours

### PCI DSS Compliance (Payment Security)

- [ ] **No card data stored** - Use Razorpay
- [ ] **No card data logged** - Audit logging
- [ ] **HTTPS everywhere** - SSL/TLS
- [ ] **Access control** - Role-based
- [ ] **Regular security testing** - Quarterly

### Regular Security Audits

quarterly:
- [ ] Code review (focus on security)
- [ ] Dependency vulnerability scan
- [ ] Penetration testing (or OWASP ZAP)
- [ ] Database access review
- [ ] Firewall rule review
- [ ] Log analysis

---

## Security Checklist Summary

### Critical (Must Have)
- [ ] PostgreSQL user (NOT superuser)
- [ ] Strong passwords (12+ chars, mixed case, numbers, symbols)
- [ ] HTTPS/TLS enabled
- [ ] JWT token signing
- [ ] Password hashing (BCrypt strength 12)
- [ ] Input validation
- [ ] SQL injection prevention (prepared statements)
- [ ] XSS prevention (output encoding)
- [ ] CSRF protection
- [ ] Rate limiting
- [ ] Security headers

### Important (Should Have)
- [ ] Firewall configured
- [ ] Database backups and encryption
- [ ] Audit logging
- [ ] Error handling (no stack traces in HTTP response)
- [ ] Secrets management
- [ ] Log monitoring
- [ ] WAF (Web Application Firewall)
- [ ] DDoS protection

### Best Practice (Nice to Have)
- [ ] API rate limiting per user
- [ ] Account lockout after failed logins
- [ ] Two-factor authentication (2FA)
- [ ] VPN for admin access
- [ ] Security scanning (SAST/DAST)
- [ ] API key rotation
- [ ] Certificate pinning
- [ ] Security training for team

---

**For support or questions about security hardening, contact your security team.**

