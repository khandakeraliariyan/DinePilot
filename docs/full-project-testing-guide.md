# Full Project Testing and Demo Guide

This guide matches the current DinePilot codebase and shows how to test every exposed API with Postman or any HTTP client.

## 1. Project overview

DinePilot is a Spring Boot microservices backend for restaurant management and online ordering.

Services:

- `user-service` on `8081`
- `restaurant-service` on `8082`
- `reservation-service` on `8083`
- `order-service` on `8084`
- `billing-service` on `8085`
- `api-gateway` on `8080`
- `eureka-server` on `8761`

Supporting infrastructure:

- MongoDB running locally on `localhost:27017`
- RabbitMQ and Eureka running through Docker Compose

## 2. Local startup

Recommended environment:

- JDK 17+
- Docker Desktop
- local MongoDB

Start the infrastructure first:

```powershell
docker compose up -d rabbitmq eureka-server
```

Then start the services from separate terminals:

```powershell
.\mvnw.cmd -f api-gateway/pom.xml spring-boot:run
.\mvnw.cmd -f user-service/pom.xml spring-boot:run
.\mvnw.cmd -f restaurant-service/pom.xml spring-boot:run
.\mvnw.cmd -f reservation-service/pom.xml spring-boot:run
.\mvnw.cmd -f order-service/pom.xml spring-boot:run
.\mvnw.cmd -f billing-service/pom.xml spring-boot:run
```

Useful URLs:

- Gateway: `http://localhost:8080`
- Eureka dashboard: `http://localhost:8761`
- RabbitMQ management: `http://localhost:15672`
- User Service: `http://localhost:8081`
- Restaurant Service: `http://localhost:8082`
- Reservation Service: `http://localhost:8083`
- Order Service: `http://localhost:8084`
- Billing Service: `http://localhost:8085`

## 3. Postman environment

Create one Postman environment with these variables:

- `base_url` = `http://localhost:8080`
- `customer_token`
- `admin_token`
- `kitchen_token`
- `restaurant_id`
- `category_id`
- `menu_id`
- `table_id`
- `order_id`
- `invoice_id`
- `reservation_id`
- `user_id`

Use these headers on protected requests:

- `Authorization: Bearer {{token}}`
- `Content-Type: application/json`

## 4. Auth APIs

### 4.1 Register user

- Method: `POST`
- URL: `{{base_url}}/api/auth/register`
- Auth: none
- Body:

```json
{
  "email": "customer@example.com",
  "password": "Pass1234!",
  "fullName": "Customer One",
  "phone": "+8801700000000"
}
```

Expected:

- `200 OK`
- Response message: `Registration successful`
- Response `data` contains access token, refresh token, and user profile fields

Important:

- The field name is `fullName`, not `name`
- `password` must be at least 8 characters

### 4.2 Login

- Method: `POST`
- URL: `{{base_url}}/api/auth/login`
- Auth: none
- Body:

```json
{
  "email": "customer@example.com",
  "password": "Pass1234!"
}
```

Expected:

- `200 OK`
- Response message: `Login successful`
- Response `data` contains access token, refresh token, and profile information

### 4.3 Refresh token

- Method: `POST`
- URL: `{{base_url}}/api/auth/refresh`
- Auth: none
- Body:

```json
{
  "refreshToken": "{{refresh_token}}"
}
```

Expected:

- `200 OK`
- Response message: `Token refreshed`
- New access token in `data`

### 4.4 Logout

- Method: `POST`
- URL: `{{base_url}}/api/auth/logout`
- Auth: none
- Body:

```json
{
  "refreshToken": "{{refresh_token}}"
}
```

Expected:

- `200 OK`
- Response message: `Logged out`

## 5. User APIs

### 5.1 Get my profile

- Method: `GET`
- URL: `{{base_url}}/api/users/me`
- Auth: customer/admin/kitchen token

Expected:

- `200 OK`
- `data` contains the current user's profile

### 5.2 Update my profile

- Method: `PUT`
- URL: `{{base_url}}/api/users/me`
- Auth: current user token
- Body:

```json
{
  "fullName": "Customer One Updated",
  "phone": "+8801711111111"
}
```

Expected:

- `200 OK`
- Response message: `Profile updated`

### 5.3 Assign kitchen restaurant

- Method: `PATCH`
- URL: `{{base_url}}/api/users/{{user_id}}/kitchen-restaurant`
- Auth: `RESTAURANT_ADMIN` or `SUPER_ADMIN`
- Body:

```json
{
  "restaurantId": "{{restaurant_id}}"
}
```

Expected:

- `200 OK`
- Response message: `Kitchen restaurant assigned`

### 5.4 Address list

- Method: `GET`
- URL: `{{base_url}}/api/users/me/addresses`
- Auth: current user token

Expected:

- `200 OK`
- `data` is a list of addresses

### 5.5 Create address

- Method: `POST`
- URL: `{{base_url}}/api/users/me/addresses`
- Auth: current user token
- Body:

```json
{
  "label": "Home",
  "line1": "123 Main Street",
  "line2": "Apt 4B",
  "city": "Dhaka",
  "state": "Dhaka",
  "postalCode": "1205",
  "country": "Bangladesh",
  "defaultAddress": true
}
```

Expected:

- `200 OK`
- Response message: `Address created`

### 5.6 Update address

- Method: `PUT`
- URL: `{{base_url}}/api/users/me/addresses/{{address_id}}`
- Auth: current user token
- Body: same shape as create address

Expected:

- `200 OK`
- Response message: `Address updated`

### 5.7 Delete address

- Method: `DELETE`
- URL: `{{base_url}}/api/users/me/addresses/{{address_id}}`
- Auth: current user token

Expected:

- `200 OK`
- Response message: `Address deleted`

## 6. Restaurant APIs

### 6.1 Create restaurant

- Method: `POST`
- URL: `{{base_url}}/api/restaurants`
- Auth: `RESTAURANT_ADMIN`
- Body:

```json
{
  "name": "DinePilot Bistro",
  "description": "Demo restaurant",
  "address": {
    "line1": "12 Food Street",
    "line2": "",
    "city": "Dhaka",
    "state": "Dhaka",
    "postalCode": "1207",
    "country": "Bangladesh"
  },
  "openingHours": [],
  "active": true
}
```

Expected:

- `200 OK`
- Response message: `Restaurant created`
- Save returned restaurant id to `restaurant_id`

### 6.2 List restaurants

- Method: `GET`
- URL: `{{base_url}}/api/restaurants`
- Auth: none

Expected:

- `200 OK`
- `data` contains all restaurants

### 6.3 Get restaurant by id

- Method: `GET`
- URL: `{{base_url}}/api/restaurants/{{restaurant_id}}`
- Auth: none

Expected:

- `200 OK`
- `data` contains the restaurant

### 6.4 Update restaurant

- Method: `PUT`
- URL: `{{base_url}}/api/restaurants/{{restaurant_id}}`
- Auth: `RESTAURANT_ADMIN`
- Body: same shape as create restaurant

Expected:

- `200 OK`
- Response message: `Restaurant updated`

### 6.5 Delete restaurant

- Method: `DELETE`
- URL: `{{base_url}}/api/restaurants/{{restaurant_id}}`
- Auth: `RESTAURANT_ADMIN`

Expected:

- `200 OK`
- Response message: `Restaurant deleted`

### 6.6 Create category

- Method: `POST`
- URL: `{{base_url}}/api/categories`
- Auth: restaurant admin
- Body:

```json
{
  "restaurantId": "{{restaurant_id}}",
  "name": "Main Course",
  "description": "Primary dishes",
  "displayOrder": 1
}
```

Expected:

- `200 OK`
- Response message: `Category created`
- Save id to `category_id`

### 6.7 List categories

- Method: `GET`
- URL: `{{base_url}}/api/categories?restaurantId={{restaurant_id}}`
- Auth: none

Expected:

- `200 OK`
- `data` contains category list

### 6.8 Update category

- Method: `PUT`
- URL: `{{base_url}}/api/categories/{{category_id}}`
- Auth: restaurant admin
- Body: same shape as create category

Expected:

- `200 OK`
- Response message: `Category updated`

### 6.9 Delete category

- Method: `DELETE`
- URL: `{{base_url}}/api/categories/{{category_id}}`
- Auth: restaurant admin

Expected:

- `200 OK`
- Response message: `Category deleted`

### 6.10 Create menu item

- Method: `POST`
- URL: `{{base_url}}/api/menu`
- Auth: restaurant admin
- Body:

```json
{
  "restaurantId": "{{restaurant_id}}",
  "categoryId": "{{category_id}}",
  "name": "Grilled Chicken Steak",
  "description": "Served with veggies",
  "price": 850.00,
  "available": true
}
```

Expected:

- `200 OK`
- Response message: `Food created`
- Save id to `menu_id`

### 6.11 Search menu

- Method: `GET`
- URL: `{{base_url}}/api/menu?restaurantId={{restaurant_id}}&categoryId={{category_id}}&available=true&q=steak`
- Auth: none

Expected:

- `200 OK`
- `data` contains matching menu items

### 6.12 Get menu item by id

- Method: `GET`
- URL: `{{base_url}}/api/menu/{{menu_id}}`
- Auth: none

Expected:

- `200 OK`
- `data` contains the menu item

### 6.13 Update menu item

- Method: `PUT`
- URL: `{{base_url}}/api/menu/{{menu_id}}`
- Auth: restaurant admin
- Body: same shape as create menu item

Expected:

- `200 OK`
- Response message: `Food updated`

### 6.14 Update availability

- Method: `PATCH`
- URL: `{{base_url}}/api/menu/{{menu_id}}/availability`
- Auth: restaurant admin
- Body:

```json
{
  "available": false
}
```

Expected:

- `200 OK`
- Response message: `Availability updated`

### 6.15 Delete menu item

- Method: `DELETE`
- URL: `{{base_url}}/api/menu/{{menu_id}}`
- Auth: restaurant admin

Expected:

- `200 OK`
- Response message: `Food deleted`

### 6.16 Create table

- Method: `POST`
- URL: `{{base_url}}/api/tables`
- Auth: restaurant admin
- Body:

```json
{
  "restaurantId": "{{restaurant_id}}",
  "tableNumber": "T1",
  "capacity": 4
}
```

Expected:

- `200 OK`
- Response message: `Table created`
- Save id to `table_id`

### 6.17 List tables

- Method: `GET`
- URL: `{{base_url}}/api/tables?restaurantId={{restaurant_id}}`
- Auth: none

Expected:

- `200 OK`
- `data` contains tables

### 6.18 Get table by id

- Method: `GET`
- URL: `{{base_url}}/api/tables/{{table_id}}`
- Auth: none

Expected:

- `200 OK`
- `data` contains the table

### 6.19 Update table

- Method: `PUT`
- URL: `{{base_url}}/api/tables/{{table_id}}`
- Auth: restaurant admin
- Body: same shape as create table

Expected:

- `200 OK`
- Response message: `Table updated`

### 6.20 Update table status

- Method: `PATCH`
- URL: `{{base_url}}/api/tables/{{table_id}}/status`
- Auth: restaurant admin
- Body:

```json
{
  "status": "AVAILABLE"
}
```

Expected:

- `200 OK`
- Response message: `Status updated`

### 6.21 Delete table

- Method: `DELETE`
- URL: `{{base_url}}/api/tables/{{table_id}}`
- Auth: restaurant admin

Expected:

- `200 OK`
- Response message: `Table deleted`

## 7. Cart APIs

### 7.1 View cart

- Method: `GET`
- URL: `{{base_url}}/api/cart`
- Auth: customer token

Expected:

- `200 OK`
- `data` contains current cart or an empty cart response

### 7.2 Add item to cart

- Method: `POST`
- URL: `{{base_url}}/api/cart/items`
- Auth: customer token
- Body:

```json
{
  "foodId": "{{menu_id}}",
  "quantity": 2
}
```

Expected:

- `200 OK`
- Response message: `Item added`
- Cart total updates using live menu data

### 7.3 Update cart item quantity

- Method: `PUT`
- URL: `{{base_url}}/api/cart/items/{{menu_id}}`
- Auth: customer token
- Body:

```json
{
  "quantity": 3
}
```

Expected:

- `200 OK`
- Response message: `Item updated`

### 7.4 Remove cart item

- Method: `DELETE`
- URL: `{{base_url}}/api/cart/items/{{menu_id}}`
- Auth: customer token

Expected:

- `200 OK`
- Response message: `Item removed`

## 8. Order APIs

### 8.1 Place order

- Method: `POST`
- URL: `{{base_url}}/api/orders`
- Auth: customer token

Expected:

- `200 OK`
- Response message: `Order placed`
- Save id to `order_id`
- Cart is cleared after order creation

### 8.2 Order history

- Method: `GET`
- URL: `{{base_url}}/api/orders`
- Auth: customer token

Expected:

- `200 OK`
- `data` contains customer orders

### 8.3 Get order by id

- Method: `GET`
- URL: `{{base_url}}/api/orders/{{order_id}}`
- Auth: customer token

Expected:

- `200 OK`
- `data` contains order details and snapshots

### 8.4 Cancel order

- Method: `PATCH`
- URL: `{{base_url}}/api/orders/{{order_id}}/cancel`
- Auth: customer token

Expected:

- `200 OK`
- Response message: `Order cancelled`
- Only allowed when the order status rules permit cancellation

## 9. Kitchen order APIs

Kitchen requests require a user with `KITCHEN` role and a restaurant assignment in the JWT claims.

### 9.1 List kitchen orders

- Method: `GET`
- URL: `{{base_url}}/api/kitchen/orders`
- Auth: kitchen token

Expected:

- `200 OK`
- `data` contains orders for the assigned restaurant only

### 9.2 Accept order

- Method: `PATCH`
- URL: `{{base_url}}/api/kitchen/orders/{{order_id}}/accept`
- Auth: kitchen token

Expected:

- `200 OK`
- Response message: `Order accepted`

### 9.3 Mark preparing

- Method: `PATCH`
- URL: `{{base_url}}/api/kitchen/orders/{{order_id}}/preparing`
- Auth: kitchen token

Expected:

- `200 OK`
- Response message: `Order preparing`

### 9.4 Mark ready

- Method: `PATCH`
- URL: `{{base_url}}/api/kitchen/orders/{{order_id}}/ready`
- Auth: kitchen token

Expected:

- `200 OK`
- Response message: `Order ready`

### 9.5 Mark completed

- Method: `PATCH`
- URL: `{{base_url}}/api/kitchen/orders/{{order_id}}/completed`
- Auth: kitchen token

Expected:

- `200 OK`
- Response message: `Order completed`

This completion step is important because Billing Service expects completed orders before it generates invoices.

## 10. Reservation APIs

### 10.1 Book reservation

- Method: `POST`
- URL: `{{base_url}}/api/reservations`
- Auth: customer token
- Body:

```json
{
  "tableId": "{{table_id}}",
  "partySize": 4,
  "reservedFor": "2026-09-01T19:00:00Z",
  "notes": "Anniversary dinner"
}
```

Expected:

- `200 OK`
- Response message: `Reservation requested`
- Save id to `reservation_id`

Validation expectations:

- `tableId` must not be blank
- `partySize` must be positive
- `reservedFor` must be a future `Instant`
- `notes` must be 500 characters or fewer

### 10.2 Reservation history

- Method: `GET`
- URL: `{{base_url}}/api/reservations`
- Auth: customer token

Expected:

- `200 OK`
- `data` contains customer reservations

### 10.3 Get reservation by id

- Method: `GET`
- URL: `{{base_url}}/api/reservations/{{reservation_id}}`
- Auth: customer token

Expected:

- `200 OK`
- `data` contains the reservation

### 10.4 Cancel reservation

- Method: `PATCH`
- URL: `{{base_url}}/api/reservations/{{reservation_id}}/cancel`
- Auth: customer token

Expected:

- `200 OK`
- Response message: `Reservation cancelled`

### 10.5 List restaurant reservations

- Method: `GET`
- URL: `{{base_url}}/api/reservations/restaurant/{{restaurant_id}}`
- Auth: restaurant admin token

Expected:

- `200 OK`
- `data` contains reservations for that restaurant

### 10.6 Confirm reservation

- Method: `PATCH`
- URL: `{{base_url}}/api/reservations/restaurant/{{reservation_id}}/confirm`
- Auth: restaurant admin token

Expected:

- `200 OK`
- Response message: `Reservation confirmed`

### 10.7 Complete reservation

- Method: `PATCH`
- URL: `{{base_url}}/api/reservations/restaurant/{{reservation_id}}/complete`
- Auth: restaurant admin token

Expected:

- `200 OK`
- Response message: `Reservation completed`

### 10.8 Cancel reservation from restaurant side

- Method: `PATCH`
- URL: `{{base_url}}/api/reservations/restaurant/{{reservation_id}}/cancel`
- Auth: restaurant admin token

Expected:

- `200 OK`
- Response message: `Reservation cancelled`

## 11. Billing APIs

### 11.1 Generate invoice from order

- Method: `POST`
- URL: `{{base_url}}/api/invoices/orders/{{order_id}}`
- Auth: customer token or restaurant admin token

Expected:

- `200 OK`
- Response message: `Invoice generated`
- Save id to `invoice_id`

Rules:

- the order must already be `COMPLETED`
- the caller must own the order unless the caller is restaurant admin or super admin

### 11.2 List my invoices

- Method: `GET`
- URL: `{{base_url}}/api/invoices/me`
- Auth: customer token

Expected:

- `200 OK`
- `data` contains invoices for the current customer

### 11.3 Get invoice by id

- Method: `GET`
- URL: `{{base_url}}/api/invoices/{{invoice_id}}`
- Auth: customer token or restaurant admin token

Expected:

- `200 OK`
- `data` contains invoice details

### 11.4 Pay invoice

- Method: `POST`
- URL: `{{base_url}}/api/invoices/{{invoice_id}}/pay`
- Auth: customer token or restaurant admin token

Expected:

- `200 OK`
- Response message: `Payment completed`
- Payment record is created
- Invoice status becomes `PAID`

## 12. Expected happy-path demo flow

Use this order for a live demo:

1. Register customer, restaurant admin, and kitchen user.
2. Log in all three users and store tokens.
3. Promote users in MongoDB to the correct roles.
4. Create restaurant, category, menu item, and table as the restaurant admin.
5. Add menu item to cart as the customer.
6. Place the order as the customer.
7. Complete the order from the kitchen account.
8. Generate and pay the invoice.
9. Book a reservation as the customer.
10. Confirm and complete it as the restaurant admin.

## 13. What to verify

- Public endpoints work without tokens
- Protected endpoints reject missing or wrong-role tokens
- Request validation returns `400` with clear messages
- Cart rejects invalid food ids and invalid quantities
- Order snapshot values do not change after menu edits
- Kitchen actions only work for the assigned restaurant
- Reservation overlap rules are enforced
- Billing only generates invoices from completed orders
- Payment changes invoice state to `PAID`

## 14. Common validation errors

If you see `400 Bad Request`, check these first:

- `fullName` is missing on register or profile update
- `email` is missing or invalid
- `password` is too short
- `restaurantId`, `categoryId`, `foodId`, or `tableId` is blank
- `quantity` is less than 1
- `capacity` is not positive
- `reservedFor` is not in the future

## 15. Verification commands

Run backend tests:

```powershell
.\mvnw.cmd clean test
```

Run a specific module:

```powershell
.\mvnw.cmd -f user-service/pom.xml test
.\mvnw.cmd -f restaurant-service/pom.xml test
.\mvnw.cmd -f reservation-service/pom.xml test
.\mvnw.cmd -f order-service/pom.xml test
.\mvnw.cmd -f billing-service/pom.xml test
```

## 16. Notes

- MongoDB runs locally on `localhost:27017`
- RabbitMQ and Eureka are started with Docker Compose
- The services use Eureka automatically after startup
- If you change secrets or ports, update `.env` and the service YAML files together

