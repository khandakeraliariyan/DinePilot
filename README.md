# DinePilot

Microservices-based restaurant management system built with Spring Boot, Spring Cloud, MongoDB, and RabbitMQ.

## Status

**Phases 1–3** are complete: Eureka, API Gateway, `common` library, User Service (auth/profile/addresses), and Restaurant Service (restaurants/categories/menu/tables). Reservation, Order, and Billing services are not implemented yet.

## Modules

| Module | Description | Port |
|---|---|---|
| `common` | Shared library: `ApiResponse` envelope, `BaseEntity`, `Role` enum, exceptions, global exception handler, JWT validation | — |
| `eureka-server` | Service registry / discovery | 8761 |
| `api-gateway` | Edge routing, CORS, request logging | 8080 |
| `user-service` | Registration, JWT login/refresh/logout, profile, addresses | 8081 |
| `restaurant-service` | Restaurant/category/menu/table CRUD | 8082 |

## Prerequisites

- JDK 17
- Docker + Docker Compose
- No local Maven install needed — use the bundled wrapper (`./mvnw` / `mvnw.cmd`)

## Running everything with Docker Compose

```bash
cp .env.example .env
docker compose up -d --build
```

This starts MongoDB, RabbitMQ, Eureka Server, API Gateway, User Service, and Restaurant Service.

- Eureka dashboard: http://localhost:8761
- API Gateway health: http://localhost:8080/actuator/health
- User Service: http://localhost:8081
- Restaurant Service: http://localhost:8082
- RabbitMQ management UI: http://localhost:15672 (user/pass from `.env`)

Stop everything with `docker compose down` (add `-v` to also drop data volumes).

### Using MongoDB Atlas instead of the local container

`user-service` and `restaurant-service` each read their database connection from `MONGO_URI_USER_SERVICE` / `MONGO_URI_RESTAURANT_SERVICE` in `.env`. Point both at the **same Atlas cluster**, changing only the database name in the URI path (`user_db`, `restaurant_db`) — that keeps the project's database-per-service design while sharing one cluster. Get the base connection string from Atlas → Database → Connect → Drivers, then set:

```
MONGO_URI_USER_SERVICE=mongodb+srv://<user>:<password>@<cluster>.mongodb.net/user_db?retryWrites=true&w=majority
MONGO_URI_RESTAURANT_SERVICE=mongodb+srv://<user>:<password>@<cluster>.mongodb.net/restaurant_db?retryWrites=true&w=majority
```

`docker compose up` will use Atlas automatically once these are set — the local `mongodb` container stays defined in `docker-compose.yml` but nothing depends on it.

## Running a service locally (without Docker)

```bash
./mvnw -pl eureka-server -am spring-boot:run
```

Or from an IDE, run the module's `*Application` main class directly. Start `eureka-server` first, then `api-gateway`, then the business services.

`user-service` and `restaurant-service` need `MONGO_URI` and `JWT_SECRET` — either export them before running, or edit the defaults baked into each module's `application.yml`:

```bash
export MONGO_URI="mongodb+srv://<user>:<password>@<cluster>.mongodb.net/user_db?retryWrites=true&w=majority"
export JWT_SECRET="<same secret across every service>"
./mvnw -pl user-service -am spring-boot:run
```

## Building all modules

```bash
./mvnw clean install
```

## Next phases

See the project development plan: Reservation Service → Order Service → Billing Service → event-driven wiring via RabbitMQ → Swagger/logging/tests/Docker polish.
