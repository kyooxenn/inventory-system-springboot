# 📦 Inventory System Setup

> ⚠️ **Note:** This project is intended for production-grade use with **MySQL**. You do **not** need to use the embedded H2 Database unless you're working in a local or development environment.

---

## 📖 OpenAPI Documentation

Explore the API endpoints via Swagger UI:  
https://inventory-system-springboot-sea.onrender.com/swagger-ui/index.html

---

## 🔐 Authentication Flow
import curl on postman
### ✅ Register (optional)
```
curl --location 'https://inventory-system-springboot-sea.onrender.com/api/auth/register' \
--header 'Content-Type: application/json' \
--data-raw '{
    "username": "bob",
    "password": "secret",
    "email": "your_email_adress@gmail.com",
    "mobile":"+63912345678"
}'
```

### 🔑 Login — Get Token
```
curl -X POST https://inventory-system-springboot-sea.onrender.com/api/auth/login \
-H "Content-Type: application/json" \
-d '{"username":"bob","password":"secret"}'
```
###  Sample Login Response (Verified User): 
```json
{
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJyb290Iiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYxNzExMTM5LCJleHAiOjE3NjE3MTQ3Mzl9.mUZS0Jg7fCXon2fjkQwroBgA5nHiRfEsHle4qsvB_pU"
}
```
###  Sample Login Response (Unverified User):
```json
{
    "email": "your_email_adress@gmail.com",
    "tempToken": "4d4d0073-8ed9-4d12-b4fe-f857b6403ef9"
}
```

### 🔑 Generate OTP (For Unverified User)
```
curl --location 'https://inventory-system-springboot-sea.onrender.com/api/auth/generate-otp' \
--header 'Content-Type: application/json' \
--data-raw '{
"tempToken": "4d4d0073-8ed9-4d12-b4fe-f857b6403ef9",
"email": "your_email_adress@gmail.com"
}'
```
###  Sample Generate OTP Response (Unverified User):
```json
{
    "message":"OTP sent to your registered email"
}
```

### 🔑 Verify OTP (For Unverified User)
```
curl --location 'https://inventory-system-springboot-sea.onrender.com/api/auth/verify-otp' \
--header 'Content-Type: application/json' \
--data '{
"tempToken": "4d4d0073-8ed9-4d12-b4fe-f857b6403ef9",
"otp": "123456"
}'
```
### Sample OTP Verification Response (Unverified User)

#### ❌ Error Response
```json
{
  "error": "Invalid or expired session"
}
```
#### ✅ Success Response
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJyb290Iiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYxNzExOTM5LCJleHAiOjE3NjE3MTU1Mzl9.0gh8moNC5RHWD7IM128kSbasqHiETuGx_Ql2Pt57G3k"
}

```

###  Health Check Endpoint
```
curl https://inventory-system-springboot-sea.onrender.com/api/health \
-H "Content-Type: application/json" \
```
###  Sample Health Check Response:
```
✅ Inventory System API is alive! All systems operational 🚀
```

###  Redis Viewer Endpoint
```
curl https://inventory-system-springboot-sea.onrender.com/redis/all \
-H "Content-Type: application/json" \
```

### MySQL Setup (for production use)

If you choose to use MySQL instead of H2, follow these steps:

---

#### 📋 Prerequisites

- **MySQL Server** (version 8.0 or later recommended)
- **MySQL Workbench** or another SQL client

---

#### 🏗️ Create Database and Table

Run the following SQL commands:

```sql
-- Create a new database
CREATE DATABASE inventorydb;

-- Select the database
USE inventorydb;

-- Create the 'product' table
CREATE TABLE product (
    id VARCHAR(255) NOT NULL,
    category VARCHAR(255),
    description VARCHAR(255),
    item_name VARCHAR(255),
    quantity INT NOT NULL,
    unit VARCHAR(255),
    unit_price DECIMAL(38,2),
    PRIMARY KEY (id)
);

-- Create the 'users' table
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    password VARCHAR(255) NOT NULL,
    roles VARCHAR(255),
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    mobile VARCHAR(255) NOT NULL,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

```

#### 📋 Scan Vulnerability Issues Using Trivy

install trivy and run this command:

```
trivy fs .

```
