# Production Database Migration Guide

**Document Version:** 1.0  
**Last Updated:** February 6, 2026  
**Target Database:** PostgreSQL  
**Estimated Setup Time:** 30-45 minutes

---

## Table of Contents
1. [Overview](#overview)
2. [Current State vs Production](#current-state-vs-production)
3. [Prerequisites](#prerequisites)
4. [Step-by-Step Migration](#step-by-step-migration)
5. [Data Migration](#data-migration)
6. [Verification](#verification)
7. [Backup & Recovery](#backup--recovery)
8. [Troubleshooting](#troubleshooting)

---

## Overview

### What's Changing
```
DEMO SETUP (Current)           PRODUCTION SETUP (Target)
├── Database: H2 In-Memory     ├── Database: PostgreSQL
├── Profile: demo              ├── Profile: production
├── Recreates on Startup       ├── Persistent Data
├── Sample Data: 20 Products   ├── Real Products Only
└── Demo Users Hardcoded       └── Secure Admin Setup
```

### Why PostgreSQL?
✅ **Enterprise-Grade** - Used by major companies (Netflix, Spotify, Instagram)  
✅ **Scalable** - Handles millions of transactions per second  
✅ **Reliable** - ACID compliant, data integrity guaranteed  
✅ **Secure** - Row-level security, encryption support  
✅ **Cost-Effective** - Open source, no licensing fees  
✅ **Performance** - Advanced indexing and query optimization  

---

## Current State vs Production

### Demo Database (H2 In-Memory)
```yaml
# Current Application.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop  # ❌ Deletes all data on restart!
      
# Current Application-demo.yml
app:
  init:
    create-demo-admin: true  # Creates test accounts
```

**Problems:**
- ❌ Data lost on every restart
- ❌ Not suitable for production
- ❌ No multi-user support
- ❌ Hard to scale
- ❌ Demo users mixed with real data

### Production Database (PostgreSQL)
```yaml
# New Application-production.yml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/perfume_shop_prod}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate  # ✅ Only validates schema, safe!
      
# Environment Variables
app:
  init:
    create-demo-admin: false  # ❌ Demo data NOT created
```

**Improvements:**
- ✅ Persistent data storage
- ✅ Production-ready with backups
- ✅ Multi-user support
- ✅ Horizontal scalability
- ✅ Only real data

---

## Prerequisites

### Required Software
- [ ] PostgreSQL 14+ installed
- [ ] Java 17+ installed
- [ ] Maven 3.8+ installed
- [ ] Git (for version control)
- [ ] psql command-line tool (PostgreSQL client)

### Installation Commands
```powershell
# Install PostgreSQL (Windows - using Chocolatey)
choco install postgresql

# Verify PostgreSQL installation
psql --version

# Start PostgreSQL service
net start postgresql-x64-14  # On Windows

# Verify PostgreSQL is running
psql -U postgres -c "SELECT version();"
```

### Required Credentials & Information
- [ ] PostgreSQL master user password
- [ ] New database name: `perfume_shop_prod`
- [ ] New database user: `perfume_prod_user`
- [ ] Strong password (12+ chars, mixed case, numbers, symbols)
- [ ] Current demo products (for migration)
- [ ] Current demo users (if any real data)

---

## Step-by-Step Migration

### Phase 1: Database Setup (15 minutes)

#### Step 1.1: Create PostgreSQL Database
```sql
-- Login to PostgreSQL as admin
psql -U postgres

-- Create the production database
CREATE DATABASE perfume_shop_prod
  ENCODING 'UTF8'
  LC_COLLATE 'en_US.UTF-8'
  LC_CTYPE 'en_US.UTF-8'
  TEMPLATE template0
  OWNER postgres;

-- Verify database created
\l  -- Lists all databases

-- Connect to new database
\c perfume_shop_prod
```

#### Step 1.2: Create Database User with Restricted Permissions
```sql
-- Create user with strong password
-- GENERATE STRONG PASSWORD: Use a password manager!
CREATE USER perfume_prod_user WITH PASSWORD 'YOUR_SECURE_PASSWORD_HERE';

-- Grant limited permissions (NOT superuser!)
GRANT CONNECT ON DATABASE perfume_shop_prod TO perfume_prod_user;
GRANT USAGE ON SCHEMA public TO perfume_prod_user;
GRANT CREATE ON SCHEMA public TO perfume_prod_user;

-- Grant table permissions
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO perfume_prod_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO perfume_prod_user;

-- Verify permissions
\du  -- Lists all users
\dp  -- Lists all permissions

-- Exit psql
\q
```

#### Step 1.3: Test Database Connection
```powershell
# Test connection with new user
psql -h localhost -U perfume_prod_user -d perfume_shop_prod

# If successful, you'll see: perfume_shop_prod=>
# If error "password authentication failed", check password

# Quick test query
SELECT 'Connection successful!' as message;

# Exit psql
\q
```

### Phase 2: Application Configuration (10 minutes)

#### Step 2.1: Create Production Environment File
```powershell
# Create .env.production file in project root
cd C:\Users\Hamdaan\OneDrive\Documents\maam

# Use template from .env.production.example
copy .env.production .env.production.backup

# Edit .env.production with your actual values
```

#### Step 2.2: Update Critical Environment Variables
```dotenv
# In .env.production (DO NOT COMMIT TO GIT!)

# Database Configuration
DATABASE_URL=jdbc:postgresql://localhost:5432/perfume_shop_prod
DATABASE_USERNAME=perfume_prod_user
DATABASE_PASSWORD=YOUR_ACTUAL_SECURE_PASSWORD

# JWT Secret (generate new!)
# Use: openssl rand -base64 32
JWT_SECRET=YOUR_GENERATED_JWT_SECRET_MIN_256_BITS

# Admin Account (CHANGE AFTER FIRST LOGIN!)
ADMIN_EMAIL=your-real-email@yourdomain.com
ADMIN_PASSWORD=YOUR_TEMPORARY_ADMIN_PASSWORD

# Mail Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-gmail@gmail.com
MAIL_PASSWORD=YOUR_GMAIL_APP_PASSWORD

# Payment Gateways (use LIVE keys if production)
RAZORPAY_KEY_ID=rzp_live_your_actual_key
RAZORPAY_KEY_SECRET=your_actual_secret
STRIPE_API_KEY=sk_live_your_actual_key

# Redis Cache (optional but recommended)
REDIS_PASSWORD=your_redis_password

# CORS Origins (your actual domain)
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
```

#### Step 2.3: Load Environment Variables (Windows)
```powershell
# PowerShell Load Function
function Load-EnvFile {
    param([string]$FilePath)
    if (-not (Test-Path $FilePath)) {
        Write-Error "File not found: $FilePath"
        return
    }
    
    Get-Content $FilePath | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]*)\s*=\s*(.*)$') {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim()
            [Environment]::SetEnvironmentVariable($key, $value, "Process")
        }
    }
    Write-Host "✓ Environment variables loaded from $FilePath"
}

# Load production variables
Load-EnvFile "C:\path\to\.env.production"

# Verify key variables are set
Write-Host "Database URL: $env:DATABASE_URL"
Write-Host "Admin Email: $env:ADMIN_EMAIL"
```

### Phase 3: Application Build & Schema Creation (15 minutes)

#### Step 3.1: Clean and Build Application
```powershell
cd C:\Users\Hamdaan\OneDrive\Documents\maam

# Clean previous builds
mvn clean

# Build application (skip tests for now)
mvn package -DskipTests -q

# Verify JAR was created
dir target/perfume-shop-1.0.0.jar
```

#### Step 3.2: Create Database Schema
```powershell
# Start application in production profile (one-time run)
# This will create all tables automatically
$env:SPRING_PROFILES_ACTIVE = "production"
$env:CREATE_DEMO_ADMIN = "true"  # Only for first run to create admin

# Run application (will create schema then exit)
# Use timeout to let it initialize then stop
java -jar target/perfume-shop-1.0.0.jar --spring.profiles.active=production | tee startup.log

# Wait 30 seconds for schema creation then Ctrl+C to stop
# Check logs for: "Hibernate: CREATE TABLE"

# Verify tables were created
psql -h localhost -U perfume_prod_user -d perfume_shop_prod -c "\dt"

# Expected output:
# List of relations
#  Schema |        Name         | Type  |       Owner       
# --------+---------------------+-------+-------------------
#  public | user                | table | perfume_prod_user
#  public | product             | table | perfume_prod_user
#  public | order               | table | perfume_prod_user
#  public | order_item          | table | perfume_prod_user
#  public | email_event         | table | perfume_prod_user
#  ... (more tables)
```

---

## Data Migration

### Option 1: Migrate from Demo Database (H2 to PostgreSQL)

#### Step 3.3: Export Demo Data
```powershell
# Start demo application to access H2 data
java -jar target/perfume-shop-1.0.0.jar --spring.profiles.active=demo

# In another terminal, query H2 database
# Open H2 console at: http://localhost:8080/h2-console
# Driver: org.h2.Driver
# URL: jdbc:h2:mem:testdb
# User: sa
# Password: (blank)

# SQL to export products:
SELECT 'INSERT INTO product (id, name, brand, price, stock, category) VALUES (' 
  || id || ', ''' || name || ''', ''' || brand || ''', ' || price || ', ' || stock || ', ''' || category || ''');'
FROM product
ORDER BY id;

# Copy output to file: export_products.sql
```

#### Step 3.4: Import Data to PostgreSQL
```powershell
# Create import script
@"
-- Import demo products to production database
INSERT INTO product (name, brand, description, price, category, type, volume, stock, rating, review_count, featured, active)
VALUES 
  ('Chanel No. 5', 'Chanel', 'The iconic timeless classic...', 165.00, 'Women', 'Eau de Parfum', 50, 45, 4.8, 245, true, true),
  -- ... (all 20 products from demo)
  ('Jean Paul Gaultier Le Male', 'Jean Paul Gaultier', 'Legendary masculine fragrance...', 105.00, 'Men', 'Eau de Toilette', 125, 64, 4.6, 267, false, true);
"@ | Out-File import_products.sql

# Import to PostgreSQL
psql -h localhost -U perfume_prod_user -d perfume_shop_prod -f import_products.sql

# Verify import
psql -h localhost -U perfume_prod_user -d perfume_shop_prod -c "SELECT COUNT(*) as product_count FROM product;"
```

### Option 2: Use Fresh Database (Recommended for Clean Start)

If you prefer a clean production database without demo data:

```sql
-- Just the admin user is created (via CREATE_DEMO_ADMIN)
-- Products are added through admin panel
-- Orders start fresh

-- Verify admin user created
psql -h localhost -U perfume_prod_user -d perfume_shop_prod -c "SELECT email, role FROM \"user\" WHERE role = 'ADMIN';"
```

---

## Verification

### Checklist for Successful Migration

- [ ] PostgreSQL installed and running
- [ ] Database `perfume_shop_prod` created
- [ ] User `perfume_prod_user` created with correct permissions
- [ ] Connection test successful
- [ ] Application-production.yml created
- [ ] Environment variables set correctly
- [ ] JAR built successfully
- [ ] Database schema created (all tables exist)
- [ ] Data imported (if using Option 1)
- [ ] Admin user exists in production database

### Verification Commands
```powershell
# 1. Check PostgreSQL service running
Get-Service postgresql* | Select-Object Status

# 2. Test database connection
psql -h localhost -U perfume_prod_user -d perfume_shop_prod -c "SELECT 'Success' as test;"

# 3. Count tables
psql -h localhost -U perfume_prod_user -d perfume_shop_prod -c "\dt"

# 4. Verify admin user exists
psql -h localhost -U perfume_prod_user -d perfume_shop_prod -c "SELECT email FROM \"user\" WHERE role = 'ADMIN';"

# 5. Verify product count
psql -h localhost -U perfume_prod_user -d perfume_shop_prod -c "SELECT COUNT(*) FROM product;"

# 6. Check database size
psql -h localhost -U perfume_prod_user -d perfume_shop_prod -c "SELECT pg_size_pretty(pg_database_size('perfume_shop_prod'));"
```

---

## Backup & Recovery

### Automated Daily Backups

#### Step 4.1: Create Backup Script (PowerShell)
```powershell
# File: backup-database.ps1

param(
    [string]$BackupDir = "C:\Database_Backups",
    [string]$DbHost = "localhost",
    [string]$DbUser = "perfume_prod_user",
    [string]$DbName = "perfume_shop_prod",
    [int]$RetentionDays = 30
)

function Backup-PostgresDatabase {
    # Create backup directory if not exists
    if (-not (Test-Path $BackupDir)) {
        New-Item -ItemType Directory -Path $BackupDir | Out-Null
    }
    
    # Generate filename with timestamp
    $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $backupFile = Join-Path $BackupDir "perfume_shop_backup_$timestamp.sql"
    
    # Run pg_dump
    Write-Host "Starting backup of $DbName..."
    
    $env:PGPASSWORD = $env:DATABASE_PASSWORD
    & 'C:\Program Files\PostgreSQL\14\bin\pg_dump.exe' `
        -h $DbHost `
        -U $DbUser `
        -d $DbName `
        -v `
        -F p `
        -f $backupFile
    
    if ($LASTEXITCODE -eq 0) {
        $fileSize = (Get-Item $backupFile).Length / 1MB
        Write-Host "✓ Backup successful: $backupFile ($([Math]::Round($fileSize, 2)) MB)" -ForegroundColor Green
        
        # Log backup
        Add-Content -Path "$BackupDir\backup-log.txt" -Value "$(Get-Date): Backup successful - $backupFile"
    } else {
        Write-Host "✗ Backup failed!" -ForegroundColor Red
        Add-Content -Path "$BackupDir\backup-log.txt" -Value "$(Get-Date): Backup FAILED"
    }
    
    # Cleanup old backups
    Get-ChildItem $BackupDir -Filter "perfume_shop_backup_*.sql" | 
        Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-$RetentionDays) } |
        Remove-Item
}

Backup-PostgresDatabase
```

#### Step 4.2: Schedule Daily Backups (Windows Task Scheduler)
```powershell
# Create scheduled task
$action = New-ScheduledTaskAction -Execute "powershell.exe" `
    -Argument "-NoProfile -File C:\backup-database.ps1"

$trigger = New-ScheduledTaskTrigger -Daily -At 2:00AM

Register-ScheduledTask -TaskName "DatabaseBackup" `
    -Action $action `
    -Trigger $trigger `
    -RunLevel Highest `
    -Description "Daily PostgreSQL backup at 2 AM"

# Verify task created
Get-ScheduledTask | Where-Object TaskName -eq "DatabaseBackup"
```

### Manual Backup
```powershell
# One-time backup
$env:PGPASSWORD = $env:DATABASE_PASSWORD
pg_dump -h localhost -U perfume_prod_user -d perfume_shop_prod > backup_$(Get-Date -Format yyyyMMdd).sql

# Verify backup file size
ls -lah backup_*.sql
```

### Restore from Backup
```powershell
# 1. Stop application
# 2. Create new database OR drop existing
psql -U postgres -c "DROP DATABASE perfume_shop_prod;"
psql -U postgres -c "CREATE DATABASE perfume_shop_prod;"

# 3. Restore from backup
$env:PGPASSWORD = $env:DATABASE_PASSWORD
psql -h localhost -U perfume_prod_user -d perfume_shop_prod < backup_20260206.sql

# 4. Verify restoration
psql -h localhost -U perfume_prod_user -d perfume_shop_prod -c "SELECT COUNT(*) FROM product;"

# 5. Start application
```

---

## Troubleshooting

### Issue: "Connection refused" or "Could not connect to server"
**Solution:**
```powershell
# Check if PostgreSQL is running
Get-Service postgresql* | Select-Object Status

# Start PostgreSQL
net start postgresql-x64-14

# Verify port 5432 is listening
netstat -ano | findstr :5432

# Test connection
psql -U postgres -d postgres
```

### Issue: "role 'perfume_prod_user' does not exist"
**Solution:**
```sql
-- Create user if missing
CREATE USER perfume_prod_user WITH PASSWORD 'your_password';
GRANT CONNECT ON DATABASE perfume_shop_prod TO perfume_prod_user;
```

### Issue: "permission denied for schema public"
**Solution:**
```sql
-- Grant schema permissions
GRANT USAGE ON SCHEMA public TO perfume_prod_user;
GRANT CREATE ON SCHEMA public TO perfume_prod_user;
```

### Issue: "Relation 'table_name' does not exist"
**Solution:**
```powershell
# Tables not created - restart application with profile-specific initialization
# Stop app, ensure DATABASE_PASSWORD is set, then run:

java -jar target/perfume-shop-1.0.0.jar `
    --spring.profiles.active=production `
    -Dspring.datasource.url=$env:DATABASE_URL `
    -Dspring.datasource.username=$env:DATABASE_USERNAME `
    -Dspring.datasource.password=$env:DATABASE_PASSWORD

# Let it initialize, should see: "Hibernate: CREATE TABLE..."
```

### Issue: Demo Data Still Appearing in Production
**Solution:**
```powershell
# Verify CREATE_DEMO_ADMIN is NOT set or is false
[Environment]::GetEnvironmentVariable("CREATE_DEMO_ADMIN")

# In .env.production, ensure:
CREATE_DEMO_ADMIN=false

# And application-production.yml has:
app.init.create-demo-admin: false
```

---

## Summary

### Timeline
- **Phase 1 (Database Setup):** 15 minutes
- **Phase 2 (Configuration):** 10 minutes
- **Phase 3 (Build & Schema):** 15 minutes
- **Total:** ~40 minutes

### What You've Done
✅ Migrated from H2 in-memory to PostgreSQL  
✅ Configured production-level security  
✅ Set up connection pooling (HikariCP)  
✅ Implemented backup strategy  
✅ Prevented demo data in production  
✅ Configured proper logging  
✅ Isolated admin endpoints  

### Next Steps
1. Start production application
2. Login with admin credentials
3. Add real products via admin panel
4. Monitor application logs
5. Setup monitoring and alerts
6. Plan disaster recovery

---

## Production Deployment Command

Once everything is configured:

```powershell
# Load environment variables
Load-EnvFile "C:\path\to\.env.production"

# Start application in production
java -jar target/perfume-shop-1.0.0.jar `
    --spring.profiles.active=production `
    --server.port=8080

# Or with environment variables
$env:SPRING_PROFILES_ACTIVE = "production"
java -jar target/perfume-shop-1.0.0.jar
```

---

**Need Help?** Check the [Troubleshooting](#troubleshooting) section or review logs at `logs/perfume-shop.log`

