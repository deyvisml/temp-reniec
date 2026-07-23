## MODIFIED Requirements

### Requirement: Callback processing is strict and provider-compatible
The backend SHALL expose the registered callback through HTTP GET query parameters and HTTP POST form data for `code`, `state`, `session_state` and optional `error`. Both transports SHALL invoke the same use case, which SHALL validate and atomically consume state before exchanging a code, enforce the documented field rules for the configured ID Perú version, and finish with HTTP 303 to a fixed configured frontend return route without tokens, codes, state, session state, DNI, provider messages or technical details in the URL or response body. Controlled callback failures MUST redirect to the frontend instead of exposing the common API error document in the browser.

#### Scenario: Valid GET callback arrives from ID Perú v1
- **WHEN** code, matching unexpired state and fields required by v1 arrive once through HTTP GET
- **THEN** the attempt is reserved atomically, token exchange proceeds and the browser receives a safe frontend redirect

#### Scenario: Valid POST callback arrives
- **WHEN** the provider submits a valid callback as form data through HTTP POST
- **THEN** the same validation and callback use case executes and the browser receives the same safe redirect behavior

#### Scenario: Provider reports cancellation
- **WHEN** a callback contains a valid state and a cancellation error
- **THEN** the attempt becomes `CANCELLED`, continuation remains blocked, no token exchange occurs and the browser is redirected to the configured frontend route

#### Scenario: State is unknown
- **WHEN** a callback contains an absent, malformed, expired, reused or unknown state
- **THEN** no token exchange or authorization occurs and the browser is redirected to a generic safe recovery state without revealing whether a request exists

#### Scenario: Callback omits required successful fields
- **WHEN** no provider error is present but a field required by the configured ID Perú version is absent or malformed
- **THEN** no partial authentication is accepted and the browser returns to the frontend with a controlled failure resolved from backend state

#### Scenario: Callback redirect is inspected
- **WHEN** any successful or controlled callback result generates its response
- **THEN** `Location`, response headers and response body contain no authorization code, state, session state, token, DNI or provider diagnostic

### Requirement: Step 1 presents an accessible five-step identity experience
The frontend SHALL render identity verification inside the configured citizen-flow route using the current five-step flow and the composition of `docs/ui-reference/step-1.png`. It SHALL explain that ID Perú verifies identity and that no certificate is cancelled in this step. After returning from the provider, it SHALL derive the outcome from backend-validated temporary context rather than browser parameters. A verified result SHALL activate a minimal step 2 transition without inventing certificate data or selection behavior; cancellation, rejection, mismatch, expiration and technical failures SHALL retain step 1 and present one accessible, non-technical SweetAlert2 notice with only valid recovery actions.

#### Scenario: Citizen opens the identity step
- **WHEN** a valid current-browser continuation is available
- **THEN** the page shows step 1 of 5 and one clear action to begin ID Perú authentication

#### Scenario: Citizen activates authentication twice
- **WHEN** the start control is triggered repeatedly while a request is pending
- **THEN** only one backend start call occurs and the busy state is announced

#### Scenario: Authentication returns successfully
- **WHEN** the callback has established a verified temporary authorization
- **THEN** the frontend activates step 2 and renders only a minimal verified transition state until the detailed certificate-list capability is implemented

#### Scenario: Authentication fails safely
- **WHEN** ID Perú is cancelled, rejects, expires, mismatches, times out or is unavailable
- **THEN** the frontend remains on step 1, shows exactly one citizen-facing alert for that outcome and permits only a controlled retry or clean restart

#### Scenario: Citizen refreshes after an alert
- **WHEN** an already presented callback failure state is rendered again without a new callback
- **THEN** the frontend does not open duplicate alerts or submit a new authentication automatically

#### Scenario: Browser parameters attempt to force success
- **WHEN** a citizen adds an apparent authentication outcome to the URL
- **THEN** the frontend ignores it and advances only when the backend confirms valid temporary authorization
