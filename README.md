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
    PRIMARY KEY (id)
);


```