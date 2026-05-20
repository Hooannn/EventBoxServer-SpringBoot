# CompletableFuture to JobRunr Migration Design

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the current fire-and-forget `CompletableFuture.runAsync` usage with durable JobRunr background jobs.

**Architecture:** Introduce JobRunr via the Spring Boot 3 starter, backed by the existing application database. Move asynchronous side effects into small background-job methods grouped by concern, so business services only enqueue jobs and job workers own the actual email or broadcast work. For transactional flows, enqueue after the main database write has succeeded so external side effects are not emitted for rolled-back operations.

**Tech Stack:** Spring Boot 3.2, JobRunr, Maven, PostgreSQL, Spring MVC, Spring Scheduling, Java 17.

---

## Scope

This migration covers only the current `CompletableFuture.runAsync` call sites in the application:

- `AuthService`
- `OrderService`
- `EventService`
- `TicketService`
- `OrganizationService`

It does not refactor the existing `@Scheduled` cron jobs or any other background execution path.

## Current Behavior

The codebase uses `CompletableFuture.runAsync` as a fire-and-forget mechanism for:

- registration, resend-verify, forgot-password, member-added, member-removed, order-paid, order-refunded, reminder, and giveaway emails
- socket broadcast updates for event stock and ticket trace changes
- push notification delivery for event publication

These jobs are currently:

- non-durable
- not retried
- not visible in any job dashboard
- tied to the default JVM thread pool behavior

## Proposed Design

### JobRunr integration

Add the JobRunr Spring Boot starter and configure it through application properties so the app can both enqueue jobs and process them in the same deployment.

Expected configuration includes:

- enabling the background job server
- enabling the dashboard
- using the application database as JobRunr storage

### Job boundary

Replace each `CompletableFuture.runAsync` block with a dedicated JobRunr-backed service method. Keep the enqueueing call in the original business service, but move the actual async work to a separate bean method that JobRunr can invoke.

The worker methods should accept small, serializable inputs only:

- IDs
- email addresses
- names
- basic payload fields

Avoid passing whole JPA entities into background jobs.

### Transaction boundary

For methods that persist data and then notify external systems, enqueue the job only after the database write is complete. If a method is already `@Transactional`, the enqueueing step should happen after the state change is known to be valid so that JobRunr does not publish an email or notification for a failed transaction.

### Error handling

JobRunr should own retries for transient failures. Inside the worker method:

- log the failure with enough context to trace the affected entity or recipient
- let unexpected exceptions fail the job so JobRunr can retry
- only swallow errors when the business requirement is explicitly best-effort, such as socket broadcasts

## File Impact

### Configuration

- Modify `pom.xml` to add the JobRunr Spring Boot starter
- Modify `src/main/java/com/ht/eventbox/config/ApplicationConfiguration.java` to expose any JobRunr-specific beans if needed
- Remove or simplify `src/main/java/com/ht/eventbox/config/AsyncConfiguration.java` so the project no longer advertises `CompletableFuture`-style async execution as its background mechanism
- Modify `src/main/resources/application*.properties` or equivalent config files to enable JobRunr server/dashboard and storage settings

### New job services

Create small worker services for the current async concerns, likely under `src/main/java/com/ht/eventbox/modules/jobrunr/` or a similar shared background-job package:

- mail job service
- notification job service
- socket broadcast job service

### Existing services

Modify the following services to enqueue jobs instead of spawning ad hoc futures:

- `src/main/java/com/ht/eventbox/modules/auth/AuthService.java`
- `src/main/java/com/ht/eventbox/modules/order/OrderService.java`
- `src/main/java/com/ht/eventbox/modules/event/EventService.java`
- `src/main/java/com/ht/eventbox/modules/ticket/TicketService.java`
- `src/main/java/com/ht/eventbox/modules/organization/OrganizationService.java`

## Migration Details by Concern

### Mail jobs

Move these email sends to JobRunr:

- registration email
- resend verification email
- forgot password email
- member added email
- member removed email
- order paid email
- order refunded email
- reminder email
- giveaway notification email

The worker should fetch any required entity data by ID when the template needs more than the enqueue-time payload.

### Socket jobs

Move the stock update and ticket trace socket broadcasts to JobRunr so those broadcasts are not lost if the request thread dies immediately after save.

### Push notification jobs

Move the event publication push notification job to JobRunr and keep its payload focused on the event ID and message metadata.

## Risks and Constraints

- JobRunr requires a durable storage provider; the migration assumes the current PostgreSQL-backed application database is available for that purpose.
- Existing async methods currently log and continue on failure in several places. Switching to JobRunr will make those failures observable as jobs, which is preferable for durability but changes the failure model.
- Some socket broadcasts are effectively best-effort notifications. Those should still be background jobs, but their retry policy may need to be conservative so they do not create unnecessary noise.
- This scope intentionally avoids refactoring the scheduled cleanup/reminder cron jobs, even though they are also background work.

## Success Criteria

- No application code calls `CompletableFuture.runAsync` for the targeted flows.
- The app can enqueue and execute the migrated jobs through JobRunr.
- Mail, socket, and push side effects remain functionally equivalent from the user’s perspective.
- Transactional request handlers do not enqueue side effects for rolled-back writes.
- Job execution is visible in JobRunr dashboard/monitoring.

## Implementation Notes

The implementation should favor small, incremental commits:

1. add dependency and configuration
2. introduce job worker services
3. migrate mail call sites
4. migrate socket and push call sites
5. verify tests and startup behavior
