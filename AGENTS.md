# Repository Guidelines

## Agent Workflow

- Use Superpowers skills when they apply to the task before proceeding with implementation.
- Prefer the smallest relevant skill set. Common matches are brainstorming for feature work, debugging for bugs, TDD for new behavior, and verification-before-completion before claiming success.
- If no Superpowers skill fits the task, continue with the repository guidance below.

## Project Structure & Module Organization

This is a single-module Spring Boot service built with Maven. Main application code lives under `src/main/java/com/ht/eventbox/`, organized by feature area:

- `modules/` for controllers, services, repositories, and DTOs
- `entities/` for JPA models
- `config/`, `filter/`, `utils/`, `enums/`, and `constant/` for shared infrastructure

Templates and email views are under `src/main/resources/templates/`. Tests live in `src/test/java/com/ht/eventbox/`.

## Build, Test, and Development Commands

Use the Maven wrapper from the repo root:

- `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` starts the API locally with the `dev` profile.
- `./mvnw spring-boot:run` starts with the default profile if no profile is set.
- `./mvnw test` runs the Spring Boot test suite.
- `./mvnw clean package` builds the application JAR in `target/`.
- `./mvnw clean test` is a good pre-push check when dependencies or generated files change.

`docker-compose.yml` and `Dockerfile` are present for container-based runs if you need the full stack.

## Coding Style & Naming Conventions

Use standard Java 17 and Spring conventions. Keep indentation consistent with the existing codebase: tabs are used in several source files, so avoid reformatting entire files unless needed. Prefer:

- `PascalCase` for classes and DTOs
- `camelCase` for methods, fields, and variables
- `*Controller`, `*Service`, `*Repository`, and `*Dto` suffixes

Keep package placement feature-oriented, for example `modules/order/` for order-related logic.

## Testing Guidelines

The repo uses JUnit 5 with `spring-boot-starter-test`. The shared Spring test base is `src/test/java/com/ht/eventbox/support/AbstractSpringBootTest.java`, and the isolated profile lives in `src/test/resources/application-test.properties`. Add new tests beside the code they cover under `src/test/java/`, and name them `*Tests` or `*Test`.

## Commit & Pull Request Guidelines

Git history uses conventional-style prefixes such as `feat(auth): ...`, `fix(order): ...`, and `refactor: ...`. Follow that pattern for new commits, keeping the scope short and specific.

Before committing, make sure the commit is clean:

- Stage only the files that belong to the current task.
- Do not include unrelated user changes, generated artifacts, or local scratch files.
- Check `git status` and `git diff --stat` before committing so the change set is obvious.
- Verify the relevant tests before committing, and note the command in the commit body or PR description.
- Use a conventional commit title plus a short body that explains what changed and why.
- If the work is broad, split it into multiple commits by concern rather than one large mixed commit.

Pull requests should include:

- a short summary of the change
- linked issue or task, if available
- test results or notes about any manual verification
- screenshots only when a UI or email template changes

## Security & Configuration Tips

Do not commit secrets, API keys, or environment-specific credentials. Review changes to security, payment, webhook, and notification code carefully, especially anything touching cookies, JWTs, Redis, or external integrations such as PayPal, Firebase, or Cloudinary.
