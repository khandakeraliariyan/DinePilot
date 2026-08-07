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

## Roles

`CUSTOMER` (default on self-registration), `RESTAURANT_ADMIN`, `KITCHEN`, `SUPER_ADMIN`. JWTs carry the role as a claim; each service enforces authorization itself (no central auth gateway yet — see `common`'s `JwtAuthenticationFilter`). There's no admin-provisioning endpoint yet, so promoting a user to `RESTAURANT_ADMIN`/`SUPER_ADMIN` currently means editing their document in `user_db.users` directly.

## API overview

All endpoints are reachable through the gateway at `http://localhost:8080`, or directly against each service's own port. Responses are wrapped in `{ success, message, data, timestamp }`.

**User Service** (`user-service`, public unless noted)
| Method | Path | Notes |
|---|---|---|
| POST | `/api/auth/register` | always creates a `CUSTOMER` |
| POST | `/api/auth/login` | |
| POST | `/api/auth/refresh` | rotates the refresh token |
| POST | `/api/auth/logout` | revokes the refresh token |
| GET / PUT | `/api/users/me` | requires auth |
| GET / POST | `/api/users/me/addresses` | requires auth |
| PUT / DELETE | `/api/users/me/addresses/{id}` | requires auth |

**Restaurant Service** (`restaurant-service`)
| Method | Path | Notes |
|---|---|---|
| GET | `/api/restaurants`, `/api/restaurants/{id}` | public |
| POST | `/api/restaurants` | requires `RESTAURANT_ADMIN` |
| PUT / DELETE | `/api/restaurants/{id}` | owner or `SUPER_ADMIN` only |
| GET | `/api/categories?restaurantId=` | public |
| POST / PUT / DELETE | `/api/categories` | owner only |
| GET | `/api/menu?restaurantId=&categoryId=&available=&q=` | public search/filter |
| POST / PUT / DELETE | `/api/menu` | owner only |
| PATCH | `/api/menu/{id}/availability` | owner only |
| GET | `/api/tables?restaurantId=` | public |
| POST / PUT / DELETE | `/api/tables` | owner only |
| PATCH | `/api/tables/{id}/status` | owner only |

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

- **Reservation Service** — table availability, booking, cancellation, history
- **Order Service** — cart, orders, kitchen workflow
- **Billing Service** — invoices, simulated payments, receipts
- **Event-driven wiring** — RabbitMQ producers/consumers connecting the above (`ReservationCreated`, `OrderCreated`, `PaymentCompleted`, `OrderCompleted`); RabbitMQ is already running in `docker-compose.yml`, just unused so far
- **Quality & deployment** — Swagger/OpenAPI per service, structured logging, full compose coverage, Postman collection, unit/integration tests
