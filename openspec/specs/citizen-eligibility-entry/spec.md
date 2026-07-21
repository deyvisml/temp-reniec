# Citizen Eligibility Entry Specification

## Purpose

Define the citizen-facing home, DNI validation, cancellation-request initiation, and normalized certificate-eligibility consultation that begin the cancellation journey.

> **Implementation alignment notice (SPEC-08):** These requirements describe the currently implemented binary eligibility increment. The current domain authority at `docs/context/PROJECT_CONTEXT.md` now defines a list of current certificate issues, disclosure only after authentication, and a mandatory certificate-selection step. A later functional change must replace the incompatible requirements and implementation; this specification MUST NOT be used to override the updated project context.
## Requirements
### Requirement: Citizen home communicates the service accurately
The `/` route SHALL present an accessible, responsive citizen-facing home page based on `docs/ui-reference/home.png` and SHALL implement its component presentation through the Tailwind-first styling baseline defined by `frontend-foundation`. The migration from global component selectors MUST preserve the approved institutional header, service purpose, supplied image assets, DNI entry area, primary action, trust information, functional states, responsive behavior, semantic structure, and visible focus. It MUST NOT state or imply that the DNI, civil identity, physical document, DNIe, or ID Perú account is cancelled, expose or allow selection of individual certificates, alter the original reference files, or redesign the flow as part of the styling refactor.

#### Scenario: Citizen opens the home page
- **WHEN** a visitor opens `/` on a supported desktop, intermediate, or mobile viewport
- **THEN** the page presents the institutional header, service purpose, supplied hero image, DNI entry area, primary action, and trust information with the approved responsive composition and no horizontal overflow

#### Scenario: Form state is rendered after migration
- **WHEN** the DNI form displays its initial, validation, loading, eligible, non-eligible, inconclusive, unavailable, timeout, network, or controlled error state
- **THEN** its existing behavior, accessible announcements, focus treatment, readable hierarchy, and safe actions remain available after replacing global component selectors with Tailwind utilities

#### Scenario: Reference and context differ functionally
- **WHEN** a visual detail in `home.png` implies behavior not confirmed by `PROJECT_CONTEXT.md`
- **THEN** the implementation follows the context, records the difference for validation, and does not invent the behavior

#### Scenario: Styling implementation is reviewed
- **WHEN** the home page, form, header, footer, benefits, and result components are inspected
- **THEN** their presentation uses colocated Tailwind utilities and contains neither visual inline styles nor dependencies on component selectors in `app/globals.css`

### Requirement: DNI validation is strict and centralized
The frontend and backend SHALL accept a DNI only when it contains exactly eight ASCII digits matching `^[0-9]{8}$`. The backend validation SHALL be authoritative, and both implementations SHALL use one named rule and shared test vectors within their respective codebases.

#### Scenario: Empty DNI is submitted
- **WHEN** the citizen submits an empty DNI
- **THEN** the frontend prevents the request, associates a clear error with the field, and moves focus to the invalid control

#### Scenario: Invalid DNI reaches the backend
- **WHEN** the endpoint receives a DNI with letters, spaces, Unicode digits, fewer than eight digits, more than eight digits, or an oversized payload
- **THEN** it returns the common validation error without creating a request or eligibility attempt

#### Scenario: Valid DNI is submitted
- **WHEN** the input contains exactly eight ASCII digits
- **THEN** the frontend permits submission and the backend accepts the value for case-use processing

### Requirement: Versioned request initiation contract
The backend SHALL expose `POST /api/v1/cancellation-requests` with a JSON DNI body to create a new request and determine current certificate eligibility. A functional success response SHALL include the new numeric `requestId`, masked DNI, current request status, normalized eligibility result, `canContinue`, and `nextStep`. It MUST NOT expose a reuse indicator, full DNI, certificate details before authentication, provider payloads, historical request identifiers, or a redundant public-reference UUID. `requestId` MUST NOT authenticate or authorize a caller by itself.

#### Scenario: Eligible initiation succeeds
- **WHEN** a valid DNI produces an eligible result
- **THEN** the endpoint returns a typed success response for a newly created request with `canContinue=true`, `nextStep=IDENTITY_VERIFICATION`, and no historical or sensitive data

#### Scenario: Request body is malformed
- **WHEN** the endpoint receives invalid JSON or an unsupported content type
- **THEN** it returns the common API error format with a stable code and correlation identifier

#### Scenario: Previous terminal request exists
- **WHEN** the citizen submits a DNI that has a completed request or available constancia
- **THEN** the endpoint creates a new request and does not return, reopen, or navigate to the previous result

### Requirement: Eligibility integration is replaceable and normalized
The use case SHALL depend on an internal eligibility gateway rather than a provider-specific DTO. Its normalized outcomes SHALL include `ELIGIBLE`, `NOT_ELIGIBLE`, `UNAVAILABLE`, `INCONCLUSIVE`, and `ERROR`, with only an optional external reference and controlled technical code. Complete external payloads MUST NOT be persisted or returned.

#### Scenario: Provider adapter is replaced
- **WHEN** an institutional contract becomes available
- **THEN** a new adapter can implement the gateway without changing the use-case outcome model or frontend contract

#### Scenario: Provider returns certificate details
- **WHEN** a future adapter receives certificate-level information
- **THEN** the use case reduces it to the eligibility outcome and does not expose individual certificate data to the citizen

### Requirement: Local eligibility mock is deterministic
Local and test profiles SHALL provide a deterministic mock adapter with documented fictitious DNI fixtures for eligible, not eligible, unavailable, inconclusive, technical error, and timeout scenarios. It MUST NOT use randomness, real citizen data, or pretend to be the institutional contract.

#### Scenario: Documented fixture is used repeatedly
- **WHEN** the same documented DNI fixture is submitted more than once under equivalent state
- **THEN** the mock produces the same configured external outcome each time

#### Scenario: Unlisted valid DNI is submitted
- **WHEN** the local mock receives a valid DNI not listed as a special fixture
- **THEN** it returns the documented deterministic default result

### Requirement: Eligibility attempts and current state remain consistent
The backend SHALL prepare, execute, and finalize eligibility through short transactional phases. Each new request SHALL start its own attempt numbering, persist every started attempt with correlation, invoke external I/O without holding the database lock, and finalize the attempt only while it remains submitted and its request remains current for that attempt. A stale or superseded attempt MUST NOT reactivate an abandoned request.

#### Scenario: Eligible result is finalized
- **WHEN** the gateway returns `ELIGIBLE` for the current submitted attempt
- **THEN** the attempt is `COMPLETED`, the request stores the available-certificate result required by the current model, and continuation targets identity verification

#### Scenario: Not-eligible result is finalized
- **WHEN** the gateway returns an empty eligible-certificate result for the current attempt
- **THEN** the attempt is completed, the request records that no certificates are available, and continuation remains blocked

#### Scenario: Inconclusive result is finalized
- **WHEN** the gateway returns `INCONCLUSIVE`
- **THEN** the attempt is completed with that result and continuation remains blocked

#### Scenario: Dependency is unavailable or times out
- **WHEN** the gateway reports unavailability or exceeds its timeout
- **THEN** the attempt is failed with a controlled code and the API returns the corresponding common error response

#### Scenario: Submitted attempt becomes stale
- **WHEN** a submitted attempt exceeds the configured in-progress threshold after interrupted execution and the citizen initiates again
- **THEN** the old attempt is failed, its request is abandoned, and a different request with attempt number 1 is created

#### Scenario: Late provider response arrives
- **WHEN** a response arrives for an attempt that was failed or whose request became `ABANDONED`
- **THEN** finalization rejects the stale update and does not change the abandoned request or the new request

### Requirement: Eligibility outcomes use accessible separated feedback
The frontend SHALL present every completed eligibility outcome through a compact, persistent and responsive SweetAlert2 modal that is visually and semantically separate from the DNI form. The form SHALL remain mounted as background context and MUST NOT be replaced by a full-width result panel inside the consultation card. The application MUST use SweetAlert2's supported public API and MUST NOT retain a project-owned modal implementation, depend on internal SweetAlert2 markup, or add component-specific global CSS.

#### Scenario: Citizen has no cancellable certificates
- **WHEN** the backend returns `NOT_ELIGIBLE`
- **THEN** a SweetAlert2 modal states concisely that no digital certificates are available for cancellation with the entered DNI, blocks continuation, avoids unrelated reassurance about the DNI or identity, and offers a conventional action to acknowledge the result

#### Scenario: Citizen receives an eligible result
- **WHEN** the backend returns `ELIGIBLE` with `canContinue=true`
- **THEN** the same SweetAlert2 presentation offers the authorized continuation action and an optional safe action to start another consultation

#### Scenario: Citizen receives a retryable result
- **WHEN** the result is inconclusive, unavailable, timed out, affected by network loss, or represented by a retryable stable error code
- **THEN** the modal explains the temporary condition without technical detail and provides an explicit safe retry plus an action to enter another DNI

#### Scenario: Outcome modal is reviewed visually
- **WHEN** an eligibility outcome is displayed on desktop, intermediate or mobile viewports
- **THEN** the modal remains compact, preserves at least 16 pixels of viewport clearance, keeps all actions reachable, avoids horizontal overflow, and does not recreate the oversized consultation card as an overlay

### Requirement: Eligibility outcome dialogs manage focus and dismissal safely
The SweetAlert2 integration SHALL expose an accessible name and description, open as a modal dialog, contain keyboard focus, and restore focus to the DNI field after a reset. Visual status MUST NOT rely only on color. Backdrop clicks MUST NOT dismiss the result, and Escape SHALL map to the safe non-continuing action for the current outcome. Retry and continuation SHALL occur only through explicit actions.

#### Scenario: Modal opens after a submitted consultation
- **WHEN** the pending consultation resolves to a functional result or controlled error
- **THEN** focus moves into the labeled SweetAlert2 modal and assistive technology can identify its result, explanation and available actions without duplicate announcements

#### Scenario: Citizen navigates with the keyboard
- **WHEN** the citizen uses Tab or Shift+Tab while the modal is open
- **THEN** focus remains inside the modal and every available action has a visible focus indicator and a target of at least 44 by 44 pixels

#### Scenario: Citizen dismisses with Escape
- **WHEN** the citizen presses Escape instead of selecting an action
- **THEN** the integration executes the variant's safe reset or return behavior, never continues the citizen flow, never retries automatically, and restores focus to the DNI field when the form is shown

#### Scenario: Citizen clicks the backdrop
- **WHEN** the citizen clicks outside the modal surface
- **THEN** the result remains open so that a terminal or retryable outcome cannot be lost accidentally

#### Scenario: Citizen prefers reduced motion
- **WHEN** the browser reports `prefers-reduced-motion: reduce`
- **THEN** the SweetAlert2 presentation does not use non-essential entrance or exit animation

### Requirement: Eligibility response semantics are explicit
The frontend SHALL distinguish eligible, no certificates available, inconclusive, service unavailable, timeout, technical backend error, network loss, request-in-progress conflict, protected-operation conflict, and concurrency conflict using typed results or stable API error codes. Citizen messages SHALL be understandable, non-technical, non-enumerating, and SHALL provide continuation, a new explicit initiation, or acknowledgement only when safe. Completed outcomes SHALL use the maintained SweetAlert2 integration instead of replacing the DNI form or invoking a project-owned modal.

#### Scenario: DNI has no available certificates
- **WHEN** the backend returns the normalized empty-list result
- **THEN** the modal blocks continuation, states that no digital certificates are available for cancellation, and offers a conventional acknowledgement action

#### Scenario: Result is inconclusive or temporarily unavailable
- **WHEN** the client receives an inconclusive, unavailable, timeout, or network result
- **THEN** the modal explains the condition, preserves no browser-stored DNI, displays correlation when available, and does not claim that previous progress can be recovered

#### Scenario: Previous operation is protected
- **WHEN** the backend rejects initiation because a confirmed revocation is active or uncertain
- **THEN** the frontend shows a generic controlled message without navigating to, identifying, or reopening the previous request

#### Scenario: Eligible result authorizes continuation
- **WHEN** the backend returns an eligible new request with `canContinue=true` and the next step
- **THEN** the frontend offers continuation without placing the DNI in the URL and does not navigate until the citizen activates the explicit action

### Requirement: Duplicate frontend submissions are prevented
The DNI form SHALL allow only one active submission per mounted form instance. It SHALL disable the primary action, expose a programmatic busy state, ignore repeated submit events until completion, and cancel in-flight work when the component is unmounted.

#### Scenario: Citizen activates submit twice
- **WHEN** the primary action is clicked or triggered repeatedly while a request is pending
- **THEN** only one HTTP request is issued and the loading state is announced accessibly

#### Scenario: Page is left during submission
- **WHEN** the form unmounts while the request is active
- **THEN** the client aborts the request and does not update stale UI state

### Requirement: Continuation is emitted only for eligible requests
The backend SHALL set `canContinue=true` and `nextStep=IDENTITY_VERIFICATION` only for an eligible active request. The frontend SHALL prepare navigation to `/verificacion-identidad` using only `requestId` and SHALL never include the DNI in the URL. The numeric identifier MUST NOT be treated as authentication or authorization. This change MUST NOT implement a provisional identity-verification screen.

#### Scenario: Eligible result authorizes transition
- **WHEN** the frontend receives an eligible response with the expected next step
- **THEN** it enables or performs the controlled transition using `requestId` and no DNI value

#### Scenario: Non-eligible or failed result is received
- **WHEN** the result is not eligible or any error occurs
- **THEN** the frontend does not navigate to identity verification and does not expose a continuation control

### Requirement: DNI exposure is minimized outside MySQL
The system SHALL persist the DNI only in the consolidated request column, derive masked presentation from it, and MUST NOT place the full value in logs, metrics, error bodies, URLs, OpenAPI examples, browser storage, cookies, or technical endpoints. The frontend SHALL clear the input value after a terminal response or controlled reset.

#### Scenario: Application logs are inspected
- **WHEN** initiation succeeds, fails validation, times out, conflicts, or raises an unexpected error
- **THEN** logs contain correlation and technical state but not the submitted DNI

#### Scenario: Browser storage and history are inspected
- **WHEN** the citizen completes or abandons the initial consultation
- **THEN** the DNI is absent from local storage, session storage, cookies, and visited URLs

### Requirement: Initial anti-abuse controls remain simple
The endpoint SHALL enforce strict media type, body size, DNI format, bounded timeout, active-request deduplication, and in-progress attempt deduplication. The change MUST NOT introduce an in-memory per-IP limiter presented as a production control; final distributed rate limiting and perimeter controls SHALL remain documented for a later infrastructure decision.

#### Scenario: Automated duplicate burst targets one DNI
- **WHEN** multiple equivalent submissions overlap
- **THEN** validation, database serialization, and in-progress conflict handling prevent multiplication of active requests and provider calls

### Requirement: Functional contract remains synchronized
The OpenAPI document SHALL describe initiation of a new request, all success outcomes, numeric `requestId`, correlation header, and expected common error responses. It MUST NOT describe recovery or include `reused` or `publicReference`. Frontend generated types SHALL be regenerated from that document and contract drift checks SHALL remain mandatory.

#### Scenario: Backend contract changes
- **WHEN** the reuse indicator is removed from the DTO and endpoint semantics
- **THEN** the committed OpenAPI artifact, generated TypeScript types, fixtures, tags, summaries, descriptions, and contract tests are synchronized

### Requirement: End-to-end behavior is verified at appropriate layers
Tests SHALL cover backend validation and orchestration, frontend rendering and interaction, MySQL persistence and concurrency, deterministic mock outcomes, OpenAPI drift, and a real frontend-to-backend-to-MySQL eligibility path. They SHALL verify that repeated entries create distinct safe requests, prior terminal results are not reopened, replaceable history becomes abandoned, and protected operations block without recovery. Fast suites MUST remain independent of manually installed MySQL or external services.

#### Scenario: Isolated test suites run
- **WHEN** backend and frontend baseline tests execute without the local full stack
- **THEN** DNI validation, form behavior, outcome mapping, duplicate-submit protection, mock scenarios, non-recovery semantics, and use-case transitions are verified with controlled dependencies

#### Scenario: Persistence integration suite runs
- **WHEN** Testcontainers starts MySQL from an empty database
- **THEN** Flyway migrates successfully and tests verify new request creation, abandonment, historical preservation, protected states, attempts, and concurrent submissions

#### Scenario: Full local integration suite runs
- **WHEN** frontend, backend, and MySQL are available and the same documented eligible fixture starts separate completed journeys
- **THEN** each safe new initiation receives a different correlated request and neither response restores the previous progress or constancia

### Requirement: Every home-page entry starts a new journey
Submitting a DNI from the home page SHALL express a new cancellation intention. The backend SHALL query current certificates for a newly created request and MUST NOT resume a prior request based on DNI, elapsed time, browser, device, or previous status. Continuing immediately from the response belongs to the same current journey and SHALL NOT be described as recovery.

#### Scenario: Previous pre-confirmation journey exists
- **WHEN** the citizen returns to the home page and submits the same DNI after leaving a request before confirmation
- **THEN** the old request is abandoned and the response belongs to a new request with a new eligibility consultation

#### Scenario: Previous completed journey exists
- **WHEN** the citizen previously reached the constancia and later submits the same DNI again
- **THEN** the system starts from current certificate consultation and never returns the old constancia as the active screen

#### Scenario: One certificate was previously left unselected
- **WHEN** a prior request canceled only some certificates and the citizen initiates again
- **THEN** the new request uses a fresh provider result so remaining currently valid certificates can participate independently

#### Scenario: Browser storage is inspected
- **WHEN** the citizen closes or reloads the application
- **THEN** no DNI, request progress, or restoration token is read from `localStorage` or `sessionStorage` to resume the prior journey

### Requirement: Unsafe overlapping operations are blocked without recovery
A new initiation SHALL be rejected while the same DNI has a live eligibility call that is not stale or a confirmed revocation whose outcome remains active or uncertain. The response MUST use a stable generic error and correlation identifier and MUST NOT return the previous `requestId`, step, certificates, selection, or constancia.

#### Scenario: Eligibility call is active
- **WHEN** another submission arrives while the current eligibility call is within its active threshold
- **THEN** the backend returns the existing in-progress conflict and creates no second request or provider call

#### Scenario: Revocation is active or uncertain
- **WHEN** another submission arrives while a confirmed operation is executing or its outcome is unknown
- **THEN** the backend blocks initiation without recovering or exposing the previous journey

#### Scenario: Prior revocation becomes terminal
- **WHEN** the previous operation reaches a confirmed terminal result and the citizen initiates again
- **THEN** a new request is allowed and historical operation and constancia rows remain unchanged

