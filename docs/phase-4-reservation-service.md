# Phase 4 — Reservation Service

## Scope

Reservation Service owns table booking: availability checks, requesting a reservation, cancellation, customer history, and the restaurant-side confirm/complete/cancel workflow. It does not own tables — those belong to `restaurant-service` — and it does not yet mutate table status; that side effect is deferred to Phase 7 (see "Known limitations" below).

## Service boundary

- Port `8083`, Mongo database `reservation_db`, Eureka name `RESERVATION-SERVICE`.
- Registered at the gateway under `/api/reservations/**` (the route already existed in `api-gateway`'s configuration before this phase landed).
- Depends on `restaurant-service` at runtime for two synchronous reads: does a table exist and what is its capacity, and who owns a given restaurant. It has no dependency on `order-service` or `user-service`.

## Module layout

Mirrors `order-service`, the closest existing analog (a per-user resource service that calls another service synchronously):

```
reservation-service/src/main/java/com/dinepilot/reservation/
  ReservationServiceApplication.java
  client/RestaurantServiceClient.java
  config/SecurityConfig.java
  controller/ReservationController.java
  controller/RestaurantReservationController.java
  dto/ReservationRequest.java
  dto/ReservationResponse.java
  entity/Reservation.java
  enums/ReservationStatus.java
  repository/ReservationRepository.java
  service/ReservationService.java
  service/RestaurantAccessGuard.java
```

## Runtime wiring

`ReservationServiceApplication` scans `com.dinepilot` (picking up `common`'s `GlobalExceptionHandler`, `JwtAuthenticationFilter`, and `JwtValidator` unchanged), enables Mongo auditing for `createdAt`/`updatedAt`, and exposes a `@LoadBalanced RestClient.Builder` bean so `RestaurantServiceClient` can call `http://RESTAURANT-SERVICE` and have Eureka resolve the address.

## Data model

`Reservation` (collection `reservations`, extends `common`'s `BaseEntity`):

| Field | Type | Notes |
|---|---|---|
| `userId` | `String` | indexed; the booking customer |
| `restaurantId` | `String` | indexed; derived server-side from the table, never trusted from the request |
| `tableId` | `String` | indexed |
| `partySize` | `int` | |
| `reservedFor` | `Instant` | start of the reservation window |
| `status` | `ReservationStatus` | indexed; `PENDING`, `CONFIRMED`, `CANCELLED`, `COMPLETED` |
| `notes` | `String` | optional, customer-supplied |

`restaurantId` is intentionally not part of `ReservationRequest`. The client only supplies `tableId`; the service resolves the owning restaurant from `restaurant-service`'s table lookup. This removes an entire class of bug where a client-supplied `restaurantId` disagrees with the table's real restaurant.

## Business rules

**Capacity.** `partySize` must not exceed the table's `capacity` as reported by `restaurant-service`, or the request is rejected with `409 Conflict`.

**Availability / overlap.** Every reservation occupies a fixed 90-minute slot (`ReservationService.SLOT_DURATION`) starting at `reservedFor`. Before creating a reservation, the service loads every `PENDING` or `CONFIRMED` reservation for the same `tableId` (`CANCELLED`/`COMPLETED` reservations never block a slot) and rejects the request if `[newStart, newEnd)` intersects `[existingStart, existingEnd)` for any of them. A booking that starts exactly when a prior slot ends is allowed (half-open interval).

**Status state machine**, enforced the same way `order-service` enforces `OrderStatus` transitions — an explicit required-state check before every transition, throwing `ConflictException` with a message naming both states:

```
PENDING --(restaurant confirms)--> CONFIRMED --(restaurant completes)--> COMPLETED
PENDING --(customer or restaurant cancels)--> CANCELLED
CONFIRMED --(customer or restaurant cancels)--> CANCELLED
```

`CANCELLED` and `COMPLETED` are terminal. Cancellation is valid from either `PENDING` or `CONFIRMED`; confirm and complete each require one specific prior state.

## Authorization

Two controllers, split by role exactly like `order-service` splits `OrderController` from `KitchenOrderController`:

- `ReservationController` (`/api/reservations`, `@PreAuthorize("hasRole('CUSTOMER')")`) — book, history, get-by-id, cancel. Ownership is a direct `reservation.getUserId().equals(userId)` comparison, matching `OrderManagementService`.
- `RestaurantReservationController` (`/api/reservations/restaurant`, `@PreAuthorize("hasAnyRole('RESTAURANT_ADMIN', 'SUPER_ADMIN')")`) — list-by-restaurant, confirm, complete, cancel.

Restaurant-side authorization cannot use the JWT's `restaurantId` claim the way kitchen staff do (`order-service`'s `JwtClaims.restaurantId`), because that claim is only populated for `KITCHEN` users, not `RESTAURANT_ADMIN`. Instead, `RestaurantAccessGuard` — the same shape as `restaurant-service`'s `OwnershipGuard`, but backed by a remote call instead of a local field — fetches the restaurant from `restaurant-service` and compares its `ownerId` against `authentication.getName()`, short-circuiting for `SUPER_ADMIN`.

## Cross-service communication

`RestaurantServiceClient` follows `order-service`'s `RestaurantMenuClient` pattern exactly: a `RestClient` bean scoped to `http://RESTAURANT-SERVICE`, deserializing the shared `ApiResponse<T>` envelope, translating a `404` into `ResourceNotFoundException` and any other failure into `ConflictException` ("could not validate"). Two calls:

- `GET /api/tables/{id}` → table existence + `restaurantId` + `capacity`.
- `GET /api/restaurants/{id}` → `ownerId`, for the restaurant-admin ownership check.

Both response records carry `@JsonIgnoreProperties(ignoreUnknown = true)` so they only declare the fields Reservation Service actually needs, rather than mirroring every field `restaurant-service` returns.

This is a deliberate sync-read choice: cross-service *reads* (does this table exist, who owns this restaurant) are synchronous HTTP calls resolved through Eureka, while cross-service *reactions* (flipping the table to `RESERVED` when a reservation is confirmed) are left for the RabbitMQ event that Phase 7 introduces. Doing the table-status write synchronously now would create a second service with write access to `restaurant-service`'s tables and would need to be re-plumbed once `ReservationCreated`/`ReservationConfirmed` events exist — better to defer it once than build it twice.

## Examples

Book a table:

```
POST /api/reservations
Authorization: Bearer <customer token>

{ "tableId": "table-1", "partySize": 4, "reservedFor": "2026-09-01T19:00:00Z", "notes": "Anniversary dinner" }
```

Restaurant admin confirms it:

```
PATCH /api/reservations/restaurant/{id}/confirm
Authorization: Bearer <restaurant admin token>
```

## Error behavior

| Condition | Exception | Status |
|---|---|---|
| Table or restaurant does not exist | `ResourceNotFoundException` | 404 |
| Reservation ID does not exist | `ResourceNotFoundException` | 404 |
| Party size exceeds table capacity | `ConflictException` | 409 |
| Requested slot overlaps an active reservation | `ConflictException` | 409 |
| Illegal status transition | `ConflictException` | 409 |
| `restaurant-service` reachable but returned an unexpected error | `ConflictException` | 409 |
| Reservation belongs to another customer | `ForbiddenException` | 403 |
| Restaurant not managed by the caller | `ForbiddenException` | 403 |
| `reservedFor` missing, in the past, or `tableId`/`partySize` invalid | validation (400) | 400 |

All of these are handled by `common`'s existing `GlobalExceptionHandler` — no per-service override.

## Indexes

`userId`, `restaurantId`, `tableId`, and `status` are all `@Indexed` on `Reservation`, matching the query patterns used by history (`userId`), restaurant listing (`restaurantId`), and the overlap check (`tableId` + `status`).

## Testing strategy

Pure Mockito/AssertJ unit tests, no Testcontainers/MockMvc/`@SpringBootTest`, matching Phase 5's approach:

- `ReservationServiceTest` — booking (capacity rejection, overlap rejection, back-to-back boundary acceptance, restaurantId/status derivation), customer access (ownership, history ordering, missing resource), and restaurant management (ownership delegation, every legal and illegal state transition).
- `RestaurantAccessGuardTest` — super-admin bypass, owning admin allowed, non-owning admin rejected.

`ReservationRepository` and `RestaurantServiceClient` are mocked at the boundary; no MongoDB, Eureka, or running `restaurant-service` is required to run the suite.

## Known limitations

- Table status in `restaurant-service` is not updated when a reservation is created, confirmed, or cancelled. It stays whatever `restaurant-service` already has it as until Phase 7 adds the `ReservationCreated` (and confirm/cancel) events for `restaurant-service` to consume.
- A reservation's time cannot be rescheduled — only cancelled and rebooked. There is no reschedule endpoint in this phase.
- The 90-minute slot duration is a fixed constant, not configurable per restaurant or table.

## Extension points

- Phase 7 producers: `reservation-service` should publish a `ReservationCreated` (and likely `ReservationConfirmed`/`ReservationCancelled`) event after each successful state change, for `restaurant-service` to consume and update `RestaurantTable.status`.
- Phase 5's `order-service` already notes it can attach a reservation or table reference to an order without changing its item snapshots — Reservation Service's `id` is a natural foreign key for that if dine-in ordering against a reservation is ever added.

## Design summary

Reservation Service is a thin, per-user resource service in the same shape as `order-service`: entity → repository → service (business rules + explicit state machine) → two role-scoped controllers, secured by the shared JWT filter, with all cross-service data (table capacity, restaurant ownership) fetched synchronously through `RestClient` + Eureka rather than duplicated locally.
