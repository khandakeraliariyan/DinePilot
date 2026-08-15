# Phase 6 - Billing Service

## Scope

Billing Service owns invoices and simulated payments for completed orders. It does not own the order itself; it reads order data to create an invoice and then tracks payment state locally.

## Service boundary

- Port `8085`
- Mongo database `billing_db`
- Eureka name `BILLING-SERVICE`
- Gateway routes under `/api/invoices/**` and `/api/payments/**`

## Data model

### Invoice

- `orderId`
- `userId`
- `restaurantId`
- `status` (`PENDING`, `PAID`, `VOID`)
- `amount`
- `receiptNumber`

### Payment

- `invoiceId`
- `status` (`PROCESSING`, `COMPLETED`, `FAILED`)
- `amount`
- `processedAt`
- `providerRef`

## Runtime behavior

- Billing Service reads `order-service` through Eureka using `RestClient`.
- Invoice creation only happens for completed orders.
- Customers can only create, read, and pay their own invoices.
- A simulated payment marks the invoice as paid and stores a fake provider reference.

## API

| Method | Path | Access |
|---|---|---|
| POST | `/api/invoices/orders/{orderId}` | Authenticated user |
| GET | `/api/invoices/me` | Customer |
| GET | `/api/invoices/{id}` | Owning customer or elevated role |
| POST | `/api/invoices/{id}/pay` | Owning customer or elevated role |

## Testing strategy

Phase 6 is covered with Mockito-based unit tests around the service boundary. Order lookups and repository writes are mocked.

## Notes

- Payment is simulated. There is no external gateway integration.
- Billing publishes `PaymentCompleted` for Phase 7 consumers.
