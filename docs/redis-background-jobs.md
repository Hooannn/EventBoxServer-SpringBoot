# Redis Background Jobs

This repository uses Redis Streams for durable background work that should survive request completion and retries. The job layer lives in `src/main/java/com/ht/eventbox/modules/jobs/` and replaces production `CompletableFuture`-based side effects.

## What Belongs In The Queue

- Mail delivery
- Push notifications that should not be lost

## What Stays Out Of The Queue

- Socket emits
- Other intentionally ephemeral UI signals

Socket notifications are kept direct because they are non-durable by design and should not pay queue latency.

## Core Types

- `BackgroundJobType` defines the job category
- `MailKind` selects the concrete email template behind `SEND_MAIL`
- `BackgroundJobEnvelope` is the serialized job payload
- `RedisBackgroundJobService` enqueues jobs and writes job metadata
- `RedisBackgroundJobWorker` reads the Redis Stream and dispatches handlers
- `RedisBackgroundJobRetryScheduler` handles backoff and dead-lettering
- `MailJobHandler` renders and sends mail
- `PushNotificationJobHandler` sends push notifications

## Job Types

- `SEND_MAIL`
- `SEND_PUSH_NOTIFICATION`

Email variants are routed through one `SEND_MAIL` job type. The concrete template is selected from the payload using `mailKind`.

## Redis Keys

- `jobs:stream` for pending jobs
- `jobs:consumer-group` for the Redis Stream consumer group
- `jobs:retry` for delayed retries
- `jobs:dead` for exhausted jobs
- `jobs:meta:{jobId}` for status and attempt metadata

## Delivery Flow

1. The service finishes its main database work.
2. It enqueues a Redis job for mail or push work.
3. The worker claims stream entries from the consumer group.
4. The worker dispatches to the correct handler.
5. On success, the worker acknowledges the stream entry and updates metadata.
6. On failure, the retry scheduler either requeues the job with backoff or moves it to dead-letter storage.

## Payload Conventions

Keep payloads small and serializable. Prefer IDs and simple strings over full entity graphs.

Mail payloads use `mailKind` plus the data needed by the template. Current mail kinds include:

- `REGISTRATION`
- `VERIFY_RESEND`
- `FORGOT_PASSWORD`
- `MEMBER_ADDED`
- `MEMBER_REMOVED`
- `ORDER_REFUNDED`
- `ORDER_PAID`
- `GIVEAWAY_NOTIFICATION`
- `REMINDER`

Push payloads currently use:

- `userIds`
- `title`
- `body`
- optional `image`
- extra data fields for the push message

## Operational Notes

- Jobs are idempotency-sensitive. Handlers should tolerate retries.
- The Redis integration tests use Testcontainers and skip automatically when Docker is unavailable.
- `RedisService` remains in the codebase for auth/session storage and is separate from the job queue API.

## Tests

- Unit tests cover enqueueing and worker dispatch.
- Integration tests cover Redis Stream consumption, acknowledgement, and retry handling.
- Service tests assert that business code enqueues jobs instead of sending mail inline.
