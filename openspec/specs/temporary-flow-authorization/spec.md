# temporary-flow-authorization Specification

## Purpose
TBD - created by archiving change integrate-id-peru-authentication. Update Purpose after archive.
## Requirements
### Requirement: Pre-authentication continuation is bound to the current browser
After a positive availability result, the backend SHALL issue a short-lived, signed, HttpOnly continuation cookie whose purpose is limited to starting identity verification for that request. It SHALL contain no DNI or personal data, SHALL be validated with the persisted request state, and SHALL NOT make a historical request recoverable in another browser.

#### Scenario: Positive availability continues
- **WHEN** the initial query returns `AVAILABLE`
- **THEN** the current browser receives an identity-init continuation and can open `/verificacion-identidad` without request data in the URL

#### Scenario: Browser lacks continuation
- **WHEN** the identity page or start API is opened without a valid cookie
- **THEN** the system blocks the flow and directs the citizen to begin a new initial query

#### Scenario: Previous browser returns later
- **WHEN** the cookie expired or the request was finalized and the citizen revisits the page
- **THEN** no previous progress or receipt is restored

### Requirement: Successful identity rotates into temporary flow authorization
Only a verified, matching ID Perú attempt SHALL cause the backend to replace the identity-init credential with a short-lived flow authorization. The signed value SHALL include purpose, request ID, verified attempt ID, audience, jti and lifetime but no DNI or identity claims; the database SHALL store only a hash of its jti, validity and invalidation state.

#### Scenario: Identity verification succeeds
- **WHEN** a matching callback is committed
- **THEN** the backend rotates the cookie and subsequent protected calls can prove the verified request context

#### Scenario: Identity verification is not successful
- **WHEN** the attempt is rejected, cancelled, mismatched, expired or technically fails
- **THEN** no post-identity authorization is issued

### Requirement: Authorization cookie uses secure browser attributes
The authorization cookie SHALL be `HttpOnly`, `SameSite=Lax`, path-scoped to the application and short-lived. It SHALL use `Secure` whenever HTTPS is used; disabling `Secure` SHALL be explicit and limited to local HTTP development. Frontend calls that require it SHALL use credentials without reading the cookie.

#### Scenario: Application runs under HTTPS
- **WHEN** a continuation or flow authorization is set
- **THEN** the response applies HttpOnly, Secure, SameSite=Lax, explicit Path and Max-Age/Expires attributes

#### Scenario: Frontend calls a protected API
- **WHEN** the browser has a valid authorization cookie
- **THEN** it sends the cookie through `credentials: include` while JavaScript cannot read its value

### Requirement: Protected access validates token and persisted state
Every API designated as post-identity SHALL validate signature, algorithm, purpose, audience, lifetime and jti, and SHALL confirm that the jti hash belongs to the stated verified attempt and that the request remains in an allowed state. Numeric request identifiers SHALL never substitute for this authorization.

#### Scenario: Valid authorization accesses the next boundary
- **WHEN** token and persisted verification are current and correspond to the same request
- **THEN** the backend authorizes access to the future detailed-list boundary

#### Scenario: Token was modified or belongs to another request
- **WHEN** signature, purpose, attempt, request or jti does not match
- **THEN** access is denied with the common error format and no request data is returned

#### Scenario: Database state was invalidated
- **WHEN** the JWT remains cryptographically valid but its persisted jti was invalidated
- **THEN** access is denied and the browser cookie is cleared where appropriate

### Requirement: Authorization ends without creating a recoverable session
Logout, abandonment, expiration and completion SHALL invalidate the persisted authorization reference and clear the cookie. The system MUST NOT create a session table, refresh token, permanent login, multi-device recovery or automatic restoration of a prior completed cancellation request.

#### Scenario: Citizen exits the current flow
- **WHEN** logout or abandonment is requested
- **THEN** the authorization is invalidated, the cookie is cleared and a new visit begins with a new DNI query

#### Scenario: Authorization expires
- **WHEN** its short lifetime elapses
- **THEN** protected access is blocked even if the request row still exists

#### Scenario: Cancellation request finishes
- **WHEN** a later step finalizes the current request
- **THEN** its flow authorization can no longer reopen the receipt or previous progress

