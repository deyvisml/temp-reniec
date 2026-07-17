## 1. Persistence evolution

- [x] 1.1 Add a forward Flyway migration that introduces `public_reference`, backfills existing requests, enforces `NOT NULL` and uniqueness, and preserves all current data.
- [x] 1.2 Update `CertificateCancellationRequestEntity` to generate and expose the UUID public reference while keeping the numeric identifier internal.
- [x] 1.3 Add `ERROR` to the controlled eligibility result enums without introducing a status catalog or new table.
- [x] 1.4 Add repository queries for public-reference lookup and active-request lookup with the MySQL locking semantics required by create-or-recover.
- [x] 1.5 Add repository support for locking a request, finding the latest attempt, and identifying an obsolete submitted attempt.

## 2. Backend eligibility model and mock

- [x] 2.1 Create the feature-oriented cancellation initiation package with request/response models, normalized outcome, next-step value, and the central eight-ASCII-digit DNI rule.
- [x] 2.2 Define `CertificateEligibilityGateway` and its provider-independent result without certificate-level fields or external payloads.
- [x] 2.3 Implement the local/test deterministic mock for `00000001` through `00000006` and the documented default result.
- [x] 2.4 Externalize request expiration, stale-attempt threshold, and mock timeout duration with safe local/test defaults and no secrets.
- [x] 2.5 Document the fictitious mock matrix and make the profile activation explicit so it cannot be mistaken for a production integration.

## 3. Transactional initiation use case

- [x] 3.1 Implement the first short transaction that expires stale requests, serializes by DNI, recovers eligible requests, rejects in-progress work, or creates a new request.
- [x] 3.2 Allocate and persist the next eligibility attempt, set `SUBMITTED` and `CHECKING_ELIGIBILITY`, and carry the current correlation identifier without logging the DNI.
- [x] 3.3 Invoke the gateway outside database transactions with bounded timeout and controlled exception mapping.
- [x] 3.4 Implement the finalization transaction for `ELIGIBLE`, `NOT_ELIGIBLE`, `INCONCLUSIVE`, `UNAVAILABLE`, and `ERROR` so attempt and request state remain consistent.
- [x] 3.5 Detect and close stale submitted attempts before a safe retry, preserving attempt history and monotonic numbering.
- [x] 3.6 Translate pessimistic/optimistic concurrency failures and an already-running consultation into stable, retryable domain errors.
- [x] 3.7 Ensure recovered `ELIGIBLE` or `PENDING_IDENTITY_VERIFICATION` requests return continuity without invoking the mock or adding an attempt.

## 4. Versioned API and contract

- [x] 4.1 Expose `POST /api/v1/cancellation-requests` with strict JSON input validation and a response that contains only public reference, masked DNI, status, outcome, continuation, next step, and reuse flag.
- [x] 4.2 Extend the common exception handling with stable validation, in-progress, concurrency, unavailable, timeout, and provider-error codes while preserving correlation and hiding internal details.
- [x] 4.3 Confirm CORS permits the functional POST and correlation header without broadening origins, methods, headers, or credentials beyond current needs.
- [x] 4.4 Annotate the endpoint and DTOs so OpenAPI documents success outcomes, correlation, validation, conflict, timeout, dependency, and unexpected-error responses.
- [x] 4.5 Add privacy-focused logging checks so no successful or failed path records the submitted DNI, certificate detail, or provider payload.

## 5. Citizen home frontend

- [x] 5.1 Replace the temporary project-preparation page with the semantic home composition derived from `home.png` and the authoritative functional context.
- [x] 5.2 Adapt the shared header, page background, responsive container, metadata, and trust content without rasterizing the reference or inventing institutional links and claims.
- [x] 5.3 Implement the focused client-side DNI form with visible label, numeric input assistance, maximum length, associated help/error text, and the central validation rule.
- [x] 5.4 Add pending state, `aria-live` announcements, visible focus, disabled submission, duplicate-submit protection, and abort-on-unmount behavior.
- [x] 5.5 Implement the typed eligibility API function through the existing HTTP client and generated OpenAPI types, without handwritten duplicate transport DTOs.
- [x] 5.6 Map eligible, not eligible, inconclusive, unavailable, timeout, network, technical, in-progress, and concurrency outcomes to accessible citizen messages and safe actions.
- [x] 5.7 Clear full DNI state after terminal outcomes or reset and verify it is never written to browser storage, cookies, URLs, diagnostics, or analytics.
- [x] 5.8 Prepare navigation to `/verificacion-identidad` with only the public request reference when `canContinue` and `nextStep` authorize it, without creating a provisional destination screen.
- [x] 5.9 Remove the visible `IntegrationStatusIndicator` from the home page while retaining the system-status client and dedicated technical integration coverage.

## 6. Backend and persistence verification

- [x] 6.1 Add unit tests for all DNI vectors, response masking, gateway result mapping, timeout mapping, and deterministic mock fixtures.
- [x] 6.2 Add use-case tests for new request, eligible recovery, in-progress conflict, terminal history, expiration, retryable outcomes, and stale-attempt recovery.
- [x] 6.3 Extend MySQL/Testcontainers tests to verify the migration from an existing request, public-reference uniqueness, request creation, attempt persistence, and state transitions.
- [x] 6.4 Add real MySQL concurrency tests proving simultaneous submissions produce one active request and at most one active attempt and that version conflicts are controlled.
- [x] 6.5 Add MVC/API tests for valid and invalid payloads, every normalized outcome and error code, correlation propagation, privacy, unsupported media type, and absence of internal identifiers.

## 7. Frontend and contract verification

- [x] 7.1 Update rendering tests for the real home, institutional structure, responsive classes, metadata, semantic landmarks, and absence of later flow controls.
- [x] 7.2 Add form interaction tests for empty/invalid/valid DNI, keyboard behavior, focus placement, loading announcements, duplicate submission, retry, reset, and unmount cancellation.
- [x] 7.3 Add frontend API tests for typed success results and HTTP, network, timeout, invalid-response, in-progress, and concurrency errors using the centralized transport.
- [x] 7.4 Test that only an eligible response prepares navigation and that neither full DNI nor numeric database identifiers appear in the URL or rendered response.
- [x] 7.5 Regenerate `frontend/openapi/backend-api.json` and `frontend/lib/api/generated.ts`, expose stable aliases in `contracts.ts`, and run the drift check.
- [x] 7.6 Extend the real integration suite to submit documented mock fixtures through frontend, backend, and MySQL and verify persistence, correlation, outcomes, and authorized continuation.

## 8. Documentation and final verification

- [x] 8.1 Update backend and frontend local documentation with the endpoint, mock DNI matrix, relevant environment values, retry semantics, and commands for isolated and full-stack tests.
- [x] 8.2 Update the data-model documentation with `public_reference`, `ERROR`, transactional active-request strategy, derived DNI masking, and the fact that the reference is not authentication.
- [x] 8.3 Record unresolved institutional text, asset, real-provider contract, retention, rate-limiting, and JWT-binding decisions without inventing defaults.
- [x] 8.4 Run backend formatting/build/unit tests and Testcontainers persistence/integration tests from a clean database.
- [x] 8.5 Run frontend typecheck, unit tests, OpenAPI check, production build, and the opted-in full-stack integration suite.
- [x] 8.6 Review the final diff against `PROJECT_CONTEXT.md`, `home.png`, privacy constraints, dependency minimalism, and explicit out-of-scope list, confirming no provisional ID Perú screen or later functionality was added.

