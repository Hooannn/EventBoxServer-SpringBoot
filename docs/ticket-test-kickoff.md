# Ticket Test Kickoff

This document is the starting point for the `ticket` module test pass.

## Current State

- There are no ticket module tests in the current tree.
- The ticket module has one controller and one service:
  - [src/main/java/com/ht/eventbox/modules/ticket/TicketController.java](/Users/nguyenduckhaihoan/Working/backend/EventboxServer-SpringBoot-s0ngnguyen/src/main/java/com/ht/eventbox/modules/ticket/TicketController.java)
  - [src/main/java/com/ht/eventbox/modules/ticket/TicketService.java](/Users/nguyenduckhaihoan/Working/backend/EventboxServer-SpringBoot-s0ngnguyen/src/main/java/com/ht/eventbox/modules/ticket/TicketService.java)

## Recommended Test Order

1. `TicketServiceTests`
2. `TicketControllerTests`

Service tests should come first because the ticket service contains QR generation, validation rules, trace creation, feedback checks, giveaway flows, reminders, and authorization logic. Controller tests can remain thin and focus on request mapping, status codes, and response payloads.

## First Ticket Service Targets

Start with the highest-risk service methods:

- `getTicketItemsByUserIdAndOrderStatusIs`
- `getTicketItemsByUserIdAndOrderStatusIn`
- `getTicketItemById`
- `getTicketItemQrCode`
- `validateTicketItem`
- `createTicketItemTrace`
- `getTicketItemByShowId`
- `createTicketItemFeedback`
- `triggerReminder`
- `remindUpcomingEvents`
- `giveawayTicketItem`
- `getLatestTicketItemFeedbackByOrganizationId`
- `getTicketItemFeedbackByEventId`

Suggested first assertions:

- missing ticket item returns `TICKET_ITEM_NOT_FOUND`
- QR code generation rejects tickets before show start and after show end
- validation rejects invalid or empty QR tokens
- validation rejects users who are not organization members
- trace creation alternates between `CHECKED_IN` and `WENT_OUT`
- show-based queries reject missing events and unauthorized users
- feedback rejects unused tickets and tickets from unfinished shows
- reminder flow handles email or push failures without crashing
- giveaway rejects self-gifting, used tickets, ended shows, and missing recipients

## First Ticket Controller Targets

Start with the controller routes that map directly to service behavior:

- `GET /api/v1/tickets/items/me`
- `GET /api/v1/tickets/items/{ticketItemId}`
- `GET /api/v1/tickets/items/{ticketItemId}/qrcode`
- `POST /api/v1/tickets/validate`
- `POST /api/v1/tickets/traces`
- `GET /api/v1/tickets/shows/{showId}/items`
- `POST /api/v1/tickets/items/{ticketItemId}/feedback`
- `POST /api/v1/tickets/items/{ticketItemId}/giveaway`
- `GET /api/v1/tickets/items/feedback/organizations/{organizationId}`
- `GET /api/v1/tickets/items/feedback/event/{eventId}`
- `POST /api/v1/tickets/items/{ticketItemId}/reminder/trigger`

Keep controller tests focused on:

- request binding
- response wrappers
- status codes
- permissions on protected endpoints

## Suggested File Ownership For Parallel Work

If multiple agents are used in the next session, split by file so work stays isolated:

- Agent 1: `src/test/java/com/ht/eventbox/modules/ticket/TicketServiceTests.java`
- Agent 2: `src/test/java/com/ht/eventbox/modules/ticket/TicketControllerTests.java`

## Likely Shared Fixtures

Expect to reuse fixtures for:

- fulfilled and unfulfilled orders
- ticket items with and without traces
- event shows before, during, and after the show window
- organization memberships for validators
- JWT QR secrets and token generation
- mail and push notification side effects
- feedback and giveaway DTOs

## Verification

Use the narrowest useful test command while building:

```bash
./mvnw -Dtest=TicketServiceTests,TicketControllerTests test
```

If the ticket module is being built incrementally, run only the files that exist so far.
