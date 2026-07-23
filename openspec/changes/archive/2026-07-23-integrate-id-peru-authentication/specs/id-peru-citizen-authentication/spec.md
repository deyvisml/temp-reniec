## ADDED Requirements

### Requirement: Identity verification starts only from an eligible current request
The backend SHALL start ID Perú authentication only for the request represented by a valid current-browser continuation credential whose persisted availability is `AVAILABLE` and whose status permits identity verification. It MUST build provider parameters in the backend and MUST NOT accept client-selected DNI, provider endpoints, `client_id`, `redirect_uri`, `acr_values`, state or PKCE values.

#### Scenario: Eligible citizen starts verification
- **WHEN** the current continuation identifies a request in `PENDING_IDENTITY_VERIFICATION` with confirmed availability
- **THEN** the backend creates a new identity attempt and returns its provider authorization URL

#### Scenario: Caller supplies only a request identifier
- **WHEN** a caller knows a numeric request ID but lacks the matching continuation credential
- **THEN** the backend refuses to start ID Perú and reveals no DNI or request details

#### Scenario: Request cannot authenticate
- **WHEN** availability is not confirmed or the request is expired, abandoned, completed or otherwise outside an allowed identity state
- **THEN** no identity attempt or provider URL is created

### Requirement: Real authorization request follows ID Perú v1.2
The real adapter SHALL build an HTTPS authorization URL using `response_type=code`, `scope=openid`, configured `client_id`, exact registered `redirect_uri`, one configured `acr_values`, `state`, PKCE `code_challenge`, `code_challenge_method=S256`, optional positive `max_age`, and `vd` only for a mechanism that requires it. Query values SHALL be encoded once and the full URL MUST NOT be logged.

#### Scenario: Facial authentication is configured
- **WHEN** `acr_values=face_mobile` and a valid attempt is prepared
- **THEN** the URL contains an encrypted `vd`, PKCE S256 and every mandatory v1.2 parameter without plaintext DNI

#### Scenario: DNIe authentication is configured
- **WHEN** `acr_values=pki_dnie`
- **THEN** the URL omits `vd` and contains no second authentication mechanism

#### Scenario: Unsupported mechanism is configured
- **WHEN** `acr_values` is absent, contains multiple values or is not allowed by the v1.2 contract
- **THEN** real-mode configuration fails closed before an authentication can start

### Requirement: DNI is encrypted for vd exactly as specified
The backend SHALL calculate `vd` with `AES/CBC/PKCS5Padding`, using the first 16 UTF-8 characters of `client_id` as both AES key and IV, Base64-encoding the ciphertext and URL-encoding it as a query value. This operation SHALL be encapsulated and covered by deterministic vectors.

#### Scenario: vd vector is calculated
- **WHEN** a known fictitious DNI and eligible test `client_id` are encrypted
- **THEN** the produced Base64 ciphertext matches the approved vector and decrypts to the original test DNI

#### Scenario: client identifier is too short
- **WHEN** `client_id` cannot supply 16 characters
- **THEN** configuration is rejected without logging the DNI or client value

### Requirement: State and PKCE are unique, expiring and single-use
Each identity attempt SHALL use a cryptographically random state of at least 256 bits and a random PKCE verifier between 43 and 128 allowed characters. The backend SHALL store only the state hash, SHALL protect the recoverable verifier at rest, SHALL derive `BASE64URL(SHA256(verifier))` without padding, and SHALL expire and consume both artifacts exactly once.

#### Scenario: Two attempts are prepared
- **WHEN** the same request legitimately retries identity verification
- **THEN** each attempt receives a distinct state, verifier and challenge

#### Scenario: Callback is replayed
- **WHEN** a callback reuses an already consumed state
- **THEN** it is rejected before token exchange and no second successful verification is recorded

#### Scenario: Attempt expires
- **WHEN** the callback arrives after the configured state expiry
- **THEN** the attempt is marked expired, its protected verifier is cleared and no token request is sent

### Requirement: Callback processing is strict and provider-compatible
The backend SHALL expose the registered callback as HTTP POST form handling `code`, `state`, `session_state` and optional `error`. It SHALL validate and atomically consume state before exchanging a code, validate the documented shape of `code` and `session_state`, and redirect with HTTP 303 to a fixed frontend return route without tokens, codes, state, DNI or technical details in the URL.

#### Scenario: Valid callback arrives
- **WHEN** code, matching unexpired state and valid session_state arrive once
- **THEN** the attempt is reserved atomically and token exchange proceeds

#### Scenario: Provider reports cancellation
- **WHEN** a callback contains a valid state and a cancellation error
- **THEN** the attempt becomes `CANCELLED`, continuation remains blocked and no token exchange occurs

#### Scenario: State is unknown
- **WHEN** a callback contains an absent, malformed or unknown state
- **THEN** the callback is rejected without revealing whether a request exists

#### Scenario: Callback omits required successful fields
- **WHEN** no provider error is present but code or session_state is absent or malformed
- **THEN** the attempt ends in a controlled error and no partial authentication is accepted

### Requirement: Authorization code exchange is complete and non-retrying
The real adapter SHALL call the configured token endpoint once with HTTP POST, `application/x-www-form-urlencoded`, required `Referer`, `grant_type=authorization_code`, code, exact redirect URI, client ID, client secret and recovered code verifier. It SHALL require a well-formed access token, ID token, positive expiry and token type, apply configured timeouts, and MUST NOT retry a consumed or outcome-unknown code automatically.

#### Scenario: Token exchange succeeds
- **WHEN** ID Perú returns every required token field in a successful response
- **THEN** the backend validates the ID token before invoking userinfo and keeps all tokens server-side only

#### Scenario: Code is invalid, expired or reused
- **WHEN** the token endpoint reports `invalid_code` or an equivalent controlled error
- **THEN** the attempt is rejected and the verifier is cleared without another exchange

#### Scenario: Token outcome is uncertain
- **WHEN** the token call times out or loses connection after sending the request
- **THEN** the attempt records an outcome-unknown technical error and requires a fresh authentication attempt

### Requirement: ID token and userinfo JWT are cryptographically validated
The backend SHALL validate JWTs with a maintained JOSE implementation before reading claims. Validation SHALL enforce allowed algorithm, signature, `kid`, expiry, not-before when present, configured issuer, audience equal to `client_id`, subject and required claims. JWKS SHALL be cached for a configurable TTL and refreshed once when a `kid` is unknown.

#### Scenario: Signed tokens are valid
- **WHEN** ID token and userinfo JWT have valid RS256 signatures, recognized keys and expected claims
- **THEN** the backend may use their validated subject and citizen document

#### Scenario: JWT is merely decodable
- **WHEN** a JWT payload can be decoded but its signature, algorithm, issuer, audience or lifetime is invalid
- **THEN** authentication is rejected and no claim is trusted

#### Scenario: Signing key rotates
- **WHEN** a token references an unknown kid while cached JWKS is still valid
- **THEN** the backend refreshes JWKS once and accepts the token only if the refreshed set validates it

#### Scenario: JWKS cannot be obtained
- **WHEN** neither a valid cached key nor a successful controlled refresh is available
- **THEN** authentication remains unverified and a temporary provider error is recorded

### Requirement: Authenticated identity must match the initiating DNI
After validating `/userinfo`, the backend SHALL compare its `doc` claim with the DNI held by the current cancellation request. It SHALL require a consistent subject between validated ID token and userinfo data, persist only the necessary safe subject reference and match result, and MUST NOT expose the authenticated document or person data in mismatch responses.

#### Scenario: Documents match
- **WHEN** validated userinfo contains the same eight-digit DNI as the request
- **THEN** the attempt becomes `VERIFIED`, the request becomes `IDENTITY_VERIFIED` and temporary flow authorization is issued

#### Scenario: Documents differ
- **WHEN** validated userinfo identifies a different DNI
- **THEN** the attempt becomes `IDENTITY_MISMATCH`, the step 2 remains blocked and no data about the authenticated person is returned

#### Scenario: Required identity claim is absent
- **WHEN** userinfo omits or malforms `sub` or `doc`
- **THEN** the response is invalid, no comparison is inferred and authorization is not issued

### Requirement: Real and simulated providers preserve the same use case
The application SHALL provide real and deterministic simulated identity adapters selected by backend configuration. The mock SHALL cover matching identity, mismatch, rejection, cancellation, expiration, timeout, unavailability, malformed response and repeated callback, and SHALL traverse the same attempt, callback, match and authorization rules as real mode. Production MUST refuse mock mode.

#### Scenario: Local matching fixture authenticates
- **WHEN** the documented fictitious matching scenario is executed in mock mode
- **THEN** it completes through the callback use case and produces the same persisted success state as real mode

#### Scenario: Mock callback is repeated
- **WHEN** the simulated provider submits the same callback twice
- **THEN** the second callback is rejected by the common single-use state rule

#### Scenario: Production requests mock mode
- **WHEN** the production profile starts with the simulated adapter selected
- **THEN** application startup fails with a non-secret configuration error

### Requirement: Step 1 presents an accessible five-step identity experience
The frontend SHALL implement `/verificacion-identidad` and its fixed return route using the current five-step flow and the composition of `docs/ui-reference/step-1.png`. It SHALL explain that ID Perú verifies identity, that no certificate is cancelled in this step, and that successful verification leads to certificate selection. It SHALL provide accessible and responsive states for ready, redirecting, processing, success, cancellation, rejection, mismatch, expiration, timeout and unavailability.

#### Scenario: Citizen opens the identity step
- **WHEN** a valid current-browser continuation is available
- **THEN** the page shows step 1 of 5 and one clear action to begin ID Perú authentication

#### Scenario: Citizen activates authentication twice
- **WHEN** the start control is triggered repeatedly while a request is pending
- **THEN** only one backend start call occurs and the busy state is announced

#### Scenario: Authentication returns successfully
- **WHEN** the callback has established a verified authorization
- **THEN** the return page confirms identity and enables only the controlled transition toward step 2 without rendering a placeholder selection screen

#### Scenario: Authentication fails safely
- **WHEN** ID Perú is cancelled, rejects, expires, mismatches or is unavailable
- **THEN** the page presents a non-technical outcome and only a permitted retry or return action

### Requirement: Identity integration protects sensitive artifacts
The system MUST NOT log, expose to the browser or persist permanently client secret, plaintext code verifier, authorization code, access token, ID token, userinfo JWT, plaintext state, biometric data, photographs or complete provider responses. Tokens SHALL exist only in backend memory for the current exchange, and any protected verifier SHALL be cleared when the attempt ends.

#### Scenario: Diagnostic logs are reviewed
- **WHEN** successful and failing real/mock scenarios execute
- **THEN** logs contain correlation and normalized status but none of the prohibited artifacts or full DNI values

#### Scenario: Browser storage is inspected
- **WHEN** the citizen completes or fails authentication
- **THEN** localStorage, sessionStorage, URLs and frontend-readable cookies contain no tokens, verifier, state or DNI

