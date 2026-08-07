# Phase 5: Order Service

This guide explains the decisions behind DinePilot's cart, order, and kitchen workflow. It is intended for someone maintaining the service, reviewing the phase, or building the next reservation and billing phases on top of it.

## 1. Scope

Phase 5 adds the path from choosing a menu item to completing it in the kitchen:

1. A customer adds a menu item to a cart.
2. Order Service asks Restaurant Service for the current item.
3. The item is accepted only when it exists and is available.
4. The cart keeps the name and current unit price returned by Restaurant Service.
5. A customer places the cart as an order.
6. The order copies each line into an immutable snapshot.
7. Kitchen staff see orders for their assigned restaurant.
8. The kitchen advances the order through preparation and completion.
9. The customer can see the order status in history.

Phase 5 does not include payment authorization, table reservations, delivery dispatch, stock decrementing, or RabbitMQ publication. Those boundaries are deliberate: the service has a complete synchronous core without pretending later phases already exist.

## 2. Service boundary

Order Service owns:

- one active cart per customer;
- cart line quantities;
- the restaurant attached to a cart;
- order item snapshots;
- customer order history;
- order status;
- kitchen state transitions.

Order Service does not own:

- whether a menu item exists;
- whether a menu item is currently available;
- the live name or price of a menu item;
- restaurant profiles;
- user passwords, roles, or refresh tokens;
- the relationship between a restaurant and its owner.

This ownership line determines where data is read and where it is copied. Restaurant Service remains authoritative for live food information. Order Service becomes authoritative for what the customer actually ordered.

## 3. Module layout

```text
order-service/
|-- Dockerfile
|-- pom.xml
`-- src/
    |-- main/
    |   |-- java/com/dinepilot/order/
    |   |   |-- OrderServiceApplication.java
    |   |   |-- client/
    |   |   |   `-- RestaurantMenuClient.java
    |   |   |-- config/
    |   |   |   `-- SecurityConfig.java
    |   |   |-- controller/
    |   |   |   |-- CartController.java
    |   |   |   |-- KitchenOrderController.java
    |   |   |   `-- OrderController.java
    |   |   |-- dto/
    |   |   |-- entity/
    |   |   |-- enums/
    |   |   |-- repository/
    |   |   |-- security/
    |   |   `-- service/
    |   `-- resources/application.yml
    `-- test/
        `-- java/com/dinepilot/order/
```

The service follows the same controller-service-repository arrangement as the earlier modules. Business rules stay in services. Controllers translate HTTP requests into service calls. Repositories only express persistence queries.

## 4. Runtime wiring

Order Service uses port `8084` and registers with Eureka as `ORDER-SERVICE`.

The gateway routes these paths to it:

```text
/api/cart/**
/api/orders/**
/api/kitchen/orders/**
```

The service reads:

```text
SERVER_PORT
MONGO_URI
JWT_SECRET
EUREKA_HOST
```

Compose maps its database URI to `order_db`. Database-per-service is preserved even when every database lives in the same MongoDB process or Atlas cluster.

## 5. Cart model

`Cart` contains:

- `userId`: unique owner of the active cart;
- `restaurantId`: restaurant shared by all lines;
- `items`: embedded `CartItem` values;
- audit fields inherited from `BaseEntity`.

`CartItem` contains:

- `foodId`;
- `restaurantId`;
- `foodName`;
- `unitPrice`;
- `quantity`.

The unique index on `userId` represents the one-active-cart rule in storage. The service also consistently resolves carts with `findByUserId` instead of letting callers choose a cart ID.

### Why carts embed their lines

Cart lines have no useful lifecycle outside the cart. They are small, always loaded together, and changed through the cart aggregate. Embedding avoids a second collection and makes removal or replacement a single aggregate save.

### Empty cart behavior

Reading a cart for a customer without a stored cart returns an empty response. It does not create a MongoDB document merely because the customer opened the cart page.

Mutation behavior is stricter. Updating or removing an item requires a stored cart and a matching line. Missing state becomes `404`, which helps clients distinguish an empty cart from a validation failure.

### One restaurant per cart

A cart can contain several foods, but every food must have the same `restaurantId`.

The rule prevents ambiguous fulfillment. Without it, one customer order would need to be split across kitchens, payments, preparation times, and possibly delivery trips. That can be designed later as a parent checkout containing multiple restaurant orders. It should not emerge accidentally from one `Order` record.

The check runs before a new line is added:

```text
cart has no restaurant -> accept item and set restaurant
cart restaurant matches item restaurant -> accept item
cart restaurant differs -> return conflict
```

Removing the last line clears `restaurantId`. The next item can then establish a different restaurant.

## 6. Synchronous menu validation

`RestaurantMenuClient` performs a service-discovered HTTP GET:

```http
GET http://RESTAURANT-SERVICE/api/menu/{foodId}
```

The logical hostname is resolved by Spring Cloud LoadBalancer using Eureka. No host or container address is hardcoded in application code.

The call happens when an item is added because the cart needs three authoritative values:

- availability;
- current price;
- current name.

The response is read through the shared `ApiResponse<MenuItem>` envelope. A missing response or empty `data` value is treated as a missing food. An unavailable food produces a conflict. Other remote failures produce a stable business-facing conflict rather than leaking low-level client exceptions.

### Why validation is not delegated to the client

The browser or mobile application can show menu data, but its request cannot be trusted as the source of price or availability. A user could modify a request, use stale cached data, or call the API directly.

The add request therefore contains only:

```json
{
  "foodId": "food-42",
  "quantity": 2
}
```

There is intentionally no client-supplied price, name, or restaurant ID.

### Failure mapping

| Restaurant response | Order Service result |
|---|---|
| `200` with available food | item added |
| `200` with unavailable food | `409 Conflict` |
| `404` | `404 Not Found` |
| empty body or empty data | `404 Not Found` |
| other remote HTTP failure | `409 Conflict` with stable message |

The generic exception handler wraps these results in the normal API envelope.

## 7. Cart calculations

Line totals and cart totals are derived values:

```text
line total = unit price * quantity
cart total = sum of line totals
```

They are calculated while mapping an entity to `CartResponse`. The database stores the unit price and quantity, not redundant totals that could drift out of sync.

`BigDecimal` is used for money. Floating point types are not appropriate for currency because common decimal values cannot be represented exactly as binary fractions.

## 8. Adding an item

The add flow is:

1. Validate the food with Restaurant Service.
2. Load the customer's cart or create an in-memory empty cart.
3. Enforce the one-restaurant rule.
4. Find a line with the same `foodId`.
5. Create a line or increment the existing quantity.
6. Refresh the line's name and unit price from the authoritative response.
7. Save the cart.
8. Return calculated totals.

Refreshing the name and price when an existing line is added again means a deliberate new add action picks up current menu information. The order still receives a final independent copy at placement time.

## 9. Updating and removing items

Updating a line changes its quantity. Request validation requires a quantity of at least one. A quantity of zero is not treated as an implicit delete; clients use the explicit DELETE endpoint, which keeps API intent clear.

Removing a line returns the remaining cart. When the line was the final item, the response has no restaurant and a zero total.

Both operations reject a missing cart or missing item with `ResourceNotFoundException`.

## 10. Order model

`Order` contains:

- `userId`;
- `restaurantId`;
- `status`;
- embedded order items;
- total;
- creation and modification timestamps.

`OrderItem` contains:

- original `foodId` for reference;
- snapshotted `foodName`;
- snapshotted `unitPrice`;
- quantity;
- snapshotted line total.

The order stores its total because it is a historical business fact. Recalculating from live menu data would be incorrect, while recalculating from embedded snapshots is unnecessary on every read.

## 11. Why order items are snapshots

Restaurants legitimately change menus. A food can be renamed, repriced, made unavailable, or deleted. None of those changes should alter an order that was already placed.

For example:

```text
12:00 customer orders "House Burger" at 8.50
14:00 restaurant renames it to "Classic Burger"
16:00 restaurant raises price to 9.25
```

The 12:00 order must continue to show `House Burger`, `8.50`. Kitchen tickets, customer history, future invoices, refunds, and support conversations all depend on that stability.

Only identifiers would force every historical read to depend on a live menu record. Snapshots remove that coupling.

## 12. Placing an order

The placement flow is:

1. Find the customer's stored cart.
2. Reject a missing or empty cart.
3. Create an `Order` with customer and restaurant IDs.
4. Copy every cart line into a new `OrderItem`.
5. Calculate line totals.
6. Calculate and store the order total.
7. Set status to `PLACED`.
8. Save the order.
9. Delete the cart.
10. Return the order snapshot.

The order is saved before the cart is deleted. If saving fails, the cart remains available for retry. In a production environment with replica-set transactions, these operations could be placed in a MongoDB transaction. A future event-driven checkout could instead use an idempotency key and recovery process.

## 13. Order status model

The defined states are:

```text
PLACED
PREPARING
READY
COMPLETED
CANCELLED
```

The normal kitchen path is:

```text
PLACED -> PREPARING -> READY -> COMPLETED
```

The customer cancellation path is:

```text
PLACED -> CANCELLED
```

Terminal states are `COMPLETED` and `CANCELLED`.

### Transition table

| Actor | Current | Action | Result |
|---|---|---|---|
| Customer | `PLACED` | cancel | `CANCELLED` |
| Kitchen | `PLACED` | accept/preparing | `PREPARING` |
| Kitchen | `PREPARING` | ready | `READY` |
| Kitchen | `READY` | completed | `COMPLETED` |

Every other combination is rejected with a conflict. This includes skipping states, repeating a transition, reopening a completed order, or cancelling after preparation begins.

The `/accept` and `/preparing` endpoints deliberately reach the same state. They support two common UI labels without introducing an `ACCEPTED` state that was not part of the phase model.

## 14. Customer authorization

Customer endpoints are protected with:

```java
@PreAuthorize("hasRole('CUSTOMER')")
```

The JWT subject becomes `Authentication.getName()`. Controllers do not accept `userId` from the request.

When reading or cancelling one order, the service loads it and compares its `userId` with the authenticated subject. A customer cannot discover another customer's order merely by guessing an ID.

History uses `findByUserIdOrderByCreatedAtDesc`, returning only the caller's records and placing the newest first.

## 15. Kitchen assignment

Phase 2 originally defined a `KITCHEN` role but did not associate kitchen staff with a restaurant. Phase 5 adds `restaurantId` to the User document.

An assignment is made through:

```http
PATCH /api/users/{userId}/kitchen-restaurant
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "restaurantId": "restaurant-17"
}
```

Only restaurant admins and super admins pass the endpoint's method security. The target user must already have the `KITCHEN` role; assigning restaurant scope to another role returns a conflict.

The assignment is returned in `UserProfileResponse`, making it visible during profile inspection and administrative workflows.

### Token refresh requirement

JWTs are self-contained. Updating the User document does not mutate access tokens already issued to that person.

After assignment, the kitchen user must log in again or refresh. The new token contains:

```json
{
  "sub": "kitchen-user-8",
  "email": "cook@example.com",
  "role": "KITCHEN",
  "restaurantId": "restaurant-17"
}
```

## 16. Propagating JWT claims

The shared `JwtAuthenticationFilter` validates the token, builds authorities from the role, and stores parsed claims as authentication details.

Order Service's `JwtClaims` helper reads `restaurantId` from those details. Missing or blank scope becomes a forbidden result.

This approach keeps controllers independent of raw authorization headers. It also avoids parsing and verifying the same token twice.

## 17. Kitchen isolation

Kitchen endpoints never accept `restaurantId` as a query or path parameter. The signed claim is the only source.

The list endpoint queries:

```text
findByRestaurantIdOrderByCreatedAtDesc(claim.restaurantId)
```

Transition endpoints perform a second object-level check after loading the order:

```text
order.restaurantId must equal claim.restaurantId
```

This defense matters even when IDs are difficult to guess. Authorization should not depend on obscurity.

An unassigned kitchen user is authenticated but forbidden. That distinction is useful:

- `401` means the token is absent or invalid;
- `403` means identity is valid but restaurant scope is missing or wrong.

## 18. Kitchen workflow examples

List the assigned restaurant's orders:

```bash
curl http://localhost:8080/api/kitchen/orders \
  -H "Authorization: Bearer $KITCHEN_TOKEN"
```

Accept an order:

```bash
curl -X PATCH http://localhost:8080/api/kitchen/orders/order-1/accept \
  -H "Authorization: Bearer $KITCHEN_TOKEN"
```

Mark it ready:

```bash
curl -X PATCH http://localhost:8080/api/kitchen/orders/order-1/ready \
  -H "Authorization: Bearer $KITCHEN_TOKEN"
```

Complete pickup or service:

```bash
curl -X PATCH http://localhost:8080/api/kitchen/orders/order-1/completed \
  -H "Authorization: Bearer $KITCHEN_TOKEN"
```

## 19. Customer examples

Add two units of a menu item:

```bash
curl -X POST http://localhost:8080/api/cart/items \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"foodId":"food-42","quantity":2}'
```

Update quantity:

```bash
curl -X PUT http://localhost:8080/api/cart/items/food-42 \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"quantity":3}'
```

Place the order:

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

Read history:

```bash
curl http://localhost:8080/api/orders \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

Cancel a placed order:

```bash
curl -X PATCH http://localhost:8080/api/orders/order-1/cancel \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

## 20. Error behavior

Business exceptions from the common module map to consistent statuses:

| Exception | Status | Example |
|---|---:|---|
| `ResourceNotFoundException` | 404 | missing cart item or order |
| `ConflictException` | 409 | unavailable food or invalid transition |
| `ForbiddenException` | 403 | another customer's or restaurant's order |
| `UnauthorizedException` | 401 | invalid authentication operation |

Validation errors return `400`, including blank IDs and quantities lower than one.

## 21. Persistence indexes

The main indexes support the access patterns:

- unique cart `userId` for one active cart;
- order `userId` for customer history;
- order `restaurantId` for kitchen queues;
- order `status` for future operational filtering.

The current kitchen query retrieves all statuses for a restaurant. A future UI may add a repository query that filters active statuses while leaving completed history accessible separately.

## 22. Testing strategy

Phase 5 unit tests isolate business logic from infrastructure. Mockito substitutes repositories, Cart Service collaborators, and the menu client.

Cart tests cover:

- reading a non-persisted empty cart;
- adding the first item;
- merging a repeated item;
- refreshing live price and name;
- rejecting a second restaurant;
- calculating line and cart totals;
- updating quantity;
- missing carts and items;
- clearing restaurant scope after final removal.

Order tests cover:

- rejecting empty checkout;
- copying item snapshots;
- calculating totals;
- clearing the cart after save;
- preserving the cart when save fails;
- customer ownership;
- history delegation;
- cancellation rules;
- every valid kitchen transition;
- skipped, repeated, terminal, and cross-restaurant transitions.

Security and user tests cover:

- assigning only kitchen users;
- exposing assignment in profile responses;
- including restaurant scope in kitchen tokens;
- omitting the claim for unassigned users;
- reading scope from authentication details;
- rejecting missing scope.

These tests do not start MongoDB or Eureka. They run quickly and identify business-rule regressions without network timing noise.

## 23. Running tests

All modules:

```powershell
.\mvnw.cmd clean test
```

Order Service and required reactor modules:

```powershell
.\mvnw.cmd -pl order-service -am test
```

User Service tests:

```powershell
.\mvnw.cmd -pl user-service -am test
```

Docker can also compile the affected images:

```powershell
docker compose build user-service restaurant-service order-service api-gateway
```

## 24. Operational checks

After starting Compose:

1. Open Eureka and confirm `ORDER-SERVICE` is registered.
2. Check `http://localhost:8084/actuator/health`.
3. Register and authenticate a customer.
4. Create or identify an available menu item.
5. Add it through the gateway.
6. Place an order and record its ID.
7. Assign a kitchen user to that restaurant.
8. refresh the kitchen user's access token.
9. List kitchen orders and process the recorded order.
10. Confirm the customer history shows `COMPLETED`.

If Order Service is healthy but gateway calls return `503`, check Eureka registration and the configured logical service name. If cart adds fail while other endpoints work, inspect Restaurant Service registration and the food's availability.

## 25. Concurrency notes

The current service expresses the required phase behavior but does not add optimistic locking. Two simultaneous updates to the same cart can produce a last-write-wins result. Two kitchen workers could also race on one transition.

Production hardening options include:

- an entity version field with optimistic locking;
- atomic MongoDB update predicates that include current status;
- idempotency keys for order placement;
- a MongoDB transaction for order save and cart deletion;
- serialized command processing per order.

These controls should be chosen using observed traffic and failure requirements rather than added as decorative complexity.

## 26. Trust boundaries

The implementation follows four trust rules:

1. Never trust customer-supplied price, name, restaurant ID, or user ID.
2. Never trust kitchen-supplied restaurant scope.
3. Never trust live menu data to describe historical orders.
4. Never trust a requested state transition without checking current state.

Most Phase 5 business rules are consequences of these boundaries.

## 27. Known limitations

- Restaurant admins are authorized to call the assignment endpoint, but User Service does not yet synchronously verify that the assigned restaurant belongs to that admin.
- Cart updates do not re-read menu availability; live validation is performed when adding.
- Checkout has no payment or reservation step.
- Order placement has no idempotency key.
- Cart deletion and order creation are not wrapped in a MongoDB transaction.
- Kitchen queues do not yet filter or paginate by status.
- RabbitMQ is provisioned but Phase 5 does not publish order events.

The first limitation is the most important cross-service follow-up. A robust solution can ask Restaurant Service whether the authenticated admin owns the requested restaurant, or centralize staff membership under a dedicated administrative workflow.

## 28. Extension points

Reservation Service can attach a reservation or table reference to an order without changing item snapshots.

Billing Service can consume the stored total and order ID, then maintain payment status separately. Order status should not be overloaded to represent payment state.

RabbitMQ events can be emitted after successful state changes:

```text
OrderPlaced
OrderPreparing
OrderReady
OrderCompleted
OrderCancelled
```

Consumers should treat event IDs idempotently. Event publication should use an outbox or another durable strategy if losing an event is unacceptable.

Future delivery support can create fulfillment records rather than placing addresses and driver state directly in the order aggregate.

## 29. Design summary

Phase 5 is centered on a small number of durable choices:

- Restaurant Service owns live menu truth.
- Order Service owns checkout history.
- Cart input carries identifiers and quantity, never price.
- One cart represents one restaurant.
- Orders snapshot customer-visible commercial data.
- State changes are explicit and sequential.
- Customer identity and kitchen scope come from signed tokens.
- Object-level ownership is checked after loading records.

Those choices keep the current service understandable and give later billing, reservation, and event-driven phases a reliable foundation.
