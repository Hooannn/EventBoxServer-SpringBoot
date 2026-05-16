# Testing Coverage Map

This document tracks the current test coverage in the repository, grouped by module.

## Auth

Current test file:

- [src/test/java/com/ht/eventbox/modules/auth/AuthControllerTests.java](/Users/nguyenduckhaihoan/Working/backend/EventboxServer-SpringBoot-s0ngnguyen/src/test/java/com/ht/eventbox/modules/auth/AuthControllerTests.java)

Current controller coverage:

- `login` sets access and refresh cookies and returns token payload
- `refresh` reads the refresh token from cookie and returns new tokens
- `refresh` returns `INVALID_TOKEN` when the token is missing

Current gaps:

- No auth service test file is present in the current tree
- No auth validation-failure controller cases are covered yet

## Order

Current test files:

- [src/test/java/com/ht/eventbox/modules/order/OrderServiceTests.java](/Users/nguyenduckhaihoan/Working/backend/EventboxServer-SpringBoot-s0ngnguyen/src/test/java/com/ht/eventbox/modules/order/OrderServiceTests.java)
- [src/test/java/com/ht/eventbox/modules/order/OrderControllerTests.java](/Users/nguyenduckhaihoan/Working/backend/EventboxServer-SpringBoot-s0ngnguyen/src/test/java/com/ht/eventbox/modules/order/OrderControllerTests.java)

Current service coverage:

- `createReservation` persists an order and reserves tickets
- `createReservation` rejects sales that have not started
- `cancelReservation` clears waiting orders
- `cancelReservation` by order id
- `getByShowId` rejects users without manager or owner role
- `createPayment` reuses an existing PayPal session
- `createPayment` creates a new PayPal order and session
- `createPayment` rejects a missing order
- `processPayment` approves and fulfills an active order
- `processPayment` refunds an expired order
- `refund` stores a missed-refund audit when PayPal does not return success
- `refund` persists full refund details and triggers notifications
- `getByShowId` returns orders for an authorized manager
- `getByShowId` rejects a missing event

Current controller coverage:

- `createReservation`
- `createPayment`
- `cancelReservation`
- `cancelReservation` by order id
- `getByShowId`
- `getByShowId/all`
- `handlePaypalWebhookCheckout` happy path
- `handlePaypalWebhookCheckout` invalid webhook path
- `handlePaypalWebhookPayment` expired-order refund path
- `handlePaypalWebhookPayment` active-order fulfillment path
- `handlePaypalWebhookPayment` already-processed skip path
- `handlePaypalWebhookPayment` invalid webhook path

Current gaps:

- No explicit controller test yet for `getByShowId` unauthorized access
- No explicit service test yet for the `ORDER_NOT_FOUND` branch inside refund lookup
- No explicit service test yet for the deprecated `fulfill(long orderId)` overload

Last verified command:

```bash
./mvnw -Dtest=OrderServiceTests,OrderControllerTests test
```

Result:

- `26` tests run
- `0` failures
- `0` errors

## Event

Current test files:

- None in the current tree

Current coverage:

- No `event` module tests are present yet

Current gaps:

- Controller coverage is missing
- Service coverage is missing
- Repository-focused behavior is also unverified

## Organization

Current test files:

- None in the current tree

Current coverage:

- No `organization` module tests are present yet

Current gaps:

- Controller coverage is missing
- Service coverage is missing
- Authorization and member-management paths are unverified

## Ticket

Current test files:

- None in the current tree

Current coverage:

- No `ticket` module tests are present yet

Current gaps:

- Controller coverage is missing
- Service coverage is missing
- Stock, QR, validation, and reminder flows are unverified
