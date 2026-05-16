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

- [src/test/java/com/ht/eventbox/modules/event/EventServiceTests.java](/Users/nguyenduckhaihoan/Working/backend/EventboxServer-SpringBoot-s0ngnguyen/src/test/java/com/ht/eventbox/modules/event/EventServiceTests.java)
- [src/test/java/com/ht/eventbox/modules/event/VoucherServiceTests.java](/Users/nguyenduckhaihoan/Working/backend/EventboxServer-SpringBoot-s0ngnguyen/src/test/java/com/ht/eventbox/modules/event/VoucherServiceTests.java)
- [src/test/java/com/ht/eventbox/modules/event/EventControllerTests.java](/Users/nguyenduckhaihoan/Working/backend/EventboxServer-SpringBoot-s0ngnguyen/src/test/java/com/ht/eventbox/modules/event/EventControllerTests.java)
- [src/test/java/com/ht/eventbox/modules/event/VoucherControllerTests.java](/Users/nguyenduckhaihoan/Working/backend/EventboxServer-SpringBoot-s0ngnguyen/src/test/java/com/ht/eventbox/modules/event/VoucherControllerTests.java)

Current coverage:

- `getById` returns an event when it exists and throws `EVENT_NOT_FOUND` when it does not
- `getDiscovery` queries featured, trending, and latest event buckets
- `search` covers province and non-province branches
- `create` persists a pending event with shows, categories, keywords, and assets
- `update` rejects non-pending events and persists updated metadata for pending events
- `publishByAdmin` publishes pending events
- `archiveByAdmin` archives pending events
- `archive`, `inactive`, and `active` enforce ownership and status guards
- `updateTags` rejects non-published events
- `getByIdAndStatusIsNot` and `getWithRealStockByIdAndStatusIsNot` cover stock and visibility reads
- `getAllByStatusIn` returns repository results directly
- `eventPayout` covers not-ended, non-owner, zero-total, and payout success flows
- `isMember` covers matching and non-matching organization roles
- `getAllByEventId` rejects non-members and returns vouchers for organization members
- `getAllPublicByEventId` covers the public time-window query
- `createByEventId`, `updateByEventId`, and `deleteByEventId` cover code uniqueness and used-voucher guards
- `getUsage` covers the fulfilled-order counting path
- `applyByOrderId` covers order lookup, voucher lookup, time-window, condition, usage-limit, per-user-limit, and success branches
- `removeByOrderId` covers fulfilled-order rejection and successful removal
- Event controller routes are covered for list, create, update, admin publish/archive, user archive/inactive/active, organization reads, event reads, discovery, search, category, public, shows, and payout request endpoints
- Voucher controller routes are covered for usage, order lookup, apply/remove, event list/public list, create, update, and delete endpoints

Current gaps:

- Repository-focused behavior is also unverified

Last verified command:

```bash
./mvnw -Dtest=EventServiceTests,VoucherServiceTests test
```

Result:

- `46` tests run
- `0` failures
- `0` errors

Latest verified command:

```bash
./mvnw -Dtest=EventControllerTests,VoucherControllerTests test
```

Result:

- `27` tests run
- `0` failures
- `0` errors

## Organization

Current test files:

- [src/test/java/com/ht/eventbox/modules/organization/OrganizationServiceTests.java](/Users/nguyenduckhaihoan/Working/backend/EventboxServer-SpringBoot-s0ngnguyen/src/test/java/com/ht/eventbox/modules/organization/OrganizationServiceTests.java)
- [src/test/java/com/ht/eventbox/modules/organization/OrganizationControllerTests.java](/Users/nguyenduckhaihoan/Working/backend/EventboxServer-SpringBoot-s0ngnguyen/src/test/java/com/ht/eventbox/modules/organization/OrganizationControllerTests.java)

Current coverage:

- `getById` returns an organization and throws `ORGANIZATION_NOT_FOUND` when missing
- `getDetailsById` combines organization data with subscriber and event counts
- `create` covers org creation with and without logo upload
- `update` covers owner checks, remove-logo, and replacement-logo branches
- `deleteById` rejects organizations with events and deletes allowed organizations
- `addMember` covers duplicate member, missing user, and success paths
- `updateMember` covers missing target and role update paths
- `removeMember` covers missing target and member removal paths
- `subscribe` covers add and remove toggle behavior
- Organization controller routes are covered for list, mine, member list, details, get by id, update, delete, create, add member, update member, remove member, and subscribe endpoints

Current gaps:

- Repository-focused behavior is also unverified

Last verified command:

```bash
./mvnw -Dtest=OrganizationServiceTests,OrganizationControllerTests test
```

Result:

- `32` tests run
- `0` failures
- `0` errors

## Ticket

Current test files:

- [src/test/java/com/ht/eventbox/modules/ticket/TicketServiceTests.java](/Users/nguyenduckhaihoan/Working/backend/EventboxServer-SpringBoot-s0ngnguyen/src/test/java/com/ht/eventbox/modules/ticket/TicketServiceTests.java)
- [src/test/java/com/ht/eventbox/modules/ticket/TicketControllerTests.java](/Users/nguyenduckhaihoan/Working/backend/EventboxServer-SpringBoot-s0ngnguyen/src/test/java/com/ht/eventbox/modules/ticket/TicketControllerTests.java)

Current coverage:

- `getTicketItemById` returns a projection and rejects missing items
- `getTicketItemQrCode` covers before-start, after-end, and success branches
- `validateTicketItem` covers invalid token, non-member, and success paths
- `createTicketItemTrace` persists traces through the validation flow
- `getTicketItemByShowId` rejects unauthorized users
- `createTicketItemFeedback` rejects unused tickets and persists valid feedback
- `giveawayTicketItem` controller wiring is covered and the service pass includes the self-gift, already-used, ended, and reminder-related safety branches
- `triggerReminder` covers the mail-failure return path
- `getLatestTicketItemFeedbackByOrganizationId` rejects missing organizations
- `getTicketItemFeedbackByEventId` rejects non-members
- Ticket controller routes are covered for my-items, item details, QR code, validate, traces, show-items, feedback, giveaway, organization feedback, event feedback, and reminder trigger endpoints

Current gaps:

- Repository-focused behavior is also unverified

Last verified command:

```bash
./mvnw -Dtest=TicketServiceTests,TicketControllerTests test
```

Result:

- `27` tests run
- `0` failures
- `0` errors
