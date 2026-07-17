## ADDED Requirements

### Requirement: Citizen home communicates the service accurately
The `/` route SHALL present an accessible, responsive citizen-facing home page based on `docs/ui-reference/home.png` and SHALL explain that the service cancels digital certificates associated with a DNI. It MUST NOT state or imply that the DNI, civil identity, physical document, DNIe, or ID Perú account is cancelled, and it MUST NOT expose or allow selection of individual certificates.

#### Scenario: Citizen opens the home page
- **WHEN** a visitor opens `/` on a supported viewport
- **THEN** the page presents the institutional header, service purpose, DNI entry area, primary action, and trust information with a semantic and responsive layout

#### Scenario: Reference and context differ functionally
- **WHEN** a visual detail in `home.png` implies behavior not confirmed by `PROJECT_CONTEXT.md`
- **THEN** the implementation follows the context, records the difference for validation, and does not invent the behavior

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
The backend SHALL expose `POST /api/v1/cancellation-requests` with a JSON DNI body to create or recover a compatible request and determine eligibility. A functional success response SHALL include an opaque public request reference, masked DNI, current request status, normalized eligibility result, `canContinue`, `nextStep`, and a reuse indicator. It MUST NOT expose the full DNI, internal numeric identifiers, certificate details, or provider payloads.

#### Scenario: Eligible initiation succeeds
- **WHEN** a valid DNI produces an eligible result
- **THEN** the endpoint returns a typed success response with `canContinue=true`, `nextStep=IDENTITY_VERIFICATION`, a public request reference, and no sensitive or certificate-level data

#### Scenario: Request body is malformed
- **WHEN** the endpoint receives invalid JSON or an unsupported content type
- **THEN** it returns the common API error format with a stable code and correlation identifier

### Requirement: Public request references are opaque and non-authorizing
Each cancellation request SHALL have a unique UUID public reference distinct from its numeric database identifier. The reference MAY locate a request for the prepared next-step transition but MUST NOT by itself authenticate a citizen or authorize cancellation, revocation, or access to personal data.

#### Scenario: API response is inspected
- **WHEN** a request initiation response is returned
- **THEN** it contains the UUID public reference and does not contain the numeric primary key

#### Scenario: Public reference is presented without future authentication
- **WHEN** a caller later presents only the public reference for a sensitive operation
- **THEN** the backend does not treat it as proof of identity or authorization

### Requirement: One compatible active request is recovered per DNI
The initiation use case SHALL define `STARTED`, `CHECKING_ELIGIBILITY`, `ELIGIBLE`, and `PENDING_IDENTITY_VERIFICATION` as incompatible active states for this stage and SHALL serialize the create-or-recover decision in MySQL. It SHALL preserve terminal history, expire stale requests, and MUST NOT create another active request when a compatible one exists.

#### Scenario: Eligible active request already exists
- **WHEN** the citizen submits a DNI with an unexpired request in `ELIGIBLE` or `PENDING_IDENTITY_VERIFICATION`
- **THEN** the endpoint recovers that request, returns the existing eligible result, and does not execute or persist another eligibility attempt

#### Scenario: Eligibility check is already in progress
- **WHEN** a request for the DNI is in `CHECKING_ELIGIBILITY`
- **THEN** the endpoint returns a controlled conflict indicating that processing is in progress and does not create another request or attempt

#### Scenario: Existing request has expired
- **WHEN** the latest incompatible request has passed its expiration instant
- **THEN** the use case marks it `EXPIRED` and may create a new request within the same controlled workflow

#### Scenario: Concurrent initial submissions arrive
- **WHEN** two transactions submit the same DNI without an existing active request
- **THEN** database locking and retry handling result in one active request and at most one active eligibility attempt

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

### Requirement: Eligibility response semantics are explicit
The frontend SHALL distinguish eligible, not eligible, inconclusive, service unavailable, timeout, technical backend error, network loss, request-in-progress conflict, and concurrency conflict using typed results or stable API error codes. Citizen messages SHALL be understandable, non-technical, non-enumerating, and SHALL provide a retry or restart action only when safe.

#### Scenario: DNI is not eligible
- **WHEN** the backend returns `NOT_ELIGIBLE`
- **THEN** the page blocks continuation, explains that the process cannot continue without listing certificates, and offers a controlled return to the form

#### Scenario: Result is inconclusive
- **WHEN** the backend returns `INCONCLUSIVE`
- **THEN** the page blocks continuation and offers a safe retry against the same compatible request

#### Scenario: Service is unavailable or times out
- **WHEN** the HTTP client receives a service-unavailable or timeout result
- **THEN** the page remains usable, announces temporary unavailability, preserves no browser-stored DNI, and offers a safe manual retry

#### Scenario: Network connection is lost
- **WHEN** the browser cannot reach the backend
- **THEN** the page shows a generic connection message without technical details and allows an explicit retry

### Requirement: Duplicate frontend submissions are prevented
The DNI form SHALL allow only one active submission per mounted form instance. It SHALL disable the primary action, expose a programmatic busy state, ignore repeated submit events until completion, and cancel in-flight work when the component is unmounted.

#### Scenario: Citizen activates submit twice
- **WHEN** the primary action is clicked or triggered repeatedly while a request is pending
- **THEN** only one HTTP request is issued and the loading state is announced accessibly

#### Scenario: Page is left during submission
- **WHEN** the form unmounts while the request is active
- **THEN** the client aborts the request and does not update stale UI state

### Requirement: Continuation is emitted only for eligible requests
The backend SHALL set `canContinue=true` and `nextStep=IDENTITY_VERIFICATION` only for an eligible active request. The frontend SHALL prepare navigation to `/verificacion-identidad` using only the public request reference and SHALL never include the DNI in the URL. This change MUST NOT implement a provisional identity-verification screen.

#### Scenario: Eligible result authorizes transition
- **WHEN** the frontend receives an eligible response with the expected next step
- **THEN** it enables or performs the controlled transition using the public reference and no DNI value

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
The OpenAPI document SHALL describe the initiation request, all success outcomes, the public reference, correlation header, and expected common error responses. Frontend generated types SHALL be regenerated from that document and contract drift checks SHALL remain mandatory.

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
- **THEN** Flyway migrates successfully and tests verify request creation, recovery, attempts, transitions, uniqueness, expiration, and concurrent submissions

#### Scenario: Full local integration suite runs
- **WHEN** frontend, backend, and MySQL are available and a documented eligible fixture is submitted
- **THEN** the response is correlated and typed, the request and attempt are persisted, and the frontend exposes only the authorized next transition
