# DigitalOcean Deployment Track

This track captures the recommended production path for deploying Prepaid Credit Tracker for a small business whose domain is owned by the business.

## Target Architecture

```text
Business domain
  -> Cloudflare or DigitalOcean DNS
  -> DigitalOcean Droplet
  -> Caddy HTTPS reverse proxy
  -> Docker Compose Spring Boot app
  -> DigitalOcean Managed PostgreSQL
```

Recommended production URL pattern:

```text
https://app.businessdomain.com
```

Use a subdomain so the business can keep the root domain for its main website and email.

## Ownership Model

Business owns:

- Domain and DNS access.
- DigitalOcean account, project, Droplet, and Managed PostgreSQL.
- Google OAuth credentials.
- Billing.
- Production data and backups.

Developer owns or manages:

- Public app source repo.
- Private deployment repo.
- CI/CD workflows.
- Deployment automation.

## Repository Model

Public source repo:

```text
github.com/asit18/prepaid-credit-tracker
```

Private deployment repo:

```text
github.com/asit18/noelani-deployment
```

Public repo contains source code only:

```text
src/
pom.xml
Dockerfile
docker-compose.yml
.env.example
README.md
.github/workflows/docker-publish.yml
deploy/digitalocean/
```

Private deployment repo contains per-business deployment files:

```text
businesses/
  business-name/
    docker-compose.prod.yml
    Caddyfile
    README.md
.github/
  workflows/
    deploy-business-name.yml
```

Create the private deployment repo as:

```text
asit18/noelani-deployment
```

Recommended initial contents:

```text
businesses/
  noelani/
    docker-compose.yml
    Caddyfile
    env.production.example
    backup-db.sh
.github/
  workflows/
    deploy-noelani.yml
README.md
```

Copy these templates from the public source repo:

```text
deploy/digitalocean/docker-compose.prod.yml -> businesses/noelani/docker-compose.yml
deploy/digitalocean/Caddyfile.example -> businesses/noelani/Caddyfile
deploy/digitalocean/env.production.example -> businesses/noelani/env.production.example
deploy/digitalocean/backup-db.sh -> businesses/noelani/backup-db.sh
deploy/digitalocean/deploy-workflow.template.yml -> .github/workflows/deploy-noelani.yml
```

Never commit:

- `.env`
- database passwords
- Google OAuth client secrets
- SSH keys
- customer card images
- CSV imports with customer PII
- database backups or dumps

## Recommended Monthly Cost

Start with:

```text
DigitalOcean 2 GB Droplet: about $12/month
DigitalOcean 1 GB Managed PostgreSQL: about $15/month
Droplet backups: about $2-$4/month
Cloudflare DNS: free
Let's Encrypt SSL through Caddy: free
```

Expected total:

```text
About $29-$31/month
```

This is enough for roughly:

```text
100,000 requests/month
10,000 new records/month
```

Upgrade database to the next managed PostgreSQL plan when:

- reports/search are slow,
- database CPU is often above 60%-70%,
- connection usage approaches the plan limit,
- data grows beyond included storage,
- multiple app instances are added,
- high availability is required.

## DigitalOcean Setup

1. Create or select the business-owned DigitalOcean team.
2. Create a DigitalOcean project named for the business.
3. Add your SSH public key to the team.
4. Create a 2 GB Ubuntu LTS Droplet in the same region where the database will live.
5. Enable VPC networking, monitoring, and Droplet backups.
6. Attach a Cloud Firewall that allows inbound `22`, `80`, and `443` only.
7. Create a Managed PostgreSQL cluster in the same project and region.
8. Add the Droplet or its VPC CIDR as a trusted source for the database.
9. Create a database named `credittracker`.
10. Create a database user named `credittracker`.
11. Use the private database connection string in the app `.env`.
12. Install Docker, Caddy, PostgreSQL client tools, and UFW on the Droplet.
13. Create `/opt/prepaid-credit-tracker`.
14. Put production `docker-compose.yml`, `.env`, `Caddyfile`, and `backup-db.sh` in that folder.

## DNS Setup

Create a DNS record owned by the business:

```text
A record:
app.businessdomain.com -> Droplet public IP
```

If Cloudflare is used, point the domain to Cloudflare nameservers first, then create the record in Cloudflare.

## Server Folder

Production files live on the server:

```text
/opt/prepaid-credit-tracker/
  docker-compose.yml
  .env
  Caddyfile
  backup-db.sh
```

The `.env` file must not be committed to Git.

## Production Docker Compose

Use a prebuilt image from GitHub Container Registry.

```yaml
services:
  prepaid-credit-tracker:
    image: ghcr.io/asit18/prepaid-credit-tracker:latest
    restart: unless-stopped
    ports:
      - "127.0.0.1:8090:8080"
    env_file:
      - .env
```

Binding to `127.0.0.1` keeps the app private to the server. Only Caddy exposes HTTPS.

## Production Environment

Example `/opt/prepaid-credit-tracker/.env`:

```properties
APP_PORT=8090

SPRING_DATASOURCE_URL=jdbc:postgresql://private-db-host:25060/credittracker?sslmode=require
SPRING_DATASOURCE_USERNAME=credittracker
SPRING_DATASOURCE_PASSWORD=replace-with-strong-password

APP_AUTH_GOOGLE_ENABLED=true
APP_ADMIN_SEED_EMAIL=admin@businessdomain.com
APP_LOCAL_ADMIN_EMAIL=backup-admin@businessdomain.com
APP_LOCAL_ADMIN_PASSWORD=replace-with-strong-backup-password
APP_TIME_ZONE=America/Los_Angeles

SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID=replace-with-google-client-id
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET=replace-with-google-client-secret
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_SCOPE=openid,email,profile
```

## Caddy HTTPS

Example `/opt/prepaid-credit-tracker/Caddyfile`:

```caddy
app.businessdomain.com {
    reverse_proxy 127.0.0.1:8090
}
```

Caddy automatically obtains and renews HTTPS certificates.

## Google OAuth

Create the OAuth client in the business-owned Google Cloud project.

Authorized redirect URI:

```text
https://app.businessdomain.com/login/oauth2/code/google
```

The admin email must exist in the app's `admin_users` table with `is_active = true`.

## CI/CD Flow

Public repo workflow:

```text
push to main
  -> compile/test
  -> build Docker image
  -> push image to GHCR
```

Publish image tags:

```text
ghcr.io/asit18/prepaid-credit-tracker:latest
ghcr.io/asit18/prepaid-credit-tracker:<commit-sha>
```

Private deployment repo workflow:

```text
manual deploy or workflow dispatch
  -> SSH into business server
  -> cd /opt/prepaid-credit-tracker
  -> docker compose pull
  -> docker compose up -d
  -> docker image prune -f
```

Private repo GitHub Actions secrets:

```text
DO_HOST
DO_USER
DO_SSH_PRIVATE_KEY
DO_APP_DIR=/opt/prepaid-credit-tracker
```

## Database Backups

Use two layers:

1. DigitalOcean Managed PostgreSQL automatic daily point-in-time backups.
2. Daily logical `pg_dump` backups uploaded outside the database provider.

Example `/opt/prepaid-credit-tracker/backup-db.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

BACKUP_DIR=/opt/prepaid-credit-tracker/backups
DATE=$(date +%F-%H%M)
FILE="$BACKUP_DIR/credittracker-$DATE.sql.gz"

mkdir -p "$BACKUP_DIR"

pg_dump "$DATABASE_URL" | gzip > "$FILE"

find "$BACKUP_DIR" -type f -name "*.sql.gz" -mtime +14 -delete
```

For stronger protection, upload the backup to object storage:

```bash
aws s3 cp "$FILE" s3://business-backups/prepaid-credit-tracker/
```

DigitalOcean Spaces is S3-compatible and can use the AWS CLI.

Cron entry:

```cron
0 2 * * * /opt/prepaid-credit-tracker/backup-db.sh >> /var/log/prepaid-credit-tracker-backup.log 2>&1
```

Run a restore test monthly.

## Server Security Checklist

- Firewall allows only ports `22`, `80`, and `443`.
- SSH uses key login only.
- Root password login is disabled.
- PostgreSQL is not publicly exposed.
- App container binds to `127.0.0.1`, not `0.0.0.0`.
- `.env` exists only on the server.
- Caddy handles HTTPS.
- Backups run daily.
- Restore process is tested.
- Docker images are updated through CI/CD.
- GitHub repo secrets are used for SSH deployment credentials.

## Deployment Commands

Concrete templates are included in this repo under:

```text
deploy/digitalocean/
```

Initial server deployment:

```bash
cd /opt/prepaid-credit-tracker
docker compose pull
docker compose up -d
```

Update deployment:

```bash
cd /opt/prepaid-credit-tracker
docker compose pull
docker compose up -d
docker image prune -f
```

Check status:

```bash
docker compose ps
docker compose logs --tail=100 prepaid-credit-tracker
```

## Scaling Track

Start:

```text
2 GB Droplet
1 GB Managed PostgreSQL
single app instance
```

Next upgrade:

```text
2 GB or 4 GB Managed PostgreSQL
larger Droplet if JVM memory becomes tight
```

Later:

```text
one app/database per business
separate private deployment workflow per business
optional high availability database
optional GraalVM native image to reduce memory footprint
```
