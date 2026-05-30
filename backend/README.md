# InsurTech Claims Platform — Backend

Spring Boot 3 / Java 21 backend for automated validation of Swiss medical invoices (Tarmed/Tardoc format) against Krankenkassen policy rules.

---

## Architecture

This service is a **Modular Monolith** built with [Spring Modulith](https://spring.io/projects/spring-modulith). The codebase is structured as independent modules with enforced boundaries — identical to a microservices design, but deployed as a single process to minimise infrastructure cost at the portfolio stage.

```
ch.insurtech.platform
├── claim/          Claim ingestion — document upload, status lifecycle, REST API
├── ocr/            OCR extraction — listens for DocumentUploadedEvent, calls OCR provider
├── rules/          Validation engine — Strategy Pattern, runs all rules against extracted data
├── notification/   Notification dispatch — listens for approval/review events
└── shared/         Cross-cutting concerns — exceptions, error handler, audit, configs
```

### Event flow

```
POST /api/v1/claims  →  ClaimApplicationService
                              │
                    DocumentUploadedEvent (Kafka)
                              │
                    OcrProcessingService  (ocr module)
                              │
                    DataExtractedEvent    (Kafka)
                              │
                    ClaimValidationService (rules module)
                         ┌────┴────┐
               ClaimApprovedEvent   ManualReviewRequiredEvent
                         │                   │
                    NotificationService  NotificationService
```

All inter-module communication is asynchronous via Spring Modulith's event publication log, which guarantees **at-least-once delivery** even if the application crashes between steps.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4 + Spring Modulith 1.3 |
| Persistence | PostgreSQL 16 + Spring Data JPA |
| Migrations | Liquibase |
| Messaging | Apache Kafka (Upstash for cloud) |
| Security | OAuth2 Resource Server (JWT) |
| API Docs | SpringDoc OpenAPI / Swagger UI |
| Build | Maven 3.9 |
| Tests | JUnit 5 + Mockito + AssertJ + TestContainers |

---

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose (for local infrastructure)

---

## Local Development

### 1. Start infrastructure

```bash
docker-compose up -d
```

This starts PostgreSQL and Kafka locally.

### 2. Configure environment

```bash
cp .env.example .env
# Edit .env with your local values (defaults work with docker-compose)
```

### 3. Run the application

```bash
# Windows
.\mvnw.cmd spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`.  
Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## Running Tests

### Unit + integration tests with coverage report

```bash
# Windows
.\mvnw.cmd verify

# macOS / Linux
./mvnw verify
```

- **Unit tests** (`*Test.java`) run in the `test` phase via Surefire
- **Integration tests** (`*IT.java`) run in the `integration-test` phase via Failsafe, using TestContainers (requires Docker)
- **JaCoCo** generates a coverage report at `target/site/jacoco/index.html` and enforces ≥ 70% line coverage
- **Spring Modulith** modularity tests verify that no module violates declared boundaries

### Unit tests only (no Docker required)

```bash
# Windows
.\mvnw.cmd test

# macOS / Linux
./mvnw test
```

---

## Module Boundaries

Spring Modulith enforces that modules only communicate through their public API surface (classes at package root) or via domain events. Internal classes (e.g., `ClaimJpaRepository`, `ClaimRepositoryAdapter`) are package-private and inaccessible to other modules.

Run `ModularityTests` to generate PlantUML architecture diagrams under `target/modulith-docs/`.

---

## Key Design Decisions

### Hexagonal Architecture (Ports & Adapters)
Each module defines `domain/port` interfaces. Infrastructure adapters implement them and are annotated with `@Profile` so they can be swapped without touching domain code.

| Port | Dev/Test adapter | Production adapter |
|---|---|---|
| `DocumentStoragePort` | `LocalDocumentStorageAdapter` | `AzureBlobStorageAdapter` (to implement) |
| `OcrProviderPort` | `StubOcrProviderAdapter` | `AzureDocumentIntelligenceAdapter` (to implement) |
| `PolicyContextPort` | `StubPolicyContextAdapter` | `PolicyManagementServiceAdapter` (to implement) |
| `NotificationPort` | `StubEmailNotificationAdapter` | `SendGridNotificationAdapter` (to implement) |

### Strategy Pattern — Rules Engine
`ClaimValidationService` iterates over all `List<ValidationStrategy>` beans injected by Spring. Adding a new business rule requires only creating a new `@Component` that implements `ValidationStrategy` — no other class changes.

Current strategies:
- `DeductibleValidationStrategy` — checks remaining franchise (Selbstbehalt)
- `CoverageAmountValidationStrategy` — checks max policy coverage
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
`GlobalExceptionHandler` maps each type to the correct HTTP status and never leaks internal details to the client.

---

## API Reference

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `POST` | `/api/v1/claims` | Submit medical invoice (multipart/form-data) | Bearer JWT |
| `GET` | `/api/v1/claims/{claimId}` | Get claim status | Bearer JWT |
| `GET` | `/api/v1/claims?policyHolderId=` | List claims for a policy holder | Bearer JWT |
| `GET` | `/api/v1/claims/status/{status}` | List by status (backoffice) | JWT + `SCOPE_backoffice` |

Full interactive docs: `http://localhost:8080/swagger-ui.html`

---

## Compliance Notes

- All endpoints require a valid JWT issued by the configured OAuth2 provider
- No sensitive claim data is included in error responses (`GlobalExceptionHandler` sanitises all 5xx messages)
- Audit columns (`created_at`, `updated_at`, `created_by`, `updated_by`) are automatically populated via Spring Data JPA Auditing
- Production deployment must be in a Swiss data centre (AWS `eu-central-2` Zurich or Azure `switzerlandnorth`) to comply with FADP/nDSG

---

## Roadmap

- [ ] `AzureDocumentIntelligenceAdapter` — real Tarmed/Tardoc OCR
- [ ] `AzureBlobStorageAdapter` — secure document storage (Swiss region)
- [ ] `PolicyManagementServiceAdapter` — real policy lookup
- [ ] `SendGridNotificationAdapter` — transactional email
- [ ] SonarQube quality gate in CI
- [ ] Snyk dependency vulnerability scanning in CI
- [ ] Helm chart for Kubernetes deployment (AKS / Swiss region)
