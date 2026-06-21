# Payments API

A demo REST API for processing payments via M-Pesa and Card, built with Spring Boot 4 and Java 21.

## Features

- JWT-based authentication
- Payment initiation via M-Pesa (STK push, async) or Card (synchronous)
- Idempotent payment requests
- Webhook simulation for payment callbacks
- SMS notifications via an in-memory queue backed by a mocked Twilio provider, with automatic retry and circuit breaking
- Transaction history
- H2 in-memory database

## Tech Stack

- **Java 21** / **Spring Boot 4**
- **Spring Security** + **JJWT** for auth
- **Spring Data JPA** + **H2** for persistence
- **Twilio** for SMS
- **Resilience4j** for retries and circuit breaking
- **Docker** for containerisation
- **Railway** for deployment

---

## Getting Started

### Prerequisites

- Java 21
- Maven (or use the included `./mvnw` wrapper)

### Environment Variables

| Variable | Required | Description |
|---|---|---|
| `JWT_SECRET` | Yes | Secret key for signing JWTs (min 32 chars) |
| `TWILIO_ACCOUNT_SID` | No | Twilio account SID (defaults to `mock` — SMS is simulated locally) |
| `TWILIO_AUTH_TOKEN` | No | Twilio auth token (defaults to `mock`) |
| `TWILIO_FROM_NUMBER` | No | Twilio sender number (default: `+254714851234`) |
| `ALLOWED_ORIGINS` | No | CORS allowed origins (default: `http://localhost:5173`) |
| `AUTH_USERNAME` | Prod only | Login username (overrides the hardcoded default) |
| `AUTH_PASSWORD` | Prod only | Login password (overrides the hardcoded default) |

Copy `.env.example` to `.env` and fill in the values, or export them directly.

### Run locally

```bash
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`.

### Run with Docker

```bash
docker compose up --build
```

---

## API Reference

All endpoints (except login and health) require a `Bearer` token in the `Authorization` header.

### Auth

#### Login
```
POST /api/v1/auth/login
```
```json
{
  "username": "johndoe",
  "password": "johndoe123"
}
```
**Response**
```json
{
  "status": 200,
  "description": "Success",
  "data": {
    "token": "<jwt>",
    "username": "johndoe"
  }
}
```

---

### Payments

#### Initiate a payment
```
POST /api/v1/payments
Authorization: Bearer <token>
Idempotency-Key: <optional-unique-key>
```
```json
{
  "amount": 500.00,
  "phoneNumber": "+254712345678",
  "paymentMethod": "MPESA"
}
```
`paymentMethod` accepts `MPESA` or `CARD`.

**M-Pesa** returns `PROCESSING` immediately — the STK push is dispatched asynchronously. Poll `GET /api/v1/payments/{id}` until the status reaches `COMPLETED` or `FAILED`.

**Card** returns a final status (`COMPLETED` or `FAILED`) synchronously in the same response.

#### Get payment status
```
GET /api/v1/payments/{id}
Authorization: Bearer <token>
```

#### Get transaction history
```
GET /api/v1/payments
Authorization: Bearer <token>
```

#### Simulate a webhook callback
```
POST /api/v1/payments/{id}/webhook
```
```json
{
  "status": "COMPLETED",
  "resultCode": "0",
  "resultDesc": "Success",
  "mpesaReceiptNumber": "QHJ9KL123X"
}
```
`status` can be `INITIATED`, `PROCESSING`, `STK_PUSH_SENT`, `COMPLETED`, or `FAILED`.

---

### Health

```
GET /api/v1/health
```

---

## Transaction Statuses

| Status | Description |
|---|---|
| `INITIATED` | Payment created |
| `PROCESSING` | Sent to payment gateway |
| `STK_PUSH_SENT` | M-Pesa STK push delivered to phone |
| `COMPLETED` | Payment confirmed |
| `FAILED` | Payment failed |

---

## CI/CD

GitHub Actions runs tests on every push and pull request to `master`. On a successful push, the app is automatically deployed to Railway.

Required GitHub secrets: `RAILWAY_TOKEN`, `RAILWAY_PROJECT_ID`.

---

## H2 Console

Available at `http://localhost:8080/h2-console` in the default profile (disabled in prod).

- **JDBC URL:** `jdbc:h2:mem:payments_schema`
- **Username:** `sa`
- **Password:** *(empty)*
