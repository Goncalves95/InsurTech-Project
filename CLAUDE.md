# CLAUDE.md — InsurTech Claims Platform

Project instructions for Claude Code. Read this before doing anything.

---

## What this project is

Swiss Krankenkassen medical invoice reimbursement platform. Policy holders upload PDF invoices; the system OCR-extracts Tarmed billing codes, validates against the policy (deductible/franchise, coverage limits, code whitelist), and auto-approves or flags for manual review. Built as a portfolio project targeting Swiss insurance companies.

Contact / owner: create@raigonlab.com  
License: Proprietary — no commercial use, copying, or redistribution without consent.

---

## Hard rules (never break these)

- All code, comments, DB schemas, API contracts, commit messages **strictly in English**.
- **Never commit** `.env`, `*.pem`, `*.key`, `*.p12`, `*.jks`, `*.keystore`, `*.truststore`, `secrets/`, `credentials/`, `docker-compose.override.yml`. All are in `.gitignore`.
- `GlobalExceptionHandler` 5xx handler **deliberately omits internal error details** from responses — do not add them.
- No Lombok. Java 21 records + explicit accessors cover every use case.
- No comments explaining WHAT code does. Only add a comment when the WHY is non-obvious (hidden constraint, workaround, subtle invariant).
- Clean separation of responsibilities: domain knows nothing about infrastructure.

---

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.4.1, Spring Modulith 1.3.1 |
| Architecture | Hexagonal (ports & adapters), DDD aggregate roots |
| Messaging | Apache Kafka (KRaft, no ZooKeeper) via Spring Modulith events |
| Persistence | PostgreSQL 16, JPA/Hibernate, Liquibase migrations |
| Security | Keycloak 26.0, OAuth2 Resource Server, PKCE S256 |
| OCR | Azure Document Intelligence (prebuilt-invoice model) |
| Storage | Azure Blob Storage |
| Email | SendGrid (azure profile only) |
| Frontend | Angular 21, standalone components, Angular Signals, Angular Material |
| Auth (FE) | keycloak-js 26.2.4 |
| Serving | nginx 1.27 (Docker), ng serve (local dev) |
| Containers | Docker Compose (postgres, kafka, keycloak, backend, frontend) |
| CI | GitHub Actions — parallel backend + frontend jobs + ci-gate aggregator |

---

## Project structure

```
InsurTech-Project/
├── backend/                        # Spring Boot application
│   └── src/main/java/ch/insurtech/platform/
│       ├── claim/                  # Claim aggregate: submit, OCR trigger, status
│       ├── ocr/                    # OCR extraction: Azure DI adapter + stub
│       ├── rules/                  # Validation strategies (deductible, coverage, Tarmed)
│       ├── notification/           # Email notifications: SendGrid adapter + stub
│       └── shared/                 # Exceptions, security config, JPA auditing, OpenAPI
├── frontend/                       # Angular 21 SPA
│   ├── Dockerfile                  # Multi-stage: Node build → nginx serve
│   ├── nginx.conf                  # SPA routing + /api/ proxy to backend:8080
│   └── src/app/
│       ├── core/                   # auth (AuthService), api clients, interceptors, guards
│       ├── features/
│       │   ├── portal/             # Policy holder views: my-claims, submit-claim
│       │   └── backoffice/         # Reviewer views (backoffice role)
│       └── shared/                 # Reusable components (StatusChip, etc.)
├── docker-compose.yml              # All 5 services
├── .env.example                    # Template — copy to .env and fill secrets
├── .env                            # Local secrets — gitignored, never commit
├── keycloak/insurtech-realm.json   # Realm export (imported on container start)
└── .github/workflows/ci.yml        # CI pipeline
```

---

## Backend module breakdown

Each module has: `domain/` (model, ports, events), `application/` (use-case services), `api/` (controllers, DTOs), `infrastructure/` (adapters).

### claim
- **Claim** aggregate root — state machine: `PENDING_OCR → OCR_PROCESSING → PENDING_VALIDATION → APPROVED | MANUAL_REVIEW_REQUIRED | REJECTED`
- **ClaimApplicationService** — orchestrates submission, delegates to DocumentStoragePort
- **ClaimController** — REST endpoints, JWT-secured
- Ports: `ClaimRepository`, `DocumentStoragePort`
- Events published: `DocumentUploadedEvent` → triggers OCR

### ocr
- **OcrProcessingService** — listens to `DocumentUploadedEvent`, calls `OcrProviderPort`
- Ports: `OcrProviderPort`
- Events published: `DataExtractedEvent` → triggers validation
- Adapters: `StubOcrProviderAdapter` (dev), `AzureDocumentIntelligenceAdapter` (azure)

### rules
- **ClaimValidationService** — listens to `DataExtractedEvent`, runs validation strategies
- Strategies: `DeductibleValidationStrategy`, `CoverageAmountValidationStrategy`, `TarmedCodeValidationStrategy`
- Ports: `PolicyContextPort`
- Events published: `ClaimApprovedEvent` | `ManualReviewRequiredEvent`
- Adapters: `StubPolicyContextAdapter` (dev) — CHF 300 franchise, CHF 200 paid, CHF 5 000 max

### notification
- **NotificationService** — listens to `ClaimApprovedEvent` / `ManualReviewRequiredEvent`
- Ports: `NotificationPort`, `UserEmailResolverPort`
- Adapters (dev): `StubEmailNotificationAdapter`, `StubUserEmailResolverAdapter`
- Adapters (azure): `SendGridEmailNotificationAdapter`, `KeycloakAdminUserEmailAdapter`

### shared
- Custom exceptions: `InsurTechException`, `ResourceNotFoundException`, `ValidationException`, `ExternalServiceException`, `DuplicateResourceException`
- `GlobalExceptionHandler` — maps exceptions to RFC 7807 error responses; 5xx never exposes internals
- `SecurityConfig`, `JpaAuditingConfig`, `OpenApiConfig`
- `AzureConfig` (@Profile azure) — `DocumentAnalysisClient` + `BlobContainerClient` beans

---

## Spring profiles

| Profile | Adapters active | Use when |
|---|---|---|
| `dev` / `default` | Stubs (OCR, storage, email, policy) | Local development |
| `test` | Same stubs | Integration tests (Testcontainers) |
| `azure` | Azure DI, Blob Storage, SendGrid, Keycloak Admin | Production / staging |

Set via `SPRING_PROFILES_ACTIVE` env var.

---

## Key domain facts

- **Tarmed code format**: `\b(\d{2}\.\d{4})\b` (e.g. `00.0010`, `35.0020`)
- **GLN (Swiss provider ID)**: `\b(\d{13})\b` — extracted from VendorTaxId or VendorAddress; fallback `"0000000000000"`
- **Stub policy**: franchise CHF 300, already paid CHF 200, max coverage CHF 5 000
- **Azure DI model**: `prebuilt-invoice`
- **Blob key pattern**: `{claimId}/{uuid}_{sanitizedFilename}`

---

## Frontend architecture

- **All components are standalone** — no NgModules.
- **Signals everywhere**: `signal()`, `computed()`, `input.required()`. No RxJS Subjects for local state.
- **Smart/dumb pattern**: feature components are smart (inject services), shared components are dumb (inputs/outputs only).
- **Lazy routing**: features loaded with `loadComponent()` in the route config.
- **Auth flow**: `AuthService` wraps keycloak-js; initialized via `APP_INITIALIZER` before the app renders.
- **Keycloak quirk**: this realm issues **opaque access tokens** (not JWTs). `kc.tokenParsed` is `null` and `kc.subject` is `undefined`. The user `sub` is in `kc.idTokenParsed.sub` (ID token). Always resolve `policyHolderId` from there.
- **HTTP interceptor**: `authInterceptor` attaches `Bearer` token to all `/api/` calls via `AuthService.getToken()`.
- **Guards**: `authGuard` (authenticated?), `backofficeGuard` (has `backoffice` role?).

---

## Common commands

### Backend
```bash
# Run locally (needs postgres + kafka + keycloak from docker-compose)
cd backend && mvn spring-boot:run

# Run tests
cd backend && mvn verify

# Compile only (quick check)
cd backend && mvn compile -q
```

### Frontend
```bash
# Dev server (proxies /api/ to localhost:8080)
cd frontend && npm start

# Production build
cd frontend && npm run build

# Install dependencies
cd frontend && npm ci
```

### Docker
```bash
# Full stack (all 5 services)
docker-compose up

# Infrastructure only (for local IDE dev)
docker-compose up postgres kafka keycloak

# Rebuild a single service
docker-compose up --build frontend
docker-compose up --build backend
```

---

## Environment variables

Copy `.env.example` to `.env` and fill in values. Never commit `.env`.

| Variable | Used by | Notes |
|---|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Backend | PostgreSQL connection |
| `KAFKA_BOOTSTRAP_SERVERS` | Backend | `localhost:9092` locally, `kafka:29092` in Docker |
| `JWT_ISSUER_URI` | Backend | Keycloak realm URL |
| `AZURE_DOCUMENT_INTELLIGENCE_ENDPOINT` | Backend (azure) | Form Recognizer endpoint |
| `AZURE_DOCUMENT_INTELLIGENCE_KEY` | Backend (azure) | Form Recognizer API key |
| `AZURE_STORAGE_CONNECTION_STRING` | Backend (azure) | Blob Storage connection string |
| `AZURE_STORAGE_CONTAINER_NAME` | Backend (azure) | Blob container (default: `claims`) |
| `SENDGRID_API_KEY` | Backend (azure) | SendGrid v3 API key |
| `SENDGRID_FROM_EMAIL` | Backend (azure) | Sender address |
| `KEYCLOAK_ADMIN_CLIENT_ID` | Backend (azure) | Service account for user email lookup |
| `KEYCLOAK_ADMIN_CLIENT_SECRET` | Backend (azure) | Service account secret |
| `SPRING_PROFILES_ACTIVE` | Backend | `dev` (local) or `azure` (production) |

---

## Keycloak local setup

- Admin console: http://localhost:8180 (admin / admin)
- Realm: `insurtech` — imported from `keycloak/insurtech-realm.json` on container start
- Frontend client: `insurtech-frontend` (public, PKCE S256)
- Backend validates JWT at: `http://localhost:8180/realms/insurtech`
- Inside Docker network, Keycloak is accessed as `http://host.docker.internal:8180` to keep the `iss` claim consistent between host and container

---

## CI pipeline (.github/workflows/ci.yml)

```
backend  ─┐
           ├─► ci-gate  (branch protection rule: "CI — All checks passed")
frontend ─┘
```

- **backend**: Java 21, `mvn verify` (unit + integration tests with Testcontainers)
- **frontend**: Node 22, `npm ci && npm run build`
- **ci-gate**: `needs: [backend, frontend]`, `if: always()` — aggregates result; required for PR merge

---

## Database migrations (Liquibase)

Files in `backend/src/main/resources/db/changelog/`:
- `V001`: `claims` table
- `V002`: `tarmed_positions` table (FK → claims)
- `V003`: Spring Modulith event publication table

Schema is validated on startup (`ddl-auto: validate`) — never auto-created. Always add a new migration file for schema changes; never modify existing ones.

---

## Azure production adapters

All under `@Profile("azure")`. They replace stubs transparently — no application code changes needed.

| Stub (dev) | Azure adapter | Port |
|---|---|---|
| `StubOcrProviderAdapter` | `AzureDocumentIntelligenceAdapter` | `OcrProviderPort` |
| `LocalDocumentStorageAdapter` | `AzureBlobStorageAdapter` | `DocumentStoragePort` |
| `StubEmailNotificationAdapter` | `SendGridEmailNotificationAdapter` | `NotificationPort` |
| `StubUserEmailResolverAdapter` | `KeycloakAdminUserEmailAdapter` | `UserEmailResolverPort` |
| `StubPolicyContextAdapter` | *(not yet built)* | `PolicyContextPort` |

`KeycloakAdminUserEmailAdapter` caches the admin bearer token in memory (renewed 30 s before expiry) to avoid a Keycloak round-trip per notification.

---

## What is not yet built

- Real `PolicyContextPort` adapter (currently stub only — hardcoded CHF 300 franchise)
- Backoffice UI (reviewer dashboard)
- Prometheus/Grafana observability stack
- SendGrid email templates moved to a template engine (currently inline HTML)
