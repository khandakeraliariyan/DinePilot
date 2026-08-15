# Full Project Testing and Demo Guide

This guide shows how to run, test, and demo the full DinePilot backend end to end.

## 1. What this project covers

DinePilot is a Spring Boot microservices backend for restaurant management and online ordering.

Main services:

- `user-service` on `8081`
- `restaurant-service` on `8082`
- `reservation-service` on `8083`
- `order-service` on `8084`
- `billing-service` on `8085`
- `api-gateway` on `8080`
- `eureka-server` on `8761`

Supporting services:

- MongoDB
- RabbitMQ

## 2. Local setup

You only need:

- Docker Desktop
- JDK 17
- the repo

The repo already includes Docker Compose for MongoDB, RabbitMQ, and the app services.

### Start the stack

```powershell
Copy-Item .env.example .env
docker compose up -d --build
docker compose ps
```

### Useful URLs

- Eureka dashboard: `http://localhost:8761`
- API Gateway: `http://localhost:8080`
- User Service: `http://localhost:8081`
- Restaurant Service: `http://localhost:8082`
- Reservation Service: `http://localhost:8083`
- Order Service: `http://localhost:8084`
- Billing Service: `http://localhost:8085`
- RabbitMQ management: `http://localhost:15672`

## 3. Before you demo

For a clean demo, create three users:

- one `CUSTOMER`
- one `RESTAURANT_ADMIN`
- one `KITCHEN`

You can register users through the public auth API, then promote them by editing the user record in MongoDB as the README already mentions.

Also prepare:

- one restaurant owned by the restaurant admin
- one category
- one or two menu items
- one or more tables
- one reservation slot

## 4. Demo flow

This is the easiest full flow to show in front of someone.

### Step 1. Register and log in

Use the auth APIs:

- `POST /api/auth/register`
- `POST /api/auth/login`

Do this for:

- customer
- restaurant admin
- kitchen user

Keep the returned JWT token for later requests.

### Step 2. Create restaurant resources

Use the restaurant admin token.

Recommended API order:

1. `POST /api/restaurants`
2. `POST /api/categories`
3. `POST /api/menu`
4. `POST /api/tables`

Useful read APIs:

- `GET /api/restaurants`
- `GET /api/categories?restaurantId=...`
- `GET /api/menu?restaurantId=...`
- `GET /api/tables?restaurantId=...`

### Step 3. Show customer menu browsing

Use the customer token and browse:

- `GET /api/restaurants`
- `GET /api/menu?restaurantId=...`
- `GET /api/menu/{id}`

This is a good moment to show that menu data is public but order validation is still server-side.

### Step 4. Add items to cart

Use the customer token.

Order Service APIs:

- `GET /api/cart`
- `POST /api/cart/items`
- `PUT /api/cart/items/{foodId}`
- `DELETE /api/cart/items/{foodId}`

Suggested demo:

1. Add a menu item to the cart.
2. Increase quantity.
3. Show that the cart total is calculated from live price data.

### Step 5. Place the order

Use:

- `POST /api/orders`

Then show:

- `GET /api/orders`
- `GET /api/orders/{id}`

You can explain that the order stores a snapshot of price, name, and totals so later menu edits do not change existing orders.

### Step 6. Kitchen workflow

Use the kitchen token.

Kitchen APIs:

- `GET /api/kitchen/orders`
- `PATCH /api/kitchen/orders/{id}/accept`
- `PATCH /api/kitchen/orders/{id}/preparing`
- `PATCH /api/kitchen/orders/{id}/ready`
- `PATCH /api/kitchen/orders/{id}/completed`

Suggested demo order:

1. list kitchen orders
2. accept the order
3. mark it ready
4. complete it

This is where the event flow becomes visible because the order completion can trigger billing.

### Step 7. Reservation flow

Use the customer token.

Reservation APIs:

- `POST /api/reservations`
- `GET /api/reservations`
- `GET /api/reservations/{id}`
- `PATCH /api/reservations/{id}/cancel`

Then use the restaurant admin token to show restaurant-side management:

- `GET /api/reservations/restaurant/{restaurantId}`
- `PATCH /api/reservations/restaurant/{id}/confirm`
- `PATCH /api/reservations/restaurant/{id}/complete`
- `PATCH /api/reservations/restaurant/{id}/cancel`

Good demo sequence:

1. customer books a table
2. restaurant admin confirms it
3. restaurant admin completes it
4. show the table status update in restaurant-service

### Step 8. Billing flow

Billing APIs:

- `POST /api/invoices/orders/{orderId}`
- `GET /api/invoices/me`
- `GET /api/invoices/{id}`
- `POST /api/invoices/{id}/pay`

Suggested demo:

1. show the invoice created for a completed order
2. open the invoice
3. run the simulated payment
4. show that the invoice becomes `PAID`

## 5. Event-driven behavior to mention

Phase 7 uses RabbitMQ to connect services:

- reservation events update restaurant table status
- completed orders create billing invoices
- payment completion marks orders as paid

You do not need to run RabbitMQ by hand. Docker Compose starts it.

## 6. Best demo script

If you want a simple 5 to 10 minute presentation, use this script:

1. Show service health on the gateway and Eureka.
2. Log in as admin and create restaurant data.
3. Log in as customer and add items to cart.
4. Place an order.
5. Log in as kitchen and complete the order.
6. Show the billing invoice.
7. Pay the invoice.
8. Book a reservation.
9. Confirm it as restaurant admin.
10. Show the table status change in restaurant-service.

## 7. Testing checklist

### Happy path

- user registration/login
- restaurant creation
- menu browsing
- cart add/update/remove
- order placement
- kitchen completion
- invoice generation
- payment completion
- reservation booking
- reservation confirm/complete

### Error cases to demo

- add an unavailable menu item
- add items from a second restaurant into the same cart
- place an empty cart
- cancel an order after kitchen work has started
- book a reservation that overlaps another reservation
- book a table for too many guests
- view or edit another user's record

## 8. Verification commands

Run tests:

```powershell
.\mvnw.cmd clean test
```

Run targeted tests:

```powershell
.\mvnw.cmd -pl reservation-service -am test
.\mvnw.cmd -pl order-service -am test
.\mvnw.cmd -pl billing-service -am test
```

## 9. Notes for local testing

- The project is set up to use local MongoDB through Docker Compose.
- RabbitMQ is also started by Docker Compose.
- If you change credentials, update `.env` and the service environment variables together.
- The services use Eureka automatically; you do not need to configure service discovery manually for normal local testing.

## 10. What to mention in a report

If you are presenting the project, explain these points:

- database-per-service architecture
- JWT-based service-side authorization
- synchronous reads for authoritative data
- RabbitMQ for cross-service side effects
- immutable order snapshots
- reservation overlap checking
- billing from completed orders

