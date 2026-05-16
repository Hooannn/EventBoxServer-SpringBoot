# Event Test Kickoff

This document is the starting point for the `event` module test pass.

## Current State

- There are no event module tests in the current tree.
- The event module has two controllers and two services:
  - `src/main/java/com/ht/eventbox/modules/event/EventController.java`
  - `src/main/java/com/ht/eventbox/modules/event/VoucherController.java`
  - `src/main/java/com/ht/eventbox/modules/event/EventService.java`
  - `src/main/java/com/ht/eventbox/modules/event/VoucherService.java`

## Recommended Test Order

1. `EventServiceTests`
2. `EventControllerTests`
3. `VoucherServiceTests`
4. `VoucherControllerTests`

Service tests should come first because the event service contains the branching logic, authorization checks, and side effects. Controller tests can stay thin and focus on request mapping, status codes, and response payloads.

## First Event Service Targets

Start with the highest-risk paths in `EventService`:

- `getById`
- `getDiscovery`
- `search`
- `create`
- `update`
- `publishByAdmin`
- `archiveByAdmin`
- `archive`
- `inactive`
- `active`
- `updateTags`
- `getByIdAndStatusIsNot`
- `getWithRealStockByIdAndStatusIsNot`
- `getAllByStatusIn`
- `eventPayout`
- `isMember`

Suggested first assertions:

- missing event returns `EVENT_NOT_FOUND`
- status guards reject invalid transitions
- published and archived flows persist the expected state
- `search` handles province and non-province branches
- `getWithRealStockByIdAndStatusIsNot` subtracts reserved stock
- `eventPayout` fails for not-ended events, non-owners, and already-paid events

## First Event Controller Targets

Start with the controller routes that map directly to service branches:

- `GET /api/v1/events`
- `POST /api/v1/events/{eventId}/admin/publish`
- `POST /api/v1/events/{eventId}/admin/archive`
- `PUT /api/v1/events/{eventId}/admin/tags`
- `POST /api/v1/events`
- `PUT /api/v1/events/{eventId}`
- `POST /api/v1/events/{eventId}/archive`
- `POST /api/v1/events/{eventId}/inactive`
- `POST /api/v1/events/{eventId}/active`
- `GET /api/v1/events/organization/{organizationId}`
- `GET /api/v1/events/organization/{organizationId}/published`
- `GET /api/v1/events/{eventId}`
- `GET /api/v1/events/{eventId}/shows`
- `GET /api/v1/events/public/{eventId}`
- `GET /api/v1/events/discovery`
- `GET /api/v1/events/search`
- `GET /api/v1/events/categories/{categoryId}`
- `POST /api/v1/events/{eventId}/payout/request`

Keep controller tests focused on:

- request binding
- status codes
- response envelopes
- permission annotations being exercised through mocked auth setup

## Voucher Service Targets

Once the event service/controller base is in place, move to vouchers:

- `getAllByEventId`
- `getAllPublicByEventId`
- `createByEventId`
- `updateByEventId`
- `deleteByEventId`
- `getUsage`
- `getByOrderId`
- `applyByOrderId`
- `removeByOrderId`

High-value branches:

- membership authorization failures
- duplicate voucher code checks
- voucher usage and per-user limits
- order not found and voucher not found cases
- voucher time window validation
- voucher condition validation
- applied voucher removal for non-fulfilled orders

## Voucher Controller Targets

Controller routes to cover after the service layer:

- `GET /api/v1/vouchers/{id}/event/{eventId}/usage`
- `GET /api/v1/vouchers/order/{orderId}`
- `POST /api/v1/vouchers/order/{orderId}/apply`
- `POST /api/v1/vouchers/order/{orderId}/remove`
- `GET /api/v1/vouchers/event/{eventId}`
- `GET /api/v1/vouchers/event/{eventId}/public`
- `POST /api/v1/vouchers/event/{eventId}`
- `PUT /api/v1/vouchers/{id}/event/{eventId}`
- `DELETE /api/v1/vouchers/{id}/event/{eventId}`

## Suggested File Ownership For Parallel Work

If multiple agents are used in the next session, split by file so work stays isolated:

- Agent 1: `src/test/java/com/ht/eventbox/modules/event/EventServiceTests.java`
- Agent 2: `src/test/java/com/ht/eventbox/modules/event/EventControllerTests.java`
- Agent 3: `src/test/java/com/ht/eventbox/modules/event/VoucherServiceTests.java`
- Agent 4: `src/test/java/com/ht/eventbox/modules/event/VoucherControllerTests.java`

The service tests should be written first. Controller tests can follow once the service fixtures are stable.

## Likely Shared Fixtures

Expect to reuse fixtures for:

- organization owner/member relationships
- event statuses: `PENDING`, `PUBLISHED`, `DRAFT`, `ARCHIVED`
- event shows with ended and not-ended time windows
- tickets with reserved stock and total stock
- orders with fulfilled totals for payout calculations
- voucher objects with time windows and usage limits

## Verification

Use the narrowest useful test command while building:

```bash
./mvnw -Dtest=EventServiceTests,EventControllerTests,VoucherServiceTests,VoucherControllerTests test
```

If the event module is being built incrementally, run only the files that exist so far.
