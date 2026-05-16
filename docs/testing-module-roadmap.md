# Testing Module Roadmap

This note captures the order we agreed to use when expanding module test coverage.

## Order Of Work

1. `order`
2. `event`
3. `organization`
4. `ticket`

## Why This Order

- `order` is the highest-risk module because it covers reservation state, payment creation, PayPal capture, refund handling, and webhook orchestration.
- `event` comes next because it tends to sit close to search, publishing, and ticket visibility logic.
- `organization` follows because it usually drives authorization and management flows.
- `ticket` comes last because it is often heavily tied to the data already validated by the earlier modules.

## Current Decision

- `order` was the first module expanded.
- The next session should continue with `event`.
- If a future bug report changes the priority, keep the same order only if the risk still matches the list above.
