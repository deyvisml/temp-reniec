## ADDED Requirements

### Requirement: Positive availability creates the active transactional session
The backend SHALL create exactly one `cancellation_flow_session` for the newly created cancellation request only after DNI, reCAPTCHA and certificate availability have all been accepted. Session creation and the positive request transition SHALL be transactionally consistent. Negative, inconclusive, unavailable, timeout or technical results MUST NOT create a session or issue session cookies.

#### Scenario: Availability is confirmed
- **WHEN** the public initiation completes with `AVAILABLE`
- **THEN** one active session linked to that request is persisted and access and refresh cookies are issued

#### Scenario: Availability is not confirmed
- **WHEN** initiation finishes with any result other than `AVAILABLE`
- **THEN** no session row or session cookie is created

#### Scenario: Concurrent successful initiation is attempted
- **WHEN** overlapping transactions attempt to create a session for the same request
- **THEN** database constraints and application locking allow exactly one session

### Requirement: JWTs contain only technical session identity
Access and refresh JWTs SHALL use an explicitly configured allowed algorithm, issuer, audience, issued-at, expiry and unique token identifier. They SHALL identify only the persisted session and cancellation request plus refresh-family/version data where required. They MUST NOT contain DNI, masked DNI, names, certificates, reasons, provider tokens, current step or other personal or functional data.

#### Scenario: Session tokens are decoded for security review
- **WHEN** a generated access or refresh JWT payload is inspected
- **THEN** it contains only the permitted technical claims and no personal or certificate data

#### Scenario: Token uses an unexpected algorithm or audience
- **WHEN** a protected operation receives a JWT with a different algorithm, issuer or audience
- **THEN** authentication is rejected before session state is used

### Requirement: Session cookies are secure and inaccessible to application JavaScript
The backend SHALL deliver access and refresh tokens only through separate `HttpOnly` cookies with explicit path, `SameSite=Lax`, expiry and `Secure` whenever HTTPS is used. Disabling `Secure` SHALL be allowed only by explicit local HTTP configuration. The refresh cookie SHALL be scoped to the session-renewal API as narrowly as the deployment path permits. Tokens MUST NOT appear in JSON, URLs, browser storage or logs.

#### Scenario: Production cookies are issued
- **WHEN** a session is created or renewed under HTTPS configuration
- **THEN** both cookies include `HttpOnly`, `Secure`, `SameSite=Lax`, explicit path and bounded lifetime

#### Scenario: Frontend code is inspected
- **WHEN** the application handles a successful session response
- **THEN** it neither reads token values nor writes them to `localStorage` or `sessionStorage`

### Requirement: Access validates JWT and persisted active state
Every protected API SHALL validate the access JWT and SHALL load the corresponding session to confirm request binding, active status, expiration, non-invalidation and a non-terminal request state. Cryptographic validity alone MUST NOT authorize access. A request identifier supplied by the client MUST NOT substitute for the session.

#### Scenario: Active access token is presented
- **WHEN** a valid token identifies an active matching session and request
- **THEN** the backend establishes the technical session principal and evaluates the requested operation

#### Scenario: Session was invalidated after token issuance
- **WHEN** a cryptographically valid access token references an invalidated session
- **THEN** the backend rejects it with the common unauthorized response

#### Scenario: Token and persisted request differ
- **WHEN** token claims do not match the session's request
- **THEN** access is denied and no request data is disclosed

### Requirement: Refresh rotation is single-use and concurrency-aware
The backend SHALL renew access only through a valid, unexpired refresh cookie whose hash and family match the locked session row. A successful renewal SHALL rotate the refresh token and retain only hashes needed for the current token and a short previous-token concurrency window. Reuse outside that window SHALL invalidate the session family. A legitimate concurrent stale refresh inside the window SHALL return a recoverable conflict without clearing or invalidating the newer cookies.

#### Scenario: Refresh succeeds
- **WHEN** the current valid refresh token is submitted once
- **THEN** new access and refresh cookies are issued and the previous refresh becomes unusable except for the bounded concurrency rule

#### Scenario: Two tabs refresh concurrently
- **WHEN** a second request presents the immediately previous token inside the concurrency window
- **THEN** it receives a stable recoverable conflict and the active rotated session remains valid

#### Scenario: Old refresh is replayed
- **WHEN** a previous refresh token is presented outside the concurrency window
- **THEN** the family is invalidated and further access or renewal is denied

### Requirement: Current session exposes only safe flow context
The backend SHALL provide a versioned current-session operation returning session validity, masked DNI and the single backend-authorized next step derived from persisted request state. It MUST NOT expose a full DNI, raw token, provider artifact, certificate data before its step, or allow the caller to select a different request.

#### Scenario: Pending identity session is queried
- **WHEN** a valid session belongs to a request in `PENDING_IDENTITY_VERIFICATION`
- **THEN** the response contains the masked DNI and authorizes only `IDENTITY_VERIFICATION`

#### Scenario: Session is absent or expired
- **WHEN** current-session is called without a renewable valid session
- **THEN** it returns the normalized unauthenticated result and exposes no request context

### Requirement: Logout invalidates session and abandons only reversible work
Logout SHALL atomically invalidate the session family and clear both cookies. When the associated request is active and still reversible before confirmation, logout SHALL transition it to `ABANDONED`. A future irreversible or outcome-unknown operation MUST retain its domain result even though the session is invalidated. Logout MUST NOT call an invented remote ID Perú logout contract.

#### Scenario: Citizen exits before confirmation
- **WHEN** logout is requested with an active pre-confirmation session
- **THEN** the session is invalidated, the request becomes `ABANDONED` and both cookies expire

#### Scenario: Logout is repeated
- **WHEN** logout is called again after invalidation or with absent cookies
- **THEN** it remains idempotent from the browser perspective and returns expired cookies without exposing prior state

#### Scenario: Irreversible outcome exists
- **WHEN** logout occurs after a future revocation became irreversible or uncertain
- **THEN** the session ends but the request and operation outcome are not rewritten as abandoned

### Requirement: Session is limited to the currently active journey
The session SHALL maintain only its associated active cancellation request until logout, expiration, abandonment or terminal completion. It MUST NOT recover a historical or finalized request, create multiple browser/device session rows for one request, or reopen a receipt. A new home submission after the prior session ends SHALL follow the existing new-journey rules.

#### Scenario: Active page is reloaded
- **WHEN** the same browser reloads while access or refresh remains valid
- **THEN** the current permitted step is restored from the active session and request state

#### Scenario: Completed journey is revisited
- **WHEN** the citizen returns after the request and session became terminal
- **THEN** the old step or receipt is not restored and a new journey must begin from home
