# 📦 Inventory Database Setup

This guide explains how to create and initialize the `inventorydb` database for the project.

---

## 🧰 Prerequisites

Before you begin, make sure you have:
- **MySQL Server** installed (version 8.0 or later recommended)
- **MySQL Workbench** or another SQL client

---

## 🏗️ Create the Database and Table

Run the following SQL commands in MySQL Workbench or your SQL terminal:

-- Create a new database
CREATE DATABASE inventorydb;

-- Select the database
USE inventorydb;

-- Create the 'product' table
create table product (
    id varchar(255) not null,
    category varchar(255),
    description varchar(255),
    item_name varchar(255),
    quantity integer not null,
    unit varchar(255),
    unit_price numeric(38,2),
    primary key (id)
);

---

## 🌐 MySQL Host
https://console.aiven.io/account/a567d6ec9d6b/project/project-xen/services/mysql-inventory/overview

---

## 📖 View OpenAPI Documentation
https://inventory-system-dair.onrender.com/swagger-ui/index.html

---

## Register (optional)

curl -X POST http://localhost:8080/api/auth/register \
-H "Content-Type: application/json" \
-d '{"username":"bob","password":"secret"}'

---

## Login — get token

curl -X POST http://localhost:8080/api/auth/login \
-H "Content-Type: application/json" \
-d '{"username":"bob","password":"secret"}'
# Response: {"token":"eyJ..."}

---

## Call protected endpoint

curl http://localhost:8080/api/hello \
-H "Authorization: Bearer eyJ..." 

---