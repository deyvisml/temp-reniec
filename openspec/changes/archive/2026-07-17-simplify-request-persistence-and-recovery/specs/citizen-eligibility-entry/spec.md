## MODIFIED Requirements

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

### Requirement: Continuation is emitted only for eligible requests
The backend SHALL set `canContinue=true` and `nextStep=IDENTITY_VERIFICATION` only for an eligible active request. The frontend SHALL prepare navigation to `/verificacion-identidad` using only `requestId` and SHALL never include the DNI in the URL. The numeric identifier MUST NOT be treated as authentication or authorization. This change MUST NOT implement a provisional identity-verification screen.

#### Scenario: Eligible result authorizes transition
- **WHEN** the frontend receives an eligible response with the expected next step
- **THEN** it enables or performs the controlled transition using `requestId` and no DNI value

#### Scenario: Non-eligible or failed result is received
- **WHEN** the result is not eligible or any error occurs
- **THEN** the frontend does not navigate to identity verification and does not expose a continuation control

### Requirement: Functional contract remains synchronized
The OpenAPI document SHALL describe the initiation request, all success outcomes, numeric `requestId`, correlation header, and expected common error responses. Frontend generated types SHALL be regenerated from that document and contract drift checks SHALL remain mandatory. The contract MUST NOT retain `publicReference`.

#### Scenario: Backend contract changes
- **WHEN** DTOs or endpoint responses differ from the committed OpenAPI artifact
- **THEN** the contract check fails until the artifact and generated TypeScript types are synchronized

## REMOVED Requirements

### Requirement: Public request references are opaque and non-authorizing
**Reason**: The UUID duplicates the numeric request identifier and was introduced before a demonstrated authorization requirement.

**Migration**: Replace `publicReference` with numeric `requestId` throughout backend DTOs, OpenAPI, generated TypeScript, frontend navigation and tests. Authorization remains a responsibility of future identity and JWT work.
