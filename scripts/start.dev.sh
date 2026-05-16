#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"
cd ..

exec ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev