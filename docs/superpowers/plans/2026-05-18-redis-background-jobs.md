# Redis Background Jobs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace every production `CompletableFuture`-based side effect with a Redis Streams job pipeline for durable mail and push work, while keeping socket emits direct and out of the queue.

**Architecture:** Add a small `modules/jobs` package that owns job envelopes, enqueueing, worker dispatch, retries, and dead-letter handling. Services will write the main business transaction first, then enqueue a `SEND_MAIL` or `SEND_PUSH_NOTIFICATION` job after commit; socket events stay synchronous or post-commit and never enter the queue.

**Tech Stack:** Java 17, Spring Boot 3.2, Spring Data Redis, Redis Streams, JUnit 5, Mockito, AssertJ, Spring MockMvc, Testcontainers Redis, Maven.

---

## File Structure

- `src/main/java/com/ht/eventbox/modules/jobs/` owns the Redis job queue implementation.
- `src/main/java/com/ht/eventbox/modules/auth/AuthService.java` stops sending mail inline and enqueues `SEND_MAIL`.
- `src/main/java/com/ht/eventbox/modules/organization/OrganizationService.java` stops sending member mail inline and enqueues `SEND_MAIL`.
- `src/main/java/com/ht/eventbox/modules/order/OrderService.java` stops sending mail and push inline and enqueues jobs, while keeping socket emits direct.
- `src/main/java/com/ht/eventbox/modules/ticket/TicketService.java` stops sending mail and push inline and enqueues jobs, while keeping socket emits direct.
- `src/main/java/com/ht/eventbox/modules/event/EventService.java` stops sending push inline and enqueues `SEND_PUSH_NOTIFICATION`.
- `src/main/java/com/ht/eventbox/config/AsyncConfiguration.java` is deleted after the last production `CompletableFuture` disappears.
- `src/test/java/com/ht/eventbox/modules/jobs/` holds the new queue and worker tests.
- `src/test/java/com/ht/eventbox/modules/auth/AuthServiceTests.java`, `src/test/java/com/ht/eventbox/modules/organization/OrganizationServiceTests.java`, `src/test/java/com/ht/eventbox/modules/order/OrderServiceTests.java`, `src/test/java/com/ht/eventbox/modules/event/EventServiceTests.java`, and `src/test/java/com/ht/eventbox/modules/ticket/TicketServiceTests.java` are updated to assert enqueueing instead of inline side effects.
- `pom.xml` gains the Redis integration test dependency needed for stream-level verification.

---

### Task 1: Add the Redis job contract and worker skeleton

**Files:**
- Create: `src/main/java/com/ht/eventbox/modules/jobs/BackgroundJobType.java`
- Create: `src/main/java/com/ht/eventbox/modules/jobs/BackgroundJobEnvelope.java`
- Create: `src/main/java/com/ht/eventbox/modules/jobs/MailKind.java`
- Create: `src/main/java/com/ht/eventbox/modules/jobs/RedisBackgroundJobService.java`
- Create: `src/main/java/com/ht/eventbox/modules/jobs/RedisBackgroundJobWorker.java`
- Create: `src/main/java/com/ht/eventbox/modules/jobs/RedisBackgroundJobRetryScheduler.java`
- Create: `src/main/java/com/ht/eventbox/modules/jobs/RedisBackgroundJobHandler.java`
- Create: `src/main/java/com/ht/eventbox/modules/jobs/MailJobHandler.java`
- Create: `src/main/java/com/ht/eventbox/modules/jobs/PushNotificationJobHandler.java`
- Modify: `pom.xml`

- [ ] **Step 1: Write the failing tests**

Create `src/test/java/com/ht/eventbox/modules/jobs/RedisBackgroundJobServiceTests.java` and `src/test/java/com/ht/eventbox/modules/jobs/RedisBackgroundJobWorkerTests.java` with these assertions:

```java
class RedisBackgroundJobServiceTests {
    // enqueueSendMail should write a SEND_MAIL envelope to the Redis stream
    // enqueueSendPushNotification should write a SEND_PUSH_NOTIFICATION envelope to the Redis stream
    // enqueue should persist job metadata with attempt=0 and status=PENDING
}

class RedisBackgroundJobWorkerTests {
    // processOne should dispatch SEND_MAIL to MailJobHandler
    // processOne should dispatch SEND_PUSH_NOTIFICATION to PushNotificationJobHandler
    // processOne should retry transient failures with a higher attempt count
    // processOne should move exhausted jobs to dead-letter storage
}
```

The tests should fail initially because the queue contract and worker classes do not exist yet.

- [ ] **Step 2: Run the narrow test command**

Run:

```bash
./mvnw -Dtest=RedisBackgroundJobServiceTests,RedisBackgroundJobWorkerTests test
```

Expected: FAIL with missing types or missing bean wiring.

- [ ] **Step 3: Write the minimal implementation**

Implement a small Redis Streams queue API with these signatures:

```java
public enum BackgroundJobType {
    SEND_MAIL,
    SEND_PUSH_NOTIFICATION
}

public record BackgroundJobEnvelope(
        String jobId,
        BackgroundJobType type,
        String payload,
        int attempt,
        Instant createdAt,
        Instant runAfter,
        String correlationId) {
}

public interface RedisBackgroundJobHandler {
    BackgroundJobType supports();
    void handle(BackgroundJobEnvelope envelope);
}
```

`RedisBackgroundJobService` should serialize envelopes to a dedicated Redis Stream, write metadata under `jobs:meta:{jobId}`, and expose enqueue methods for `SEND_MAIL` and `SEND_PUSH_NOTIFICATION`. `RedisBackgroundJobWorker` should read from the consumer group, call the right handler, ack on success, and hand failures to `RedisBackgroundJobRetryScheduler`.

Add the Redis Stream configuration and the Testcontainers dependencies in `pom.xml` so the integration tests can exercise real stream behavior.

- [ ] **Step 4: Run the narrow test command again**

Run:

```bash
./mvnw -Dtest=RedisBackgroundJobServiceTests,RedisBackgroundJobWorkerTests test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/main/java/com/ht/eventbox/modules/jobs src/test/java/com/ht/eventbox/modules/jobs
git commit -m "feat(jobs): add redis background job skeleton"
```

### Task 2: Add Redis Stream integration coverage

**Files:**
- Create: `src/test/java/com/ht/eventbox/modules/jobs/RedisBackgroundJobIntegrationTests.java`
- Create: `src/test/java/com/ht/eventbox/support/RedisTestSupport.java`
- Modify: `src/test/resources/application-test.properties` only if the Redis container setup needs a fallback port or host note

- [ ] **Step 1: Write the failing test**

Create a stream-level integration test that starts Redis with Testcontainers and verifies:

```java
class RedisBackgroundJobIntegrationTests {
    // enqueue a SEND_MAIL job
    // worker claims and processes the job
    // the stream entry is acknowledged
    // retries reappear after a transient handler failure
}
```

Use `@DynamicPropertySource` in `RedisTestSupport` to point Spring Data Redis at the container.

- [ ] **Step 2: Run the narrow test command**

Run:

```bash
./mvnw -Dtest=RedisBackgroundJobIntegrationTests test
```

Expected: FAIL until the container wiring and queue code exist.

- [ ] **Step 3: Write the minimal implementation**

Wire the Testcontainers Redis container, expose the dynamic Redis host/port, and add one end-to-end test that pushes a job into the stream and verifies the worker acknowledges it after successful handling.

- [ ] **Step 4: Run the narrow test command again**

Run:

```bash
./mvnw -Dtest=RedisBackgroundJobIntegrationTests test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/ht/eventbox/modules/jobs/RedisBackgroundJobIntegrationTests.java src/test/java/com/ht/eventbox/support/RedisTestSupport.java src/test/resources/application-test.properties
git commit -m "test(jobs): add redis stream integration coverage"
```

### Task 3: Migrate all mail-producing services to `SEND_MAIL`

**Files:**
- Modify: `src/main/java/com/ht/eventbox/modules/auth/AuthService.java`
- Modify: `src/main/java/com/ht/eventbox/modules/organization/OrganizationService.java`
- Modify: `src/main/java/com/ht/eventbox/modules/order/OrderService.java`
- Modify: `src/main/java/com/ht/eventbox/modules/ticket/TicketService.java`
- Modify: `src/test/java/com/ht/eventbox/modules/auth/AuthServiceTests.java`
- Modify: `src/test/java/com/ht/eventbox/modules/organization/OrganizationServiceTests.java`
- Modify: `src/test/java/com/ht/eventbox/modules/order/OrderServiceTests.java`
- Modify: `src/test/java/com/ht/eventbox/modules/ticket/TicketServiceTests.java`

- [ ] **Step 1: Write the failing tests**

Update the existing service tests so they assert `RedisBackgroundJobService.enqueueSendMail(...)` instead of direct `MailService` calls.

Key expectations:
- `AuthService.register`, `AuthService.resendVerify`, and `AuthService.forgotPassword` enqueue `SEND_MAIL` with `MailKind.REGISTRATION`, `MailKind.VERIFY_RESEND`, and `MailKind.FORGOT_PASSWORD`.
- `OrganizationService.addMember` and `OrganizationService.removeMember` enqueue `SEND_MAIL` with `MailKind.MEMBER_ADDED` and `MailKind.MEMBER_REMOVED`.
- `OrderService.onOrderRefunded` and `OrderService.onOrderFulfilled` enqueue `SEND_MAIL` with `MailKind.ORDER_REFUNDED` and `MailKind.ORDER_PAID`.
- `TicketService.remindUpcomingEvents` and `TicketService.giveawayTicketItem` enqueue `SEND_MAIL` with `MailKind.REMINDER` and `MailKind.GIVEAWAY_NOTIFICATION`.

Make the tests fail by expecting `RedisBackgroundJobService` to be invoked and by no longer expecting inline mail dispatch.

- [ ] **Step 2: Run the narrow test command**

Run:

```bash
./mvnw -Dtest=AuthServiceTests,OrganizationServiceTests,OrderServiceTests,TicketServiceTests test
```

Expected: FAIL because the services still call `MailService` directly.

- [ ] **Step 3: Write the minimal implementation**

Inject `RedisBackgroundJobService` into each service and replace each direct mail `CompletableFuture.runAsync(...)` block with one enqueue call that passes:
- the recipient address
- the `MailKind`
- the minimal template data needed by `MailJobHandler`

Keep the actual mail rendering and delivery inside `MailJobHandler`, not the producing service.

- [ ] **Step 4: Run the narrow test command again**

Run:

```bash
./mvnw -Dtest=AuthServiceTests,OrganizationServiceTests,OrderServiceTests,TicketServiceTests test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ht/eventbox/modules/auth/AuthService.java src/main/java/com/ht/eventbox/modules/organization/OrganizationService.java src/main/java/com/ht/eventbox/modules/order/OrderService.java src/main/java/com/ht/eventbox/modules/ticket/TicketService.java src/test/java/com/ht/eventbox/modules/auth/AuthServiceTests.java src/test/java/com/ht/eventbox/modules/organization/OrganizationServiceTests.java src/test/java/com/ht/eventbox/modules/order/OrderServiceTests.java src/test/java/com/ht/eventbox/modules/ticket/TicketServiceTests.java
git commit -m "feat(mail): route mail side effects through redis jobs"
```

### Task 4: Migrate push notifications and remove async socket wrappers

**Files:**
- Modify: `src/main/java/com/ht/eventbox/modules/event/EventService.java`
- Modify: `src/main/java/com/ht/eventbox/modules/order/OrderService.java`
- Modify: `src/main/java/com/ht/eventbox/modules/ticket/TicketService.java`
- Modify: `src/main/java/com/ht/eventbox/modules/cronjobs/Scheduler.java` only if a direct socket emit needs the same cleanup pattern
- Modify: `src/test/java/com/ht/eventbox/modules/event/EventServiceTests.java`
- Modify: `src/test/java/com/ht/eventbox/modules/order/OrderServiceTests.java`
- Modify: `src/test/java/com/ht/eventbox/modules/ticket/TicketServiceTests.java`

- [ ] **Step 1: Write the failing tests**

Update the tests so they assert:
- `EventService.publishByAdmin` enqueues a `SEND_PUSH_NOTIFICATION` job for subscribers
- `OrderService.onOrderRefunded` and `OrderService.onOrderFulfilled` enqueue `SEND_PUSH_NOTIFICATION`
- `TicketService.createTicketItemTrace` still sends socket events, but no longer wraps them in `CompletableFuture`
- `OrderService.onStockUpdated` still sends socket events, but no longer wraps them in `CompletableFuture`

The service tests should expect `RedisBackgroundJobService.enqueueSendPushNotification(...)` where push is durable, and direct socket emit interactions where the event is intentionally non-durable.

- [ ] **Step 2: Run the narrow test command**

Run:

```bash
./mvnw -Dtest=EventServiceTests,OrderServiceTests,TicketServiceTests test
```

Expected: FAIL until the push jobs and socket cleanup are implemented.

- [ ] **Step 3: Write the minimal implementation**

Replace the remaining production `CompletableFuture.runAsync(...)` blocks in these paths:
- `EventService.publishByAdmin`
- `OrderService.onOrderRefunded`
- `OrderService.onOrderApproved`
- `OrderService.onOrderFulfilled`
- `OrderService.onStockUpdated`
- `TicketService.createTicketItemTrace`

Use `RedisBackgroundJobService` for push notifications and call `SocketIOServer` directly for socket events after the transaction is complete. Keep the socket work out of Redis entirely.

- [ ] **Step 4: Run the narrow test command again**

Run:

```bash
./mvnw -Dtest=EventServiceTests,OrderServiceTests,TicketServiceTests test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ht/eventbox/modules/event/EventService.java src/main/java/com/ht/eventbox/modules/order/OrderService.java src/main/java/com/ht/eventbox/modules/ticket/TicketService.java src/test/java/com/ht/eventbox/modules/event/EventServiceTests.java src/test/java/com/ht/eventbox/modules/order/OrderServiceTests.java src/test/java/com/ht/eventbox/modules/ticket/TicketServiceTests.java
git commit -m "feat(push): route push jobs through redis"
```

### Task 5: Remove the async plumbing and finish verification

**Files:**
- Delete: `src/main/java/com/ht/eventbox/config/AsyncConfiguration.java`
- Modify: any remaining production files that still import `java.util.concurrent.CompletableFuture`
- Modify: `docs/testing-coverage-map.md` only if the test coverage map should record the new job system

- [ ] **Step 1: Write the failing cleanup check**

Run a repository-wide search and make it fail if any production `CompletableFuture` remains:

```bash
rg -n "CompletableFuture" src/main/java/com/ht/eventbox
```

Expected: no output after the migration is complete.

- [ ] **Step 2: Remove the leftover async code**

Delete `AsyncConfiguration.java`, remove unused `CompletableFuture` imports, and clean up any leftover async-only comments. Keep `@EnableScheduling` untouched because the app already uses scheduled jobs for cron work.

- [ ] **Step 3: Run the full targeted verification**

Run:

```bash
./mvnw test
```

Expected: PASS for the full suite, including the Redis job tests and the updated service tests.

- [ ] **Step 4: Run the search check again**

Run:

```bash
rg -n "CompletableFuture" src/main/java/com/ht/eventbox
```

Expected: no output.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ht/eventbox
git commit -m "refactor(async): replace production CompletableFuture usage"
```

**Execution note:** Do not leave any production `CompletableFuture`-based side effects behind. Socket signaling may remain direct, but every other durable background side effect should flow through the Redis job pipeline.
