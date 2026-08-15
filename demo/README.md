# demo

Spring Boot backend for Placement Tracker. Implements JWT auth (register/login), an admin API for managing companies, placement drives, and interview rounds, and a student API for maintaining a profile, browsing drives, and applying.

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

**Grading and offers:**

- `GET /api/admin/drives/{driveId}/applications` / `GET /api/admin/applications/{id}` — view applications (any student's, unlike the student-facing endpoints which are self-only)
- `PATCH /api/admin/round-results/{id}` — body `{ "status": "PENDING" | "PASSED" | "FAILED", "remarks" }`. Marking `FAILED` auto-sets the application to `REJECTED`; marking `PASSED` moves a fresh `APPLIED` application to `IN_PROGRESS`
- `POST /api/admin/applications/{applicationId}/offer` — body `{ "ctcOffered", "offerDate": "YYYY-MM-DD" }` (`offerDate` defaults to today). One offer per application — a second attempt is rejected with `400`. Creating an offer sets the application to `SELECTED`
- `PATCH /api/admin/offers/{id}/status` — body `{ "status": "PENDING" | "ACCEPTED" | "DECLINED" }`, for an admin to override/correct an offer's status

## Student API

Everything under `/api/student/**` requires a token for a user with `role=STUDENT` (the default on registration).

- `GET,PUT /api/student/profile` — body `{ "branch", "graduationYear", "cgpa", "phone", "resumeUrl" }`. `PUT` creates the profile on first call, updates it after. Applying to a drive requires a profile to exist first.
- `GET /api/student/drives` / `GET /api/student/drives/{id}` — lists all non-`CLOSED` drives; each has an `eligible` flag computed from your profile's branch and CGPA against the drive's `eligibleBranches`/`minCgpa`
- `POST /api/student/drives/{id}/apply` — rejected (`400`) if the drive is closed, its deadline has passed, you've already applied, or you're not eligible; on success, pre-creates a `PENDING` round result for each of the drive's rounds
- `GET /api/student/applications` / `GET /api/student/applications/{id}` — your own applications only; other students' applications 404 rather than 403, so IDs can't be enumerated
- `PATCH /api/student/offers/{id}/status` — body `{ "status": "ACCEPTED" | "DECLINED" }`, for your own pending offer only. Rejected with `400` if you've already responded or pass anything other than `ACCEPTED`/`DECLINED`; another student's offer 404s. Visible to admin immediately via `GET /api/admin/applications/{id}`
