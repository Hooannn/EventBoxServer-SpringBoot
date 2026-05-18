# Redis Background Jobs Design

**Goal:** Replace ad hoc `CompletableFuture.runAsync(...)` side effects with a Redis-backed job queue for durable work, while keeping socket emits out of the queue.

**Scope:** All production `CompletableFuture`-based side effects in the application. The primary targets are mail delivery, push notification work, and any other background task still using `CompletableFuture` in `src/main/java/com/ht/eventbox`.

**Non-Goals:**
- Do not queue socket events.
- Do not introduce RabbitMQ, Kafka, or a DB-backed job table in this phase.
- Do not redesign the business flows that produce the side effects.
- Do not leave any production `CompletableFuture` usage behind if it is used for background side effects.

## Architecture

Use Redis Streams as the durable transport for background jobs. Business services enqueue a small job envelope after the main transaction succeeds. A Spring-managed worker consumes stream entries, dispatches them to the correct handler, and acknowledges the message only after the handler succeeds.

This keeps the request path short, removes unstructured `CompletableFuture` usage, and preserves retryable delivery for side effects that must not be lost.

## Job Model

Each job contains:
- `jobId`
- `type`
- `payload`
- `attempt`
- `createdAt`
- `runAfter` for delayed retries
- `correlationId` when a request needs traceability across logs

Suggested job types:
- `SEND_REGISTRATION_EMAIL`
- `SEND_VERIFY_EMAIL`
- `SEND_FORGOT_PASSWORD_EMAIL`
- `SEND_MEMBER_ADDED_EMAIL`
- `SEND_MEMBER_REMOVED_EMAIL`
- `SEND_ORDER_REFUNDED_EMAIL`
- `SEND_ORDER_PAID_EMAIL`
- `SEND_GIVEAWAY_NOTIFICATION_EMAIL`
- `SEND_REMINDER_EMAIL`
- `SEND_PUSH_NOTIFICATION`

Payloads should be small and serializable. Prefer IDs and immutable fields that the worker can use to reconstruct the message, instead of embedding full entity graphs.

## Redis Structures

Use a small set of dedicated Redis keys:
- `jobs:stream` for pending jobs
- `jobs:consumer-group` for the stream consumer group name
- `jobs:retry` as a sorted set for delayed retries
- `jobs:dead` as a dead-letter bucket for exhausted jobs
- `jobs:meta:{jobId}` for status, attempts, timestamps, and the last error message

The existing `RedisService` can remain for auth/session use. The job system should sit beside it as a dedicated queue abstraction rather than turning the current key-value helper into a general-purpose queue API.

## Worker Flow

1. Service code completes the main business transaction.
2. The service enqueues one or more job envelopes into the Redis Stream.
3. The worker reads from the consumer group.
4. The worker deserializes the payload and dispatches to a typed handler.
5. On success, the worker acknowledges the stream entry and marks the job complete in metadata.
6. On failure, the worker increments the attempt count and either:
   - schedules a delayed retry in `jobs:retry`, or
   - moves the job to `jobs:dead` after the max retry count.

## Retry Policy

Use bounded retries with exponential backoff.

Recommended defaults:
- max attempts: 3
- initial backoff: 10 seconds
- backoff multiplier: 2x
- dead-letter after the final attempt

Failures should be logged with enough context to identify:
- job type
- job id
- user or order reference
- attempt number
- exception message

Handlers must be idempotent because retries can happen after partial success or worker restarts.

## Transaction Boundary

Jobs should be enqueued only after the business transaction is committed.

Preferred implementation:
- keep the business write in the service transaction
- emit an application event or use an after-commit hook
- enqueue the Redis job from that post-commit path

This avoids enqueueing work for database changes that later roll back.

## Socket Events

Socket events should stay out of the queue.

For current real-time updates such as order status or ticket trace updates:
- emit them directly after the state change is committed
- do not store them as durable jobs

This keeps UI signaling lightweight and avoids adding queue latency where reliability is not the primary requirement.

## Migration Plan

Phase 1: Introduce the queue abstraction
- Add a job DTO, job type enum, serializer, and enqueue API.
- Add a Spring worker that can claim and process jobs.
- Add retry and dead-letter handling.

Phase 2: Migrate all email jobs
- Replace every `CompletableFuture.runAsync(...)` email call in production code.
- Keep the old code path only until the worker is verified.

Phase 3: Migrate all other production `CompletableFuture` side effects
- Move push notification dispatch onto the same worker pipeline.
- Replace any remaining production `CompletableFuture` usage with either the worker pipeline or a direct post-commit synchronous emit if the work is intentionally non-durable, such as socket signaling.
- Keep handlers separate so failures are isolated by job type.

Phase 4: Remove direct async calls
- Delete the remaining `CompletableFuture` usage from production code.
- Retain direct socket emits where appropriate.

## Error Handling

The worker should treat these cases explicitly:
- serialization failure at enqueue time: fail fast in the calling service
- missing or malformed payload: move to dead-letter immediately
- external mail or Firebase failure: retry with backoff
- Redis transient failure: log and let the job remain pending for the next poll or claim cycle

If a handler depends on a database entity that no longer exists, the job should fail fast and move to dead-letter after the configured retry policy.

## Testing Strategy

Add tests around three layers:
- enqueue tests for each producing service
- worker dispatch tests for job type routing and retry behavior
- integration tests for Redis Stream consumption and job acknowledgement

Target cases:
- registration email gets enqueued instead of sent inline
- member added and removed emails are enqueued correctly
- order refund and fulfillment notifications enqueue both mail and push jobs
- worker retries after a transient mail or push exception
- dead-letter capture after max attempts
- socket emit paths remain direct and outside the queue

## Implementation Notes

- Keep the queue API small and explicit.
- Keep job payloads stable and versioned if the schema evolves.
- Prefer per-type handler classes over a single monolithic worker method.
- Do not let the queue layer leak into controllers.
