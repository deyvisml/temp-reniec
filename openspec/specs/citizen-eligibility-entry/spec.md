# Citizen Eligibility Entry Specification

## Purpose

Define the citizen-facing home, DNI validation, cancellation-request initiation, and normalized certificate-eligibility consultation that begin the cancellation journey.
## Requirements
### Requirement: Citizen home communicates the service accurately
The `/` route SHALL present an accessible, responsive citizen-facing home page based on `docs/ui-reference/home.png` and SHALL describe the initial action as checking whether certificates are available for cancellation. It MUST NOT state or imply that this stage obtains a detailed list, reveal a certificate count, expose or select individual certificates, or cancel the DNI, civil identity, physical document, DNIe or ID Perú account. Its existing institutional composition, DNI form, feedback, responsive behavior, semantic structure and visible focus SHALL remain intact.

#### Scenario: Citizen opens the home page
- **WHEN** a visitor opens `/` on a supported desktop, intermediate, or mobile viewport
- **THEN** the page presents the service and DNI query without promising or revealing certificate-level information

#### Scenario: Positive result is presented
- **WHEN** the backend confirms that certificates are available
- **THEN** the home offers identity verification without displaying quantity, order number, creation date or UUID

### Requirement: DNI validation is strict and centralized
The frontend and backend SHALL accept a DNI only when it contains exactly eight ASCII digits matching `^[0-9]{8}$`. The backend validation SHALL be authoritative, and both implementations SHALL use one named rule and shared test vectors within their respective codebases.

#### Scenario: Empty DNI is submitted
- **WHEN** the citizen submits an empty DNI
- **THEN** the frontend prevents the request, associates a clear error with the field, and moves focus to the invalid control

#### Scenario: Invalid DNI reaches the backend
- **WHEN** the endpoint receives a DNI with letters, spaces, Unicode digits, fewer than eight digits, more than eight digits, or an oversized payload
- **THEN** it returns the common validation error without creating a request or eligibility attempt

#### Scenario: Valid DNI is submitted
- **WHEN** the input contains exactly eight ASCII digits
- **THEN** the frontend permits submission and the backend accepts the value for case-use processing

### Requirement: Versioned request initiation contract
The backend SHALL expose `POST /api/v1/cancellation-requests` with a JSON body containing an eight-digit DNI and nonblank `recaptchaToken` to create a new request and determine whether at least one certificate is currently available for cancellation. CAPTCHA verification SHALL succeed before any request is created. A functional success response SHALL include the new numeric `requestId`, masked DNI, current request status, normalized `availabilityResult`, `canContinue`, and `nextStep`. It MUST NOT expose or echo CAPTCHA evidence, an eligibility alias, provider boolean, full DNI, certificate collection, count, order number, creation date, UUID, historical request identifier or public-reference UUID. `requestId` MUST NOT authenticate or authorize a caller by itself.

#### Scenario: Availability is confirmed
- **WHEN** a valid DNI and accepted CAPTCHA produce `AVAILABLE`
- **THEN** the endpoint returns a newly created request with `canContinue=true`, `nextStep=IDENTITY_VERIFICATION`, and no CAPTCHA or certificate-level data

#### Scenario: Absence is confirmed
- **WHEN** a valid DNI and accepted CAPTCHA produce `NOT_AVAILABLE`
- **THEN** the endpoint returns a controlled functional response with continuation blocked and no CAPTCHA or certificate-level data

#### Scenario: Request body is malformed
- **WHEN** the endpoint receives invalid JSON, an unsupported content type, an invalid DNI or missing/oversized CAPTCHA evidence
- **THEN** it returns the common API error format with a stable code and correlation identifier without creating a request

#### Scenario: CAPTCHA is rejected
- **WHEN** request fields are valid but backend CAPTCHA verification fails
- **THEN** it returns a controlled CAPTCHA error before request creation or certificate-availability consultation

### Requirement: Eligibility integration is replaceable and normalized
The initial use case SHALL depend on an internal certificate-availability gateway rather than a provider-specific DTO. The external provider's `true` SHALL normalize to `AVAILABLE` and `false` to `NOT_AVAILABLE`. The internal model SHALL also distinguish `INCONCLUSIVE`, `UNAVAILABLE`, and `ERROR`, with only an optional external reference and controlled technical code. Provider exceptions, invalid responses, transport failure and timeout MUST NOT normalize to `NOT_AVAILABLE`. Complete external payloads and certificate objects MUST NOT be persisted or returned.

#### Scenario: Boolean provider responds positively
- **WHEN** the first service returns `true`
- **THEN** the gateway returns `AVAILABLE` without constructing a certificate list

#### Scenario: Boolean provider responds negatively
- **WHEN** the first service returns `false`
- **THEN** the gateway returns `NOT_AVAILABLE` and no error state is inferred

#### Scenario: Provider fails
- **WHEN** the provider times out, is unavailable, returns an invalid result or raises a technical error
- **THEN** the corresponding non-negative normalized outcome is preserved and continuation remains blocked

#### Scenario: Institutional adapter is replaced
- **WHEN** an official first-service contract becomes available
- **THEN** a new adapter can implement the gateway without changing the use-case outcome model or frontend contract

### Requirement: Local eligibility mock is deterministic
Local and test profiles SHALL provide a deterministic availability mock whose normal result for any valid DNI not reserved as a special fixture is `AVAILABLE`. Documented fictitious fixtures SHALL produce `NOT_AVAILABLE`, `INCONCLUSIVE`, `UNAVAILABLE`, timeout and technical error. The mock MUST NOT use randomness, obtain real citizen data, produce certificate counts or return certificate objects, and it MUST NOT be active outside local or test profiles.

#### Scenario: Normal valid DNI is submitted
- **WHEN** the local mock receives a valid DNI that is not a documented special fixture
- **THEN** it deterministically returns `AVAILABLE` so the same DNI can be verified by the selected ID Perú adapter

#### Scenario: Documented alternative fixture is used repeatedly
- **WHEN** the same fictitious DNI reserved for a non-success scenario is submitted more than once under equivalent state
- **THEN** the mock produces the same documented outcome each time

#### Scenario: Positive result is inspected
- **WHEN** availability is confirmed for a normal valid DNI
- **THEN** the result contains no order number, creation date, UUID or certificate collection

### Requirement: Eligibility attempts and current state remain consistent
The backend SHALL prepare, execute, and finalize certificate-availability checks through short transactional phases. Each new request SHALL start its own attempt numbering, persist every started attempt with correlation, invoke external I/O without holding the database lock, and finalize the attempt only while it remains submitted and its request remains current. The initial operation MUST NOT insert or update `cancellation_request_certificate` rows.

#### Scenario: Available result is finalized
- **WHEN** the gateway returns `AVAILABLE` for the current submitted attempt
- **THEN** the attempt is completed, the request stores confirmed availability, transitions to `PENDING_IDENTITY_VERIFICATION`, and has zero certificate rows

#### Scenario: Not-available result is finalized
- **WHEN** the gateway returns `NOT_AVAILABLE`
- **THEN** the attempt is completed, the request transitions to `NO_CERTIFICATES_AVAILABLE`, continuation remains blocked, and no certificate row is created

#### Scenario: Inconclusive result is finalized
- **WHEN** the gateway returns `INCONCLUSIVE`
- **THEN** the attempt preserves that result, continuation remains blocked and the result is not presented as absence

#### Scenario: Dependency is unavailable or times out
- **WHEN** the gateway reports unavailability or exceeds its timeout
- **THEN** the attempt is failed with a controlled code and the API returns the corresponding common error without changing availability to negative

#### Scenario: Late provider response arrives
- **WHEN** a response arrives for an attempt that was failed or whose request became `ABANDONED`
- **THEN** finalization rejects the stale update and creates no certificate data

### Requirement: Eligibility outcomes use accessible separated feedback
The frontend SHALL present every completed availability outcome through the maintained SweetAlert2 modal, visually and semantically separate from the DNI form. The form SHALL remain mounted as background context. Messages MUST distinguish confirmed absence from inconclusive, unavailable, timeout, network and technical outcomes, MUST NOT expose quantities or certificate details, and MUST NOT use a project-owned modal implementation.

#### Scenario: Citizen has no available certificates
- **WHEN** the backend returns `NOT_AVAILABLE`
- **THEN** the modal states concisely that no certificates are currently available for cancellation, blocks continuation and offers a conventional acknowledgement action

#### Scenario: Citizen receives an available result
- **WHEN** the backend returns `AVAILABLE` with `canContinue=true`
- **THEN** the modal offers identity verification without implying that the detailed list has already been obtained

#### Scenario: Citizen receives a retryable result
- **WHEN** the result is inconclusive, unavailable, timed out or affected by network loss
- **THEN** the modal explains that availability could not be confirmed and provides an explicit safe retry without claiming absence

### Requirement: Eligibility outcome dialogs manage focus and dismissal safely
The SweetAlert2 integration SHALL expose an accessible name and description, open as a modal dialog, contain keyboard focus, and restore focus to the DNI field after a reset. Visual status MUST NOT rely only on color. Backdrop clicks MUST NOT dismiss the result, and Escape SHALL map to the safe non-continuing action for the current outcome. Retry and continuation SHALL occur only through explicit actions.

#### Scenario: Modal opens after a submitted consultation
- **WHEN** the pending consultation resolves to a functional result or controlled error
- **THEN** focus moves into the labeled SweetAlert2 modal and assistive technology can identify its result, explanation and available actions without duplicate announcements

#### Scenario: Citizen navigates with the keyboard
- **WHEN** the citizen uses Tab or Shift+Tab while the modal is open
- **THEN** focus remains inside the modal and every available action has a visible focus indicator and a target of at least 44 by 44 pixels

#### Scenario: Citizen dismisses with Escape
- **WHEN** the citizen presses Escape instead of selecting an action
- **THEN** the integration executes the variant's safe reset or return behavior, never continues the citizen flow, never retries automatically, and restores focus to the DNI field when the form is shown

#### Scenario: Citizen clicks the backdrop
- **WHEN** the citizen clicks outside the modal surface
- **THEN** the result remains open so that a terminal or retryable outcome cannot be lost accidentally

#### Scenario: Citizen prefers reduced motion
- **WHEN** the browser reports `prefers-reduced-motion: reduce`
- **THEN** the SweetAlert2 presentation does not use non-essential entrance or exit animation

### Requirement: Eligibility response semantics are explicit
The frontend SHALL distinguish available, not available, inconclusive, service unavailable, timeout, technical backend error, network loss, request-in-progress conflict, protected-operation conflict and concurrency conflict using generated types or stable API codes. Citizen messages SHALL be understandable, non-technical and non-enumerating. Only `AVAILABLE` with `canContinue=true` SHALL authorize the identity-verification action.

#### Scenario: DNI has no available certificates
- **WHEN** the client receives `NOT_AVAILABLE`
- **THEN** it blocks continuation and does not reinterpret the result as an error

#### Scenario: Result cannot be confirmed
- **WHEN** the client receives inconclusive, unavailable, timeout, technical or network failure
- **THEN** it offers only safe retry or acknowledgement and does not claim that no certificates exist

#### Scenario: Available result authorizes continuation
- **WHEN** the backend returns `AVAILABLE`, `canContinue=true` and `nextStep=IDENTITY_VERIFICATION`
- **THEN** the frontend offers continuation without certificate details and without placing the DNI in the URL

### Requirement: Duplicate frontend submissions are prevented
The DNI form SHALL allow only one active submission per mounted form instance. It SHALL disable the primary action, expose a programmatic busy state, ignore repeated submit events until completion, and cancel in-flight work when the component is unmounted.

#### Scenario: Citizen activates submit twice
- **WHEN** the primary action is clicked or triggered repeatedly while a request is pending
- **THEN** only one HTTP request is issued and the loading state is announced accessibly

#### Scenario: Page is left during submission
- **WHEN** the form unmounts while the request is active
- **THEN** the client aborts the request and does not update stale UI state

### Requirement: Continuation is emitted only for eligible requests
The backend SHALL set `canContinue=true` and `nextStep=IDENTITY_VERIFICATION` only when certificate availability is positively confirmed for the current active request. Negative, inconclusive, unavailable, timeout and technical outcomes SHALL block continuation. The frontend SHALL render the identity-verification step within the canonical `/cancelacion` route and SHALL NOT place the request identifier, DNI, certificate data or step name in the URL.

#### Scenario: Positive availability authorizes transition
- **WHEN** the frontend receives the expected positive response
- **THEN** it renders identity verification within `/cancelacion` without changing the visible URL or exposing request data

#### Scenario: Any other result is received
- **WHEN** availability is negative or cannot be confirmed
- **THEN** the frontend exposes no continuation control, remains at `/cancelacion` and presents the controlled outcome

### Requirement: DNI exposure is minimized outside MySQL
The system SHALL persist the DNI only in the consolidated request column, derive masked presentation from it, and MUST NOT place the full value in logs, metrics, error bodies, URLs, OpenAPI examples, browser storage, cookies, or technical endpoints. The frontend SHALL clear the input value after a terminal response or controlled reset.

#### Scenario: Application logs are inspected
- **WHEN** initiation succeeds, fails validation, times out, conflicts, or raises an unexpected error
- **THEN** logs contain correlation and technical state but not the submitted DNI

#### Scenario: Browser storage and history are inspected
- **WHEN** the citizen completes or abandons the initial consultation
- **THEN** the DNI is absent from local storage, session storage, cookies, and visited URLs

### Requirement: Initial anti-abuse controls remain simple
The endpoint SHALL enforce Google reCAPTCHA v2 Checkbox verification, strict media type, bounded body and token size, DNI format, bounded external timeouts, active-request deduplication, and in-progress attempt deduplication. The change MUST NOT introduce an in-memory per-IP limiter presented as a production control, fingerprinting, automatic CAPTCHA retries or a circuit breaker used only by this integration; final distributed rate limiting and perimeter controls SHALL remain documented for a later infrastructure decision.

#### Scenario: Automated submission lacks accepted CAPTCHA
- **WHEN** a client submits valid-looking DNI data without evidence accepted by the backend
- **THEN** no request, availability attempt or provider call is created

#### Scenario: Automated duplicate burst has accepted CAPTCHA
- **WHEN** multiple equivalent submissions overlap after valid anti-bot verification
- **THEN** frontend guarding, database serialization, and in-progress conflict handling prevent multiplication of active requests and availability-provider calls

### Requirement: Functional contract remains synchronized
The OpenAPI document SHALL describe initiation of a new request and certificate-existence query, required `recaptchaToken`, the normalized availability response, numeric `requestId`, correlation header, and every expected common and CAPTCHA error. It MUST NOT expose token examples containing real evidence, the backend secret, `eligibilityResult`, a raw provider boolean, a certificate collection, count, order number, creation date or UUID. Frontend generated types SHALL be regenerated from that document and contract drift checks SHALL remain mandatory.

#### Scenario: Initial contract is inspected
- **WHEN** the OpenAPI operation and schemas are reviewed
- **THEN** they require CAPTCHA evidence, identify existence-only semantics and contain no secret, reusable token or certificate-level response property

#### Scenario: Generated contract is stale
- **WHEN** the snapshot or TypeScript declarations omit `recaptchaToken`, retain the old eligibility field or include detailed certificate data
- **THEN** contract verification fails

### Requirement: End-to-end behavior is verified at appropriate layers
Tests SHALL cover backend validation and orchestration, frontend result mapping, MySQL persistence and concurrency, deterministic availability mock outcomes, OpenAPI drift, and a real frontend-to-backend-to-MySQL initial query. They SHALL verify positive, negative, inconclusive, unavailable, timeout and technical-error paths; continuation only after positive confirmation; and zero certificate rows after every initial-query scenario.

#### Scenario: Isolated test suites run
- **WHEN** backend and frontend baseline tests execute
- **THEN** validation, outcome mapping, duplicate-submit protection, mock fixtures and absence of certificate details are verified with controlled dependencies

#### Scenario: Persistence integration suite runs
- **WHEN** Testcontainers starts MySQL and executes initial-query scenarios
- **THEN** requests and availability attempts are persisted correctly while `cancellation_request_certificate` remains empty for those requests

#### Scenario: Contract and privacy checks run
- **WHEN** OpenAPI, generated types, logs, URLs and browser storage are inspected
- **THEN** the initial flow exposes neither UUID nor other individual certificate data and never stores the DNI in browser storage or URLs

### Requirement: Every home-page entry starts a new journey
Submitting a DNI from the home page SHALL express a new cancellation intention. The backend SHALL query current certificates for a newly created request and MUST NOT resume a prior request based on DNI, elapsed time, browser, device, or previous status. Continuing immediately from the response belongs to the same current journey and SHALL NOT be described as recovery.

#### Scenario: Previous pre-confirmation journey exists
- **WHEN** the citizen returns to the home page and submits the same DNI after leaving a request before confirmation
- **THEN** the old request is abandoned and the response belongs to a new request with a new eligibility consultation

#### Scenario: Previous completed journey exists
- **WHEN** the citizen previously reached the constancia and later submits the same DNI again
- **THEN** the system starts from current certificate consultation and never returns the old constancia as the active screen

#### Scenario: One certificate was previously left unselected
- **WHEN** a prior request canceled only some certificates and the citizen initiates again
- **THEN** the new request uses a fresh provider result so remaining currently valid certificates can participate independently

#### Scenario: Browser storage is inspected
- **WHEN** the citizen closes or reloads the application
- **THEN** no DNI, request progress, or restoration token is read from `localStorage` or `sessionStorage` to resume the prior journey

### Requirement: Unsafe overlapping operations are blocked without recovery
A new initiation SHALL be rejected while the same DNI has a live eligibility call that is not stale or a confirmed revocation whose outcome remains active or uncertain. The response MUST use a stable generic error and correlation identifier and MUST NOT return the previous `requestId`, step, certificates, selection, or constancia.

#### Scenario: Eligibility call is active
- **WHEN** another submission arrives while the current eligibility call is within its active threshold
- **THEN** the backend returns the existing in-progress conflict and creates no second request or provider call

#### Scenario: Revocation is active or uncertain
- **WHEN** another submission arrives while a confirmed operation is executing or its outcome is unknown
- **THEN** the backend blocks initiation without recovering or exposing the previous journey

#### Scenario: Prior revocation becomes terminal
- **WHEN** the previous operation reaches a confirmed terminal result and the citizen initiates again
- **THEN** a new request is allowed and historical operation and constancia rows remain unchanged

### Requirement: Positive availability creates only a current-browser identity handoff
The initial query SHALL treat `AVAILABLE` as permission to begin identity verification, not as verified identity or authorization for certificate listing. Its successful response SHALL establish the short-lived current-browser continuation required by the identity step, and the frontend SHALL navigate to `/verificacion-identidad` without DNI, request ID or certificate data in the URL.

#### Scenario: Availability is positive
- **WHEN** the protected initial query returns `AVAILABLE`
- **THEN** the browser can enter the identity page but cannot access the post-authentication listing boundary

#### Scenario: Availability is not positive
- **WHEN** the initial result is negative, inconclusive or technical failure
- **THEN** no identity continuation is established and ID Perú cannot be started

#### Scenario: Continuation is copied to another browser
- **WHEN** a citizen opens the identity route elsewhere without the HttpOnly continuation
- **THEN** the prior request is not recovered and the initial query must be repeated
