## ADDED Requirements

### Requirement: Executable single-package frontend project
The repository SHALL contain a private single-package npm project at `/frontend` using Node.js 24 LTS, Next.js 16.2.10, React and React DOM 19.2.7, TypeScript 6.0.2, App Router, and a committed npm lockfile. The project SHALL type-check, build, start, and serve without a database, backend process, or external service.

#### Scenario: Frontend is installed from a clean checkout
- **WHEN** a contributor with Node.js 24 LTS runs `npm ci` in `/frontend`
- **THEN** npm installs the exact locked dependency graph without requiring another package manager

#### Scenario: Frontend production build starts
- **WHEN** a contributor runs the documented type-check, test, build, and start commands
- **THEN** Next.js starts the production build and serves the application successfully without the backend running

### Requirement: Minimal dependency and folder set
The frontend SHALL have only `next`, `react`, and `react-dom` as direct production dependencies and only the TypeScript/types, Tailwind/PostCSS, and Vitest tooling required by this change as direct development dependencies. It MUST NOT add UI kits, icon libraries, HTTP libraries, form libraries, validation libraries, state managers, DOM test environments, E2E tools, or preventive dependencies. Every committed source directory SHALL contain code used by this change.

#### Scenario: Dependency and source tree are reviewed
- **WHEN** `package.json` and the source tree are inspected
- **THEN** every dependency and directory has a current purpose and no empty `features`, `styles`, `public`, future-route, or speculative layer exists

### Requirement: App Router root shell
The frontend SHALL use App Router with a root layout that declares Spanish as the document language and renders a skip link, textual institutional header, main content landmark with a stable target, polite global-message region, adaptive container, and minimal footer. The layout SHALL define base title and description metadata and MUST NOT reproduce the complete reference designs.

#### Scenario: Root shell is statically rendered
- **WHEN** the root layout is rendered around page content
- **THEN** the HTML contains `lang="es"`, semantic header/main/footer landmarks, a skip target, base metadata configuration, and the supplied child content

### Requirement: Temporary non-functional home page
The `/` route SHALL render a temporary Server Component identifying the provisional system, stating that the project is in preparation, showing a technical readiness state and the public environment label. It MUST NOT contain a DNI form, certificate lookup, stepper, identity verification, cancellation reason, confirmation, revocation, receipt, or functional backend request.

#### Scenario: Temporary home page is rendered
- **WHEN** a visitor opens `/`
- **THEN** the page returns a successful response with preparation and technical-status content and no citizen-flow control

### Requirement: Minimal Tailwind and global styling baseline
The frontend SHALL use Tailwind CSS 4.3.2 through `@tailwindcss/postcss` and a single global CSS import. Global styles SHALL provide box sizing, readable provisional colors, coherent spacing, a system sans-serif font stack, an adaptive content width, and a clearly visible `:focus-visible` treatment. The change MUST NOT define a complete design system, extensive tokens, component variants, or definitive institutional colors.

#### Scenario: Tailwind and global styles compile
- **WHEN** the production build processes the global stylesheet and Tailwind utility classes used by the shell
- **THEN** CSS generation succeeds and the rendered base has readable text, layout spacing, and visible keyboard focus

### Requirement: Initial semantic and keyboard accessibility
The base frontend SHALL use Spanish page language, logical headings, native interactive elements, text labels, semantic landmarks, a keyboard-operable skip link, legible contrast, and non-color-only status text. Loading content SHALL be announced through a status role. This change MUST NOT claim completion of a full accessibility audit.

#### Scenario: Keyboard user enters the base page
- **WHEN** a keyboard user focuses the first navigation aid
- **THEN** the skip link becomes visible and can move focus to the main-content target

### Requirement: Safe App Router technical states
The frontend SHALL provide `loading.tsx`, `not-found.tsx`, `error.tsx`, and `global-error.tsx`. Loading SHALL communicate a neutral status; not-found SHALL explain the missing resource and link to `/`; error boundaries SHALL show generic recovery messages and retry actions without rendering exception messages, digests, stacks, configuration, or other internal details.

#### Scenario: Unknown route is requested
- **WHEN** a visitor requests a route that does not exist
- **THEN** Next.js renders the custom not-found content with a link back to the temporary home page

#### Scenario: Unexpected render failure reaches an error boundary
- **WHEN** an unexpected rendering error is caught by a segment or global boundary
- **THEN** the visitor sees a generic Spanish message and retry action without technical error content

#### Scenario: Route is loading
- **WHEN** App Router displays the global loading fallback
- **THEN** a neutral Spanish loading message is exposed with `role="status"`

### Requirement: Explicit server and public environment variables
The frontend SHALL document `BACKEND_URL` as server-only with safe local default `http://localhost:8080` and `NEXT_PUBLIC_APP_ENV` as a non-sensitive public environment label. `.env.example` SHALL contain only these variables, `.env.local` SHALL be ignored, and documentation SHALL state that browser-exposed variables MUST NOT contain secrets. The change MUST NOT define a public backend URL or production secret.

#### Scenario: Local environment is configured
- **WHEN** a contributor copies `.env.example` to `.env.local` without changes
- **THEN** server code resolves the existing backend's local URL and the temporary page displays the `local` public label without exposing a secret

### Requirement: Native JSON HTTP client foundation
The frontend SHALL provide a small fetch-based `requestJson<T>` client and typed `HttpClientError`. The client SHALL resolve relative paths against server-only `BACKEND_URL`, request JSON, preserve caller options and headers, set `credentials: "include"`, and return parsed data with any `X-Correlation-ID`. It SHALL defensively interpret the backend error shape, expose status/code/correlation, and use generic public messages for invalid bodies or network failures. It MUST NOT implement authorization, JWT, refresh, interceptors, automatic retries, session storage, cookies, or real functional requests.

#### Scenario: Successful JSON response contains correlation
- **WHEN** fetch returns an OK JSON response with `X-Correlation-ID`
- **THEN** the client returns the typed data and the same correlation identifier

#### Scenario: Backend returns a structured error
- **WHEN** fetch returns a non-success JSON response using the backend error contract
- **THEN** the client throws `HttpClientError` with the public code, message, status, and correlation identifier

#### Scenario: Network request fails
- **WHEN** native fetch rejects before receiving a response
- **THEN** the client throws `HttpClientError` with code `NETWORK_ERROR` and a generic Spanish message without exposing the native exception text

#### Scenario: Successful response is not valid JSON
- **WHEN** fetch returns success but the body cannot be parsed as JSON
- **THEN** the client throws `HttpClientError` with code `INVALID_RESPONSE` and a generic Spanish message

#### Scenario: Future cookie transport remains possible
- **WHEN** the client invokes native fetch
- **THEN** the request uses `credentials: "include"` without creating session, JWT, cookie, or storage logic

### Requirement: Fast infrastructure-free baseline tests
The frontend SHALL use Vitest 4.1.10 in Node environment and React static rendering to verify the temporary page, root layout, not-found page, and HTTP client success/error behavior. Tests MUST NOT use jsdom, Testing Library, Playwright, a running backend, MySQL, browser automation, or external network services.

#### Scenario: Baseline suite is executed
- **WHEN** a contributor runs `npm test`
- **THEN** all rendering and HTTP client tests pass using only the local Node process

### Requirement: Concise frontend operation documentation
The frontend SHALL include a concise README covering Node.js 24 LTS, dependency installation, development, type-check, tests, production build/start, environment setup, server/public variable distinction, and the future connection to the backend at port 8080. It SHALL state that the current page and styles are temporary and production deployment is deferred.

#### Scenario: New contributor follows frontend documentation
- **WHEN** a contributor follows the README from a clean checkout
- **THEN** the contributor can install, test, build, start, and open the temporary frontend without undocumented infrastructure

### Requirement: Frontend-foundation-only boundary
The change MUST NOT modify UI reference images or implement the DNI page, certificate lookup, stepper, ID Perú, cancellation reasons, review, confirmation, revocation, receipt, JWT, refresh tokens, progress recovery, MySQL persistence, external integrations, complete responsive views, notification system, administrative modules, or production deployment. It MUST NOT add Redux, Zustand, another global store, `localStorage` session data, a full design system, unused routes, or overly generic components.

#### Scenario: Completed change is reviewed for scope
- **WHEN** the implementation diff, route tree, dependencies, and rendered home page are inspected
- **THEN** they contain only the technical frontend foundation and no citizen-flow implementation, modified design reference, or out-of-scope architecture
