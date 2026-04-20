#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/prepaid-credit-tracker}"
ENV_FILE="${ENV_FILE:-$APP_DIR/.env}"

if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck source=/dev/null
  source "$ENV_FILE"
  set +a
fi

if [[ -z "${DATABASE_URL:-}" ]]; then
  echo "DATABASE_URL is required for backups" >&2
  exit 1
fi

BACKUP_DIR="${BACKUP_DIR:-$APP_DIR/backups}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"
DATE="$(date +%F-%H%M%S)"
FILE="$BACKUP_DIR/credittracker-$DATE.sql.gz"

mkdir -p "$BACKUP_DIR"
umask 077

pg_dump "$DATABASE_URL" | gzip > "$FILE"

if [[ -n "${BACKUP_S3_URI:-}" ]]; then
  if command -v aws >/dev/null 2>&1; then
    aws s3 cp "$FILE" "$BACKUP_S3_URI/"
  else
    echo "BACKUP_S3_URI is set but aws CLI is not installed; local backup kept at $FILE" >&2
  fi
fi

find "$BACKUP_DIR" -type f -name "*.sql.gz" -mtime "+$BACKUP_RETENTION_DAYS" -delete

echo "Backup complete: $FILE"
