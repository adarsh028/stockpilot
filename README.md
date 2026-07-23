# StockPilot

A multi-tenant, multi-channel **inventory management SaaS**. Brands manage a central inventory,
allocate stock to sales channels (Amazon, Flipkart, Myntra, own website, offline, custom), record
sales per channel (manually or via CSV/Excel import), and view dashboard analytics — units and
revenue sold, sales by channel, top products, and trends over selectable date ranges.

- **Backend:** Java 21, Spring Boot 3, Maven, Spring Data JPA, Spring Security, Bean Validation,
  Flyway, Lombok, jjwt, Apache POI, springdoc-openapi. **PostgreSQL only — no H2 anywhere.**
- **Frontend:** React 18 + Vite + **TypeScript**, React Router, TanStack Query, Axios, Tailwind CSS, Recharts.
- **Email:** Brevo transactional email v3 (with a dev fallback — see below).
- **Auth:** JWT access + rotating refresh tokens, BCrypt, 6-digit OTP email verification.

```
stockpilot/
  backend/               # Spring Boot (Maven)
  frontend/              # React + Vite + TypeScript
```

---

## Prerequisites

- PostgreSQL installed locally (managed with pgAdmin 4, bundled with the PostgreSQL installer)
- Java 21 and Maven 3.9+
- Node 20+ and npm

---

## 1. Create the database

Using pgAdmin 4 (or `psql`), create an empty database for the app to connect to — Flyway creates
and updates every table/column automatically on backend startup, so nothing else is needed here.

```sql
CREATE DATABASE stockpilot;
```

Note the port your local PostgreSQL server listens on (check `postgresql.conf`, or the pgAdmin
server properties) — it may not be the default `5432` if something else is already using that port.

---

## 2. Configure environment

**Backend** — copy the example and adjust as needed:

```bash
cp backend/.env.example backend/.env
```

Update the database variables to match your local PostgreSQL install (port, username, password);
everything else works out of the box. Key variables:

| Variable | Purpose | Default |
|---|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | PostgreSQL connection | points at your local Postgres server |
| `JWT_SECRET` | JWT signing key (≥ 256 bits) | dev default provided |
| `BREVO_API_KEY` | Brevo transactional email key | **blank** |
| `BREVO_SENDER_EMAIL` / `BREVO_SENDER_NAME` | Email sender identity | placeholders |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Google Drive OAuth client (per-tenant product image storage) | **blank** |
| `DRIVE_TOKEN_ENCRYPTION_KEY` | Encrypts stored Drive OAuth tokens at rest | dev default provided |
| `CORS_ALLOWED_ORIGIN` | Frontend origin | `http://localhost:5173` |
| `APP_INVENTORY_STOCK_POLICY` | `WARN` (allow + flag) or `BLOCK` | `WARN` |

> **Email is optional for local dev.** If `BREVO_API_KEY` is blank (or Brevo is unreachable), the
> backend does **not** fail — it logs OTP codes and email bodies to the console, clearly labeled
> `[DEV EMAIL FALLBACK]`, so signup/verify/reset flows are fully testable without a real account.
> Drop a real key into `backend/.env` later to send actual emails — no code changes needed.

**Frontend** — the backend URL lives in a single env var:

```bash
cp frontend/.env.example frontend/.env   # VITE_API_BASE_URL=http://localhost:8080/api/v1
```

`src/api/client.ts` is the only place a base URL is constructed; every API call goes through that one
Axios instance, so changing `VITE_API_BASE_URL` repoints the whole app.

The backend auto-loads `backend/.env` on startup (via `spring-dotenv`) when run from the `backend/`
directory — no manual `export` needed. You can still override any value via your shell / IDE run
configuration, and the app boots fine on the built-in defaults if `.env` is absent.

---

## 3. Run the backend

```bash
cd backend
mvn spring-boot:run
```

- Boots on `http://localhost:8080`
- Flyway applies migrations `V1`–`V10` on startup
- In the `dev` profile (default), a **demo organization** is seeded on first run
- API docs (Swagger UI): **http://localhost:8080/swagger-ui.html**

---

## 4. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173**. The login page is pre-filled with the demo credentials.

---

## Demo login

The `dev` profile seeds a demo org (`StockPilot Demo`) with the 5 default channels, ~15 products
(25 SKUs), inventory levels (some intentionally low-stock), and ~150 days of sample sales spread
across channels — so the dashboard is populated immediately.

| Role | Email | Password |
|---|---|---|
| Owner | `owner@demo.stockpilot.io` | `Demo@12345` |
| Admin | `admin@demo.stockpilot.io` | `Demo@12345` |
| Staff | `staff@demo.stockpilot.io` | `Demo@12345` |

**Re-seed:** the seeder is idempotent (skips if the demo org exists). To regenerate, drop and
recreate the database in pgAdmin 4 (or `DROP DATABASE stockpilot; CREATE DATABASE stockpilot;` in
`psql`), then restart the backend — Flyway rebuilds the schema and the seeder repopulates demo data.

---

## Signing up a brand new org

1. **Sign up** with an org name, your details, and a password.
2. The backend emails a 6-digit OTP — with no Brevo key configured, find it in the backend console
   on the line `[DEV EMAIL FALLBACK] ... OTP=NNNNNN ...`.
3. Enter it on the **Verify** screen → you're logged in as the org OWNER, with default channels seeded.

---

## Architecture highlights

- **Strict tenant isolation.** Every domain row carries `organization_id`. The org id is derived
  *only* from the authenticated user's JWT (`CurrentTenant`) — never from request input. Repositories
  are org-scoped by method signature, a service-level `TenantGuard` re-checks ownership (returning
  404, not 403, on cross-org access), and a `TenantSecurityAspect` is a last-resort net.
- **Auth.** 15-min access JWT + opaque refresh token (only its SHA-256 hash is stored), rotated on
  every use with reuse-detection. Roles OWNER/ADMIN/STAFF enforced via `@PreAuthorize`.
- **Sales → inventory.** Recording a sale (manual or import) runs in one transaction: writes the
  sale, decrements on-hand stock (with a `SELECT ... FOR UPDATE` row lock), draws down the channel
  allocation, and writes a `StockMovement`. Negative stock is flagged (`WARN`) or blocked (`BLOCK`).
- **Allocation.** Allocating to a channel reserves — never removes — central stock; total allocations
  across channels can't exceed on-hand quantity.
- **Imports.** CSV and XLSX (Apache POI) share one row-parsing pipeline; each row commits in its own
  transaction so one bad row never rolls back the good ones. Failed rows produce a downloadable error
  report. Sample templates: `GET /api/v1/templates/{products,sales}-import-sample`.
- **Analytics.** DB-side aggregate SQL, org-scoped, with preset (`LAST_7D/30D/90D`, `THIS_MONTH`,
  `THIS_YEAR`) and custom date ranges.

---

## Tests

Integration tests cover the auth flow, tenant isolation, sale→inventory decrement + return, and
analytics aggregation. **They use a real PostgreSQL — never H2.**

```bash
cd backend
mvn test                          # uses Testcontainers (ephemeral PostgreSQL) — needs a reachable Docker engine
```

If your local Docker engine is too new for the bundled docker-java client (Testcontainers can't
connect), run against a dedicated local test database instead — the spec explicitly allows either:

```bash
docker exec stockpilot-postgres createdb -U stockpilot stockpilot_test    # once, on a clean DB
mvn test -Duse.local.postgres=true
# optional overrides: -Dlocal.postgres.url=... -Dlocal.postgres.username=... -Dlocal.postgres.password=...
```

---

## API overview (prefix `/api/v1`)

- **Auth:** `POST /auth/{signup,verify-otp,resend-otp,login,refresh,forgot-password,reset-password}`, `GET /auth/me`
- **Team:** `GET/POST /users`, `PATCH/DELETE /users/{id}`
- **Organization:** `GET/PATCH /organization`
- **Channels:** `GET/POST /channels`, `PATCH/DELETE /channels/{id}`
- **Products & SKUs:** `GET/POST /products`, `GET/PATCH/DELETE /products/{id}`, `POST /products/import`
- **Inventory:** `GET /inventory`, `GET /inventory/low-stock`, `POST /inventory/adjust`,
  `GET/POST /channels/{id}/listings`, `PATCH /listings/{id}`
- **Sales:** `GET/POST /sales`, `POST /sales/{id}/return`, `DELETE /sales/{id}`, `POST /sales/import`,
  `GET /import-batches`, `GET /import-batches/{id}/error-report`
- **Analytics:** `GET /analytics/{summary,sales-by-channel,sales-trend,top-products,channel-comparison}`

Full, interactive documentation is at `/swagger-ui.html`.
