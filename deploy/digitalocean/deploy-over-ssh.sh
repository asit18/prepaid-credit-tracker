#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/prepaid-credit-tracker}"

cd "$APP_DIR"
docker compose pull
docker compose up -d
docker image prune -f
docker compose ps
