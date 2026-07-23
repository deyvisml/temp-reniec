## ADDED Requirements

### Requirement: Identity API contracts are versioned and synchronized
The backend SHALL expose versioned contracts for starting identity verification, processing the provider callback, reading current identity state and invalidating local authorization. OpenAPI SHALL document request/response models, form fields, cookies, status codes and normalized errors, and generated TypeScript types SHALL be regenerated from the validated contract.

#### Scenario: Developer inspects Swagger UI
- **WHEN** the identity API group is opened
- **THEN** each operation, state, field, cookie effect and principal error is described without exposing secrets or suggesting nonexistent security mechanisms

#### Scenario: Contract generation runs
- **WHEN** OpenAPI changes are accepted
- **THEN** frontend generated types match the backend and handwritten duplicate DTOs are not introduced

### Requirement: Frontend HTTP supports secure credential continuity
The centralized HTTP client SHALL support `credentials: include` for same-project identity and protected-flow calls, retain correlation identifiers and existing JSON/error handling, and handle the provider callback only through browser navigation. It MUST NOT attempt to read, copy or persist the HttpOnly cookie.

#### Scenario: Identity start succeeds
- **WHEN** the frontend calls the start endpoint with a valid continuation cookie
- **THEN** it receives only the authorization URL and correlation-safe response data before navigating

#### Scenario: Protected call is unauthorized
- **WHEN** the cookie is absent, invalid, expired or revoked
- **THEN** the client maps the standard API error to a controlled restart message without exposing raw token details

### Requirement: Identity integration errors remain normalized
Provider rejection, cancellation, mismatch, expired/replayed state, invalid callback, token failure, JWT failure, timeout, unavailability, invalid configuration and unauthorized flow access SHALL use stable backend codes and the existing common error envelope. Provider payloads and exceptions MUST NOT be returned to the browser.

#### Scenario: Provider service times out
- **WHEN** a real token, userinfo or JWKS call exceeds its timeout
- **THEN** the client receives a stable temporary-error code, timestamp, path and correlation ID but no endpoint credentials or token material

#### Scenario: Identity differs
- **WHEN** the authenticated DNI does not match the request
- **THEN** the client receives a dedicated controlled outcome without either DNI value

