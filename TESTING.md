# Testing Guide

## Current Setup

The repository now includes a test profile under `src/test/resources/application-test.properties` and a shared base class at `src/test/java/com/ht/eventbox/support/AbstractSpringBootTest.java`.
The test profile disables startup bootstrap work, so the context can load without starting the Socket.IO server.

## How To Add Tests

- Put unit tests next to the code they cover under `src/test/java/com/ht/eventbox/...`.
- Extend `AbstractSpringBootTest` for Spring integration tests.
- Use `@WebMvcTest`, `@DataJpaTest`, or plain JUnit 5 for narrower scope when possible.
- Keep fixture data in `src/test/resources/`.

## Running Tests

- `./mvnw test` runs all tests.
- `./mvnw -Dspring.profiles.active=test test` is useful when you want to force the isolated test profile.

## Canonical Rules

- Name test classes `*Test` or `*Tests`.
- Prefer small, deterministic tests with mocked external services.
- Avoid real network calls to PayPal, Cloudinary, Firebase, Redis, or email services.
- Use the test profile for any test that starts the Spring context.
