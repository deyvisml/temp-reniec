# single-cancellation-flow-route Specification

## Purpose
TBD - created by archiving change use-single-cancellation-flow-route. Update Purpose after archive.
## Requirements
### Requirement: The citizen flow has one canonical route
The frontend SHALL expose `/cancelacion` as the single canonical browser route for the citizen certificate-cancellation flow. The initial consultation, identity verification, certificate selection, reason, confirmation and receipt views SHALL render within this route without changing the visible path between internal steps.

#### Scenario: Citizen opens the application root
- **WHEN** the citizen navigates to `/`
- **THEN** the frontend redirects to `/cancelacion` and displays the appropriate initial view

#### Scenario: Citizen advances between internal steps
- **WHEN** the backend authorizes progression to another step
- **THEN** the frontend renders that step while the browser path remains exactly `/cancelacion`

### Requirement: Flow state is not encoded in the URL
The frontend MUST NOT place the current step, DNI, request identifier, certificate data, authentication result, authorization code, token or technical state in the path, query string or fragment. The active view SHALL be derived from ephemeral presentation state and backend-validated current context.

#### Scenario: A successful availability check advances to identity verification
- **WHEN** certificate availability is positively confirmed
- **THEN** the identity verification view is shown at `/cancelacion` without sensitive or technical URL parameters

#### Scenario: The canonical route is opened without active context
- **WHEN** `/cancelacion` is loaded without a valid current flow context
- **THEN** the frontend displays the initial consultation and does not infer a later step from browser-controlled data

#### Scenario: A completed or expired flow revisits the canonical route
- **WHEN** the citizen loads `/cancelacion` after the temporary context has completed or expired
- **THEN** the frontend does not restore the final or previous step as historical progress

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

### Requirement: Legacy frontend routes canonicalize safely
The frontend SHALL redirect `/verificacion-identidad` and `/verificacion-identidad/retorno` to `/cancelacion`. These legacy routes MUST NOT retain independent step implementations or accept browser parameters as proof of authentication.

#### Scenario: A legacy identity link is opened
- **WHEN** the citizen navigates to either legacy identity route
- **THEN** the browser ends at `/cancelacion` and the current view is resolved from valid backend context

### Requirement: Route decisions are centralized and tested
The frontend SHALL maintain one shared definition for the canonical flow route and SHALL use it for internal navigation, redirects and configured return behavior. Automated tests SHALL verify canonical redirects, stable navigation and the absence of sensitive URL data.

#### Scenario: A route-dependent component is changed
- **WHEN** the frontend builds and its navigation tests run
- **THEN** route consumers resolve `/cancelacion` from the shared definition and no active flow transition targets a step-specific route

