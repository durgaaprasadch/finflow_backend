# 🏗️ FinFlow Project Structure

This document provides a high-level overview of the microservice modules and their responsibilities within the FinFlow Loan Management System.

## 📂 Core Business Services

### 🏁 `admin-service`
Dedicated administrative management service. Allows staff to review pending applications, analyze documentation, and make final approval/rejection decisions.
- **Key Flow**: Fetches application details and triggers state changes via the API Gateway to the `application-service`.

### 📄 `application-service`
The heart of the loan lifecycle. Manages the transition from `DRAFT` to `SUBMITTED`, `PENDING_DOCS`, through to `APPROVED` or `REJECTED`.
- **Key Flow**: Emits RabbitMQ events on status changes for down-flow notification.

### 🔐 `auth-service`
Gateway to identity. Responsible for user registration, multi-role (APPLICANT/ADMIN) authentication, and JWT issuance.
- **Key Flow**: Issues secure tokens consumed by the API Gateway to authorize downstream requests.

### 📁 `document-service`
Specialized service for unstructured data. Orchestrates the upload, storage, and metadata management of legal and financial documents.
- **Key Flow**: Links uploaded files to their respective loan application IDs.

### 🔔 `notification-service`
Asynchronous message consumer. Listens for RabbitMQ events and dispatches templates for OTPs, loan updates, and administrative alerts.
- **Key Flow**: Decoupled from core logic to ensure low latency for users.

---

## 🛠️ Infrastructure Modules

### 🛣️ `api-gateway`
The single entry point for all client traffic. Handles:
- **Routing**: Dynamic routing based on Eureka registration.
- **Security**: JWT validation and header enrichment (injecting `loggedInUser` / `userRole`).

### 📍 `eureka-server`
Service Registry. All services register their health and instance details here to enable dynamic discovery.

### 🛠️ `config-server`
Centralized configuration management. Serves `.yml` and `.properties` from the `config-repo` to all services during bootstrap.

---

## 📋 Common Patterns Across Services

- **DTOs**: All services use Data Transfer Objects for API inputs and outputs to decouple the internal database entities from external clients.
- **Exceptions**: `GlobalExceptionHandler` ensures standardized, type-safe error responses (JSON) across all boundaries.
- **Logging**: Slf4j with `@Slf4j` for consistent logging behavior.
- **Mapping**: Minimalist auto-mapping with Lombok to reduce boilerplate.

---

## 🏗️ Maven Multi-Module Configuration

The root `pom.xml` acts as the parent aggregator, managing shared dependency versions (Spring Boot 3.2.3, Spring Cloud 2023.0.0) and common plugins (Jacoco, SonarQube, Maven Compiler).
