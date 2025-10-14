# 📦 Inventory System Setup

> ⚠️ **Note:** This project uses an embedded **H2 Database** by default. You do **not** need to set up MySQL unless you're switching to a production-grade database.

---

## 📖 OpenAPI Documentation

Explore the API endpoints via Swagger UI:  
https://inventory-system-springboot-sea.onrender.com/swagger-ui/index.html

---

## 🔐 Authentication Flow
import curl on postman
### ✅ Register (optional)
```
curl -X POST https://inventory-system-springboot-sea.onrender.com/api/auth/register \
-H "Content-Type: application/json" \
-d '{"username":"bob","password":"secret"}'
```

### 🔑 Login — Get Token
```
curl -X POST https://inventory-system-springboot-sea.onrender.com/api/auth/login \
-H "Content-Type: application/json" \
-d '{"username":"bob","password":"secret"}'
```
###  Sample Login Response: 
```
{"token":"eyJ..."}
```

### 🔒 Call Protected Endpoint
```
curl https://inventory-system-springboot-sea.onrender.com/api/hello \
-H "Authorization: Bearer eyJ..."
```

### 🧰 Optional: MySQL Setup (for production use)

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
    quantity INTEGER NOT NULL,
    unit VARCHAR(255),
    unit_price NUMERIC(38,2),
    PRIMARY KEY (id)
);

```