# DinePilot

DinePilot is a restaurant-management backend built as a set of Spring Boot microservices. It currently covers identity, restaurant and menu management, customer carts, ordering, and the kitchen fulfillment workflow.

## Project status

Phases 1-3 and Phase 5 are complete. Reservation and Billing services remain planned work.

| Module | Responsibility | Port |
|---|---|---:|
| `common` | API responses, shared entities, exceptions, roles, JWT validation | - |
| `eureka-server` | Service registration and discovery | 8761 |
| `api-gateway` | Public routing, CORS, and request logging | 8080 |
| `user-service` | Authentication, profiles, addresses, kitchen assignment | 8081 |
| `restaurant-service` | Restaurants, categories, menu items, and tables | 8082 |
| `order-service` | Customer carts, orders, and kitchen workflow | 8084 |

## Roles

- `CUSTOMER` is the default role created through public registration.
- `RESTAURANT_ADMIN` manages restaurant resources and can assign kitchen staff.
- `KITCHEN` sees and processes orders for one assigned restaurant.
- `SUPER_ADMIN` has elevated management access.

Access tokens contain the user ID and role. Tokens issued to assigned kitchen users also contain `restaurantId`. Services validate authorization themselves through the shared JWT filter.

There is not yet a public role-promotion endpoint. Development environments can promote a user by updating the role in `user_db.users`. After changing a role or kitchen assignment, sign in again or refresh the token so its claims reflect the new user record.

## API overview

All APIs are available through `http://localhost:8080`. A service can also be called directly through its own port while developing.

Responses use the shared envelope:

```json
{
  "success": true,
  "message": "OK",
  "data": {},
  "timestamp": "2026-08-08T12:00:00Z"
}
```

### User Service

| Method | Path | Access |
|---|---|---|
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |
| POST | `/api/auth/refresh` | Public |
| POST | `/api/auth/logout` | Public |
| GET / PUT | `/api/users/me` | Authenticated user |
| GET / POST | `/api/users/me/addresses` | Authenticated user |
| PUT / DELETE | `/api/users/me/addresses/{id}` | Authenticated user |
| PATCH | `/api/users/{userId}/kitchen-restaurant` | Restaurant or super admin |

### Restaurant Service

| Method | Path | Access |
|---|---|---|
| GET | `/api/restaurants`, `/api/restaurants/{id}` | Public |
| POST | `/api/restaurants` | Restaurant admin |
| PUT / DELETE | `/api/restaurants/{id}` | Owner or super admin |
| GET | `/api/categories?restaurantId=` | Public |
| POST / PUT / DELETE | `/api/categories` | Restaurant owner |
| GET | `/api/menu?restaurantId=&categoryId=&available=&q=` | Public |
| GET | `/api/menu/{id}` | Public |
| POST / PUT / DELETE | `/api/menu` | Restaurant owner |
| PATCH | `/api/menu/{id}/availability` | Restaurant owner |
| GET | `/api/tables?restaurantId=` | Public |
| POST / PUT / DELETE | `/api/tables` | Restaurant owner |
| PATCH | `/api/tables/{id}/status` | Restaurant owner |

### Order Service

| Method | Path | Access and behavior |
|---|---|---|
| GET | `/api/cart` | Customer's active cart |
| POST | `/api/cart/items` | Customer; validates the live menu item |
| PUT | `/api/cart/items/{foodId}` | Customer; changes quantity |
| DELETE | `/api/cart/items/{foodId}` | Customer; removes a line |
| POST | `/api/orders` | Customer; snapshots and clears the cart |
| GET | `/api/orders` | Customer's newest-first order history |
| GET | `/api/orders/{id}` | Owning customer |
| PATCH | `/api/orders/{id}/cancel` | Owning customer while `PLACED` |
| GET | `/api/kitchen/orders` | Kitchen staff for assigned restaurant |
| PATCH | `/api/kitchen/orders/{id}/accept` | `PLACED` to `PREPARING` |
| PATCH | `/api/kitchen/orders/{id}/preparing` | Alias for accepting a placed order |
| PATCH | `/api/kitchen/orders/{id}/ready` | `PREPARING` to `READY` |
| PATCH | `/api/kitchen/orders/{id}/completed` | `READY` to `COMPLETED` |

Phase 5 keeps food names and prices as immutable order snapshots. A later menu edit therefore cannot change an existing order. Carts also reject items from a second restaurant, and kitchen queries never accept a restaurant ID from the request; they derive it from the signed token instead.

Read [the Phase 5 implementation guide](docs/phase-5-order-service.md) for the full design, flows, examples, edge cases, and test strategy.

## Prerequisites

- JDK 17
- Docker Desktop with Docker Compose
- No separate Maven installation is required

## Run with Docker Compose

PowerShell:

```powershell
Copy-Item .env.example .env
docker compose up -d --build
docker compose ps
```

Bash:

```bash
cp .env.example .env
docker compose up -d --build
docker compose ps
```

Useful URLs:

- Eureka dashboard: http://localhost:8761
- API Gateway health: http://localhost:8080/actuator/health
- User Service: http://localhost:8081
- Restaurant Service: http://localhost:8082
- Order Service: http://localhost:8084
- RabbitMQ management: http://localhost:15672

Stop the stack without deleting data:

```bash
docker compose down
```

Add `-v` only when you intentionally want to delete local MongoDB and RabbitMQ volumes.

## MongoDB configuration

The checked-in `.env.example` works with the local MongoDB container. Each service has its own logical database:

- `user_db`
- `restaurant_db`
- `order_db`

To use Atlas, point all three URIs at the same deployment and change only the database name:

```dotenv
MONGO_URI_USER_SERVICE=mongodb+srv://<user>:<password>@<cluster>.mongodb.net/user_db?retryWrites=true&w=majority
MONGO_URI_RESTAURANT_SERVICE=mongodb+srv://<user>:<password>@<cluster>.mongodb.net/restaurant_db?retryWrites=true&w=majority
MONGO_URI_ORDER_SERVICE=mongodb+srv://<user>:<password>@<cluster>.mongodb.net/order_db?retryWrites=true&w=majority
```

Use one sufficiently long `JWT_SECRET` across every service. Different secrets make a token issued by User Service invalid elsewhere.

## Run services locally

Start Eureka first, then the gateway, followed by business services:

```powershell
.\mvnw.cmd -pl eureka-server -am spring-boot:run
.\mvnw.cmd -pl api-gateway -am spring-boot:run
.\mvnw.cmd -pl user-service -am spring-boot:run
.\mvnw.cmd -pl restaurant-service -am spring-boot:run
.\mvnw.cmd -pl order-service -am spring-boot:run
```

When services run on the host, set `MONGO_URI` to a host-reachable address such as `localhost:27017`. The Compose hostname `mongodb` only resolves inside the Compose network.

## Build and test

Run the complete reactor:

```powershell
.\mvnw.cmd clean test
```

Run only Phase 5 and its shared dependency:

```powershell
.\mvnw.cmd -pl order-service -am test
```

The Phase 5 tests focus on business rules without requiring MongoDB, Eureka, or another running service. Repositories and the synchronous menu client are mocked at unit boundaries.

## Next phases

- Reservation Service: availability, booking, cancellation, and history
- Billing Service: invoices, simulated payments, and receipts
- RabbitMQ events connecting reservation, order, kitchen, and billing activity
- OpenAPI documentation, integration tests, observability, and deployment hardening
