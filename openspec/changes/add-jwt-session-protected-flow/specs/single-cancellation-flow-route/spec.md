## MODIFIED Requirements

### Requirement: The citizen flow separates public entry from one canonical internal route
The frontend SHALL expose `/` as the public citizen home containing only service introduction, DNI, reCAPTCHA and initial availability consultation. It SHALL expose `/cancelacion` as the single canonical route for identity verification and every later internal step. Internal transitions SHALL render within `/cancelacion` without changing the visible path.

#### Scenario: Citizen opens the application root
- **WHEN** the citizen navigates to `/` without an active session
- **THEN** the frontend displays the public initial consultation

#### Scenario: Positive initiation completes
- **WHEN** the backend creates a session after confirming availability
- **THEN** the frontend navigates to `/cancelacion` and displays the authorized identity step

#### Scenario: Citizen advances between internal steps
- **WHEN** the backend authorizes progression to another step
- **THEN** the frontend renders that step while the browser path remains exactly `/cancelacion`

### Requirement: Flow state is not encoded in the URL
The frontend MUST NOT place the current step, DNI, request identifier, certificate data, authentication result, authorization code, token or technical state in the path, query string or fragment. The active internal view SHALL be derived from backend-validated session and request context.

#### Scenario: A successful availability check advances to identity verification
- **WHEN** certificate availability is positively confirmed and session cookies are issued
- **THEN** the identity view is shown at `/cancelacion` without sensitive or technical URL parameters

#### Scenario: The canonical route is opened without active context
- **WHEN** `/cancelacion` is loaded without a valid or renewable session
- **THEN** the server redirects to public `/` and does not infer a later step from browser-controlled data

#### Scenario: A completed or expired flow revisits the canonical route
- **WHEN** the citizen loads `/cancelacion` after session or request termination
- **THEN** the frontend returns to public home and does not restore the final or previous step

### Requirement: ID Perú returns to the canonical protected route
The backend SHALL redirect the browser after every processed ID Perú callback to the fixed frontend return URI configured for the current environment. Production SHALL use protected `/cancelacion`. Local development MAY use the registered compatibility route `/autorizacion`; that route MUST validate the same session, render the same cancellation-flow orchestrator and MUST NOT maintain an independent step implementation. The OAuth redirect URI SHALL remain the backend callback, and frontend state SHALL come from the session rather than URL parameters.

#### Scenario: ID Perú authentication succeeds in production
- **WHEN** the production backend validates the callback and elevates the active session
- **THEN** the browser returns to `/cancelacion` and resolves the authorized step 2 transition

#### Scenario: ID Perú authentication succeeds in registered local development
- **WHEN** local ID Perú returns to the registered frontend origin `/autorizacion`
- **THEN** that protected compatibility route validates the same session and renders the same authorized flow state

#### Scenario: ID Perú returns a controlled failure
- **WHEN** the backend processes cancellation, rejection, mismatch, expiration or technical failure
- **THEN** the browser returns to the fixed protected route, retains step 1 only while the session remains active and presents the controlled outcome

#### Scenario: Redirect target is manipulated
- **WHEN** callback input includes or attempts to influence a return URI
- **THEN** the backend ignores it and redirects only to the configured allowlisted frontend URI
