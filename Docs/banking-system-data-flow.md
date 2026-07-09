# Banking System - Full Data Flow 

This document outlines the complete, verified data flow of the Spring Boot Banking System from the repository: [Muhaimin-Mukammel/Banking-System](https://github.com/Muhaimin-Mukammel/Banking-System).

## High-Level Architecture

The application is a **RESTful Spring Boot** backend with:
- **Layered Architecture**: Controller → Service → Repository → Database (PostgreSQL)
- **Security**: JWT + Rate Limiting (Bucket4j + Caffeine) + BCrypt
- **Concurrency**: Pessimistic locking (`SELECT ... FOR UPDATE` with timeout) + Optimistic locking (`@Version`)
- **Caching**: Spring Cache abstraction + Redis (recent addition)
- **ORM**: JPA/Hibernate with entity relationships + Flyway migrations
- **Validation**: Jakarta Bean Validation + Global Exception Handling
- **Deployment**: Maven + Docker Compose (Postgres + Redis)

## Request-Response Data Flow (Typical HTTP Request)

```
HTTP Request (POST/GET/PUT)
        │
        ▼
  RateLimitInterceptor (preHandle - /api/**)
        │
        ├──► Resolve identity (authenticated user email preferred, fallback to IP)
        │
        ├──► Bucket4j check (Caffeine cache, method-level @RateLimit or default 60/min)
        │     ├── Success → Add Rate-Limit-Remaining header → Proceed
        │     └── Exceeded → RateLimitExceededException (429 + Retry-After)
        │
        ▼
  JwtAuthenticationFilter (OncePerRequestFilter)
        │
        ├──► Extract Bearer token (skip Swagger)
        │
        ├──► JwtService.extractEmail() → Validate → Set minimal SecurityContext
        │
        ▼
  SecurityConfig (authorizeHttpRequests)
        │
        ├──► Public: /register, /login, Swagger
        │
        └──► Protected: /api/** → Require valid JWT
        │
        ▼
  Controller Layer (UserController, AccountController, TransactionController)
        │
        ├──► @Valid DTO binding + Jakarta Validation
        │
        ├──► Call Service layer
        │
        ▼
  Service Layer (@Transactional)
        │
        ├──► Business logic + ownership checks (SecurityUtils.getCurrentUserEmail())
        │
        ├──► Repository calls (with pessimistic locking for money ops)
        │
        ├──► Caching (@Cacheable on reads)
        │
        └──► Custom exceptions → GlobalExceptionHandler
        │
        ▼
  Repository Layer (JPA)
        │
        ├──► Queries + @Lock(PESSIMISTIC_WRITE) for critical paths
        │
        ▼
  Database (PostgreSQL via Docker + Flyway)
        │
        ├──► Entities: User, Account (@Version), Transaction
        │
        └──► ACID transactions (balance + history)
        │
        ▼
  Response (DTO records) + Headers
        │
        ▼
  HTTP Response (JSON)
```

## Detailed Component Flows

### 1. Authentication & User Management Flow

```
Client Request
   ├──► /api/user/register (POST, @RateLimit 5/min IP)
   │     └──► UserController.register()
   │           └──► UserServiceImpl.register() [@Transactional]
   │                 ├──► Email uniqueness check
   │                 ├──► BCrypt hash password
   │                 └──► UserRepository.save() → UserResponse
   │
   ├──► /api/user/login (POST, @RateLimit 10/min IP)
   │     └──► UserServiceImpl.login()
   │           ├──► Find user + password match
   │           └──► JwtService.generateToken() → LoginResponse (JWT)
   │
   └──► Protected routes (/me, update profile, change password)
         └──► SecurityUtils.getCurrentUserEmail() + UserRepository.findByEmail()
               (with @Cacheable on getCurrentUser)
```

### 2. Account Management Flow

```
POST /api/account/create
   └──► AccountController.createAccount()
         └──► AccountServiceImpl.create() [@Transactional]
               ├──► Get current User
               ├──► Generate unique 10-digit account number (loop + exists check)
               └──► AccountRepository.save() → AccountResponse

GET /api/account/{id}
   └──► AccountServiceImpl.getAccountById() [@Cacheable]
         └──► Ownership check + return AccountResponse

Money Operations (deposit/withdraw/transfer - @RateLimit 20/min):
   ├──► deposit / withdraw:
   │     └──► Pessimistic lock (findByIdForUpdate, 5s timeout)
   │           ├──► Balance update (+/- amount)
   │           └──► Create Transaction record (DEPOSIT/WITHDRAW, SUCCESS)
   │
   └──► transfer:
         ├──► Lock both accounts in ascending ID order (deadlock prevention)
         ├──► Ownership + sufficient balance checks
         ├──► Atomic balance updates on sender & receiver
         └──► Create TRANSFER Transaction record
```

### 3. Transaction Query Flow (Updated)

```
GET /api/transaction/*
   └──► TransactionController → TransactionServiceImpl
         ├──► getAllTransactions() [@Cacheable]
         │     └──► Scoped to current user (sender OR receiver email)
         ├──► getTransactionById() [@Cacheable]
         │     └──► Ownership check (sender or receiver)
         └──► getTransactionForSpecificAccount()
               └──► Ownership + account-specific query (ordered by time)
```

### 4. Security & Cross-Cutting Concerns

```
Every /api/** Request
   ├──► RateLimitInterceptor (HandlerInterceptor via WebConfig)
   │     └──► Annotation-driven + Bucket4j (per-user/method key)
   │
   ├──► JwtAuthenticationFilter
   │
   ├──► GlobalExceptionHandler (@RestControllerAdvice)
   │     └──► Precise mapping (validation, locks, rate limits, business rules, etc.)
   │          + Retry-After header for 429
   │
   └──► @Transactional + Caching + Logging
```

## Entity Relationships & Persistence

- **User** ↔ **Account** (OneToMany, LAZY)
- **Account** ↔ **Transaction** (Sender/Receiver OneToMany)
- **Account** has `@Version` for optimistic locking
- Enums: `AccountType` (SAVINGS, CURRENT), `TransactionType`, `TransactionStatus`
- Balance uses `BigDecimal`
- Flyway migrations (V1__users, V2__accounts, V3__transactions)

## Startup & Configuration Flow

```
Spring Boot Startup
   │
   ├──► application.properties + application-dev.properties
   │     (Redis, Postgres 5332, JWT, Swagger, virtual threads, Flyway)
   │
   ├──► SecurityConfig → PasswordEncoder + FilterChain
   ├──► WebConfig → Register RateLimitInterceptor
   ├──► JPA + Flyway + Redis auto-config
   └──► Docker Compose (postgres + redis services)
```

## Key Design Patterns & Best Practices

- **DTO Pattern**: Immutable Java records for requests/responses
- **Service Layer**: All business logic, transactions, and ownership enforcement
- **Repository**: Custom locking queries (`findByIdForUpdate`)
- **Exception Handling**: Centralized with rich error responses
- **Rate Limiting**: Custom annotation + Bucket4j
- **Caching**: `@Cacheable` on read-heavy operations (Redis-backed)
- **Security**: Stateless JWT + ownership checks everywhere + rate limiting

## Notes / Current Status

- All transaction queries are properly scoped to the authenticated user.
- Flyway migrations are active (schema managed; `ddl-auto=validate` in dev).
- Recent Redis integration improves caching and supports rate limiting scalability.
- Concurrency for financial operations is robust (pessimistic + ordering + timeout handling).
- Ready for further enhancements (RBAC, audit logs, frontend, etc.).

**Data Flow Summary**: Incoming requests are secured and rate-limited first, then authenticated, validated, and processed transactionally with strong concurrency controls. All operations maintain ownership integrity and generate audit trails via Transaction entities. The architecture is clean, secure, and production-minded for a backend banking system.
