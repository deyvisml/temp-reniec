## MODIFIED Requirements

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

## ADDED Requirements

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

## REMOVED Requirements

### Requirement: One compatible active request is recovered per DNI
**Reason**: Reopening prior progress conflicts with selective cancellation because a returning citizen may intend to cancel other currently valid certificates, and a completed constancia must not become the active journey again.

**Migration**: Treat every home-page submission as a new intention. Abandon safe pre-confirmation history, block unsafe in-flight or uncertain operations, create a new request and consultation when allowed, and retain all prior rows solely as history.
