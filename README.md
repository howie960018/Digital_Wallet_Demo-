# Digital Wallet API

A RESTful digital wallet backend service built with Spring Boot 4, providing user registration, authentication, account management, and financial transaction capabilities including transfers, top-ups, and withdrawals.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Features](#features)
- [Architecture & Design Decisions](#architecture--design-decisions)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Endpoints](#api-endpoints)
- [Testing](#testing)
- [Security](#security)

---

## Tech Stack

| Category        | Technology                        |
|-----------------|-----------------------------------|
| Language        | Java 17                           |
| Framework       | Spring Boot 4.0.3                 |
| Web             | Spring MVC                        |
| Persistence     | Spring Data JPA / Hibernate       |
| Database        | PostgreSQL                        |
| Security        | Spring Security + JWT (JJWT 0.12) |
| API Docs        | SpringDoc OpenAPI (Swagger UI)    |
| Build Tool      | Maven                             |
| Testing         | JUnit 5, Mockito, Spring MVC Test |
| Utilities       | Lombok, Snowflake ID Generator    |

---

## Features

- User registration and login with JWT-based authentication
- Account management: query accounts, freeze and unfreeze accounts
- Fund transfer between accounts with idempotency key support
- Top-up (deposit) to wallet accounts
- Withdrawal from wallet accounts
- Transaction history with pagination
- Global exception handling with consistent API response structure
- Snowflake algorithm for distributed unique transaction ID generation

---

## Architecture & Design Decisions

### Concurrency Control
- **Pessimistic Locking** (`SELECT ... FOR UPDATE`) for balance-critical operations (transfer, withdrawal)
- **Optimistic Locking** (`@Version`) as a safety net on Account entity
- **Lock Ordering** by account ID to prevent deadlocks in bidirectional transfers

### Idempotency
- Every transaction requires a unique `idempotencyKey`
- Duplicate requests return the original transaction result without re-processing
- Enforced at both application logic and database UNIQUE constraint levels

### Financial Precision
- All monetary values use `BigDecimal` (Java) and `DECIMAL(19,4)` (PostgreSQL)
- Avoids floating-point arithmetic errors (e.g., `0.1 + 0.2 != 0.3`)

### Transaction Safety
- Transfer operations (debit + credit + transaction log) are wrapped in a single `@Transactional`
- Registration + account creation are atomic — no orphan users without wallets

### ID Generation
- Custom Snowflake ID generator for globally unique, time-ordered transaction numbers
- Supports up to 4,096 IDs per millisecond per node

### Authorization
- JWT Authentication (stateless, no server-side sessions)
- Account ownership verification — users can only operate on their own accounts
- Frozen accounts block outgoing transfers and withdrawals but allow incoming funds

---

## Project Structure

```
src/
├── main/
│   ├── java/com/wallet/digitalwallet/
│   │   ├── config/           # Security, JWT filter, Swagger, Data initializer
│   │   ├── controller/       # REST controllers (Auth, User, Transfer, TopUp, Withdraw, Transaction)
│   │   ├── dto/              # Request and response DTOs
│   │   ├── entity/           # JPA entities (User, Account, Transaction)
│   │   ├── exception/        # BusinessException and GlobalExceptionHandler
│   │   ├── repository/       # Spring Data JPA repositories
│   │   ├── service/          # Business logic (AuthService, UserService, TransferService)
│   │   └── util/             # JwtUtil, SecurityUtil, SnowflakeIdGenerator
│   └── resources/
│       ├── application.yml
│       └── application.yml.example
└── test/
    └── java/com/wallet/digitalwallet/
        ├── controller/       # Controller layer unit tests (WebMvcTest)
        ├── service/          # Service layer unit tests (Mockito)
        └── util/             # Utility unit tests
```

---

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.8 or higher
- PostgreSQL 13 or higher

### Database Setup

Create a PostgreSQL database:

```sql
CREATE DATABASE digital_wallet;
```

### Configuration

Copy the example configuration file and edit it with your own settings:

```bash
cp src/main/resources/application.yml.example src/main/resources/application.yml
```

Edit `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/digital_wallet
    username: your_db_username
    password: your_db_password
  jpa:
    hibernate:
      ddl-auto: create   # Use 'update' after first run

jwt:
  secret: your_256bit_hex_secret_key
  expiration: 86400000   # 24 hours in milliseconds
```

### Run the Application

```bash
./mvnw spring-boot:run
```

The application starts on `http://localhost:8080` by default.

### Swagger UI

Once the application is running, access the API documentation at:

```
http://localhost:8080/swagger-ui/index.html
```

---

## API Endpoints

### Authentication

| Method | Endpoint              | Description         | Auth Required |
|--------|-----------------------|---------------------|---------------|
| POST   | /api/auth/login       | Login and get JWT   | No            |

### User & Account

| Method | Endpoint                          | Description                     | Auth Required |
|--------|-----------------------------------|---------------------------------|---------------|
| POST   | /api/users/register               | Register a new user             | No            |
| GET    | /api/users/{userId}/accounts      | Get accounts for a user         | Yes           |
| POST   | /api/users/accounts/freeze        | Freeze an account               | Yes           |
| POST   | /api/users/accounts/unfreeze      | Unfreeze an account             | Yes           |

### Transactions

| Method | Endpoint                          | Description                     | Auth Required |
|--------|-----------------------------------|---------------------------------|---------------|
| POST   | /api/transfer                     | Transfer funds between accounts | Yes           |
| POST   | /api/topup                        | Top up (deposit) to an account  | Yes           |
| POST   | /api/withdraw                     | Withdraw from an account        | Yes           |
| GET    | /api/transactions/{accountId}     | Get paginated transaction history | Yes         |

### Request / Response Format

All responses follow a unified structure:

```json
{
  "code": "SUCCESS",
  "message": "Operation completed",
  "data": { }
}
```

On error:

```json
{
  "code": "INSUFFICIENT_BALANCE",
  "message": "Insufficient balance",
  "data": null
}
```

### Authentication Header

Include the JWT token in the Authorization header for protected endpoints:

```
Authorization: Bearer <token>
```

---

## Configuration

| Property                        | Description                                    | Default        |
|---------------------------------|------------------------------------------------|----------------|
| `spring.datasource.url`         | PostgreSQL JDBC URL                            | -              |
| `spring.datasource.username`    | Database username                              | -              |
| `spring.datasource.password`    | Database password                              | -              |
| `spring.jpa.hibernate.ddl-auto` | Schema generation strategy                     | `create`       |
| `jwt.secret`                    | HMAC-SHA256 signing key (256-bit hex string)   | -              |
| `jwt.expiration`                | Token expiration time in milliseconds          | `86400000`     |

---

## Testing

Run all tests:

```bash
./mvnw test
```

### Test Coverage

| Test Class              | Type            | Cases |
|-------------------------|-----------------|-------|
| AuthControllerTest      | Controller      | 3     |
| TopUpControllerTest     | Controller      | 3     |
| TransactionControllerTest | Controller    | 3     |
| TransferControllerTest  | Controller      | 4     |
| UserControllerTest      | Controller      | 7     |
| WithdrawControllerTest  | Controller      | 4     |
| AuthServiceTest         | Service         | 3     |
| TransferServiceTest     | Service         | 13    |
| UserServiceTest         | Service         | 11    |
| JwtUtilTest             | Utility         | 7     |
| SnowflakeIdGeneratorTest | Utility        | 7     |
| DigitalWalletApplicationTests | Integration | 1   |
| **Total**               |                 | **66** |

### Test Strategy

- **Controller tests** use `@WebMvcTest` with `MockMvc` to test HTTP layer in isolation, with all dependencies mocked.
- **Service tests** use `@ExtendWith(MockitoExtension.class)` with `@InjectMocks` to test business logic in isolation.
- **Utility tests** cover JWT token generation/validation and Snowflake ID uniqueness and monotonicity.
- Idempotency key behavior is explicitly tested for transfer, top-up, and withdrawal operations.

---

## Security

- All endpoints except `/api/auth/login` and `/api/users/register` require a valid JWT token.
- JWT tokens are signed with HMAC-SHA256 and expire after the configured duration.
- Account operations verify that the requesting user owns the target account, preventing unauthorized access.
- Account freeze/unfreeze blocks all fund movements on the affected account.
- Passwords are encoded using BCrypt before storage.

---

## License

This project is for educational and demonstration purposes.
