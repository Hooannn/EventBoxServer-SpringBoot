# Organization Test Kickoff

This document is the starting point for the `organization` module test pass.

## Current State

- There are no organization module tests in the current tree.
- The organization module has one controller and one service:
  - [src/main/java/com/ht/eventbox/modules/organization/OrganizationController.java](/Users/nguyenduckhaihoan/Working/backend/EventboxServer-SpringBoot-s0ngnguyen/src/main/java/com/ht/eventbox/modules/organization/OrganizationController.java)
  - [src/main/java/com/ht/eventbox/modules/organization/OrganizationService.java](/Users/nguyenduckhaihoan/Working/backend/EventboxServer-SpringBoot-s0ngnguyen/src/main/java/com/ht/eventbox/modules/organization/OrganizationService.java)

## Recommended Test Order

1. `OrganizationServiceTests`
2. `OrganizationControllerTests`

Service tests should come first because the organization service contains the ownership checks, Cloudinary side effects, member management, and subscription toggle logic. Controller tests can remain thin and verify mapping, status codes, and response envelopes.

## First Organization Service Targets

Start with the most important and failure-prone service methods:

- `getAll`
- `getByUserIdAndOrganizationRole`
- `getByUserId`
- `getById`
- `getDetailsById`
- `create`
- `update`
- `deleteById`
- `addMember`
- `updateMember`
- `removeMember`
- `subscribe`

Suggested first assertions:

- missing organization returns `ORGANIZATION_NOT_FOUND`
- `getDetailsById` returns subscriber and event counts
- `create` persists the owner membership and optional logo asset
- `update` handles logo removal and replacement
- `deleteById` rejects organizations that still have events
- `addMember` rejects existing members and missing users
- `updateMember` rejects users not already in the organization
- `removeMember` rejects users not already in the organization
- `subscribe` toggles subscription state and rejects missing user or organization

## First Organization Controller Targets

Start with the controller routes that map directly to service behavior:

- `GET /api/v1/organizations`
- `GET /api/v1/organizations/me`
- `GET /api/v1/organizations/me/member`
- `GET /api/v1/organizations/{id}/details`
- `GET /api/v1/organizations/{id}`
- `PUT /api/v1/organizations/{id}`
- `DELETE /api/v1/organizations/{id}`
- `POST /api/v1/organizations`
- `POST /api/v1/organizations/{id}/members`
- `PUT /api/v1/organizations/{id}/members`
- `POST /api/v1/organizations/{id}/members/remove`
- `POST /api/v1/organizations/{id}/subscribe`

Keep controller tests focused on:

- request binding
- response wrappers
- status codes
- permission-gated routes being exercised through mocked auth setup

## Suggested File Ownership For Parallel Work

If multiple agents are used in the next session, split by file so work stays isolated:

- Agent 1: `src/test/java/com/ht/eventbox/modules/organization/OrganizationServiceTests.java`
- Agent 2: `src/test/java/com/ht/eventbox/modules/organization/OrganizationControllerTests.java`

## Likely Shared Fixtures

Expect to reuse fixtures for:

- organization owner and member relationships
- organizations with and without assets
- organizations with and without events
- users with subscriptions
- Cloudinary upload and delete results
- mail side effects for member add/remove flows
- subscriber and event count lookups

## Verification

Use the narrowest useful test command while building:

```bash
./mvnw -Dtest=OrganizationServiceTests,OrganizationControllerTests test
```

If the organization module is being built incrementally, run only the files that exist so far.
