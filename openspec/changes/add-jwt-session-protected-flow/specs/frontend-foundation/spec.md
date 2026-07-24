## MODIFIED Requirements

### Requirement: Functional citizen home page
The `/` route SHALL render the real public citizen-facing home and DNI availability form specified by `citizen-eligibility-entry` only when no valid active session exists. It SHALL preserve the root App Router shell, semantic landmarks, Spanish metadata, global error boundaries, keyboard access, responsive layout and safe behavior when the backend is unavailable. When server-side session validation confirms an active operation, `/` SHALL redirect to the protected internal route instead of creating or recovering another journey from frontend state.

#### Scenario: Citizen home is rendered
- **WHEN** a visitor opens `/` without an active session
- **THEN** the page renders the service explanation, accessible DNI form and reCAPTCHA within the public shell

#### Scenario: Active session opens home
- **WHEN** server-side validation confirms a current active session
- **THEN** the visitor is redirected to the authorized internal flow without seeing or resubmitting the DNI form

#### Scenario: Backend is unavailable
- **WHEN** session validation or a submitted consultation cannot reach the backend
- **THEN** the page fails closed with a controlled retryable state and does not assume authentication or destroy cookies from JavaScript

### Requirement: Native JSON HTTP client foundation
The frontend SHALL retain one small fetch-based `requestJson<T>` client and typed `HttpClientError`. The client SHALL resolve relative API paths against server-only `BACKEND_URL` on the server and `NEXT_PUBLIC_BACKEND_URL` in the browser; preserve caller options and headers; request JSON; use `credentials: "include"`; generate and send a valid `X-Correlation-ID` unless supplied; return parsed or empty success data; enforce the default timeout; and respect caller cancellation. It SHALL interpret the generated backend error shape. JWT renewal SHALL be isolated in a session-specific coordinator with at most one controlled attempt and MUST NOT become a general interceptor or automatic retry facility. The frontend MUST NOT read cookies, decode JWT, use browser storage for session state, log bodies or add a third-party HTTP library for this capability.

#### Scenario: Successful JSON response contains correlation
- **WHEN** fetch returns an OK JSON response with `X-Correlation-ID`
- **THEN** the client returns the typed data and the same correlation identifier

#### Scenario: Backend returns a structured error
- **WHEN** fetch returns a non-success JSON response using the backend error contract
- **THEN** the client throws `HttpClientError` with the public code, message, status and correlation identifier

#### Scenario: Request reaches timeout or cancellation
- **WHEN** timeout or the caller aborts the request
- **THEN** the client returns the existing distinct typed outcome and does not trigger session refresh for that reason

#### Scenario: Session cookies are transported
- **WHEN** browser code invokes a protected endpoint
- **THEN** fetch includes credentials while no token is available to application JavaScript

#### Scenario: Server-side validation is performed
- **WHEN** a protected App Router page resolves its session
- **THEN** server-only code forwards the request cookies to the backend without exposing them in rendered props
