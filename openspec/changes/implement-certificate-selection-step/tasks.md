## 1. Prerequisites and provider boundary

- [x] 1.1 Verify SPEC-13 session and ID Perú changes are complete, tests pass, and the active session exposes `IDENTITY_VERIFIED` without relying on browser state.
- [x] 1.2 Review the available institutional material for the second certificate-list service and record whether an official URL, authentication scheme, request, response and error contract actually exist.
- [x] 1.3 Define provider-independent listing result and certificate value types plus a `CertificateListingPort` covering list, empty, timeout, unavailable and malformed outcomes.
- [x] 1.4 Implement configuration that selects mock or real listing mode and fails safely when real mode is requested without a verified adapter or required credentials.
- [x] 1.5 Implement the deterministic mock fixtures for empty, one, several, duplicate UUID, invalid UUID, timeout, unavailable and malformed responses, using only fictitious documented data.
- [x] 1.6 If and only if an official contract is available, implement the real adapter with externalized configuration, bounded timeouts, correlation, safe logging and provider contract tests; otherwise document this explicit production dependency without creating a placeholder adapter.

## 2. Backend listing use case and persistence

- [x] 2.1 Add `CHECKING_CERTIFICATE_LIST` to the controlled request states and align allowed transitions, session next-step resolution and OpenAPI enum metadata.
- [x] 2.2 Implement short transactional reservation and stale-reservation recovery for the first authenticated list load without holding a database transaction during provider I/O.
- [x] 2.3 Validate the complete provider collection for required order number, valid creation date, canonical UUID and duplicate order/UUID conditions before writing any row.
- [x] 2.4 Persist a valid non-empty list atomically in the existing `cancellation_request_certificate` entity and transition the request to `CERTIFICATES_AVAILABLE`.
- [x] 2.5 Handle a confirmed empty list with zero certificate rows, `NO_CERTIFICATES_AVAILABLE`, an audit event and no change to the successful identity result.
- [x] 2.6 Map timeout, unavailable, malformed response, duplicate data and unexpected provider failures to controlled retryable errors while restoring the appropriate authenticated pending-list state.
- [x] 2.7 Return the existing persisted certificate snapshot on reload without invoking the second service again.
- [x] 2.8 Inspect the effective V7 schema and create an incremental V8 migration only if a concrete missing index, constraint or Spanish comment is demonstrated; never edit prior migrations.

## 3. Backend selection use case

- [x] 3.1 Define request and response DTOs for a non-empty complete UUID selection with explicit size, format and duplicate validation.
- [x] 3.2 Implement transactional selection replacement that resolves the request from the session, locks or versions its certificate rows, and validates ownership, availability and allowed request state.
- [x] 3.3 Update `selected` and `selected_at` so persisted rows exactly match the submitted UUID set and transition a successful request to `CERTIFICATES_SELECTED`.
- [x] 3.4 Make an identical repeated selection idempotent without duplicating rows or unnecessarily changing timestamps.
- [x] 3.5 Return a controlled conflict for concurrent different selections and guarantee that validation or optimistic-lock failures leave no partially mixed set.
- [x] 3.6 Update protected-flow next-step authorization so only `CERTIFICATES_SELECTED` enables the step-3 transition and direct future-step access remains blocked.

## 4. Protected API and generated contract

- [x] 4.1 Expose `GET /api/v1/cancellation-requests/current/certificates` using only the authenticated flow context and rejecting missing, expired, invalidated or pre-identity sessions before provider access.
- [x] 4.2 Expose idempotent `PUT /api/v1/cancellation-requests/current/certificate-selection` and enforce allowed Origin plus the existing HttpOnly-cookie security boundary.
- [x] 4.3 Document both operations, schemas, cookie security, correlation and implemented 401, 403, 409, 422, 503 and 504 responses in OpenAPI without provider internals or non-existent security.
- [x] 4.4 Regenerate `frontend/openapi/backend-api.json` and `frontend/lib/api/generated.ts`, then make `npm run api:check` pass against the current backend.

## 5. Frontend step 2 experience

- [x] 5.1 Replace `CertificateSelectionTransition` with the real step-2 container inside the shared protected flow and preserve `/autorizacion` locally and `/cancelacion` in the configured non-local flow.
- [x] 5.2 Build the responsive certificate list from `docs/ui-reference/step-2.png` using Tailwind utilities, semantic rows/cards, order number, localized creation date, UUID and availability state.
- [x] 5.3 Add accessible native checkbox controls, select-all behavior, keyboard interaction, visible focus, exact selected count and an `aria-live` announcement.
- [x] 5.4 Require an explicit selection even for a single certificate, disable Continue when none is selected or a submission is active, and prevent double submission.
- [x] 5.5 Call the protected listing and selection APIs through the central typed HTTP client with credentials and correlation, without storing DNI, UUIDs, tokens or selection in URLs or persistent browser storage.
- [x] 5.6 Implement distinct accessible outcomes for empty list, timeout, unavailable, malformed response, conflict and session expiry, with only valid retry, reload or logout actions.
- [x] 5.7 After a confirmed selection, update the shared stepper and render only the controlled step-3 transition without implementing the reason view.
- [x] 5.8 Verify mobile, tablet and desktop layout, including long order values, UUIDs, localized dates, one certificate and several certificates.

## 6. Automated verification and documentation

- [x] 6.1 Add backend unit tests for normalization, fixtures, validation, state transitions, stale recovery, empty list and technical provider outcomes.
- [x] 6.2 Add MySQL Testcontainers tests for zero, one and several certificates, uniqueness, atomic persistence, exact selection replacement, foreign-request injection, idempotency and optimistic concurrency.
- [x] 6.3 Add HTTP security and integration tests proving that listing requires verified identity, arbitrary UUIDs are rejected, later steps stay blocked and cookies/correlation/errors behave as documented.
- [x] 6.4 Add frontend component tests for loading, one/many/empty lists, selection count, select-all, keyboard behavior, validation, double submit, conflicts and controlled failures.
- [x] 6.5 Add a full local mock-flow test from positive home query through ID Perú mock, list load, persisted selection and authorized step-3 transition.
- [x] 6.6 Update backend/frontend READMEs, `docs/context/PROJECT_CONTEXT.md`, technical decisions, data-model documentation and mock fixture table without duplicating or inventing the external contract.
- [x] 6.7 Run backend unit tests, Maven verify with MySQL/Flyway, frontend tests, typecheck, production build, OpenAPI contract check and diff/secret scans; resolve all failures before marking the change complete.
