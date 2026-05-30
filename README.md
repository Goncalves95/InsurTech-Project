# InsurTech Claims Platform

[![CI](https://github.com/Goncalves95/InsurTech-Project/actions/workflows/ci.yml/badge.svg)](https://github.com/Goncalves95/InsurTech-Project/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-21-DD0031?logo=angular&logoColor=white)](https://angular.dev)
[![License](https://img.shields.io/badge/License-Proprietary-red)](./LICENSE)

> Enterprise-grade Swiss Krankenkassen medical invoice processing platform.  
> Automates the validation of Tarmed/Tardoc invoices against policy rules, from
> document ingestion through OCR extraction to reimbursement decision — with a
> full audit trail and event-driven processing pipeline.

---

## Table of Contents

- [Overview](#overview)
- [System Architecture](#system-architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Running Tests](#running-tests)
- [API Reference](#api-reference)
- [Security & Compliance](#security--compliance)
- [Design Patterns](#design-patterns)
- [Roadmap](#roadmap)
- [License](#license)
- [Author](#author)

---

## Overview

Swiss Krankenkassen (health insurers) process hundreds of thousands of medical
invoices per year. Manual validation is error-prone, slow, and expensive. This
platform demonstrates how an enterprise engineering team would approach automating
that pipeline:

| Capability | Description |
|---|---|
| **Invoice ingestion** | Customers submit PDF/image invoices via a secure portal |
| **OCR extraction** | Document Intelligence extracts amounts, Tarmed codes, provider details |
| **Rules engine** | Strategy-based validator checks franchise, coverage limits and Tarmed codes |
| **Event-driven flow** | Kafka decouples each processing stage; at-least-once delivery guaranteed |
| **Backoffice dashboard** | Claims flagged for manual review appear in a role-gated queue |
| **Audit trail** | Every state transition is timestamped and user-stamped via JPA Auditing |
| **Security** | OAuth2/OIDC with Keycloak, PKCE for SPAs, role-scoped endpoints |

---

## System Architecture

### Component Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Client Layer                                │
│                                                                     │
│   ┌──────────────────────┐        ┌────────────────────────────┐   │
│   │   Angular 21 SPA     │        │     Keycloak 26 (OIDC)     │   │
│   │   Material 3 UI      │◄──────►│  Realm: insurtech          │   │
│   │   PKCE / silent SSO  │        │  Roles: customer, backoffice│  │
│   └──────────┬───────────┘        └────────────────────────────┘   │
└──────────────┼──────────────────────────────────────────────────────┘
               │ Bearer JWT
               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         Backend (Spring Boot 3.4)                   │
│                                                                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────────────────┐ │
│  │  claim   │  │   ocr    │  │  rules   │  │   notification     │ │
│  │ (API +   │  │(listener │  │(strategy │  │  (listener +       │ │
│  │ lifecycle│  │+ adapter)│  │ engine)  │  │   adapter)         │ │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └──────────┬─────────┘ │
│       │             │              │                   │            │
│  └────────────────────────────────────────────────────┘            │
│                    shared (exceptions, audit, config)               │
└──────────┬──────────────────────────┬──────────────────────────────┘
           │                          │
     ┌─────▼──────┐          ┌────────▼────────┐
     │PostgreSQL 16│          │  Apache Kafka   │
     │ + Liquibase │          │  (KRaft mode)   │
     └─────────────┘          └─────────────────┘
```

### Claim Processing Pipeline

```
POST /api/v1/claims
        │
        ▼
ClaimApplicationService ──► status: PENDING_OCR
        │
        └──► DocumentUploadedEvent (Kafka)
                        │
                        ▼
               OcrProcessingService ──► status: OCR_PROCESSING
                        │
                        └──► DataExtractedEvent (Kafka)
                                        │
                                        ▼
                             ClaimValidationService ──► runs all ValidationStrategy beans
                                        │
                          ┌─────────────┴─────────────┐
                          │                           │
                   APPROVED ◄──────────     ──────────► MANUAL_REVIEW
                          │                           │
               ClaimApprovedEvent          ManualReviewRequiredEvent
                          │                           │
                          └──────────┬────────────────┘
                                     ▼
                            NotificationService
                         (email dispatch + audit log)
```

Spring Modulith's **event publication log** persists every event before dispatch.
If the process restarts mid-pipeline, incomplete events are replayed automatically —
no claim is silently dropped.

---

## Tech Stack

### Backend

| Layer | Technology | Version |
|---|---|---|
| Language | Java | 21 |
| Framework | Spring Boot | 3.4 |
| Architecture | Spring Modulith (modular monolith) | 1.3 |
| Persistence | PostgreSQL + Spring Data JPA | 16 |
| Migrations | Liquibase | 4.30 |
| Messaging | Apache Kafka (KRaft, no ZooKeeper) | 3.7 |
| Security | Spring Security OAuth2 Resource Server | — |
| Identity Provider | Keycloak | 26 |
| API Docs | SpringDoc OpenAPI / Swagger UI | 2.7 |
| Build | Maven | 3.9 |
| Tests | JUnit 5 + Mockito + AssertJ + TestContainers | — |
| Coverage | JaCoCo (≥ 70% line coverage enforced) | — |

### Frontend

| Layer | Technology | Version |
|---|---|---|
| Framework | Angular (standalone, signals-based) | 21.2 |
| UI Library | Angular Material (M3, azure-blue theme) | 21.2 |
| Auth | keycloak-js (PKCE S256, silent SSO) | 26.2 |
| HTTP | Angular HttpClient + functional interceptor | — |
| Reactive state | Angular Signals (`signal`, `computed`) | — |
| Build tool | esbuild (`@angular/build:application`) | — |
| Test runner | Vitest | 4 |
| Language | TypeScript | 5.9 |

### Infrastructure & DevOps

| Component | Technology |
|---|---|
| Containerisation | Docker + Docker Compose |
| CI/CD | GitHub Actions |
| Code style | Prettier + EditorConfig |

---

## Project Structure

```
InsurTech-Project/
├── .github/
│   └── workflows/
│       └── ci.yml              # Parallel backend + frontend CI jobs
│
├── backend/                    # Spring Boot modular monolith
│   ├── src/main/java/ch/insurtech/platform/
│   │   ├── claim/              # Claim ingestion, lifecycle, REST API
│   │   ├── ocr/                # OCR extraction (stub → Azure Doc Intelligence)
│   │   ├── rules/              # Strategy-based validation engine
│   │   ├── notification/       # Event-driven email dispatch
│   │   └── shared/             # Exceptions, GlobalExceptionHandler, audit, config
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/changelog/       # Liquibase migrations
│   ├── src/test/               # Unit tests (*Test.java) + ITs (*IT.java)
│   ├── Dockerfile              # Multi-stage, non-root, JVM container flags
│   └── pom.xml
│
├── frontend/                   # Angular 21 SPA
│   ├── src/app/
│   │   ├── core/
│   │   │   ├── auth/           # AuthService (signals), guards, JWT interceptor
│   │   │   └── api/            # ClaimsApiService, Claim model
│   │   ├── shared/
│   │   │   └── status-chip/    # Colour-coded claim status component
│   │   └── features/
│   │       ├── portal/         # Customer: my-claims + submit-claim
│   │       └── backoffice/     # Backoffice: review-queue (role-gated)
│   ├── src/environments/       # Dev / prod environment config
│   ├── public/
│   │   └── silent-check-sso.html
│   └── proxy.conf.json         # Dev proxy: /api → localhost:8080
│
├── keycloak/
│   └── insurtech-realm.json    # Realm export: users, client, roles, scopes
│
├── docker-compose.yml          # PostgreSQL + Kafka + Keycloak + Backend
├── .env.example                # Required environment variables (no secrets)
├── LICENSE
└── README.md
```

---

## Getting Started

### Prerequisites

| Tool | Minimum version |
|---|---|
| Docker + Docker Compose | 24+ |
| Java JDK | 21 |
| Node.js | 22 LTS |
| npm | 10+ |

### 1 — Clone and configure

```bash
git clone https://github.com/Goncalves95/InsurTech-Project.git
cd InsurTech-Project

cp .env.example .env
# Default values in .env work with docker-compose out of the box
```

### 2 — Start infrastructure

```bash
docker-compose up postgres kafka keycloak -d
```

Wait ~15 seconds for Keycloak to finish its first-boot import of `insurtech-realm.json`.

### 3 — Start the backend

```bash
cd backend

# Windows
.\mvnw.cmd spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

API available at: `http://localhost:8080`  
Swagger UI: `http://localhost:8080/swagger-ui.html`

### 4 — Start the frontend

```bash
cd frontend
npm install
npm start          # ng serve with proxy → localhost:8080
```

Application available at: `http://localhost:4200`

### 5 — Test accounts

| Username | Password | Role |
|---|---|---|
| `customer` | `customer123` | Customer portal |
| `backoffice` | `backoffice123` | Backoffice + customer portal |

---

## Running Tests

### Backend — full verification (unit + integration + coverage)

```bash
cd backend

# Windows
.\mvnw.cmd verify

# macOS / Linux
./mvnw verify
```

- **Unit tests** (`*Test.java`) — Surefire, no Docker required
- **Integration tests** (`*IT.java`) — Failsafe, TestContainers spins up real PostgreSQL + Kafka
- **Modularity tests** — Spring Modulith verifies no module violates declared boundaries
- **JaCoCo** — enforces ≥ 70% line coverage; report at `target/site/jacoco/index.html`

### Backend — unit tests only (no Docker)

```bash
./mvnw test
```

### Frontend — production build check

```bash
cd frontend
npm run build
```

---

## API Reference

All endpoints require a valid Bearer JWT issued by Keycloak.  
Interactive docs: `http://localhost:8080/swagger-ui.html`

| Method | Endpoint | Description | Required scope |
|---|---|---|---|
| `POST` | `/api/v1/claims` | Submit invoice (`multipart/form-data`: `policyHolderId` + `document`) | any authenticated |
| `GET` | `/api/v1/claims/{claimId}` | Get claim by ID | any authenticated |
| `GET` | `/api/v1/claims?policyHolderId=` | List all claims for a policy holder | any authenticated |
| `GET` | `/api/v1/claims/status/{status}` | List claims by status | `SCOPE_backoffice` |

### Claim lifecycle states

```
PENDING_OCR → OCR_PROCESSING → PENDING_VALIDATION → APPROVED
                                                   ↘ REJECTED
                                                   ↘ MANUAL_REVIEW
```

---

## Security & Compliance

### Authentication & Authorisation

- All endpoints are protected by Spring Security OAuth2 Resource Server
- JWTs are validated against Keycloak's JWKS endpoint on every request
- Role-based access: `SCOPE_backoffice` required for the review queue endpoint
- Angular SPA uses **PKCE with S256** code challenge — immune to authorisation code interception
- **Silent SSO** via hidden iframe (`silent-check-sso.html`) — no visible redirects on page load
- Tokens are refreshed automatically 30 seconds before expiry (JWT interceptor)

### Data protection

- Error responses never expose internal stack traces or system details (`GlobalExceptionHandler`)
- Every record carries `created_at`, `updated_at`, `created_by`, `updated_by` audit columns
- Secrets are loaded from environment variables — never hardcoded, never committed
- Production deployment must reside in a Swiss data centre (`eu-central-2` or `switzerlandnorth`) to comply with **FADP / nDSG** (Swiss Federal Act on Data Protection)

---

## Design Patterns

### Hexagonal Architecture (Ports & Adapters)

Each module exposes a `domain/port` interface. Infrastructure adapters implement it
and are selected via Spring `@Profile`. Swapping from stub to production adapter
requires zero changes to domain code.

| Port | Dev adapter (current) | Production adapter (roadmap) |
|---|---|---|
| `DocumentStoragePort` | `LocalDocumentStorageAdapter` | `AzureBlobStorageAdapter` |
| `OcrProviderPort` | `StubOcrProviderAdapter` | `AzureDocumentIntelligenceAdapter` |
| `PolicyContextPort` | `StubPolicyContextAdapter` | `PolicyManagementServiceAdapter` |
| `NotificationPort` | `StubEmailNotificationAdapter` | `SendGridNotificationAdapter` |

### Strategy Pattern — Rules Engine

`ClaimValidationService` iterates over all `List<ValidationStrategy>` beans injected
by Spring. Adding a new business rule requires only a new `@Component` — no other
class changes.

Active strategies:
- `DeductibleValidationStrategy` — checks remaining franchise (Selbstbehalt)
- `CoverageAmountValidationStrategy` — checks maximum policy coverage
- `TarmedCodeValidationStrategy` — validates Tarmed code format (XX.XXXX)

### Custom Exception Hierarchy

```
InsurTechException (abstract)
├── ResourceNotFoundException
│   └── ClaimNotFoundException
├── ValidationException
├── DuplicateResourceException
└── ExternalServiceException
```

### Modular Monolith (Spring Modulith)

Modules communicate exclusively via:
1. **Public API classes** at the module root package
2. **Domain events** published to Spring's ApplicationEventPublisher / Kafka

Internal classes (`*JpaRepository`, `*RepositoryAdapter`, etc.) are package-private
and inaccessible to other modules. `ModularityTests` enforces this at every CI run
and generates PlantUML architecture diagrams to `target/modulith-docs/`.

---

## Roadmap

- [ ] `AzureDocumentIntelligenceAdapter` — real Tarmed/Tardoc OCR
- [ ] `AzureBlobStorageAdapter` — secure document storage (Swiss region)
- [ ] `PolicyManagementServiceAdapter` — live policy lookup
- [ ] `SendGridNotificationAdapter` — transactional email
- [ ] Add Angular frontend container (nginx) to Docker Compose
- [ ] SonarQube quality gate in CI
- [ ] Snyk dependency vulnerability scanning in CI
- [ ] Helm chart for Kubernetes deployment (AKS / Swiss region)

---

## License

This software is proprietary and all rights are reserved.  
See [LICENSE](./LICENSE) for full terms.

**In summary:** viewing the source code for personal educational reference is
permitted. Any reproduction, distribution, commercial use, or derivative work
requires the author's prior written consent.

For permissions and licensing inquiries: **create@raigonlab.com**

---

## Author

**Fernando Goncalves**  
Full-stack Software Engineer — Java / Spring Boot / Angular  
Portfolio project targeting Swiss Krankenkassen engineering roles  

[![GitHub](https://img.shields.io/badge/GitHub-Goncalves95-181717?logo=github)](https://github.com/Goncalves95)
