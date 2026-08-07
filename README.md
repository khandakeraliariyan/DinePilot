# DinePilot

Microservices-based restaurant management system built with Spring Boot, Spring Cloud, MongoDB, and RabbitMQ.

## Status

**Phase 1 — Foundation** is complete: Eureka service registry, API Gateway, and a shared `common` library. Business services (User, Restaurant, Reservation, Order, Billing) are not implemented yet.

## Modules

| Module | Description | Port |
|---|---|---|
| `common` | Shared library: `ApiResponse` envelope, `BaseEntity`, `Role` enum, exceptions, global exception handler | — |
| `eureka-server` | Service registry / discovery | 8761 |
| `api-gateway` | Edge routing, CORS, request logging | 8080 |

## Prerequisites

- JDK 17
- Docker + Docker Compose
- No local Maven install needed — use the bundled wrapper (`./mvnw` / `mvnw.cmd`)

## Running everything with Docker Compose

```bash
cp .env.example .env
docker compose up -d --build
```

This starts MongoDB, RabbitMQ, Eureka Server, and the API Gateway.

- Eureka dashboard: http://localhost:8761
- API Gateway health: http://localhost:8080/actuator/health
- RabbitMQ management UI: http://localhost:15672 (user/pass from `.env`)

Stop everything with `docker compose down` (add `-v` to also drop data volumes).

## Running a service locally (without Docker)

```bash
./mvnw -pl eureka-server -am spring-boot:run
```

Or from an IDE, run the module's `*Application` main class directly. Start `eureka-server` first, then `api-gateway`.

## Building all modules

```bash
./mvnw clean install
```

## Next phases

See the project development plan: User Service (auth/profile) → Restaurant Service → Reservation Service → Order Service → Billing Service → event-driven wiring via RabbitMQ → Swagger/logging/tests/Docker polish.
