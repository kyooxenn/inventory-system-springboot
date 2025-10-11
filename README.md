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

```sql
-- Create a new database
CREATE DATABASE inventorydb;

-- Select the database
USE inventorydb;

-- Create the 'product' table
CREATE TABLE product (
  id BIGINT NOT NULL,
  description VARCHAR(255),
  product_name VARCHAR(255),
  product_type VARCHAR(255),
  quantity INT NOT NULL,
  unit_price FLOAT(53) NOT NULL,
  PRIMARY KEY (id)
);
 

## 🏗️ MYSQL Host 
https://console.aiven.io/account/a567d6ec9d6b/project/project-xen/services/mysql-inventory/overview