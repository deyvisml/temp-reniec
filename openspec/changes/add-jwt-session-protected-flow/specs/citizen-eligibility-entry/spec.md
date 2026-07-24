## MODIFIED Requirements

### Requirement: Continuation is emitted only for eligible requests
The backend SHALL create and issue the transactional session with `canContinue=true` and `nextStep=IDENTITY_VERIFICATION` only when certificate availability is positively confirmed for the new active request. Negative, inconclusive, unavailable, timeout and technical outcomes SHALL block continuation and MUST NOT create a session. The frontend SHALL leave the public `/` home and navigate to the protected `/cancelacion` route only after the successful response has installed the HttpOnly session cookies; it SHALL NOT place request, DNI, token, certificate or step data in the URL.

#### Scenario: Positive availability authorizes transition
- **WHEN** the frontend receives the expected positive response and session cookies
- **THEN** it navigates from `/` to protected `/cancelacion`, whose backend state authorizes identity verification

#### Scenario: Any other result is received
- **WHEN** availability is negative or cannot be confirmed
- **THEN** the frontend remains on public `/`, presents the controlled outcome and has no session with which to enter the internal route

### Requirement: Positive availability creates only the current active session
The initial query SHALL treat `AVAILABLE` as permission to begin identity verification, not as verified identity or authorization for certificate listing. Its successful response SHALL create the only transactional session for the current request and browser operation. The session SHALL authorize only step 1 until ID Perú confirms a matching identity; it MUST NOT restore a previous request or authorize certificate listing.

#### Scenario: Availability is positive
- **WHEN** the protected initial query returns `AVAILABLE`
- **THEN** the browser receives the session and can enter identity verification but cannot access the post-authentication listing boundary

#### Scenario: Availability is not positive
- **WHEN** the initial result is negative, inconclusive or technical failure
- **THEN** no session is established and ID Perú cannot be started

#### Scenario: Previous operation has ended
- **WHEN** the citizen returns to home after logout, expiration, abandonment or completion
- **THEN** the former session does not recover the old request and a new submission follows the new-journey rules
