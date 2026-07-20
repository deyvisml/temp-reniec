# Citizen Eligibility Entry Specification

## Purpose

Define the citizen-facing home, DNI validation, cancellation-request initiation, and normalized certificate-eligibility consultation that begin the cancellation journey.

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
The backend SHALL expose `POST /api/v1/cancellation-requests` with a JSON DNI body to create or recover a compatible request and determine eligibility. A functional success response SHALL include the numeric `requestId`, masked DNI, current request status, normalized eligibility result, `canContinue`, `nextStep`, and a reuse indicator. It MUST NOT expose the full DNI, certificate details, provider payloads, or a redundant public-reference UUID. `requestId` MUST NOT authenticate or authorize a caller by itself.

#### Scenario: Eligible initiation succeeds
- **WHEN** a valid DNI produces an eligible result
- **THEN** the endpoint returns a typed success response with `canContinue=true`, `nextStep=IDENTITY_VERIFICATION`, `requestId`, and no sensitive or certificate-level data

#### Scenario: Request body is malformed
- **WHEN** the endpoint receives invalid JSON or an unsupported content type
- **THEN** it returns the common API error format with a stable code and correlation identifier

### Requirement: One compatible active request is recovered per DNI
The initiation use case SHALL define the unfinished compatible states for the current journey and SHALL serialize the create-or-recover decision in MySQL. It SHALL find the latest compatible request directly by DNI regardless of elapsed time, return its `request_status` as persisted progress, preserve terminal history, and MUST NOT create a session row, require a recovery deadline, or expire the request automatically.

#### Scenario: Eligible active request already exists
- **WHEN** the citizen submits a DNI with an unfinished request in `ELIGIBLE` or `PENDING_IDENTITY_VERIFICATION`, regardless of when it was created
- **THEN** the endpoint recovers that request, returns its numeric `requestId` and existing state, and creates neither another request, eligibility attempt, nor session

#### Scenario: Eligibility check is already in progress
- **WHEN** a request for the DNI is in `CHECKING_ELIGIBILITY`
- **THEN** the endpoint returns a controlled conflict and does not create another request or attempt

#### Scenario: Concurrent initial submissions arrive
- **WHEN** two transactions submit the same DNI without an existing active request
- **THEN** explicit database locking and retry handling result in one active request and at most one active eligibility attempt without an optimistic-version column

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
The backend SHALL prepare, execute, and finalize eligibility through short transactional phases. It SHALL persist every started attempt with a monotonically increasing attempt number and correlation identifier, invoke external I/O without holding the database lock, and finalize both the attempt and request state for every controlled outcome.

#### Scenario: Eligible result is finalized
- **WHEN** the gateway returns `ELIGIBLE`
- **THEN** the attempt is `COMPLETED`, the request stores `ELIGIBLE`, and its state becomes `PENDING_IDENTITY_VERIFICATION`

#### Scenario: Not-eligible result is finalized
- **WHEN** the gateway returns `NOT_ELIGIBLE`
- **THEN** the attempt is `COMPLETED`, the request stores `NOT_ELIGIBLE`, and its state becomes terminal `NOT_ELIGIBLE`

#### Scenario: Inconclusive result is finalized
- **WHEN** the gateway returns `INCONCLUSIVE`
- **THEN** the attempt is completed with that result, the request returns to a retryable state, and continuation remains blocked

#### Scenario: Dependency is unavailable or times out
- **WHEN** the gateway reports unavailability or exceeds its timeout
- **THEN** the attempt is failed with a controlled code, the request remains safely retryable, and the API returns the corresponding common error response

#### Scenario: Unexpected gateway error is controlled
- **WHEN** the gateway fails with a controlled technical error
- **THEN** the attempt records `ERROR` without provider payload or sensitive data, the request remains retryable, and the API does not expose an exception or stack trace

#### Scenario: Submitted attempt becomes stale
- **WHEN** a previously submitted attempt exceeds the configured in-progress threshold after an interrupted execution
- **THEN** a later initiation closes it as a controlled technical failure before creating the next numbered attempt

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
The frontend SHALL distinguish eligible, not eligible, inconclusive, service unavailable, timeout, technical backend error, network loss, request-in-progress conflict, and concurrency conflict using typed results or stable API error codes. Citizen messages SHALL be understandable, non-technical, non-enumerating, and SHALL provide continuation, retry, restart, or return actions only when safe. Completed outcomes SHALL use the maintained SweetAlert2 modal integration instead of replacing the DNI form or invoking a project-owned modal.

#### Scenario: DNI is not eligible
- **WHEN** the backend returns `NOT_ELIGIBLE`
- **THEN** the modal blocks continuation, states that no digital certificates are available for cancellation without listing certificates or adding unrelated reassurance, and offers a conventional action to acknowledge the result

#### Scenario: Result is inconclusive
- **WHEN** the backend returns `INCONCLUSIVE`
- **THEN** the modal blocks continuation and offers a safe explicit retry against the same compatible request or a controlled restart with another DNI

#### Scenario: Service is unavailable or times out
- **WHEN** the client receives a stable unavailable or timeout error
- **THEN** the modal explains the temporary condition, preserves no browser-stored DNI, displays the correlation identifier when available, and offers only safe retry or restart actions

#### Scenario: Network connection is lost
- **WHEN** the browser cannot reach the backend
- **THEN** the modal shows a generic connection message without technical details and allows an explicit retry or controlled restart

#### Scenario: Eligible result authorizes continuation
- **WHEN** the backend returns `ELIGIBLE`, `canContinue=true`, and the next step
- **THEN** the modal offers continuation without placing the DNI in the URL and does not navigate until the citizen activates the explicit action

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
The OpenAPI document SHALL describe the initiation request, all success outcomes, numeric `requestId`, correlation header, and expected common error responses. Frontend generated types SHALL be regenerated from that document and contract drift checks SHALL remain mandatory. The contract MUST NOT retain `publicReference`.

#### Scenario: Backend contract changes
- **WHEN** DTOs or endpoint responses differ from the committed OpenAPI artifact
- **THEN** the contract check fails until the artifact and generated TypeScript types are synchronized

### Requirement: End-to-end behavior is verified at appropriate layers
Tests SHALL cover backend validation and orchestration, frontend rendering and interaction, MySQL persistence and concurrency, deterministic mock outcomes, OpenAPI drift, and a real frontend-to-backend-to-MySQL eligibility path. Fast suites MUST remain independent of manually installed MySQL or external services.

#### Scenario: Isolated test suites run
- **WHEN** backend and frontend baseline tests execute without the local full stack
- **THEN** DNI validation, form behavior, outcome mapping, duplicate-submit protection, mock scenarios, and use-case transitions are verified with controlled dependencies

#### Scenario: Persistence integration suite runs
- **WHEN** Testcontainers starts MySQL from an empty database
- **THEN** Flyway migrates successfully and tests verify request creation, recovery regardless of elapsed time, attempts, transitions, uniqueness, and concurrent submissions

#### Scenario: Full local integration suite runs
- **WHEN** frontend, backend, and MySQL are available and a documented eligible fixture is submitted
- **THEN** the response is correlated and typed, the request and attempt are persisted, and the frontend exposes only the authorized next transition
