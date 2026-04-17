# Prepaid Credit Tracker

Spring Boot application for managing prepaid customer credits, product price history, transactions, reports, and admin users.

## Stack

- Java 21
- Spring Boot 3.x
- Spring Security with optional Google OAuth2
- Thymeleaf server-rendered UI
- Spring Data JPA
- PostgreSQL 15+
- Maven

## Security And Secrets

This repository is safe to publish publicly. Runtime secrets belong in `.env`, which is ignored by Git.

Create your local environment file from the template:

```bash
cp .env.example .env
```

Then edit `.env` and set strong local values:

```properties
SPRING_DATASOURCE_PASSWORD=replace-with-a-strong-password
APP_LOCAL_ADMIN_EMAIL=admin@example.com
APP_LOCAL_ADMIN_PASSWORD=replace-with-a-strong-local-password
APP_ADMIN_SEED_EMAIL=admin@example.com
```

Do not commit `.env`.

## Local Sign-In

Google authentication is disabled by default in `.env.example`:

```properties
APP_AUTH_GOOGLE_ENABLED=false
```

When Google auth is disabled, sign in with the local admin email and password from `.env`.

## PostgreSQL Setup

Create the database and user with environment variables instead of hardcoding credentials:

```bash
export DB_NAME=credittracker
export DB_USER=credittracker
export DB_PASSWORD='replace-with-a-strong-password'
psql -d postgres -f database-setup.sql
```

On PostgreSQL installs that have a `postgres` role, add `-U postgres`:

```bash
psql -U postgres -d postgres -f database-setup.sql
```

The application runs `schema.sql` at startup and creates the required tables, indexes, seed admin user, sample products, and sample prices.

## Run Locally

Set your environment variables or source `.env`, then run:

```bash
mvn spring-boot:run
```

Open:

```text
http://localhost:8080
```

## Docker Compose

The compose file runs only the application container because PostgreSQL is expected to be installed on the host/server.

```bash
cp .env.example .env
docker compose up --build
```

With the default `APP_PORT`, open:

```text
http://localhost:8090
```

For Linux servers, replace the database host in `SPRING_DATASOURCE_URL` inside `.env` with the PostgreSQL server hostname or IP address.

## Google OAuth2 Setup

1. Go to Google Cloud Console.
2. Create or select a project.
3. Configure the OAuth consent screen.
4. Create OAuth client credentials for a web application.
5. Add this authorized redirect URI:

```text
http://localhost:8080/login/oauth2/code/google
```

6. Set these `.env` values:

```properties
APP_AUTH_GOOGLE_ENABLED=true
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID=your-google-client-id
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET=your-google-client-secret
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_SCOPE=openid,email,profile
```

After Google login, the email must exist in `admin_users` with `is_active = true`, otherwise the user is redirected to the access-denied page.

## Seeding The First Admin

Set this in `.env`:

```properties
APP_ADMIN_SEED_EMAIL=admin@example.com
```

On startup, if `admin_users` is empty, the app inserts that email. The local admin email is also inserted automatically when local auth is configured.

## Business Name

The displayed business name is stored in the database in `app_settings` under the `business_name` key. Admins can update it from the Settings page without changing code or rebuilding the app.

## REST API

All API routes require an authenticated admin session and live under `/api/v1`.

- `GET /api/v1/customers?search=name`
- `POST /api/v1/customers`
- `GET /api/v1/customers/{id}`
- `PUT /api/v1/customers/{id}`
- `GET /api/v1/customers/{id}/balance`
- `POST /api/v1/customers/{id}/transactions`
- `GET /api/v1/customers/{id}/transactions`
- `GET /api/v1/products`
- `POST /api/v1/products`
- `PUT /api/v1/products/{id}`
- `POST /api/v1/products/{id}/prices`
- `GET /api/v1/products/{id}/prices`
- `GET /api/v1/reports/transactions`
- `GET /api/v1/reports/summary`

## Notes

- Purchase transactions calculate whole credits from the current product price, rounded down.
- Price changes are stored as history and never rewrite previous transactions.
- Consumption entries are stored as negative units.
- Customer and product deletes are soft deletes so transaction history remains available.
