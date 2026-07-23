## MODIFIED Requirements

### Requirement: ID Perú returns to the canonical route
The backend SHALL redirect the browser after every processed ID Perú callback to the fixed frontend return URI configured for the current environment. Production SHALL use the canonical `/cancelacion` route. Local development MAY use the registered compatibility route `/autorizacion`; that route MUST render the same cancellation-flow orchestrator and MUST NOT maintain an independent step implementation. The OAuth redirect URI SHALL remain the backend callback, and the frontend SHALL obtain the authentication outcome from backend-validated temporary context rather than URL parameters.

#### Scenario: ID Perú authentication succeeds in production
- **WHEN** the production backend validates the callback and creates temporary authorization
- **THEN** the browser returns to `/cancelacion` and the frontend resolves the authorized step 2 transition

#### Scenario: ID Perú authentication succeeds in registered local development
- **WHEN** the local backend uses the registered frontend origin `/autorizacion` and validates the callback
- **THEN** the browser returns to `/autorizacion`, which renders the same flow controller and resolves the authorized step 2 transition

#### Scenario: ID Perú returns a controlled failure
- **WHEN** the backend processes a cancellation, rejection, mismatch, expiration or technical authentication failure
- **THEN** the browser returns to the fixed frontend route without provider details and the shared flow controller retains step 1 and presents the controlled outcome

#### Scenario: Redirect target is manipulated
- **WHEN** callback input includes or attempts to influence a return URI
- **THEN** the backend ignores it and redirects only to the configured allowlisted frontend URI
