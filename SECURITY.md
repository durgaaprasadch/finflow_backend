# 🛡️ Security Policy

FinFlow takes security seriously. We strive to maintain robust, identity-first microservices.

## 🔒 Supported Versions

Currently, only the `main` branch (V1.0.0-SNAPSHOT) is actively supported for security updates.

## 🕵️ Reporting a Vulnerability

If you discover a security vulnerability within FinFlow, please DO NOT report it via public GitHub issues. Instead, follow these steps:

1. **Email Us**: Send a detailed report to [durgaprasadch.in@gmail.com](mailto:durgaprasadch.in@gmail.com).
2. **Details**: Include a summary, potential impact, and clear reproduction steps.
3. **Response**: We aim to acknowledge receipt within 48 hours and provide a fix within 7 days.

## 🚧 Current Security Status

The project applies several core security principles:
- **RBAC (Role Based Access Control)**: Enforced via `auth-service` and API Gateway headers.
- **JWT Signing**: Uses **HS256** with configurable secret rotation.
- **Password Hashing**: Uses **BCrypt** with a strength of 10 for user credentials.
- **Header Sanitization**: Gateway limits sensitive headers reaching downstream services.
- **Dependency Auditing**: Regular Maven dependency checks (SonarQube/Jacoco).

## ⚠️ Security Disclaimer

> **IMPORTANT**: This project is for educational and portfolio demonstration purposes. The default configurations in `docker-compose.yml` and `config-repo/application.yml` contain reference passwords (e.g., `Durga@123`).
> 
> **NEVER use default credentials in Production.** Always override them using environment variables (`SPRING_MAIL_PASSWORD`, `DB_PASSWORD`, etc.) or a secure Vault (HashiCorp Vault / AWS Secrets Manager).

---

## 🚀 Future Roadmap
- Implementation of **OAuth2 / OpenID Connect** (OIDC).
- Integration with **Spring Cloud Vault**.
- Advanced **mTLS** (mutual TLS) for intra-service communication.
