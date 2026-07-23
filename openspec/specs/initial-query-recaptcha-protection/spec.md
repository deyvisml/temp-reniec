# initial-query-recaptcha-protection Specification

## Purpose
TBD - created by archiving change protect-initial-query-with-recaptcha. Update Purpose after archive.
## Requirements
### Requirement: Initial query uses Google reCAPTCHA v2 Checkbox
The citizen home SHALL render Google reCAPTCHA v2 Checkbox through a maintained React integration using `NEXT_PUBLIC_RECAPTCHA_SITE_KEY`. The widget SHALL remain within the existing DNI form, be responsive without horizontal overflow, keyboard operable, and expose understandable Spanish status and error text. The system MUST NOT implement v3, Enterprise or a second CAPTCHA provider in this change.

#### Scenario: Widget loads normally
- **WHEN** the home opens with a configured public site key and the Google script is available
- **THEN** the checkbox widget is rendered next to the DNI controls and can be completed using keyboard interaction

#### Scenario: Public configuration is missing
- **WHEN** `NEXT_PUBLIC_RECAPTCHA_SITE_KEY` is absent at build/runtime
- **THEN** the form reports that security verification is unavailable and exposes no enabled submission action

#### Scenario: Google script fails
- **WHEN** the widget reports a script or rendering error
- **THEN** the form clears any evidence, blocks submission and presents an accessible temporary-unavailability message

### Requirement: CAPTCHA evidence remains ephemeral and single-use
The frontend SHALL hold the reCAPTCHA token only in component memory and SHALL send it once as `recaptchaToken` in the initial JSON request. It MUST NOT store the token in local storage, session storage, persistent cookies, URLs, logs or reusable application state. The primary action SHALL be enabled only when the DNI is valid, a token is present and no submission is active.

#### Scenario: CAPTCHA has not been completed
- **WHEN** the citizen has a valid DNI but no token
- **THEN** the primary action remains disabled and the backend request is not issued

#### Scenario: Valid evidence is submitted
- **WHEN** DNI and reCAPTCHA are complete and the citizen submits once
- **THEN** exactly one request carries both values in its JSON body and the token is not copied to any browser storage or URL

#### Scenario: Citizen submits repeatedly
- **WHEN** click, Enter or repeated events occur while the request is active
- **THEN** the form sends one request and keeps DNI and CAPTCHA controls unavailable until it settles

### Requirement: Widget resets after every consumed or invalid token
The frontend SHALL clear the token and reset/remount the widget after every completed request attempt, backend CAPTCHA rejection, network uncertainty, widget expiration or widget error. It SHALL never automatically resubmit with the same or a newly generated token.

#### Scenario: Token expires before submission
- **WHEN** the widget emits expiration
- **THEN** the token is cleared, submission is blocked and the citizen is asked to complete verification again

#### Scenario: Backend rejects or cannot confirm evidence
- **WHEN** the backend returns a CAPTCHA rejection, timeout, unavailability or invalid-response error
- **THEN** the widget resets and a new challenge is required before an explicit retry

#### Scenario: Availability request succeeds
- **WHEN** valid evidence allows the availability operation to finish
- **THEN** the used token is cleared before the result action can start another initial query

### Requirement: Backend verification precedes persistence and availability
The initiation use case SHALL depend on an `AntiBotVerificationPort` and SHALL verify reCAPTCHA after DTO validation but before preparing a cancellation request, opening an availability attempt or invoking `CertificateAvailabilityPort`. Provider-specific HTTP models MUST remain inside the Google adapter.

#### Scenario: Evidence is valid
- **WHEN** the anti-bot port accepts the submitted token
- **THEN** the existing SPEC-10 request preparation and availability consultation execute normally

#### Scenario: Evidence is rejected
- **WHEN** the token is absent, invalid, expired, duplicated or associated with a disallowed hostname
- **THEN** the endpoint returns a controlled CAPTCHA error and creates no request, attempt or certificate row and performs no availability call

#### Scenario: Google validation fails technically
- **WHEN** Google is unavailable, times out or returns an invalid response
- **THEN** the endpoint returns the corresponding anti-bot dependency error without preparing a request or interpreting the event as `NOT_AVAILABLE`

### Requirement: Google siteverify response is validated defensively
The Google adapter SHALL POST `secret` and `response` as `application/x-www-form-urlencoded` to the official verification endpoint using a bounded connection/read timeout. It SHALL require `success=true`; when a nonblank `hostname` is returned, it SHALL require an exact case-insensitive match in the configured hostname allowlist. It MUST NOT send or persist the citizen IP, accept a suffix match, log the form, or expose Google payloads.

#### Scenario: Successful allowed response is received
- **WHEN** Google returns valid JSON with `success=true` and an allowed hostname
- **THEN** the adapter accepts the evidence without returning provider data to the use case

#### Scenario: Hostname is not allowed
- **WHEN** Google returns `success=true` with a hostname outside the exact allowlist
- **THEN** the adapter rejects the evidence and the availability service is not called

#### Scenario: Hostname is omitted
- **WHEN** a valid successful response does not include a hostname
- **THEN** the adapter applies no hostname rejection solely for that absence and still enforces every other response check

#### Scenario: Provider body is malformed
- **WHEN** Google returns null, non-JSON or structurally invalid content
- **THEN** the adapter reports `RECAPTCHA_INVALID_RESPONSE` without exposing the body

### Requirement: CAPTCHA errors remain distinct and correlated
The API SHALL preserve the common `ApiError` structure and correlation identifier and SHALL distinguish `RECAPTCHA_REQUIRED`, `RECAPTCHA_REJECTED`, `RECAPTCHA_EXPIRED_OR_DUPLICATE`, `RECAPTCHA_UNAVAILABLE`, `RECAPTCHA_TIMEOUT` and `RECAPTCHA_INVALID_RESPONSE`. Messages SHALL be non-technical and MUST NOT reveal token, secret, hostname configuration or Google error payloads.

#### Scenario: Google reports timeout-or-duplicate
- **WHEN** `siteverify` returns the `timeout-or-duplicate` error code
- **THEN** the API returns `RECAPTCHA_EXPIRED_OR_DUPLICATE` and the frontend requests a new challenge

#### Scenario: Google cannot be reached
- **WHEN** connection or HTTP exchange fails
- **THEN** the API returns `RECAPTCHA_UNAVAILABLE` with the request correlation and no availability result

#### Scenario: Google exceeds the timeout
- **WHEN** verification does not finish within the configured bound
- **THEN** the API returns `RECAPTCHA_TIMEOUT` and no protected operation executes

### Requirement: CAPTCHA configuration fails closed and keeps secrets private
The real backend adapter SHALL require a nonblank externally supplied `RECAPTCHA_SECRET_KEY`, a positive timeout and a nonempty `RECAPTCHA_ALLOWED_HOSTNAMES` allowlist. Invalid real-mode configuration SHALL prevent startup rather than bypass verification. The frontend SHALL expose only the public site key. Repository examples SHALL use placeholders and MUST NOT contain production credentials or a private secret.

#### Scenario: Backend secret is missing
- **WHEN** the real adapter starts without `RECAPTCHA_SECRET_KEY`
- **THEN** startup fails with a configuration message that names the missing property but no secret value

#### Scenario: Browser bundle is inspected
- **WHEN** generated JavaScript and public environment variables are reviewed
- **THEN** they contain the site key but no backend secret, verification response or test bypass

#### Scenario: Production configuration is prepared
- **WHEN** deployment values are supplied
- **THEN** project-specific production keys and institutional hostnames replace all development test configuration externally

### Requirement: Automated and manual verification avoid production dependencies
Backend tests SHALL use a deterministic anti-bot port double for use-case sequencing and a controlled local HTTP server for Google adapter responses. Frontend tests SHALL simulate widget callbacks without requiring Google's script. A separately documented manual check MAY use Google's official v2 test pair in ignored local environment files and MUST warn that those keys are prohibited in production.

#### Scenario: Automated suite runs offline
- **WHEN** Maven and frontend unit suites execute without internet access
- **THEN** valid, invalid, expired/duplicate, hostname, timeout, unavailable, reset and sequencing cases complete without calling Google

#### Scenario: Manual development check is performed
- **WHEN** a contributor follows the local test-key procedure
- **THEN** the real checkbox and backend `siteverify` exchange can be observed without committing either local environment file

#### Scenario: Logs are inspected after tests
- **WHEN** success and every controlled failure path have executed
- **THEN** logs contain correlation and safe error classification but no token or configured secret

