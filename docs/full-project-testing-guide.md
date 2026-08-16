# Full Project Testing and Demo Guide

This guide is ordered the way you should demo and test DinePilot in Postman.

## 1. Local startup

Start the infrastructure first:

```powershell
docker compose up -d rabbitmq eureka-server
```

Then start the services:

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
- Eureka: `http://localhost:8761`
- RabbitMQ UI: `http://localhost:15672`
- User Service: `http://localhost:8081`
- Restaurant Service: `http://localhost:8082`
- Reservation Service: `http://localhost:8083`
- Order Service: `http://localhost:8084`
- Billing Service: `http://localhost:8085`

## 2. Postman environment

Create these variables:

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
- `address_id`

Use this header for protected requests:

- `Authorization: Bearer {{token}}`
- `Content-Type: application/json`

## 3. Create the users first

### 3.1 Register customer

- Method: `POST`
- URL: `{{base_url}}/api/auth/register`
- Body:

```json
{
  "email": "customer@example.com",
  "password": "Pass1234!",
  "fullName": "Customer One",
  "phone": "+8801700000000"
}
```

Expected response:

```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "accessToken": "jwt-access-token",
    "refreshToken": "jwt-refresh-token",
    "user": {
      "id": "user-id",
      "email": "customer@example.com",
      "role": "CUSTOMER"
    }
  }
}
```

### 3.2 Register restaurant admin

- Method: `POST`
- URL: `{{base_url}}/api/auth/register`
- Body:

```json
{
  "email": "admin@example.com",
  "password": "Pass1234!",
  "fullName": "Restaurant Admin",
  "phone": "+8801711111111"
}
```

Expected response:

```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "accessToken": "jwt-access-token",
    "refreshToken": "jwt-refresh-token"
  }
}
```

### 3.3 Register kitchen user

- Method: `POST`
- URL: `{{base_url}}/api/auth/register`
- Body:

```json
{
  "email": "kitchen@example.com",
  "password": "Pass1234!",
  "fullName": "Kitchen User",
  "phone": "+8801722222222"
}
```

Expected response:

```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "accessToken": "jwt-access-token",
    "refreshToken": "jwt-refresh-token"
  }
}
```

## 4. Promote roles in MongoDB

After registration, update the user documents in MongoDB:

- customer stays `CUSTOMER`
- admin becomes `RESTAURANT_ADMIN`
- kitchen user becomes `KITCHEN`

If the kitchen user is going to process one restaurant, also assign that restaurant later with:

- `PATCH /api/users/{userId}/kitchen-restaurant`

## 5. Log in and save tokens

### 5.1 Customer login

- Method: `POST`
- URL: `{{base_url}}/api/auth/login`
- Body:

```json
{
  "email": "customer@example.com",
  "password": "Pass1234!"
}
```

Expected response:

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "customer-jwt",
    "refreshToken": "customer-refresh-token"
  }
}
```

Save `accessToken` to `customer_token`.

### 5.2 Admin login

- Method: `POST`
- URL: `{{base_url}}/api/auth/login`
- Body:

```json
{
  "email": "admin@example.com",
  "password": "Pass1234!"
}
```

Expected response:

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "admin-jwt",
    "refreshToken": "admin-refresh-token"
  }
}
```

Save `accessToken` to `admin_token`.

### 5.3 Kitchen login

- Method: `POST`
- URL: `{{base_url}}/api/auth/login`
- Body:

```json
{
  "email": "kitchen@example.com",
  "password": "Pass1234!"
}
```

Expected response:

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "kitchen-jwt",
    "refreshToken": "kitchen-refresh-token"
  }
}
```

Save `accessToken` to `kitchen_token`.

## 6. Create restaurant data

Do this with the restaurant admin token.

### 6.1 Create restaurant

- Method: `POST`
- URL: `{{base_url}}/api/restaurants`
- Header: `Authorization: Bearer {{admin_token}}`
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

Expected response:

```json
{
  "success": true,
  "message": "Restaurant created",
  "data": {
    "id": "restaurant-id",
    "name": "DinePilot Bistro"
  }
}
```

Save the restaurant id to `restaurant_id`.

### 6.2 Create category

- Method: `POST`
- URL: `{{base_url}}/api/categories`
- Header: `Authorization: Bearer {{admin_token}}`
- Body:

```json
{
  "restaurantId": "{{restaurant_id}}",
  "name": "Main Course",
  "description": "Primary dishes",
  "displayOrder": 1
}
```

Expected response:

```json
{
  "success": true,
  "message": "Category created",
  "data": {
    "id": "category-id",
    "restaurantId": "{{restaurant_id}}",
    "name": "Main Course"
  }
}
```

Save the id to `category_id`.

### 6.3 Create menu item

- Method: `POST`
- URL: `{{base_url}}/api/menu`
- Header: `Authorization: Bearer {{admin_token}}`
- Body:

```json
{
  "restaurantId": "{{restaurant_id}}",
  "categoryId": "{{category_id}}",
  "name": "Grilled Chicken Steak",
  "description": "Served with veggies",
  "price": 850,
  "available": true
}
```

Expected response:

```json
{
  "success": true,
  "message": "Food created",
  "data": {
    "id": "menu-id",
    "name": "Grilled Chicken Steak",
    "available": true
  }
}
```

Save the id to `menu_id`.

### 6.4 Create table

- Method: `POST`
- URL: `{{base_url}}/api/tables`
- Header: `Authorization: Bearer {{admin_token}}`
- Body:

```json
{
  "restaurantId": "{{restaurant_id}}",
  "tableNumber": "T1",
  "capacity": 4
}
```

Expected response:

```json
{
  "success": true,
  "message": "Table created",
  "data": {
    "id": "table-id",
    "tableNumber": "T1",
    "capacity": 4
  }
}
```

Save the id to `table_id`.

## 7. Run customer CRUD flow

Do this with the customer token.

### 7.1 Get my profile

- Method: `GET`
- URL: `{{base_url}}/api/users/me`
- Header: `Authorization: Bearer {{customer_token}}`

Expected response:

```json
{
  "success": true,
  "message": null,
  "data": {
    "id": "user-id",
    "email": "customer@example.com",
    "fullName": "Customer One"
  }
}
```

### 7.2 Update my profile

- Method: `PUT`
- URL: `{{base_url}}/api/users/me`
- Header: `Authorization: Bearer {{customer_token}}`
- Body:

```json
{
  "fullName": "Customer One Updated",
  "phone": "+8801711111111"
}
```

Expected response:

```json
{
  "success": true,
  "message": "Profile updated",
  "data": {
    "fullName": "Customer One Updated",
    "phone": "+8801711111111"
  }
}
```

### 7.3 Create address

- Method: `POST`
- URL: `{{base_url}}/api/users/me/addresses`
- Header: `Authorization: Bearer {{customer_token}}`
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

Expected response:

```json
{
  "success": true,
  "message": "Address created",
  "data": {
    "id": "address-id",
    "label": "Home",
    "city": "Dhaka"
  }
}
```

Save the id to `address_id`.

### 7.4 List addresses

- Method: `GET`
- URL: `{{base_url}}/api/users/me/addresses`
- Header: `Authorization: Bearer {{customer_token}}`

Expected response:

```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": "address-id",
      "label": "Home"
    }
  ]
}
```

### 7.5 Update address

- Method: `PUT`
- URL: `{{base_url}}/api/users/me/addresses/{{address_id}}`
- Header: `Authorization: Bearer {{customer_token}}`
- Body:

```json
{
  "label": "Home Updated",
  "line1": "123 Main Street",
  "line2": "Apt 4B",
  "city": "Dhaka",
  "state": "Dhaka",
  "postalCode": "1205",
  "country": "Bangladesh",
  "defaultAddress": true
}
```

Expected response:

```json
{
  "success": true,
  "message": "Address updated",
  "data": {
    "id": "address-id",
    "label": "Home Updated"
  }
}
```

### 7.6 Delete address

- Method: `DELETE`
- URL: `{{base_url}}/api/users/me/addresses/{{address_id}}`
- Header: `Authorization: Bearer {{customer_token}}`

Expected response:

```json
{
  "success": true,
  "message": "Address deleted",
  "data": null
}
```

## 8. Cart and order flow

### 8.1 View cart

- Method: `GET`
- URL: `{{base_url}}/api/cart`
- Header: `Authorization: Bearer {{customer_token}}`

Expected response:

```json
{
  "success": true,
  "message": null,
  "data": {
    "id": "cart-id",
    "restaurantId": "{{restaurant_id}}",
    "items": [],
    "total": 0
  }
}
```

### 8.2 Add item to cart

- Method: `POST`
- URL: `{{base_url}}/api/cart/items`
- Header: `Authorization: Bearer {{customer_token}}`
- Body:

```json
{
  "foodId": "{{menu_id}}",
  "quantity": 2
}
```

Expected response:

```json
{
  "success": true,
  "message": "Item added",
  "data": {
    "restaurantId": "{{restaurant_id}}",
    "items": [
      {
        "foodId": "{{menu_id}}",
        "quantity": 2
      }
    ],
    "total": 1700
  }
}
```

### 8.3 Update cart item quantity

- Method: `PUT`
- URL: `{{base_url}}/api/cart/items/{{menu_id}}`
- Header: `Authorization: Bearer {{customer_token}}`
- Body:

```json
{
  "quantity": 3
}
```

Expected response:

```json
{
  "success": true,
  "message": "Item updated",
  "data": {
    "items": [
      {
        "foodId": "{{menu_id}}",
        "quantity": 3
      }
    ],
    "total": 2550
  }
}
```

### 8.4 Remove cart item

- Method: `DELETE`
- URL: `{{base_url}}/api/cart/items/{{menu_id}}`
- Header: `Authorization: Bearer {{customer_token}}`

Expected response:

```json
{
  "success": true,
  "message": "Item removed",
  "data": {
    "items": [],
    "total": 0
  }
}
```

### 8.5 Place order

- Method: `POST`
- URL: `{{base_url}}/api/orders`
- Header: `Authorization: Bearer {{customer_token}}`

Expected response:

```json
{
  "success": true,
  "message": "Order placed",
  "data": {
    "id": "order-id",
    "status": "PLACED",
    "restaurantId": "{{restaurant_id}}"
  }
}
```

Save the id to `order_id`.

### 8.6 Order history

- Method: `GET`
- URL: `{{base_url}}/api/orders`
- Header: `Authorization: Bearer {{customer_token}}`

Expected response:

```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": "order-id",
      "status": "PLACED"
    }
  ]
}
```

### 8.7 Get order by id

- Method: `GET`
- URL: `{{base_url}}/api/orders/{{order_id}}`
- Header: `Authorization: Bearer {{customer_token}}`

Expected response:

```json
{
  "success": true,
  "message": null,
  "data": {
    "id": "order-id",
    "status": "PLACED"
  }
}
```

### 8.8 Cancel order

- Method: `PATCH`
- URL: `{{base_url}}/api/orders/{{order_id}}/cancel`
- Header: `Authorization: Bearer {{customer_token}}`

Expected response:

```json
{
  "success": true,
  "message": "Order cancelled",
  "data": {
    "id": "order-id",
    "status": "CANCELLED"
  }
}
```

## 9. Kitchen workflow

Before this step, make sure the kitchen user has a restaurant assignment.

### 9.1 Assign kitchen restaurant

- Method: `PATCH`
- URL: `{{base_url}}/api/users/{{user_id}}/kitchen-restaurant`
- Header: `Authorization: Bearer {{admin_token}}`
- Body:

```json
{
  "restaurantId": "{{restaurant_id}}"
}
```

Expected response:

```json
{
  "success": true,
  "message": "Kitchen restaurant assigned",
  "data": {
    "id": "kitchen-user-id",
    "restaurantId": "{{restaurant_id}}"
  }
}
```

### 9.2 List kitchen orders

- Method: `GET`
- URL: `{{base_url}}/api/kitchen/orders`
- Header: `Authorization: Bearer {{kitchen_token}}`

Expected response:

```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": "order-id",
      "status": "PLACED"
    }
  ]
}
```

### 9.3 Accept order

- Method: `PATCH`
- URL: `{{base_url}}/api/kitchen/orders/{{order_id}}/accept`
- Header: `Authorization: Bearer {{kitchen_token}}`

Expected response:

```json
{
  "success": true,
  "message": "Order accepted",
  "data": {
    "id": "order-id",
    "status": "PREPARING"
  }
}
```

### 9.4 Mark preparing

- Method: `PATCH`
- URL: `{{base_url}}/api/kitchen/orders/{{order_id}}/preparing`
- Header: `Authorization: Bearer {{kitchen_token}}`

Expected response:

```json
{
  "success": true,
  "message": "Order preparing",
  "data": {
    "id": "order-id",
    "status": "PREPARING"
  }
}
```

### 9.5 Mark ready

- Method: `PATCH`
- URL: `{{base_url}}/api/kitchen/orders/{{order_id}}/ready`
- Header: `Authorization: Bearer {{kitchen_token}}`

Expected response:

```json
{
  "success": true,
  "message": "Order ready",
  "data": {
    "id": "order-id",
    "status": "READY"
  }
}
```

### 9.6 Mark completed

- Method: `PATCH`
- URL: `{{base_url}}/api/kitchen/orders/{{order_id}}/completed`
- Header: `Authorization: Bearer {{kitchen_token}}`

Expected response:

```json
{
  "success": true,
  "message": "Order completed",
  "data": {
    "id": "order-id",
    "status": "COMPLETED"
  }
}
```

## 10. Billing flow

### 10.1 Generate invoice

- Method: `POST`
- URL: `{{base_url}}/api/invoices/orders/{{order_id}}`
- Header: `Authorization: Bearer {{customer_token}}`

Expected response:

```json
{
  "success": true,
  "message": "Invoice generated",
  "data": {
    "id": "invoice-id",
    "orderId": "{{order_id}}",
    "status": "PENDING"
  }
}
```

Save the id to `invoice_id`.

### 10.2 List my invoices

- Method: `GET`
- URL: `{{base_url}}/api/invoices/me`
- Header: `Authorization: Bearer {{customer_token}}`

Expected response:

```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": "invoice-id",
      "status": "PENDING"
    }
  ]
}
```

### 10.3 Get invoice by id

- Method: `GET`
- URL: `{{base_url}}/api/invoices/{{invoice_id}}`
- Header: `Authorization: Bearer {{customer_token}}`

Expected response:

```json
{
  "success": true,
  "message": null,
  "data": {
    "id": "invoice-id",
    "status": "PENDING"
  }
}
```

### 10.4 Pay invoice

- Method: `POST`
- URL: `{{base_url}}/api/invoices/{{invoice_id}}/pay`
- Header: `Authorization: Bearer {{customer_token}}`

Expected response:

```json
{
  "success": true,
  "message": "Payment completed",
  "data": {
    "id": "payment-id",
    "status": "COMPLETED"
  }
}
```

## 11. Reservation flow

### 11.1 Book reservation

- Method: `POST`
- URL: `{{base_url}}/api/reservations`
- Header: `Authorization: Bearer {{customer_token}}`
- Body:

```json
{
  "tableId": "{{table_id}}",
  "partySize": 4,
  "reservedFor": "2026-09-01T19:00:00Z",
  "notes": "Anniversary dinner"
}
```

Expected response:

```json
{
  "success": true,
  "message": "Reservation requested",
  "data": {
    "id": "reservation-id",
    "status": "PENDING",
    "tableId": "{{table_id}}"
  }
}
```

Save the id to `reservation_id`.

### 11.2 Reservation history

- Method: `GET`
- URL: `{{base_url}}/api/reservations`
- Header: `Authorization: Bearer {{customer_token}}`

Expected response:

```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": "reservation-id",
      "status": "PENDING"
    }
  ]
}
```

### 11.3 Get reservation by id

- Method: `GET`
- URL: `{{base_url}}/api/reservations/{{reservation_id}}`
- Header: `Authorization: Bearer {{customer_token}}`

Expected response:

```json
{
  "success": true,
  "message": null,
  "data": {
    "id": "reservation-id",
    "status": "PENDING"
  }
}
```

### 11.4 Cancel reservation

- Method: `PATCH`
- URL: `{{base_url}}/api/reservations/{{reservation_id}}/cancel`
- Header: `Authorization: Bearer {{customer_token}}`

Expected response:

```json
{
  "success": true,
  "message": "Reservation cancelled",
  "data": {
    "id": "reservation-id",
    "status": "CANCELLED"
  }
}
```

### 11.5 List restaurant reservations

- Method: `GET`
- URL: `{{base_url}}/api/reservations/restaurant/{{restaurant_id}}`
- Header: `Authorization: Bearer {{admin_token}}`

Expected response:

```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": "reservation-id",
      "status": "PENDING"
    }
  ]
}
```

### 11.6 Confirm reservation

- Method: `PATCH`
- URL: `{{base_url}}/api/reservations/restaurant/{{reservation_id}}/confirm`
- Header: `Authorization: Bearer {{admin_token}}`

Expected response:

```json
{
  "success": true,
  "message": "Reservation confirmed",
  "data": {
    "id": "reservation-id",
    "status": "CONFIRMED"
  }
}
```

### 11.7 Complete reservation

- Method: `PATCH`
- URL: `{{base_url}}/api/reservations/restaurant/{{reservation_id}}/complete`
- Header: `Authorization: Bearer {{admin_token}}`

Expected response:

```json
{
  "success": true,
  "message": "Reservation completed",
  "data": {
    "id": "reservation-id",
    "status": "COMPLETED"
  }
}
```

### 11.8 Cancel reservation from restaurant side

- Method: `PATCH`
- URL: `{{base_url}}/api/reservations/restaurant/{{reservation_id}}/cancel`
- Header: `Authorization: Bearer {{admin_token}}`

Expected response:

```json
{
  "success": true,
  "message": "Reservation cancelled",
  "data": {
    "id": "reservation-id",
    "status": "CANCELLED"
  }
}
```

## 12. Suggested demo order

Use this exact order during testing:

1. Register customer, restaurant admin, and kitchen user
2. Promote the roles in MongoDB
3. Log in all three users and save the tokens
4. Create restaurant, category, menu item, and table
5. Update customer profile and address
6. Add item to cart, update it, remove it, then add it again
7. Place the order
8. Assign the kitchen restaurant
9. Accept, prepare, ready, and complete the order
10. Generate the invoice and pay it
11. Book the reservation
12. Confirm and complete it from the restaurant admin account

## 13. Common validation failures

If you get `400 Bad Request`, check:

- `fullName` is missing
- `email` is invalid
- `password` is too short
- `restaurantId`, `categoryId`, `foodId`, or `tableId` is blank
- `quantity` is less than 1
- `capacity` is not positive
- `reservedFor` is not in the future

If you get `403 Forbidden`, check:

- the token belongs to the wrong role
- the kitchen user is not assigned to the restaurant
- the token is expired or stale

## 14. Final check

Before presenting, verify:

- `GET /api/users/me` works with a fresh token
- restaurant admin can create resources
- customer can add items to cart
- order can be placed and completed
- invoice can be generated from a completed order
- reservation can be booked and confirmed

