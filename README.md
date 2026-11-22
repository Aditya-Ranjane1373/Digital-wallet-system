# Digital Wallet Management System

🌐 **Digital Wallet Management System** is a backend application that allows users to manage their digital wallets, add money, transfer funds securely, and track transaction history. Built using Java, Spring Boot, Spring Security, JWT, and Spring Data JPA, it provides secure REST APIs with role-based access control, transaction management, and global exception handling.

## 🚀 Features

### 🧑‍💻 User Management
- User registration and login
- Secure password encryption using BCrypt
- JWT-based authentication
- Role-based authorization (USER, ADMIN)

### 💰 Wallet Management
- Automatic wallet creation during user registration
- View wallet balance
- Add money to wallet
- Transfer money between users
- BigDecimal-based balance management

### 💳 Transaction Management
- Track wallet transactions
- View transaction history
- Pagination and sorting support
- Idempotency key to prevent duplicate requests
- Transaction status management
- Safe concurrent wallet operations

### 🔐 Security
- Spring Security integration
- JWT authentication filter
- Stateless session management
- Protected user and admin APIs
- BCrypt password hashing

### ⚙️ Exception Handling & Validation
- Global exception handling using `@RestControllerAdvice`
- Request payload validation
- Custom exceptions for insufficient balance, duplicate requests, and resource not found
- Standardized API error responses

## 🛠️ Tech Stack

### 🔹 Backend
- Java 17
- Spring Boot
- Spring Security
- Spring Data JPA
- Hibernate
- RESTful APIs
- Maven

### 🔹 Database
- H2 Database

### 🔹 Security
- JSON Web Token (JWT)
- BCrypt Password Encoder
- Role-Based Access Control

### 🔹 Testing & Development Tools
- Postman
- Eclipse / Spring Tool Suite
- Git & GitHub

## 📌 API Modules
- Authentication APIs
- Wallet APIs
- Transaction APIs
- Admin APIs

## 🎯 Project Objective

To develop a secure and reliable digital wallet backend system that demonstrates REST API development, authentication and authorization, database management, transaction processing, idempotency, and exception handling using Java Spring Boot.
