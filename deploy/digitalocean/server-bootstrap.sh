#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/prepaid-credit-tracker}"
APP_USER="${APP_USER:-deploy}"

if [[ "$EUID" -ne 0 ]]; then
  echo "Run this script as root with sudo." >&2
  exit 1
fi

apt-get update
apt-get install -y ca-certificates curl gnupg ufw postgresql-client caddy

install -m 0755 -d /etc/apt/keyrings
if [[ ! -f /etc/apt/keyrings/docker.gpg ]]; then
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  chmod a+r /etc/apt/keyrings/docker.gpg
fi

. /etc/os-release
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $VERSION_CODENAME stable" \
  > /etc/apt/sources.list.d/docker.list

apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

if ! id "$APP_USER" >/dev/null 2>&1; then
  adduser --disabled-password --gecos "" "$APP_USER"
fi
usermod -aG docker "$APP_USER"

install -o "$APP_USER" -g "$APP_USER" -m 0750 -d "$APP_DIR"
install -o "$APP_USER" -g "$APP_USER" -m 0750 -d "$APP_DIR/backups"

ufw allow OpenSSH
ufw allow 80/tcp
ufw allow 443/tcp
ufw --force enable

systemctl enable --now docker
systemctl enable --now caddy

echo "Bootstrap complete. Copy docker-compose.yml, .env, Caddyfile, and backup-db.sh into $APP_DIR."
