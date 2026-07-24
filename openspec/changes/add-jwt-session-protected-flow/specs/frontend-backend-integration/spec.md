## MODIFIED Requirements

### Requirement: Identity and session API contracts are versioned and synchronized
The backend SHALL expose versioned contracts for current session, renewal, logout, starting identity verification and processing the provider callback. OpenAPI SHALL document request/response models, cookie effects, applicable cookie security schemes, status codes, allowed step values and normalized errors. Generated TypeScript types SHALL be regenerated from the validated contract, while token values and secrets MUST NOT appear in examples or response schemas.

#### Scenario: Developer inspects Swagger UI
- **WHEN** the session and identity API groups are opened
- **THEN** each operation, cookie requirement, state, field and principal error is described without exposing token material or suggesting browser-managed bearer tokens

#### Scenario: Contract generation runs
- **WHEN** OpenAPI changes are accepted
- **THEN** frontend generated types match the backend and handwritten duplicate session DTOs are not introduced

### Requirement: Frontend HTTP supports secure session continuity
The centralized HTTP client SHALL retain native fetch, correlation, typed JSON/error behavior and `credentials: include` for browser session calls. Server-rendered guards SHALL forward only the incoming Cookie header to the configured backend current-session operation. A session-specific coordinator MAY perform at most one controlled refresh when access has expired; the general HTTP client MUST NOT read cookies, decode JWT, persist tokens, add broad interceptors or apply automatic retries to arbitrary requests.

#### Scenario: Protected browser call succeeds
- **WHEN** the browser has valid HttpOnly session cookies
- **THEN** native fetch sends them without exposing their values to application JavaScript

#### Scenario: Server guard validates an internal page
- **WHEN** Next.js renders a protected route
- **THEN** it asks the backend for current session using only server-obtained cookies and trusts the validated response rather than cookie presence

#### Scenario: Access requires renewal
- **WHEN** the backend reports expired access while refresh may remain valid
- **THEN** the session coordinator attempts one renewal and does not create a general retry loop

#### Scenario: Protected call remains unauthorized
- **WHEN** cookies are absent, invalid, expired or revoked after any permitted renewal
- **THEN** the client maps the stable session error to controlled navigation to public home without exposing token details

## ADDED Requirements

### Requirement: OpenAPI declares implemented cookie security only
OpenAPI SHALL define distinct cookie authentication schemes for access and refresh only after those mechanisms exist. Protected operations SHALL reference access-cookie security, renewal SHALL reference refresh-cookie security, and public initiation and ID Perú callback SHALL remain explicitly public. Swagger documentation MUST NOT claim that users paste JWTs manually.

#### Scenario: OpenAPI security is inspected
- **WHEN** the generated document is reviewed after session implementation
- **THEN** operation security matches runtime access rules and no nonexistent OAuth or bearer workflow is documented
