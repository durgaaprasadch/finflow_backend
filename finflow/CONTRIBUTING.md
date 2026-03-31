# 🤝 Contributing to FinFlow

Thank you for your interest in FinFlow! We welcome contributions to improve our Loan Management System.

## 🚀 Development Workflow

A structured development flow ensures stability and code quality.

### 1. **Fork & Branch**
- Fork the repository and create your feature branch:
  `git checkout -b feature/amazing-feature`

### 2. **Coding Standards**
- Use **Java 17** features (Records, Text Blocks, etc.) where appropriate.
- Follow standard Java CamelCase naming conventions.
- Maintain **constructor-based dependency injection** for better testability and SonarQube compliance.
- Keep controllers thin and place business logic in service layers.

### 3. **Testing Requirements**
- Every new feature must be accompanied by relevant **JUnit 5 / Mockito** tests.
- Ensure all tests pass before making a Pull Request:
  `mvn clean test`
- Maintain a minimum of **80% code coverage** (verified via Jacoco).

### 4. **Logging & Observability**
- Use `@Slf4j` for logging.
- Ensure meaningful log levels (`INFO` for state changes, `DEBUG` for data transitions, `ERROR` for exceptions).
- Preserve **traceId** and **spanId** context in distributed operations.

### 5. **Pull Request Process**
1. Update any relevant documentation (`README.md` or `PROJECT_STRUCTURE.md`).
2. Ensure your code follows the existing style (Project Checkstyle plugin).
3. Provide a clear summary of your changes in the PR description.

---

## 🏗️ Local Environment Setup

1. **Docker Infrastructure**: Run `docker-compose up -d` to spin up MySQL, RabbitMQ, and Redis.
2. **Maven Build**: Run `mvn clean install` from the root directory.
3. **Run Config Server & Eureka**: Start these FIRST as all other services depend on them.
4. **Service Launch**: Run other services in any order.

---

## 🐞 Reporting Issues
Please use the GitHub Issue tracker to report bugs or request new features. Include a detailed reproduction steps and logs if possible.

---

## 🛡️ License
By contributing, you agree that your contributions will be licensed under the [MIT License](LICENSE).
