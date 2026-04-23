# DigitalOcean Deployment

This folder contains production deployment templates for a DigitalOcean Droplet plus DigitalOcean Managed PostgreSQL.

## Files

- `docker-compose.prod.yml`: production compose file that pulls the GHCR image.
- `env.production.example`: production environment template. Do not commit real `.env`.
- `Caddyfile.example`: HTTPS reverse proxy example.
- `server-bootstrap.sh`: installs Docker, Caddy, firewall, and PostgreSQL client on Ubuntu.
- `backup-db.sh`: daily logical PostgreSQL backup script.
- `deploy-over-ssh.sh`: server-side deployment command.
- `deploy-workflow.template.yml`: GitHub Actions SSH deployment workflow for the private `sample-business-deployment` repo.

## One-Time DigitalOcean Setup

1. Create a business-owned DigitalOcean project.
2. Create an Ubuntu LTS Droplet.
3. Create a DigitalOcean Managed PostgreSQL database.
4. Add the Droplet as a trusted source for the database.
5. Create DNS:

```text
app.businessdomain.com -> Droplet public IP
```

6. Bootstrap the server:

```bash
sudo APP_USER=deploy APP_DIR=/opt/prepaid-credit-tracker bash server-bootstrap.sh
```

7. Copy production files to the server:

```text
/opt/prepaid-credit-tracker/docker-compose.yml
/opt/prepaid-credit-tracker/.env
/opt/prepaid-credit-tracker/Caddyfile
/opt/prepaid-credit-tracker/backup-db.sh
```

Use `docker-compose.prod.yml` as the server `docker-compose.yml`.

8. Install the Caddyfile:

```bash
sudo cp /opt/prepaid-credit-tracker/Caddyfile /etc/caddy/Caddyfile
sudo systemctl reload caddy
```

9. Start the app:

```bash
cd /opt/prepaid-credit-tracker
docker compose pull
docker compose up -d
```

## GitHub Container Registry

The public source repo publishes:

```text
ghcr.io/asit18/prepaid-credit-tracker:latest
ghcr.io/asit18/prepaid-credit-tracker:<commit-sha>
```

If the GHCR package is private, authenticate Docker on the server:

```bash
echo "GITHUB_TOKEN_OR_PAT" | docker login ghcr.io -u asit18 --password-stdin
```

For a public small-business deployment, prefer making the package public and keeping all runtime secrets on the server.

## GitHub Actions Deployment

Use `deploy-workflow.template.yml` in the private `sample-business-deployment` repo:

```text
.github/workflows/deploy-production.yml
```

Required private repo secrets:

```text
DO_HOST
DO_USER
DO_SSH_PRIVATE_KEY
DO_APP_DIR=/opt/prepaid-credit-tracker
```

## Backups

Install the backup script:

```bash
chmod 750 /opt/prepaid-credit-tracker/backup-db.sh
```

Cron:

```cron
0 2 * * * /opt/prepaid-credit-tracker/backup-db.sh >> /var/log/prepaid-credit-tracker-backup.log 2>&1
```

Use DigitalOcean Managed PostgreSQL backups plus this logical backup. Test restore monthly.
