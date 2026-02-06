# Production Deployment Checklist

**Version:** 1.0  
**Last Updated:** February 6, 2026  
**Target:** PostgreSQL Production Environment  

---

## PRE-DEPLOYMENT CHECKLIST

### Infrastructure ✓
- [ ] PostgreSQL 14+ installed and running
- [ ] PostgreSQL backups configured (daily at 2 AM)
- [ ] Redis cache server running (optional but recommended)
- [ ] Application server prepared (sufficient RAM, cpu, disk)
- [ ] Firewall configured (port 5432 for DB, 8080 for app, 8081 for admin)
- [ ] SSL/TLS certificates obtained (for HTTPS)
- [ ] CDN configured for static assets
- [ ] Email SMTP server configured (Gmail or Mailgun or AWS SES)
- [ ] Domain DNS records updated

### Database Configuration ✓
- [ ] PostgreSQL database created: `perfume_shop_prod`
- [ ] Application user created: `perfume_prod_user`
- [ ] User permissions verified (NOT superuser)
- [ ] Connection pooling configured (HikariCP with 20 connections)
- [ ] Database backup script created and tested
- [ ] Backup retention policy set (30 days minimum)
- [ ] Restore procedure tested and documented
- [ ] Database monitoring configured

### Application Configuration ✓
- [ ] Create `.env.production` file (NEVER commit to Git)
- [ ] `DATABASE_URL` set correctly
- [ ] `DATABASE_USERNAME` and `DATABASE_PASSWORD` set
- [ ] `DATABASE_PASSWORD` is strong (12+ chars, mixed case, numbers, symbols)
- [ ] `JWT_SECRET` generated (at least 256 bits / 32 bytes)
- [ ] `ADMIN_EMAIL` set to real email address
- [ ] `ADMIN_PASSWORD` is temporary (MUST change after first login)
- [ ] `MAIL_HOST` configured (smtp.gmail.com, Mailgun, AWS SES)
- [ ] `MAIL_USERNAME` and `MAIL_PASSWORD` set (use app-specific passwords)
- [ ] `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET` set (LIVE keys for production)
- [ ] `STRIPE_API_KEY` set if using Stripe (LIVE keys)
- [ ] `REDIS_PASSWORD` set (strong password)
- [ ] `CORS_ALLOWED_ORIGINS` set to production domain(s)
- [ ] All secrets at least 12 characters and random

### Application Build ✓
- [ ] Source code reviewed (~7000 LOC)
- [ ] No hardcoded credentials in code
- [ ] No debug logging enabled
- [ ] Maven build successful: `mvn clean package`
- [ ] JAR file created: `target/perfume-shop-1.0.0.jar`
- [ ] JAR size reasonable (~150-200 MB)
- [ ] No warnings or errors in build log
- [ ] All dependencies resolved

### Code Quality ✓
- [ ] No active todos with "PROD CRITICAL" or "MUST FIX"
- [ ] All email features tested and working
- [ ] Order flow tested (PLACED → DELIVERED)
- [ ] Payment integration tested (test mode)
- [ ] Admin panel all features verified
- [ ] Login/authentication working
- [ ] Rate limiting configured
- [ ] Input validation in place

### Security Review ✓
- [ ] No SQL injection vulnerabilities
- [ ] No XSS vulnerabilities
- [ ] Password hashing: BCrypt (strength: 12) enabled
- [ ] JWT signing algorithm: HS256 with strong secret
- [ ] HTTPS/SSL configured
- [ ] CORS properly configured (not wildcard)
- [ ] Security headers configured:
  - [ ] Strict-Transport-Security
  - [ ] X-Content-Type-Options: nosniff
  - [ ] X-Frame-Options: DENY
  - [ ] Content-Security-Policy
- [ ] Authentication required for admin endpoints
- [ ] Rate limiting on login endpoint
- [ ] Audit logging enabled for critical operations

### Database Schema ✓
- [ ] All tables created successfully
- [ ] Indexes created for performance:
  - [ ] `user(email)` - for login
  - [ ] `order(user_id)` - for order history
  - [ ] `product(brand, category)` - for filtering
  - [ ] `email_event(order_id)` - for tracking
- [ ] Foreign keys configured
- [ ] Constraints in place
- [ ] Schema version tracked
- [ ] Backup has valid schema

### Data Verification ✓
- [ ] Demo data NOT in production database
  - [ ] Check: `app.init.create-demo-admin: false`
  - [ ] Check: No hardcoded demo users (admin@perfumeshop.local)
  - [ ] Check: Products only added via admin panel (not seeded)
- [ ] Admin user exists with correct email
- [ ] Initial product catalog loaded (if migrating from demo)
- [ ] Order history is clean (no test orders)
- [ ] Email event table clean (no test emails)
- [ ] User data verified (real users only)

### Backup & Recovery ✓
- [ ] Backup script created and tested
- [ ] Scheduled backup task created (daily 2 AM)
- [ ] Backup location: External storage (not on app server)
- [ ] Backup retention: 30 days minimum
- [ ] Restore procedure documented
- [ ] Restore tested successfully
- [ ] Recovery Time Objective (RTO): < 1 hour
- [ ] Recovery Point Objective (RPO): < 1 day

### Monitoring & Alerts ✓
- [ ] Application monitoring configured (New Relic, DataDog, or similar)
- [ ] Database monitoring configured
- [ ] Error tracking configured (Sentry, Rollbar, or similar)
- [ ] Log aggregation configured (ELK, CloudWatch, or similar)
- [ ] Uptime monitoring configured
- [ ] Alert thresholds configured:
  - [ ] CPU > 80%
  - [ ] Memory > 85%
  - [ ] Database connection pool > 90%
  - [ ] Error rate > 1%
  - [ ] Response time > 2 seconds
- [ ] Alert notification channels tested
- [ ] On-call schedule established

### Logging Configuration ✓
- [ ] Log directory created: `/logs`
- [ ] Log file rotation configured (100 MB, 30 day retention)
- [ ] Log format consistent: `timestamp [level] logger - message`
- [ ] Root level: WARN (INFO for app-specific)
- [ ] Debug logging DISABLED
- [ ] Sensitive data NOT logged:
  - [ ] Passwords
  - [ ] API keys
  - [ ] Credit card numbers
  - [ ] Personal identification details
- [ ] Email logs contain: timestamp, status, recipient, attempt count

### Browser & Frontend ✓
- [ ] Frontend built and tested
- [ ] Static assets optimized (minified, compressed)
- [ ] Service workers configured (if PWA)
- [ ] Caching headers configured
- [ ] Browser compatibility tested
- [ ] Mobile responsive verified
- [ ] Payment flow tested end-to-end
- [ ] Error pages customized

---

## PRE-PRODUCTION TESTING

### Functional Testing ✓
- [ ] User Registration:
  - [ ] Can create new account
  - [ ] Email validation working
  - [ ] Password strength enforced
  - [ ] Duplicate email rejected
- [ ] User Login:
  - [ ] Login with correct credentials works
  - [ ] Wrong password rejected
  - [ ] Account lockout after 5 attempts (if configured)
  - [ ] JWT token generated
  - [ ] Token refresh working
- [ ] Product Catalog:
  - [ ] All products display correctly
  - [ ] Filtering by category works
  - [ ] Search functionality works
  - [ ] Sorting works (price, rating)
  - [ ] Pagination works
- [ ] Shopping Cart:
  - [ ] Add to cart works
  - [ ] Update quantity works
  - [ ] Remove item works
  - [ ] Cart persists (browser reload)
  - [ ] Totals calculated correctly
- [ ] Checkout:
  - [ ] Checkout form validation works
  - [ ] Address form works
  - [ ] Delivery options display
  - [ ] Order summary accurate
  - [ ] TnC acceptance required
- [ ] Payment Processing:
  - [ ] Razorpay integration works (test mode)
  - [ ] Payment success handled
  - [ ] Payment failure handled
  - [ ] Webhook received correctly
  - [ ] Order created after payment
- [ ] Order Management:
  - [ ] Order history displays
  - [ ] Order details correct
  - [ ] Invoice generated
  - [ ] Download invoice works
  - [ ] Email sent (status updates)
- [ ] Admin Panel:
  - [ ] Only admin can access
  - [ ] Product management works
  - [ ] Order management works
  - [ ] Email logs viewable
  - [ ] User management works
  - [ ] Reports generate correctly
- [ ] Email Notifications:
  - [ ] Order confirmation email sent (~30 sec)
  - [ ] Payment success email sent
  - [ ] Order status update emails sent (PACKED, SHIPPED, etc)
  - [ ] Delivery notification email sent
  - [ ] All emails formatted correctly
  - [ ] Email images load
  - [ ] Email links working

### Performance Testing ✓
- [ ] Page load time < 2 seconds
- [ ] Database query < 100ms (average)
- [ ] API response < 500ms (average)
- [ ] 10 concurrent users - No issues
- [ ] 50 concurrent users - No issues
- [ ] 100 concurrent users - No issues
- [ ] Peak traffic simulation (if possible)

### Security Testing ✓
- [ ] SQL Injection attempts blocked
- [ ] XSS attempts blocked
- [ ] CSRF token validation working
- [ ] Authentication bypass attempts failed
- [ ] Authorization checks working
- [ ] Rate limiting working (tried 100 requests/minute)
- [ ] Can't access other user's orders
- [ ] Can't modify other user's addresses

### Integration Testing ✓
- [ ] Database connectivity verified
- [ ] Redis cache working
- [ ] Email service working
- [ ] Payment gateway connectivity verified
- [ ] Webhooks received and processed
- [ ] Log file writing working
- [ ] Backup script executing successfully

---

## DEPLOYMENT DAY CHECKLIST

### Final Verification (1 Hour Before)
- [ ] All team members notified
- [ ] Support team on standby
- [ ] Maintenance page prepared (if needed)
- [ ] Rollback plan ready
- [ ] Database backup completed and tested
- [ ] Screenshots taken of demo data (if migrating)
- [ ] Final code review completed
- [ ] Deployment procedure reviewed

### Deployment Steps
1. [ ] Stop application
2. [ ] Backup production database
   ```sql
   pg_dump -h localhost -U perfume_prod_user -d perfume_shop_prod > backup_pre_migration.sql
   ```
3. [ ] Create schema (if new database)
   ```powershell
   java -jar target/perfume-shop-1.0.0.jar --spring.profiles.active=production
   # Let it initialize schema, then Ctrl+C
   ```
4. [ ] Verify schema created
   ```sql
   \dt  -- Should show all 10+ tables
   ```
5. [ ] Migrate data (if applicable)
6. [ ] Start application
   ```powershell
   $env:SPRING_PROFILES_ACTIVE = "production"
   java -jar target/perfume-shop-1.0.0.jar
   ```
7. [ ] Wait for startup (30 seconds)
8. [ ] Verify health check
   ```
   GET http://localhost:8080/actuator/health
   Should return: { "status": "UP" }
   ```
9. [ ] Test admin login
10. [ ] Create test order
11. [ ] Verify email sent
12. [ ] Check logs for errors
13. [ ] Notify stakeholders

### Post-Deployment (First 24 Hours)
- [ ] Monitor error rates (should be < 0.5%)
- [ ] Monitor response times (should be < 2s avg)
- [ ] Monitor database performance
- [ ] Test critical flows manually:
  - [ ] User registration
  - [ ] User login
  - [ ] Product browsing
  - [ ] Add to cart
  - [ ] Checkout
  - [ ] Payment
  - [ ] Admin panel
- [ ] Check logs for warnings
- [ ] Verify emails being sent
- [ ] Verify database backups running
- [ ] Monitor server resources (CPU, RAM, disk)
- [ ] Check email support for issues

### Rollback Plan (If Problems)
```powershell
# 1. Stop application
# 2. Restore database from backup
Stop-Process -Name java -Force
psql -U postgres -c "DROP DATABASE perfume_shop_prod;"
psql -U postgres -c "CREATE DATABASE perfume_shop_prod;"
psql -h localhost -U perfume_prod_user -d perfume_shop_prod < backup_pre_migration.sql

# 3. Start application with previous version
java -jar target/perfume-shop-1.0.0.jar

# 4. Notify team and customers
# 5. Investigate issue  
# 6. Fix and re-deploy
```

---

## POST-DEPLOYMENT CHECKLIST

### Week 1
- [ ] Monitor application 24/7
- [ ] Daily database backups successful
- [ ] All emails delivered successfully
- [ ] No unexpected errors in logs
- [ ] User feedback positive
- [ ] Performance metrics normal
- [ ] Security scan completed (if available)
- [ ] Database optimization completed

### Month 1
- [ ] Disaster recovery drill performed
- [ ] Database optimization re-run
- [ ] Security audit completed
- [ ] Load test performed
- [ ] Email archive reviewed
- [ ] Support ticket review
- [ ] Usage analytics reviewed
- [ ] Plan for scaling (if needed)

---

## SUCCESS CRITERIA

✅ **Application is UP and RUNNING**
```
ps aux | grep java
# Should show: java -jar target/perfume-shop-1.0.0.jar --spring.profiles.active=production
```

✅ **Health Check PASSING**
```
curl http://localhost:8080/actuator/health
# Response: { "status": "UP" }
```

✅ **Database CONNECTED**
```
psql -h localhost -U perfume_prod_user -d perfume_shop_prod -c "SELECT COUNT(*) FROM \"user\";"
# Should return number > 0
```

✅ **Admin Can LOGIN**
```
Login at: https://yourdomain.com/admin
Email from .env.production
```

✅ **Products DISPLAY**
```
Browse: https://yourdomain.com
Should see product catalog without errors
```

✅ **Orders CAN BE PLACED** (if data migrated)
```
Create test order and verify:
- Order created in database
- Status update email sent
- Invoice generated
```

✅ **NO DEMO DATA**
```
SELECT * FROM "user" WHERE email = 'admin@perfumeshop.local';
-- Should return: No rows
```

---

## CONTACTS & ESCALATION

| Role | Name | Email | Phone |
|------|------|-------|-------|
| DevOps Lead | [Your Name] | [Email] | [Phone] |
| Database Admin | [Your Name] | [Email] | [Phone] |
| Application Support | [Your Name] | [Email] | [Phone] |
| Escalation Manager | [Your Name] | [Email] | [Phone] |

---

## SIGN-OFF

| Role | Name | Signature | Date | Time |
|------|------|-----------|------|------|
| Database Admin | __________ | __________ | __/__/__ | ______ |
| DevOps | __________ | __________ | __/__/__ | ______ |
| QA Lead | __________ | __________ | __/__/__ | ______ |
| Project Lead | __________ | __________ | __/__/__ | ______ |

---

**Deployment Completed:** [ ] Date: __________ Time: __________

**Any Issues Discovered:** _______________________________________________

**Notes:** _________________________________________________________________

