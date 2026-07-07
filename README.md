# Banking System

A modern, secure **Spring Boot** banking application built with best practices. It supports user registration, JWT authentication, account management, transactions, rate limiting, and concurrency handling.

> **Version:** 1.5
> **Latest Updates:** Rate limiting (Bucket4j + Caffeine), pessimistic-locking concurrency control, an expanded test suite (61 tests across 11 files), and Flyway added as a dependency (migration scripts aren't written yet — see **Known Issues** below).

*Audited against commit `11d032a`.*

---

## ✨ Features

### Authentication & Security
- **JWT-based stateless authentication** with custom `JwtAuthenticationFilter`
- Secure password hashing with **BCrypt**
- Protected endpoints with role-ready configuration (expansion planned — no roles enforced yet, just authenticated/not)
- **Rate Limiting** on every `/api/**` request using Bucket4j + Caffeine, with tighter custom limits on auth and money-movement endpoints
- Concurrency handling for transaction safety (see Account Management below)

### User Management
- User registration and login
- Profile management (`GET /api/user/me`, `PUT /api/user/me`, `PUT /api/user/password`)
- Multiple accounts per user

### Account Management
- Create different account types (**SAVINGS**, **CURRENT**)
- Unique account number generation
- Real-time balance tracking with **pessimistic row-level locking** (`SELECT ... FOR UPDATE`, 5s timeout) — not optimistic locking; there is no `@Version` field on `Account`. Transfers lock both accounts in a fixed order (lower ID first) specifically to avoid deadlocks.

### Transactions
- **Deposit**, **Withdraw**, and **Transfer** operations
- Transaction history with status tracking (**PENDING**, **SUCCESS**, **FAILED**)
- Transaction type categorization (**DEPOSIT**, **WITHDRAW**, **TRANSFER**)
- Concurrency-safe balance updates

### Technical Highlights
- **PostgreSQL** with Docker; **Flyway** is on the classpath but has no migration scripts yet (see **Known Issues** below)
- JPA/Hibernate ORM with proper entity relationships and locking
- DTO pattern (Java records) for clean, secure API responses
- Global exception handling — 14 exception types (custom + framework) mapped to precise HTTP statuses via `@RestControllerAdvice`, plus a `Retry-After` header on 429 responses
- Comprehensive Jakarta Bean Validation
- Lombok for reduced boilerplate
- Logging, security utilities, and custom annotations
- 61 JUnit 5 + Mockito tests across controllers, services, and security

---

## 🛠️ Technology Stack

| Layer              | Technology                                      |
|--------------------|--------------------------------------------------|
| **Backend**        | Java 17, Spring Boot 3.5.16                      |
| **Framework**      | Spring Web, Spring Data JPA, Spring Security     |
| **Database**       | PostgreSQL + Flyway*                             |
| **Security**       | JWT (jjwt 0.12.6), BCrypt, Rate Limiting (Bucket4j 8.10.1) |
| **Build Tool**     | Maven                                            |
| **ORM**            | Hibernate                                        |
| **Validation**     | Jakarta Validation                               |
| **Boilerplate**    | Lombok                                           |
| **DevOps**         | Docker Compose (Postgres only)                   |
| **Testing**        | JUnit 5, Spring Security Test, Mockito           |
| **Caching/Rate**   | Caffeine 3.1.8 + Bucket4j 8.10.1                 |

*Flyway dependency is present in `pom.xml`; no migration scripts exist yet, so it isn't managing the schema today.

**Note:** The parent POM is `spring-boot-starter-parent:3.5.16`. Flyway's own resolved version comes from that parent's dependency-management BOM rather than a version pinned directly in `pom.xml`.

---

## 📁 Project Structure

```bash
src/main/java/com/banking/
├── BankingSystemApplication.java
├── annotation/
│   └── ratelimit/
│       ├── RateLimit.java              # @RateLimit annotation (capacity/refill/window)
│       ├── RateLimitInterceptor.java   # runs on every /api/** call
│       └── RateLimitService.java       # Caffeine-backed Bucket4j cache
├── config/
│   ├── SecurityConfig.java             # JWT filter chain, BCrypt bean, stateless sessions
│   └── WebConfig.java                  # registers the rate-limit interceptor
├── controller/
│   ├── UserController.java
│   ├── AccountController.java
│   └── TransactionController.java
├── dto/
│   ├── account/      # Create/Deposit/Withdraw/Transfer requests + AccountResponse
│   ├── auth/         # Login/Register requests + responses, ErrorResponse
│   ├── transaction/  # TransactionResponse only (requests live under dto/account/)
│   └── user/         # Profile & password DTOs
├── exception/        # GlobalExceptionHandler + 10 custom exceptions
├── model/            # User, Account, Transaction + 3 enums (AccountType, TransactionType, TransactionStatus)
├── repository/       # JPA repositories (AccountRepository holds the pessimistic-lock query)
├── security/         # JwtAuthenticationFilter, JwtService, SecurityUtils
├── service/          # Interfaces + impl/ (business logic)
└── ... (tests under src/test — see Testing section)
```

---

## 🚀 Getting Started

### Prerequisites
- **Java 17+** (see the virtual-threads note below if you want `spring.threads.virtual.enabled` to actually do anything)
- **Maven 3.6+**
- **Docker** (recommended for PostgreSQL)

### 1. Clone the Repository
```bash
git clone https://github.com/Muhaimin-Mukammel/Banking-System.git
cd Banking-System
```

### 2. Start Database
```bash
docker-compose up -d
```
This starts PostgreSQL on port **5332** (mapped to the container's 5432). On Docker Compose V2, use `docker compose` (no hyphen) instead.

### 3. Configure Application
`src/main/resources/application.properties` currently contains:

```properties
spring.application.name=Banking_System

# JWT settings
# jwt.secret must be a Base64-encoded key of at least 256 bits (32 bytes) for HS256.
# Generate your own for production, e.g.: openssl rand -base64 32
jwt.secret=Y2hhbmdlLXRoaXMtc2VjcmV0LWtleS1mb3ItcHJvZHVjdGlvbi11c2Utb25seSE=
jwt.expiration-ms=864000004

spring.datasource.url=jdbc:postgresql://localhost:5332/banking_system
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.show-sql=true

spring.threads.virtual.enabled=true
```

> **Security Notes**:
> - Always replace `jwt.secret` before any real deployment — the committed value is a placeholder.
> - `jwt.expiration-ms=864000004` works out to roughly **10 days**, not 24 hours. That looks like a stray extra digit (24h would be `86400000`) — confirm which one was actually intended.
> - `spring.jpa.hibernate.ddl-auto=create-drop` is what's actually building the schema right now. Flyway is on the classpath but has no migrations to run, so it isn't doing anything yet — which also means **the database is dropped and rebuilt every time the app restarts.** Don't point this at data you want to keep.
> - `spring.threads.virtual.enabled=true` only has an effect on **JDK 21+**. On the JDK 17 named in Prerequisites, Spring Boot silently ignores it and falls back to platform threads — no error, just no virtual threads either.

### 4. Build and Run
```bash
# Build
mvn clean install

# Run
mvn spring-boot:run
```

The application starts at **http://localhost:8080**.

---

## 📡 API Endpoints

Rate limiting runs globally on every `/api/**` request via a `HandlerInterceptor` (default: 60 requests/minute, keyed by the authenticated username where available, otherwise by caller IP). The endpoints below carry tighter, custom limits.

### Authentication & User
| Method | Endpoint              | Description               | Access    | Rate Limit          |
|--------|-----------------------|----------------------------|-----------|----------------------|
| POST   | `/api/user/register`  | Register new user          | Public    | 5/min (by IP)        |
| POST   | `/api/user/login`     | Login & get JWT             | Public    | 10/min (by IP)       |
| GET    | `/api/user/me`        | Get current user profile   | Protected | 60/min (default)     |
| PUT    | `/api/user/me`        | Update profile              | Protected | 60/min (default)     |
| PUT    | `/api/user/password`  | Change password            | Protected | 30/min               |

### Accounts
| Method | Endpoint                              | Description                  | Rate Limit        |
|--------|----------------------------------------|-------------------------------|--------------------|
| POST   | `/api/account/create`                 | Create new account            | 60/min (default)  |
| GET    | `/api/account/{accountId}`            | Get account by ID             | 60/min (default)  |
| POST   | `/api/account/{accountId}/deposit`    | Deposit                        | 20/min             |
| POST   | `/api/account/{accountId}/withdraw`   | Withdraw                       | 20/min             |
| POST   | `/api/account/{accountId}/transfer`   | Transfer to another account    | 20/min             |

### Transactions
| Method | Endpoint                                | Description                                                                 |
|--------|-------------------------------------------|-------------------------------------------------------------------------------|
| GET    | `/api/transaction`                      | ⚠️ Returns **every transaction for every user** — not scoped to the caller. See **Known Issues** below. |
| GET    | `/api/transaction/{transactionId}`      | Get a single transaction (sender or receiver only)                             |
| GET    | `/api/transaction/account/{accountId}`  | Get transactions for an account (owner only)                                  |

*All three endpoints above use the default 60/min rate limit — no custom overrides.*

Requests that exceed their bucket get **HTTP 429** with a `Retry-After` header carrying the actual wait time.

**Swagger/OpenAPI** support can be added easily (planned).

---

## 🔐 Default Setup & Demo
1. Register a user via `/api/user/register`
2. Login to receive JWT token
3. Use `Authorization: Bearer <token>` header for protected routes

---

## 🧪 Testing
- **61 test methods across 11 files** (~1,293 lines total): JUnit 5 + Mockito for service and security classes, `@WebMvcTest` + MockMvc for controllers.
- Coverage includes all three controllers, all three service implementations (including lock-timeout and insufficient-balance edge cases in `AccountServiceImplTest`), and the security layer (`JwtService`, `JwtAuthenticationFilter`, `SecurityConfig`, `SecurityUtils`).
- A context-loading smoke test (`BankingSystemApplicationTests`) is also present.
- Run with:
  ```bash
  mvn test
  ```

---

## 🐳 Docker
```bash
# Database only
docker-compose up -d postgres

# Full stop
docker-compose down
```

---

## ⚠️ Known Issues / Current Limitations
Worth addressing before calling this production-grade:

- **`GET /api/transaction` isn't scoped to the caller.** It calls `transactionRepository.findAll()` directly, so any authenticated user can see every transaction in the system. Its sibling endpoints (`/api/transaction/{id}`, `/api/transaction/account/{accountId}`) do check ownership — this one doesn't.
- **Flyway isn't migrating anything yet.** `flyway-core` and `flyway-database-postgresql` are dependencies, but there's no `src/main/resources/db/migration` folder or any migration scripts anywhere in the repo. Hibernate's `ddl-auto=create-drop` is still what builds the schema, so data doesn't survive a restart.
- **`jwt.expiration-ms` is probably a typo.** The current value (`864000004`) works out to ~10 days; if 24-hour tokens were intended, it should be `86400000`.
- **Virtual threads need JDK 21+.** `spring.threads.virtual.enabled=true` is set, but the documented minimum JDK (17) doesn't support virtual threads at all — the setting is silently inactive unless you're actually running on 21+.
- `AccountLockedException` has a registered handler (`HTTP 423`) but is never thrown anywhere in the current code — harmless, but dead code.

---

## 📋 Future Enhancements (Roadmap)
- [ ] Scope `GET /api/transaction` to the authenticated user
- [ ] Write actual Flyway migration scripts (dependency is already in place)
- [x] ~~Comprehensive unit & integration tests~~ — well underway: 61 tests as of v1.5
- [ ] Full role-based access (ROLE_USER, ROLE_ADMIN)
- [ ] Admin panel
- [ ] Loan management
- [ ] Email notifications & notifications service
- [ ] Advanced fraud detection & transaction limits
- [ ] Swagger UI + OpenAPI docs
- [ ] Frontend (React/Vue)
- [ ] CI/CD pipeline
- [ ] Production-ready Docker multi-stage builds
- [ ] Audit logging

---

## 🤝 Contributing
Contributions are welcome! Feel free to submit a Pull Request.

1. Fork the project
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License
This project is licensed under the **Apache License 2.0** — see the [LICENSE](LICENSE) file for details.

---

## 👨‍💻 Author
**Muhaimin Mukammel**

---

**Topics:** Java, Spring Boot, Spring Security, JPA, PostgreSQL, JWT, Docker, Rate Limiting, Banking System