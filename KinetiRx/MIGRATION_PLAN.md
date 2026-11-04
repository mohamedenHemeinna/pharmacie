# KinetiRx: Go → Spring Boot Backend Migration Plan

Branch: `migration/spring-boot` (created from `main`).
Status legend: ✅ done · 🔧 in progress · ⬜ not started

## 0. Environment and constraints found during analysis

| Item | Finding | Consequence |
|---|---|---|
| Live database | PostgreSQL 18 runs locally, but the `kinetirx` role requires a password I don't have. No `.env` in `backend/` or `deploy/`. | I did **not** touch the live database. Testing uses a private PostgreSQL 18.6 instance I started on port 5544 (`~/.local` tooling, no root). Final verification against your real data needs your DB URL (see §9). |
| Go toolchain | `go1.26.8` present; `go build ./cmd/server` succeeds. | I built the original backend as a **reference oracle** (`~/.local/kinetirx-go-ref`) and ran it against a copy of the schema on port 8081. |
| JDK / Maven | Not installed. Installed to `~/.local/jdk/jdk-21.0.13+9` and `~/.local/maven/apache-maven-3.9.9` (no root). Env script: `~/.local/kinetirx-env.sh`. | Java 21 + Maven 3.9.9 available. |
| Docker | Not installed; no sudo. | Dockerfile, compose, and nginx changes can be **written** but **not built/run** here. Testcontainers cannot run; integration tests use the private PostgreSQL instead. |
| Network | Maven Central, Adoptium, Go proxy reachable. | Dependencies can be resolved. |
| Git | Repo has uncommitted changes in `frontend/src/App.tsx`, `frontend/src/components/modals/LoginModal.tsx`, `frontend/src/data/initialData.ts` (not mine). | These files are **excluded** from every migration commit. |
| Git identity | `git config user.name` = `mohamedenHemeinna`; `CLAUDE.md` says commits must be authored as **Raktim94**. | Not resolved; needs your decision before committing (see §9). |

## 1. Current architecture

```
KinetiRx/
├── backend/            Go 1.26 + Gin + pgx/v5 (5,749 lines, 87 routes, 22 migration files)
│   ├── cmd/server        entrypoint: config, migrate, seed admin, serve, graceful shutdown
│   ├── internal/config   env config (PORT, DATABASE_URL, JWT_SECRET, GEMINI_API_KEY, ...)
│   ├── internal/db       pgx pool + custom migrator (schema_migrations table)
│   ├── internal/handlers 16 handler files, 1 router (routes below)
│   ├── internal/middleware  RequestID, logger, CORS, security headers, JWT, RBAC, rate limit
│   ├── internal/auth     JWT HS256 issue/verify, bcrypt cost 12
│   ├── internal/backup   S3 (aws-sdk-go-v2), pg_dump/pg_restore, scheduler, retention
│   ├── internal/aiprovider  encrypted OpenAI-compatible endpoint config
│   ├── internal/crypto   AES-256-GCM (nonce||ciphertext||tag), hex key from env
│   ├── internal/events   in-process SSE pub/sub
│   ├── internal/seed     first-boot admin (EMP-ADMIN-1)
│   ├── internal/httpx    error envelope + helpers
│   └── migrations/       0001..0011 *.up.sql / *.down.sql
├── frontend/           React 18 + Vite + TypeScript (~24.5k lines). Calls /api/* via src/lib/api.ts
├── deploy/             docker-compose.yml (postgres, backend, frontend), .env.example
├── packaging/deb/      .deb installer, systemd unit, docker-compose for packaged install
├── casaos/             CasaOS app definition
├── mcp-server/         Go MCP server that calls the REST API (out of scope; see §8)
└── scripts/, install.sh
```

Frontend API client: `frontend/src/lib/api.ts` (636 lines). It sends `Authorization: Bearer`, expects the error envelope, and uses `EventSource` for live sync. **The React code is not changed by this migration.**

## 2. Complete API inventory (87 endpoints)

Auth legend: **P** = public · **A** = any authenticated employee · **Perm(x)** = permission `x` or admin · **Admin** = role admin only.

### 2.1 Platform and auth (7)
| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | /api/health | P | `{status:"ok", timestamp}` |
| POST | /api/auth/login | P | rate limit 10/min/IP; identifier = id or name (case-insensitive) |
| GET | /api/auth/setup-status | P | `{needsSetup}` |
| POST | /api/auth/setup | P | rate limit 5/min/IP; 201 + login payload; 409 once any employee exists |
| GET | /api/events/stream | token in `?token=` or Bearer | SSE; heartbeat every 25 s |
| GET | /api/auth/me | A | re-read from DB |
| PUT | /api/auth/password | A | rate limit 10/min; 204 |

### 2.2 Inventory (medicines + groups + distributors) (15)
| Method | Path | Auth | Entity |
|---|---|---|---|
| GET/POST | /api/medicines | Perm(inventory) | Medicine |
| GET/PUT/DELETE | /api/medicines/:id | Perm(inventory) | Medicine |
| GET/POST | /api/medicine-groups | Perm(inventory) | MedicineGroup |
| GET/PUT/DELETE | /api/medicine-groups/:id | Perm(inventory) | MedicineGroup |
| GET/POST | /api/distributors | Perm(inventory) | Distributor |
| GET/PUT/DELETE | /api/distributors/:id | Perm(inventory) | Distributor |

### 2.3 Patients and credit (11)
| Method | Path | Auth |
|---|---|---|
| GET/POST | /api/patients | Perm(patients) |
| GET | /api/patients/next-id | Perm(patients) — `nextval('patient_id_seq')` |
| GET/PUT/DELETE | /api/patients/:id | Perm(patients) |
| GET/POST | /api/due-khata | Perm(due-khata) |
| GET/PUT/DELETE | /api/due-khata/:id | Perm(due-khata) |

### 2.4 Sales (3)
| GET | /api/sales (`?date=`, `?patientId=`) | Perm(daily-sales) |
| GET | /api/sales/:id | Perm(daily-sales) |
| POST | /api/sales | Perm(pos) — append-only |

### 2.5 Operations (expenses, orders, OPD, business dev, staff) (30)
| Group | Methods | Auth |
|---|---|---|
| /api/expenses (`?date=` on list) | GET, GET/:id, POST, PUT/:id, DELETE/:id | Perm(expenses) |
| /api/needed-meds | same 5 | Perm(medicine-orders) |
| /api/opd-visits | same 5 | Perm(opd) |
| /api/marketing-campaigns | same 5 | Perm(business-dev) |
| /api/worksheet-tasks | same 5 | Perm(business-dev) |
| /api/employees | GET, GET/:id → Perm(employee-mgmt); POST, PUT/:id, DELETE/:id → **Admin** | mixed |

### 2.6 Cash register, invoice, OCR, AI (9)
| Method | Path | Auth |
|---|---|---|
| GET/PUT | /api/daily-register | Perm(daily-calc) (singleton) |
| GET | /api/invoice-config | A (singleton) |
| PUT | /api/invoice-config | Perm(invoice-settings) |
| POST | /api/ocr/parse-bill | Perm(inward-ocr) |
| POST | /api/ai/ask | A |
| GET/POST/DELETE | /api/ai-config | Admin |

### 2.7 Security and backup (12)
| Method | Path | Auth |
|---|---|---|
| GET | /api/security/master-pin | A |
| PUT | /api/security/master-pin | Admin |
| POST | /api/security/master-pin/verify | A, rate limit 10/min |
| GET/POST/DELETE | /api/backup/config | Admin |
| GET | /api/backup/status | Admin |
| GET/POST | /api/backup | Admin |
| POST | /api/backup/:id/restore | Admin, body `{"confirm":"RESTORE"}` |
| GET | /api/backup/download | Admin, streams a `pg_dump` |
| POST | /api/backup/restore-upload | Admin, multipart `confirm=RESTORE` + `file` |

Count check: 7 + 15 + 11 + 3 + 30 + 9 + 12 = **87** ✔ (matches `grep` of `router.go`).

## 3. Database schema

Tables (all in `public`): `employees`, `distributors`, `medicines`, `patients`, `patients_due`, `sales_history`, `expenses`, `needed_meds`, `opd_visits`, `marketing_campaigns`, `worksheet_tasks`, `daily_register` (singleton), `invoice_config` (singleton), `medicine_groups`, `security_settings` (singleton), `backup_config` (singleton), `backup_history`, `ai_provider_config` (singleton), `schema_migrations` (Go migrator), sequence `patient_id_seq`.

Key relations:
- `medicines.distributor_id → distributors.id` (ON DELETE SET NULL)
- `patients_due.patient_id`, `sales_history.patient_id`, `needed_meds.patient_id → patients.id` (ON DELETE SET NULL, deferrable)
- `opd_visits.patient_id` — no FK (by design, migration 0007)
- Partial unique index `medicines_barcode_key` on `barcode WHERE barcode IS NOT NULL`
- Check constraints on `needed_meds.status`, `marketing_campaigns.status`, `worksheet_tasks.status`, `backup_history.source/status`, singleton `id = true`.

**Column ↔ JSON mapping quirks that must be preserved:**
- `medicines.group_name` ↔ JSON `group`; `medicines.dist` ↔ `dist`; `medicines.distributor_id` ↔ `distributor`
- `expenses.description` ↔ JSON `desc`; `needed_meds.time` ↔ `time`
- `sales_history.items_detail` (JSONB) ↔ `itemsDetail`
- `patients.purchase_history` (JSONB), `patients.blood_tests` (TEXT[]) ↔ arrays
- `employees.permissions` (TEXT[]), `pin_hash`/`password_hash` never serialized
- `daily_register.denominations` JSONB map; `invoice_config.wa_group` ↔ `waGroup`

**Flyway strategy (no destructive migrations):**
1. Copy `0001..0011 *.up.sql` to `src/main/resources/db/migration/V1__init.sql … V11__ai_provider_config.sql` unchanged (only file names change).
2. Existing deployments: run Flyway with `baseline-on-migrate=true`, `baseline-version=11`. Flyway records version 11 as the baseline and **executes nothing** on the live database. Only new `V12+` scripts run.
3. Fresh installs: Flyway runs V1–V11 in order, producing the same schema the Go migrator produces.
4. The Go `schema_migrations` table is left in place (not dropped) so the rollback path (running the Go binary again) still works.
5. Down migrations are not ported; they were never run in production (`*.down.sql` are for manual use). They are kept in the repo for reference.

Hibernate: `spring.jpa.hibernate.ddl-auto=validate` against the existing schema, so Hibernate never alters tables. Reads and writes use the existing column names via explicit `@Column(name=…)`.

## 4. Behaviour that must be preserved (parity contract)

| Area | Go behaviour | Java requirement |
|---|---|---|
| Error envelope | `{type,title,status,errors?,request_id?}`; types `validation_error, unauthorized, forbidden, not_found, conflict, internal_error, rate_limited, locked, not_configured, ai_request_failed, ocr_extraction_failed, backup_failed, restore_failed` | Identical |
| Request ID | Reads `X-Request-Id` or generates a UUID, echoes it in the response header and in error bodies | Same filter |
| Security headers | `nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy`, `CSP: default-src 'none'`, HSTS | Same headers |
| CORS | Allowed origins from `ALLOWED_ORIGINS` (default `http://localhost:5173,http://localhost:3000`), credentials true, methods GET/POST/PUT/PATCH/DELETE/OPTIONS, headers Authorization/Content-Type/X-Request-Id; OPTIONS → 204 | Same |
| JWT | HS256 only; claims `eid,name,role,permissions` + `sub, iat, nbf, exp, iss="kinetirx-backend"`; TTL 12 h; secret ≥16 chars | **Same claims and secret**, so tokens issued by the Go backend remain valid during cutover |
| Passwords | bcrypt cost 12 (`$2a$`) | Spring `BCryptPasswordEncoder(12)` verifies Go's `$2a$` hashes unchanged ✔ |
| Login errors | Identical 401 "Invalid credentials" for unknown user or wrong password | Same (anti-enumeration) |
| Access checks | Permissions come from the **JWT** (not the DB) for `RequirePermission`; `Me` reads the DB | Same |
| Last-admin guard | Cannot demote or delete the last admin; cannot delete self | Same |
| Employee creation | Password required, ≥8 chars, `must_change_password=true` | Same |
| PIN | 4–8 digits; bcrypt; 5 failures → 15 min lock; lock returns **423** `locked` | Same |
| Master PIN rotation | Current PIN required if one is set | Same |
| Crypto | AES-256-GCM, `nonce(12) ‖ ciphertext ‖ tag(16)`, key = 32 bytes hex in `BACKUP_ENCRYPTION_KEY` | Java `AES/GCM/NoPadding`, 12-byte IV prepended → **byte-compatible**, existing encrypted S3 secret and AI key decrypt unchanged |
| Timestamps | RFC 3339 (`2026-11-04T17:05:09Z`) in `expiresAt`, `timestamp`, `lockedUntil`; `createdAt`/`updatedAt` as RFC 3339 with fraction | Same format |
| Optional fields | `omitempty` on pointer fields (e.g. `distributor`, `generic`, `barcode`, `age`, `addr`) | `@JsonInclude(NON_NULL)` on exactly those fields |
| Empty collections | Lists return `[]`, never `null` | Same |
| Live sync events | Publishes `medicines`, `dailyRegister`, `neededMeds`, `dueKhata`, `sales` only | Same resource names, same SSE frame `data: {"resource":"…"}` |
| Rate limits | Fixed window per client IP, in memory | Same semantics (per instance) |
| Patient IDs | `nextval('patient_id_seq')` → string id | Same |
| Singletons | GET returns zero-value default if no row; PUT upserts | Same |
| Backup | `pg_dump --format=custom --no-owner --no-privileges`; restore `pg_restore --clean --if-exists`; upload → download → SHA-256 verify; retention keeps newest N verified | Same commands, same verification |
| Backup scheduler | Hourly check; due when last verified ≥ `intervalDays` | Same |
| AI | Operator config (OpenAI-compatible) first, then `GEMINI_API_KEY`, else fallback JSON `{success:false, fallback:true, ...}` with HTTP 200 | Same |
| Uploads | Multipart restore (no limit set in Go beyond gin default) | Spring `max-file-size` set explicitly (e.g. 1 GB) |

Notable Go details to keep in mind during the port:
- Several handlers return **HTTP 409 for any insert failure** (duplicate id, FK violation). This is the observed behaviour that the frontend relies on, so the Java port returns 409 too.
- `DailyRegister` and `InvoiceConfig` return defaults on `GET` when no row exists (not 404).
- `Sales` has no update or delete.
- Medicine `UPDATE` is a full replace; omitted `trackStock` defaults to `true`, `itemType` defaults to `"medicine"`.

## 5. Module ↔ Java package mapping

Target layout (as requested):
```
backend-java/src/main/java/com/kinetirx/
  config/        SecurityConfig, WebConfig, JpaConfig, AppProperties
  security/      JwtService, JwtAuthenticationFilter, PermissionAuthorization, EmployeePrincipal
  controller/    one controller per module (thin: no business logic)
  service/       business logic + @Transactional boundaries
  repository/    Spring Data JPA repositories
  entity/        JPA entities (table/column names kept from the schema)
  dto/           request/response records (JSON names identical to Go)
  mapper/        entity ↔ dto mappers (manual, no reflection magic)
  exception/     ApiException hierarchy + GlobalExceptionHandler (envelope)
  validation/    custom validators (permission names, PIN pattern, statuses)
  common/        RequestIdFilter, SecurityHeadersFilter, RateLimiter, SseEventBus, Crypto
backend-java/src/main/resources/
  application.yml, db/migration/V1..V11
```

## 6. Migration phases (status)

| Phase | Scope | Status |
|---|---|---|
| 1. Project analysis | Full read of Go backend, schema, frontend API client, environment | ✅ (§0–§8) |
| 2. API and DB mapping | Endpoint inventory, column/JSON map, parity contract | ✅ (§2–§4) |
| 3. Spring Boot setup | pom.xml, package layout, config, Flyway V1–V11 baseline, Hibernate validate, error envelope, request-id/security/CORS filters, access log | ✅ |
| 4. Auth and security | JWT (same claims, HS256, no minimum key length, Go tokens accepted), BCrypt (Go hashes verified), login/setup/me/password, RBAC per route group, rate limits, SSE token auth, Master PIN lockout | ✅ |
| 5. Core business | medicines, medicine groups, distributors, patients (+ sequence), due-khata, sales | ✅ |
| 6. Inventory and pharmacy | expenses, needed-meds, OPD, daily-register, invoice-config, master PIN | ✅ |
| 7. Reports and remaining | marketing campaigns, worksheet tasks, employees, OCR, AI assistant, AI provider config, backup (S3, pg_dump/pg_restore, scheduler, retention) | ✅ code complete; S3 path not exercised against a real bucket (none available) |
| 8. Frontend integration | Unchanged React calls the Java API; `tsc --noEmit` passes; every endpoint `api.ts` calls is covered by the parity harness | 🔧 API-level verified; no browser session run |
| 9. Docker and deployment | `backend-java/Dockerfile` (JRE 21 + postgresql-client-16), `deploy/docker-compose.yml` builds from `backend-java`, CI job `backend-java` | 🔧 written, **not built** (no Docker here). Packaging/CasaOS still point at the Go GHCR image (needs a release decision, §9) |
| 10. Full testing and Go removal | parity suite, unit + integration tests, fresh install, backup/restore round trip | 🔧 tests pass; **Go code still in the repo** and used as the reference. Removal waits for your approval (§9) |

## 7. Testing strategy

1. **Parity tests (primary):** start the Go reference binary and the Java app on the *same* copy schema, send identical requests, diff status code, headers that matter, and JSON bodies (ignoring generated ids and timestamps).
2. **Unit tests:** JUnit 5 + Mockito for services (permission logic, last-admin guard, PIN lockout, FEFO-independent calculations, backup verification logic, crypto round-trip against Go-encrypted fixtures).
3. **Integration tests:** Spring Boot tests against PostgreSQL. Testcontainers needs Docker, which is not available here, so tests run against the private PostgreSQL 18 instance via a configurable JDBC URL. This is documented as a deviation.
4. **Frontend:** `npm run build` + type-check, plus a scripted login → each tab smoke test against the Java backend. Browser testing needs a running UI.
5. **Database:** Flyway `validate` on the copy DB; Hibernate `validate`; a data-count comparison before and after each module.

## 8. Risks and dependencies

| Risk | Impact | Mitigation |
|---|---|---|
| No access to live DB | Cannot verify on your real data | Work on a copy; ask for a DB URL or a dump for final check |
| Flyway baseline mistake | Could re-run destructive SQL | Baseline at 11 on existing DBs; test with a copy first; migrations V1–V11 are not edited |
| JSON shape drift | Frontend breaks silently | Parity test harness against the Go binary; DTO tests |
| `pg_dump` / `pg_restore` missing in Java image | Backup/restore fail | Install `postgresql-client-16`+ in Dockerfile; check `pg_dump --version` at startup |
| Multipart restore size | Large dumps rejected | Explicit `spring.servlet.multipart.max-file-size` |
| SSE through nginx | Live sync stops | Keep `X-Accel-Buffering: no` and the same route |
| Rate-limit per instance | Same as Go | Documented, unchanged |
| Lost token compatibility | Users logged out | Same JWT secret and claims |
| `mcp-server/` (Go) | Calls the REST API; should keep working | Verify after Phase 7 (it uses the same endpoints; no change expected) |
| CasaOS / .deb packaging | Reference Go image name | Update image names only in Phase 9 |
| Docker not available | Cannot run the container image here | State this clearly; verify on a machine with Docker |

## 9. Decisions needed from you

See §11 (current list).

## 10. Implementation results (verified)

Everything below was run in this environment. Nothing is claimed beyond it.

| Check | Result |
|---|---|
| Route inventory | Go router: 87 routes. Java controllers: 87 routes. Same method+path set, nothing missing, nothing extra. |
| API parity suite (`backend-java/parity/parity.py`) | **110 / 110** scenarios match the Go reference: status codes, JSON shape, error types, cross-backend JWT acceptance, permission matrix (restricted user), Master PIN lockout (5 failures → 423), write flows (create/update/delete for every module), AI/OCR fallbacks, backup validation, backup download. |
| JUnit unit tests (`mvn test`) | **34 / 34** pass: JWT (accepts a token signed by the Go backend's jwt/v5 library; rejects tampering, `alg:none`, wrong key, expiry), AES-GCM (decrypts a ciphertext produced by Go's crypto; wrong key and tamper fail), bcrypt (Go `$2a$12$` hash verifies), DB URL conversion, last-admin and self-delete guards, PIN lockout rules, rate limiter, client IP. |
| Integration tests (`ApiSmokeIT`, env `KINETIRX_TEST_DATABASE_URL`) | **5 / 5** pass against the disposable copy: health, auth gate envelope, request-id echo, security headers, unknown route. |
| Fresh install | Empty database: Flyway applied V1→V11 in order, first admin seeded from `KINETIRX_ADMIN_PASSWORD`, login works, Hibernate `validate` passes. |
| Existing database | Copy of a Go-migrated schema: Flyway baselined at 11, ran nothing, login with the Go-created admin and Go-issued tokens works. |
| Backup and restore | Download from the reference copy (`pg_dump`, custom format) → upload to the fresh database (`pg_restore --clean --if-exists`) → row counts match, reference credentials work on the restored database. |
| Live server-sent events | Same event on both backends (`data: {"resource":"dailyRegister"}`); unauthenticated stream → 401 on both. |
| CORS / headers | Allowed origin gets the Go headers; disallowed origin gets none; preflight → 204. |
| Frontend type-check | `tsc --noEmit` exits 0 (includes your uncommitted edits, which I did not touch). |

### Known differences from Go (accepted, documented)

1. **Anonymous request to an unknown path:** Java returns **401** (security runs before routing). Go returned 404. Authenticated unknown paths return the 404 envelope.
2. **Error titles on DB conflicts:** Go appended the raw PostgreSQL error text to titles such as "Failed to create medicine (…): ERROR: …". Java returns a fixed title with the same type and status. This avoids leaking database internals.
3. **Validation titles:** Go's binder messages (e.g. `Key: 'MedicineInput.Name' Error:Field validation…`) are replaced by readable titles. Status and type are unchanged.
4. **Rate limiting uses the same rule as Gin** (first `X-Forwarded-For` entry, then `X-Real-IP`, then socket address). That trusts the header, which Go also did. It only holds behind the nginx proxy in this deployment.
5. **SSE frame** and other byte-level details match Go; `Instant` timestamps keep Go's microsecond precision.

### Not done, and why

- **Docker build and container run:** Docker is not installed and I have no root access, so `backend-java/Dockerfile` is written but unbuilt.
- **Live database:** the `kinetirx` role needs a password I don't have, so all testing used a private copy. Nothing was run against your production data.
- **Real S3 backup:** no S3-compatible endpoint is available here. Connection, upload, download-verify, retention, and the scheduler were not exercised against a bucket.
- **Browser test of the React UI:** not run. The API contract is verified by the parity suite, which covers every endpoint `api.ts` calls.
- **Windows (PowerShell / Git Bash):** not applicable to the backend's runtime, but nothing was run on Windows.
- **Go code removal:** not performed. The Go backend is kept as the reference oracle.

## 11. Decisions still needed

1. **Live database check:** provide a copy or dump, or a `DATABASE_URL` with rights to a copy, so the final migration check can run on real data.
2. **Git identity:** `CLAUDE.md` requires commits authored as `Raktim94`, but git is configured as `mohamedenHemeinna`. I have not committed. Tell me which identity to use.
3. **Go removal:** approve deleting `backend/` (and its CI job) once you've accepted the parity results, or keep it as reference.
4. **Packaging and CasaOS:** these pull `ghcr.io/raktim94/kinetirx-backend` prebuilt images, produced by your release pipeline. Publishing a Java image is outward-facing, so it stays with you.
5. **Frontend edits in progress:** `App.tsx`, `LoginModal.tsx`, `initialData.ts` are uncommitted and stay untouched.

## 12. Files added or changed

- `MIGRATION_PLAN.md` (this file)
- `backend-java/` (new): `pom.xml`, `Dockerfile`, `.dockerignore`, `.gitignore`, `src/main/**` (config, security, controllers, services, repositories, entities, DTOs, mappers, Flyway migrations V1–V11), `src/test/**`, `parity/parity.py`
- `deploy/docker-compose.yml`: backend build context → `../backend-java`; removed the unused `GIN_MODE`
- `.github/workflows/ci.yml`: added the `backend-java` job; the Go job remains
- Not changed: `backend/` (Go), `frontend/` (React), `mcp-server/`, packaging, CasaOS

## 13. Earlier notes (environment)

- Branch `migration/spring-boot` created from `main`.
- Reference Go binary built and running against a copy schema (port 8081), login verified.
- Private PostgreSQL 18.6 instance for tests (port 5544), copy database `kinetirx_ref`.
- Java 21 and Maven 3.9.9 installed under `~/.local`.

Work completed in later sections of this file is marked with its phase status above; nothing is marked done without a test run.
