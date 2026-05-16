# Docker Release Script Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a shell-based release helper that runs tests, inspects Docker Hub for the next semantic-style patch tag, builds the image, and pushes it with env-driven credentials.

**Architecture:** Keep the implementation in one focused Bash script under `scripts/` so the release flow is easy to audit and invoke manually. The script loads an optional env file, validates required inputs, queries Docker Hub's API for existing tags, computes the next `vX.Y` tag, then runs `mvnw test`, `docker login`, `docker build`, and `docker push` in sequence.

**Tech Stack:** Bash, Docker CLI, curl, Python 3 for JSON parsing, Maven wrapper.

---

### Task 1: Add the release script

**Files:**
- Create: `scripts/release.sh`

- [ ] **Step 1: Write the failing script stub**

Create a Bash entrypoint that loads `ENV_FILE` or `.env`, validates `DOCKER_USERNAME`, `DOCKER_PASSWORD`, and `IMAGE_NAME`, and prints a clear error if the repo name is not in `namespace/repository` form.

```bash
#!/usr/bin/env bash
set -euo pipefail

echo "release script stub"
```

- [ ] **Step 2: Run a syntax check**

Run: `bash -n scripts/release.sh`

Expected: FAIL until the script is fully implemented.

- [ ] **Step 3: Implement the release flow**

Add the Docker Hub token exchange, tag enumeration, `vX.Y` tag bumping, Maven test command execution, Docker login, image build, and push logic.

- [ ] **Step 4: Run a local verification pass**

Run: `bash -n scripts/release.sh`

Expected: PASS with no syntax errors.

- [ ] **Step 5: Commit**

```bash
git add scripts/release.sh docs/superpowers/plans/2026-05-16-docker-release-script.md
git commit -m "chore(release): add docker release helper"
```

### Task 2: Ignore local env files

**Files:**
- Modify: `.gitignore`

- [ ] **Step 1: Add the ignore rule**

Ignore the root `.env` file so local registry credentials and release settings stay out of version control.

```gitignore
.env
```

- [ ] **Step 2: Verify the change**

Run: `git diff -- .gitignore`

Expected: The only new rule is `.env`.

- [ ] **Step 3: Commit**

```bash
git add .gitignore
git commit -m "chore(gitignore): ignore local env file"
```
