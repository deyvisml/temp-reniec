# Frontend Foundation Specification

## Purpose

Define the minimal, executable, maintainable, accessible, and non-functional technical foundation for the system frontend.

## Requirements

### Requirement: Executable single-package frontend project
The repository SHALL contain a private single-package npm project at `/frontend` using Node.js 24 LTS, Next.js 16.2.10, React and React DOM 19.2.7, TypeScript 6.0.2, App Router, and a committed npm lockfile. The project SHALL type-check, build, start, and serve without a database, backend process, or external service.

#### Scenario: Frontend is installed from a clean checkout
- **WHEN** a contributor with Node.js 24 LTS runs `npm ci` in `/frontend`
- **THEN** npm installs the exact locked dependency graph without requiring another package manager

#### Scenario: Frontend production build starts
- **WHEN** a contributor runs the documented type-check, test, build, and start commands
- **THEN** Next.js starts the production build and serves the application successfully without the backend running

### Requirement: Minimal dependency and folder set
The frontend SHALL retain only `next`, `react`, and `react-dom` as direct production dependencies and SHALL have only the TypeScript/types, Tailwind/PostCSS, Vitest, and `openapi-typescript` development tooling required by the implemented foundation and API contract synchronization. It MUST NOT add UI kits, icon libraries, HTTP libraries, form libraries, runtime validation libraries, state managers, DOM test environments, E2E tools, SDK generators, or preventive dependencies. Every committed source directory and contract artifact SHALL have a current purpose.

#### Scenario: Dependency and source tree are reviewed
- **WHEN** `package.json` and the source tree are inspected
- **THEN** every dependency and directory supports the current shell, technical integration, OpenAPI type synchronization, or tests and no empty feature, future-route, or speculative layer exists

### Requirement: App Router root shell
The frontend SHALL use App Router with a root layout that declares Spanish as the document language and renders a skip link, textual institutional header, main content landmark with a stable target, polite global-message region, adaptive container, and minimal footer. The layout SHALL define base title and description metadata and MUST NOT reproduce the complete reference designs.

#### Scenario: Root shell is statically rendered
- **WHEN** the root layout is rendered around page content
- **THEN** the HTML contains `lang="es"`, semantic header/main/footer landmarks, a skip target, base metadata configuration, and the supplied child content

### Requirement: Functional citizen home page
The `/` route SHALL render the real citizen-facing home and DNI eligibility form specified by `citizen-eligibility-entry`. It SHALL preserve the root App Router shell, semantic landmarks, Spanish language metadata, global error boundaries, keyboard access, responsive layout, and safe behavior when the backend is unavailable. It MUST NOT include identity verification, cancellation reason, confirmation, revocation, receipt, JWT session behavior, or later citizen-flow controls.

#### Scenario: Citizen home is rendered
- **WHEN** a visitor opens `/`
- **THEN** the page renders the service explanation and accessible DNI form within the existing root shell, with no temporary project-preparation content

#### Scenario: Backend is unavailable
- **WHEN** the visitor opens or uses the home page while the backend cannot be reached
- **THEN** the page remains renderable and usable and reports communication failure only after a submitted consultation

### Requirement: Minimal Tailwind and global styling baseline
The frontend SHALL use Tailwind CSS 4.3.2 through `@tailwindcss/postcss` and one global CSS import. Layout, spacing, typography, colors, responsive behavior, interactive states, and component presentation SHALL be expressed primarily through Tailwind utility classes colocated with the TSX that owns the markup. `app/globals.css` SHALL be limited to the Tailwind import, a small justified theme configuration, and rules that are genuinely global and not reasonably represented by utilities. It MUST NOT contain component-structure selectors, use `@apply` to recreate component styles, introduce another global stylesheet, or rely on inline visual styles. Any custom CSS exception MUST be scoped, documented, and justified by a current need. The frontend MUST NOT add a design system, broad token catalog, class-composition dependency, CSS framework, or UI kit as part of this baseline.

#### Scenario: Tailwind and global styles compile
- **WHEN** the production build processes the global stylesheet and Tailwind utility classes used by the shell
- **THEN** CSS generation succeeds and the rendered application retains readable text, coherent spacing, responsive layout, and visible keyboard focus

#### Scenario: Contributor inspects component presentation
- **WHEN** a contributor reviews an implemented frontend component
- **THEN** its visual composition and responsive variants are visible through Tailwind utilities in that component rather than hidden in global component selectors or inline styles

#### Scenario: Global stylesheet is reviewed
- **WHEN** `app/globals.css` is inspected after the migration
- **THEN** it contains only the Tailwind import and justified global theme or base concerns, with no selectors coupled to page, form, header, footer, result, or benefit markup

#### Scenario: Tailwind cannot reasonably express a required style
- **WHEN** a current visual requirement needs custom CSS that is not reasonably represented by Tailwind utilities
- **THEN** the implementation adds the smallest scoped and documented exception without establishing a second styling strategy

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
The frontend SHALL document `BACKEND_URL` as the server-only backend base URL, `NEXT_PUBLIC_BACKEND_URL` as the non-sensitive browser-visible backend base URL, and `NEXT_PUBLIC_APP_ENV` as a non-sensitive public environment label, with safe local values `http://localhost:8080`, `http://localhost:8080`, and `local`. `.env.example` SHALL contain only these variables, `.env.local` SHALL be ignored, and documentation SHALL state that browser-exposed variables are embedded at build time and MUST NOT contain secrets.

#### Scenario: Local environment is configured
- **WHEN** a contributor copies `.env.example` to `.env.local` without changes
- **THEN** server code and browser code resolve the local backend URL, the page displays the `local` label, and no secret is exposed

#### Scenario: Browser bundle is reviewed
- **WHEN** frontend browser configuration is inspected
- **THEN** it contains only the public backend address and environment label and no credential, token, database value, or server-only secret

### Requirement: Native JSON HTTP client foundation
The frontend SHALL provide one small fetch-based `requestJson<T>` client and typed `HttpClientError`. The client SHALL resolve relative API paths against server-only `BACKEND_URL` on the server and `NEXT_PUBLIC_BACKEND_URL` in the browser; preserve caller options and headers; request JSON; use `credentials: "include"`; generate and send a valid `X-Correlation-ID` unless the caller supplies one; return parsed or empty success data with the response correlation; enforce an 8-second default timeout; and respect caller cancellation. It SHALL defensively interpret the generated backend error shape and expose public status, code, message, and correlation for HTTP, network, timeout, aborted, and invalid-response failures. It MUST NOT implement authorization, JWT, refresh, interceptors, automatic retries, session storage, body logging, cookie management, or a third-party HTTP library.

#### Scenario: Successful JSON response contains correlation
- **WHEN** fetch returns an OK JSON response with `X-Correlation-ID`
- **THEN** the client returns the typed data and the same correlation identifier

#### Scenario: Missing caller correlation is generated and sent
- **WHEN** the caller does not provide `X-Correlation-ID`
- **THEN** the client sends a valid generated identifier and accepts the backend-selected identifier in the response

#### Scenario: Backend returns a structured error
- **WHEN** fetch returns a non-success JSON response using the backend error contract
- **THEN** the client throws `HttpClientError` with the public code, message, status, and correlation identifier

#### Scenario: Network request fails
- **WHEN** native fetch rejects before receiving a response for a reason other than timeout or caller cancellation
- **THEN** the client throws `HttpClientError` with code `NETWORK_ERROR` and a generic Spanish message without exposing the native exception text

#### Scenario: Request reaches timeout
- **WHEN** the request does not complete within eight seconds
- **THEN** the client aborts it and throws `HttpClientError` with code `TIMEOUT` and a generic Spanish message

#### Scenario: Caller cancels request
- **WHEN** the caller-provided abort signal triggers before completion
- **THEN** the client throws `HttpClientError` with code `REQUEST_ABORTED` and does not misclassify it as timeout

#### Scenario: Successful response is not valid JSON
- **WHEN** fetch declares or requires JSON success but the body cannot be parsed as JSON
- **THEN** the client throws `HttpClientError` with code `INVALID_RESPONSE` and a generic Spanish message

#### Scenario: Successful response is empty
- **WHEN** a future API returns a successful response with no content
- **THEN** the client completes without attempting to parse an absent JSON body

#### Scenario: Future cookie transport remains possible
- **WHEN** the client invokes native fetch
- **THEN** the request uses `credentials: "include"` without creating session, JWT, cookie, or storage logic

### Requirement: Fast infrastructure-free baseline tests
The frontend SHALL retain Vitest in Node environment and React static rendering for its default `npm test` suite. Baseline tests SHALL verify the temporary page and integration-indicator states, root layout, not-found page, generated-contract aliases, and HTTP client success, correlation, timeout, cancellation, invalid-response, and error behavior using controlled fetch doubles. They MUST NOT require jsdom, Testing Library, Playwright, a running backend, MySQL, browser automation, or external network services. Real backend communication SHALL run only through the separately documented integration command.

#### Scenario: Baseline suite is executed
- **WHEN** a contributor runs `npm test`
- **THEN** all rendering, contract, indicator, and HTTP client unit tests pass using only the local Node process

#### Scenario: Real integration suite is selected
- **WHEN** a contributor runs the separate integration-test command
- **THEN** only the explicitly marked live tests use the configured running backend and they are not silently included in `npm test`

### Requirement: Concise frontend operation documentation
The frontend SHALL include a concise README covering Node.js 24 LTS, dependency installation, development, type-check, unit tests, real integration tests, production build/start, server/public environment setup, backend and MySQL prerequisites, CORS origin, versioned API base, OpenAPI location, contract synchronization/check commands, generated-file policy, and the temporary integration indicator. It SHALL state that the current page and styles remain temporary, public variables contain no secrets, and production deployment is deferred.

#### Scenario: New contributor follows frontend documentation without backend
- **WHEN** a contributor follows the default frontend instructions from a clean checkout without starting backend or MySQL
- **THEN** the contributor can install, unit-test, build, start, and open the frontend with a safe unavailable technical state

#### Scenario: New contributor follows full-stack documentation
- **WHEN** a contributor follows the documented local integration sequence
- **THEN** the contributor can start MySQL and backend, synchronize and check OpenAPI types, run the real integration suite, start the frontend, and observe backend and database availability without undocumented configuration

### Requirement: Frontend-foundation-only boundary
The change MUST NOT modify UI reference images or implement the DNI page, certificate lookup, stepper, ID Perú, cancellation reasons, review, confirmation, revocation, receipt, JWT, refresh tokens, progress recovery, MySQL persistence, external integrations, complete responsive views, notification system, administrative modules, or production deployment. It MUST NOT add Redux, Zustand, another global store, `localStorage` session data, a full design system, unused routes, or overly generic components.

#### Scenario: Completed change is reviewed for scope
- **WHEN** the implementation diff, route tree, dependencies, and rendered home page are inspected
- **THEN** they contain only the technical frontend foundation and no citizen-flow implementation, modified design reference, or out-of-scope architecture
