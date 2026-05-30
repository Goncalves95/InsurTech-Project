# InsurTech Platform — Frontend

Angular 21 SPA for the Swiss Krankenkassen claims portal.  
Part of the [InsurTech Claims Platform](../README.md).

---

## Stack

| | |
|---|---|
| Framework | Angular 21 (standalone components, no NgModules) |
| UI | Angular Material 21 — M3 azure-blue theme |
| State | Angular Signals (`signal`, `computed`, `input.required`) |
| Auth | keycloak-js 26.2 — PKCE S256, ID token subject resolution |
| HTTP | `HttpClient` + functional `authInterceptor` |
| Unit tests | Vitest 4 + Angular Testing Library 19 |
| E2E tests | Playwright 1.60 |
| Production server | nginx 1.27 (Docker multi-stage build) |

---

## Development

### Prerequisites

- Node.js 22 LTS
- Backend + Keycloak running (see [Getting Started](../README.md#getting-started))

### Install and run

```bash
npm install
npm start          # ng serve on http://localhost:4200
                   # /api/* proxied to http://localhost:8080
```

The dev server requires the backend and Keycloak to be up:

```bash
# from the project root
docker-compose up postgres kafka keycloak -d
```

### Build

```bash
npm run build      # production bundle → dist/frontend/browser/
```

---

## Testing

### Unit tests

```bash
npm test                  # run once, no coverage
npm run test:coverage     # run with v8 coverage report
```

Coverage output: `coverage/` (HTML report at `coverage/index.html`)

| File | Covers |
|---|---|
| `app.spec.ts` | App shell, toolbar, authenticated vs unauthenticated state |
| `auth.service.spec.ts` | Keycloak mock, signal state, `isBackoffice` derived signal |
| `status-chip.spec.ts` | CSS modifier classes and label formatting for all statuses |
| `my-claims.spec.ts` | Loading spinner, error state, empty state, claims table render |

Tests follow the **Angular Testing Library** philosophy: components are tested through
the DOM as a user would interact with them, not by inspecting internal class state.

### E2E tests (requires full Docker stack)

```bash
npm run e2e        # headless Chromium
npm run e2e:ui     # Playwright interactive UI
```

Authenticated portal tests require:

```bash
export E2E_USERNAME=customer
export E2E_PASSWORD=customer123
npm run e2e
```

---

## Architecture

### Folder structure

```
src/app/
├── core/
│   ├── auth/
│   │   ├── auth.service.ts       # Keycloak wrapper, signals, policyHolderId
│   │   ├── auth.guard.ts         # Functional guard: authenticated?
│   │   └── backoffice.guard.ts   # Functional guard: backoffice role?
│   └── api/
│       ├── claims-api.service.ts # HTTP calls to /api/v1/claims
│       └── models/claim.model.ts # Claim interface + ClaimStatus type
├── shared/
│   └── status-chip/              # Dumb component: coloured status badge
└── features/
    ├── portal/                   # Customer-facing views
    │   ├── my-claims/            # Claim list (smart component)
    │   └── submit-claim/         # Invoice upload form (smart component)
    └── backoffice/               # Role-gated reviewer views
        └── review-queue/         # Manual review queue
```

### Key conventions

- **Standalone components only** — no `NgModule`.
- **Signals for all local state** — no `BehaviorSubject`, no `async` pipe.
- **Smart / dumb split** — feature components inject services; shared components
  receive data through `input.required()` only.
- **Lazy routing** — every feature loaded via `loadComponent()`.
- **Keycloak quirk** — this realm issues opaque access tokens (not JWTs).
  `kc.tokenParsed` is `null`; the user subject comes from `idTokenParsed.sub`.

---

## Docker

The frontend ships as a multi-stage Docker image:

```
Stage 1 (node:22-alpine)  →  npm ci && npm run build
Stage 2 (nginx:1.27-alpine) →  copy dist/frontend/browser + nginx.conf
```

`nginx.conf` handles:
- SPA routing (`try_files $uri /index.html`)
- `/api/` reverse proxy → `http://backend:8080`
- `/actuator/` reverse proxy → `http://backend:8080`
- gzip compression for JS/CSS/JSON/SVG

Run the full stack from the project root:

```bash
docker-compose up frontend
# served at http://localhost:4200
```
