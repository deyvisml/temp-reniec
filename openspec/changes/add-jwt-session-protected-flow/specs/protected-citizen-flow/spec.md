## ADDED Requirements

### Requirement: Home is public and internal flow pages require backend-validated session
The `/` page SHALL remain public. Every page from identity step 1 onward SHALL validate the session server-side before rendering protected content. Cookie presence alone MUST NOT be treated as valid authentication. A visitor without an active or renewable session SHALL be redirected to `/` without rendering internal request data.

#### Scenario: Visitor opens home without session
- **WHEN** `/` is requested without a valid active session
- **THEN** the public service explanation, DNI form and reCAPTCHA are rendered

#### Scenario: Citizen opens an internal route without session
- **WHEN** `/cancelacion` or a supported internal compatibility route is requested without valid session context
- **THEN** the server redirects to `/` before protected content is rendered

#### Scenario: Active citizen opens home
- **WHEN** `/` is requested with a valid active or renewable session
- **THEN** the server redirects to the authorized internal flow route

### Requirement: Backend state determines the only permitted step
The internal flow SHALL obtain the permitted step from the current-session contract and render only that step. Frontend state, URL input, request identifiers or stepper interaction MUST NOT authorize a transition. Backend endpoints for later steps SHALL independently enforce the same state transition rule.

#### Scenario: Citizen attempts a future step directly
- **WHEN** the request state permits only identity verification but the browser or API requests a later step
- **THEN** the later step is blocked with a stable forbidden-step outcome

#### Scenario: Request advances legitimately
- **WHEN** a backend use case commits the transition to a new step
- **THEN** all tabs resolve the newly permitted step on their next state synchronization

### Requirement: Internal pages share an authenticated layout
All internal flow views SHALL use one common responsive and accessible layout containing the institutional header, an authenticated profile with the full DNI and a clearly labeled logout action. The full DNI SHALL be obtained only from the backend-validated current-session contract and MUST NOT be included in JWT, cookies, URLs or browser storage. The layout SHALL preserve semantic landmarks, keyboard navigation, visible focus and the current five-step indicator without exposing technical session identifiers.

#### Scenario: Step 1 is rendered
- **WHEN** an authenticated session opens the internal flow
- **THEN** the common header shows the authenticated citizen profile with the full DNI and the logout action while the body shows the authorized step

#### Scenario: Keyboard user reaches logout
- **WHEN** the user navigates the internal header by keyboard
- **THEN** logout receives visible focus and has an accessible name

### Requirement: Reloads and multiple tabs converge on persisted state
Internal views SHALL treat backend session and request state as the source of truth on initial render, reload, focus recovery and controlled mutation completion. Multiple tabs SHALL share cookies but MUST NOT use browser storage or independent client state as authority. Session renewal races SHALL resolve without duplicating domain operations.

#### Scenario: Two tabs are open on the active request
- **WHEN** one tab advances the persisted request state
- **THEN** the other tab resolves the same allowed step when it reloads or synchronizes

#### Scenario: Access expires during an active request
- **WHEN** refresh remains valid at server-side navigation or an authorized API boundary
- **THEN** the application performs one controlled renewal and continues without exposing tokens

#### Scenario: Session can no longer renew
- **WHEN** both access and refresh are invalid, expired or revoked
- **THEN** all tabs return to public home and no prior flow data is rendered

### Requirement: Authenticated mutations resist cross-site requests
Every cookie-authenticated state-changing API SHALL require an exact allowed `Origin` in addition to authenticated session state and secure cookie attributes. Public ID Perú callback processing SHALL remain exempt from cookie authentication and SHALL continue to require its single-use state validation. State changes MUST NOT use HTTP GET.

#### Scenario: Allowed frontend submits logout
- **WHEN** the configured frontend origin sends authenticated POST logout
- **THEN** the backend processes it and returns expired cookies

#### Scenario: Foreign origin submits a mutation
- **WHEN** a non-allowlisted origin attempts a cookie-authenticated POST
- **THEN** the request is rejected before the domain use case executes

### Requirement: Protected navigation failures are safe and understandable
Expired, revoked, malformed or missing sessions SHALL produce stable backend errors and safe frontend redirects. The UI SHALL distinguish a normal ended session from an unexpected communication failure without displaying token, stack, database or provider details.

#### Scenario: Session expires while viewing an internal step
- **WHEN** the next state request determines that renewal is impossible
- **THEN** the citizen is returned to home with a concise session-ended message

#### Scenario: Backend is temporarily unavailable
- **WHEN** server-side validation cannot reach the backend
- **THEN** the application renders a safe retryable technical state and does not assume the session is valid or destroyed
