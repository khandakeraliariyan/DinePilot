# Phase 7 - Event-Driven Communication

## Scope

Phase 7 adds RabbitMQ-based event delivery across reservation, order, restaurant, and billing services. Cross-service reads remain synchronous through HTTP, but state-changing side effects move to events.

## Shared events

- `ReservationCreatedEvent`
- `ReservationStatusChangedEvent`
- `OrderCreatedEvent`
- `OrderStatusChangedEvent`
- `PaymentCompletedEvent`

## Routing

All services publish to the `dinepilot.events` topic exchange.

Used routing keys:

- `reservation.created`
- `reservation.status.changed`
- `order.created`
- `order.status.changed`
- `payment.completed`

## Producers

- Reservation Service publishes reservation creation and status changes.
- Order Service publishes order creation and order status changes.
- Billing Service publishes payment completion.

## Consumers

- Restaurant Service consumes reservation events and updates `RestaurantTable.status`.
- Billing Service consumes completed order status changes and creates invoices.
- Order Service consumes payment completion events and marks orders as paid.

## Environment

- RabbitMQ host, port, username, and password are read from environment variables.
- The repository already provisions RabbitMQ in Docker Compose.

## Notes

- Event payloads are shared from `common`.
- Consumers are designed to be idempotent-friendly.
- Service-to-service reads still use synchronous HTTP where the current flow needs authoritative data.
