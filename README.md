# Banking System

A modern, secure **Spring Boot** banking application built with best practices. It supports user registration, JWT authentication, account management, and banking transactions.

> **Version:** V1 — built in 11 days. ( Excluding planning )

---

## ✨ Features

### Authentication & Security
- **JWT-based stateless authentication**
- Secure password hashing with **BCrypt**
- Role-based access control (planned expansion)
- Protected endpoints with custom `JwtAuthenticationFilter`

### User Management
- User registration and login
- Profile management (`GET /api/user/me`, `PUT /api/user/me`, `PUT /api/user/password`)
- Multiple accounts per user

### Account Management
- Create different account types (**SAVINGS**, **CURRENT**)
- Unique account number generation
- Real-time balance tracking

### Transactions
- **Deposit**, **Withdraw**, and **Transfer** operations
- Transaction history with status tracking (**PENDING**, **SUCCESS**, **FAILED**)
- Transaction type categorization (**DEPOSIT**, **WITHDRAW**, **TRANSFER**)

### Technical Highlights
- **PostgreSQL** database with Docker support
- JPA/Hibernate ORM with proper entity relationships
- DTO pattern for clean API responses
- Global exception handling
- Comprehensive validation
- Lombok for boilerplate reduction
- Logging and security utilities

---

## 🛠️ Technology Stack

| Layer              | Technology                                      |
|--------------------|-------------------------------------------------|
| **Backend**        | Java 17, Spring Boot 4.1.0                     |
| **Framework**      | Spring MVC (`spring-boot-starter-webmvc`), Spring Data JPA, Spring Security |
| **Database**       | PostgreSQL                                      |
| **Security**       | JWT (jjwt 0.12.6), BCrypt                       |
| **Build Tool**     | Maven                                           |
| **ORM**            | Hibernate                                       |
| **Validation**     | Jakarta Validation                              |
| **Boilerplate**    | Lombok                                          |
| **DevOps**         | Docker Compose                                  |
| **Testing**        | JUnit 5 (scaffolded — see Testing section below) |

> **Note:** This project runs on **Spring Boot 4.1.0**, which renamed several starters (e.g. `spring-boot-starter-web` → `spring-boot-starter-webmvc`). If you're used to Spring Boot 3.x conventions, some starter names and package boundaries will look different.

---

## 📁 Project Structure

```bash
src/main/java/com/banking/
├── BankingSystemApplication.java
├── config/
│   └── SecurityConfig.java
├── controller/
│   ├── UserController.java
│   ├── AccountController.java
│   └── TransactionController.java
├── dto/
│   ├── account/          # Create/Deposit/Withdraw/Transfer requests, AccountResponse
│   ├── auth/             # Login/Register requests, LoginResponse, ErrorResponse
│   ├── transaction/       # TransactionResponse
│   └── user/              # Profile & password update requests, UserResponse
├── exception/            # GlobalExceptionHandler + custom exceptions
├── model/
│   ├── User.java
│   ├── Account.java
│   ├── AccountType.java
│   ├── Transaction.java
│   ├── TransactionType.java
│   └── TransactionStatus.java
├── repository/           # JPA Repositories
├── security/
│   ├── JwtAuthenticationFilter.java
│   ├── JwtService.java
│   └── SecurityUtils.java
└── service/              # Business logic interfaces + impl/ implementations
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 17+**
- **Maven 3.6+**
- **Docker** (recommended for PostgreSQL)

### 1. Clone the Repository

```bash
git clone https://github.com/Muhaimin-Mukammel/Banking-System.git
cd Banking-System
```

### 2. Start Database (Recommended)

```bash
docker-compose up -d
```

This starts PostgreSQL on port **5332**.

### 3. Configure Application

Edit `src/main/resources/application.properties` if needed:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5332/banking_system
spring.datasource.username=postgres
spring.datasource.password=postgres

jwt.secret=Y2hhbmdlLXRoaXMtc2VjcmV0LWtleS1mb3ItcHJvZHVjdGlvbi11c2Utb25seSE=
jwt.expiration-ms=86400000   # 24 hours
```

> **Security Note**: Change the JWT secret in production!
>
> **Known issue**: the checked-in `application.properties` currently has `jwt.expiration-ms=864000004`, which is ~10 days rather than the intended 24 hours. Fix this before relying on token expiry.
>
> Also note `spring.jpa.hibernate.ddl-auto=create-drop` is set, which **wipes and recreates the schema on every restart** — fine for local dev, not for anything you want to persist.

### 4. Build and Run

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will start at **http://localhost:8080**

---

## 📡 API Endpoints

### Authentication & User

| Method | Endpoint              | Description              | Access    |
|--------|-----------------------|--------------------------|-----------|
| POST   | `/api/user/register`  | Register new user        | Public    |
| POST   | `/api/user/login`     | Login & get JWT token    | Public    |
| GET    | `/api/user/me`        | Get current user profile | Protected |
| PUT    | `/api/user/me`        | Update profile           | Protected |
| PUT    | `/api/user/password`  | Change password          | Protected |

### Accounts

| Method | Endpoint                              | Description            |
|--------|----------------------------------------|-------------------------|
| POST   | `/api/account/create`                 | Create new account      |
| GET    | `/api/account/{accountId}`            | Get account by ID       |
| POST   | `/api/account/{accountId}/deposit`    | Deposit money           |
| POST   | `/api/account/{accountId}/withdraw`   | Withdraw money          |
| POST   | `/api/account/{accountId}/transfer`   | Transfer to another account |

### Transactions

| Method | Endpoint                                | Description                    |
|--------|-------------------------------------------|---------------------------------|
| GET    | `/api/transaction`                       | Get transaction history        |
| GET    | `/api/transaction/{transactionId}`       | Get a single transaction       |
| GET    | `/api/transaction/account/{accountId}`   | Get transactions for an account |

> Full Swagger/OpenAPI documentation coming soon.

---

## 🔐 Default Setup & Demo

After starting the app:
1. Register a user via `/api/user/register`
2. Login to receive JWT token
3. Use the token in `Authorization: Bearer <token>` header for protected routes

---

## 🧪 Testing

Test scaffolding exists under `src/test/java` for controllers, services, and security classes (JUnit 5), but most are currently empty placeholders — actual test logic is not yet written. `BankingSystemApplicationTests` (context load test) is the only test that runs meaningfully today. Filling in real unit/integration tests, including Mockito-based mocking, is on the roadmap.

Run tests with:

```bash
mvn test
```

---

## 🐳 Docker

```bash
# Start only database
docker-compose up -d postgres

# Stop services
docker-compose down
```

---

## 📋 Future Enhancements (Roadmap)

- [ ] Write real unit/integration tests (currently stubbed)
- [ ] Admin panel & user roles (ROLE_USER, ROLE_ADMIN)
- [ ] Loan management module
- [ ] Email notifications
- [ ] Transaction limits & fraud detection
- [ ] Swagger UI documentation
- [ ] React/Vue.js frontend
- [ ] CI/CD pipeline
- [ ] Docker multi-stage build

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the project
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the **Apache License 2.0** - see the [LICENSE](LICENSE) file for details.

---

## 👨‍💻 Author

**Muhaimin Mukammel**

---
