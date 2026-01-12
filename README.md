# Eagle Bank API Technical Exercise

## Description

A REST API for Eagle Bank that allows users to create, fetch, update and delete bank accounts, and deposit or withdraw money from accounts. Transactions are stored against bank accounts and can be retrieved but not modified or deleted. The project was developed using Java, Spring Boot, Spring Security, and JWT authentication.

## Features

- User management (registration, authentication, profile management)
- Bank account management (CRUD operations with ownership verification)
- Transaction processing (deposits and withdrawals with balance validation)
- JWT-based authentication and authorization
- OpenAPI/Swagger documentation
- Pessimistic locking for concurrent transaction safety

## Technology Stack

- **Language**: Java 25
- **Framework**: Spring Boot 4.0.1
- **Security**: Spring Security with JWT (JSON Web Tokens)
- **Database**: MySQL (production), H2 (testing)
- **Build Tool**: Gradle
- **API Documentation**: SpringDoc OpenAPI (Swagger UI)
