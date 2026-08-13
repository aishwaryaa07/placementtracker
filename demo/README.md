# demo

Spring Boot backend for Placement Tracker. Implements JWT auth (register/login) and an admin API for managing companies, placement drives, and interview rounds. Student-facing endpoints (browsing drives, applying, tracking application status) don't exist yet.

## Requirements

- JDK 17 or 21. **Not JDK 25** — Lombok's annotation processor does not yet support it, and the build will fail with `does not override abstract method` errors. Use the bundled wrapper (`./mvnw` / `mvnw.cmd`), which downloads Maven itself but still needs `JAVA_HOME` pointed at a compatible JDK.
- Docker (for local PostgreSQL), or a PostgreSQL instance reachable at the configured URL.

Build and run:

```bash
./mvnw clean package
./mvnw spring-boot:run
```

Or run the packaged jar:

```bash
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

PostgreSQL setup:

```bash
docker compose up -d
```

The app connects to PostgreSQL at `localhost:5432` using the database `placement_tracker`, user `postgres`, and password `postgres` by default (overridable via `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` env vars). To stop it:

```bash
docker compose down
```

## Auth API

- `POST /api/auth/register` — body `{ "name", "email", "password" }` (password min 8 chars) → `201` with `{ token, email, name }`
- `POST /api/auth/login` — body `{ "email", "password" }` → `200` with `{ token, email, name }`

All other endpoints require `Authorization: Bearer <token>`.

Set `JWT_SECRET` in production — the default in `application.properties` is dev-only.

## Admin API

Everything under `/api/admin/**` requires a token for a user with `role=ADMIN`. There's no signup path for admins yet — promote a registered user manually:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'you@example.com';
```

- `POST /api/admin/companies` / `GET /api/admin/companies` / `GET,PUT,DELETE /api/admin/companies/{id}`
- `POST /api/admin/drives` / `GET /api/admin/drives?status=&companyId=` / `GET,PUT,DELETE /api/admin/drives/{id}`
- `PATCH /api/admin/drives/{id}/status` — body `{ "status": "UPCOMING" | "ONGOING" | "CLOSED" }`
- `POST /api/admin/drives/{driveId}/rounds` / `GET /api/admin/drives/{driveId}/rounds`
- `PUT,DELETE /api/admin/rounds/{roundId}`

Drive request body: `{ "companyId", "role", "description", "ctc", "minCgpa", "eligibleBranches": [...], "applicationDeadline": "YYYY-MM-DD", "driveDate": "YYYY-MM-DD" }`.
