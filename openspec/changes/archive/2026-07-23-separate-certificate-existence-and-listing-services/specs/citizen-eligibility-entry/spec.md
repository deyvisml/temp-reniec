## MODIFIED Requirements

### Requirement: Citizen home communicates the service accurately
The `/` route SHALL present an accessible, responsive citizen-facing home page based on `docs/ui-reference/home.png` and SHALL describe the initial action as checking whether certificates are available for cancellation. It MUST NOT state or imply that this stage obtains a detailed list, reveal a certificate count, expose or select individual certificates, or cancel the DNI, civil identity, physical document, DNIe or ID Perú account. Its existing institutional composition, DNI form, feedback, responsive behavior, semantic structure and visible focus SHALL remain intact.

#### Scenario: Citizen opens the home page
- **WHEN** a visitor opens `/` on a supported desktop, intermediate, or mobile viewport
- **THEN** the page presents the service and DNI query without promising or revealing certificate-level information

#### Scenario: Positive result is presented
- **WHEN** the backend confirms that certificates are available
- **THEN** the home offers identity verification without displaying quantity, order number, creation date or UUID

### Requirement: Versioned request initiation contract
The backend SHALL expose `POST /api/v1/cancellation-requests` with a JSON DNI body to create a new request and determine whether at least one certificate is currently available for cancellation. A functional success response SHALL include the new numeric `requestId`, masked DNI, current request status, normalized `availabilityResult`, `canContinue`, and `nextStep`. It MUST NOT expose an eligibility alias, provider boolean, full DNI, certificate collection, count, order number, creation date, UUID, historical request identifier or public-reference UUID. `requestId` MUST NOT authenticate or authorize a caller by itself.

#### Scenario: Availability is confirmed
- **WHEN** a valid DNI produces `AVAILABLE`
- **THEN** the endpoint returns a newly created request with `canContinue=true`, `nextStep=IDENTITY_VERIFICATION`, and no certificate-level data

#### Scenario: Absence is confirmed
- **WHEN** a valid DNI produces `NOT_AVAILABLE`
- **THEN** the endpoint returns a controlled functional response with continuation blocked and no certificate-level data

#### Scenario: Request body is malformed
- **WHEN** the endpoint receives invalid JSON or an unsupported content type
- **THEN** it returns the common API error format with a stable code and correlation identifier

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
Local and test profiles SHALL provide a deterministic availability mock with documented fictitious DNI fixtures for `AVAILABLE`, `NOT_AVAILABLE`, `INCONCLUSIVE`, `UNAVAILABLE`, timeout and technical error. It MUST NOT use randomness, real citizen data, certificate counts or certificate objects, and its default result SHALL be documented.

#### Scenario: Documented fixture is used repeatedly
- **WHEN** the same fictitious DNI is submitted more than once under equivalent state
- **THEN** the mock produces the same availability outcome each time

#### Scenario: Positive fixture is inspected
- **WHEN** the fixture configured as available is executed
- **THEN** its result contains no order number, creation date, UUID or certificate collection

#### Scenario: Unlisted valid DNI is submitted
- **WHEN** the local mock receives a valid DNI not listed as a special fixture
- **THEN** it returns the documented deterministic default result

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

### Requirement: Continuation is emitted only for eligible requests
The backend SHALL set `canContinue=true` and `nextStep=IDENTITY_VERIFICATION` only when certificate availability is positively confirmed for the current active request. Negative, inconclusive, unavailable, timeout and technical outcomes SHALL block navigation. The frontend SHALL prepare `/verificacion-identidad` using only `requestId` and SHALL never include the DNI or certificate data in the URL.

#### Scenario: Positive availability authorizes transition
- **WHEN** the frontend receives the expected positive response
- **THEN** it enables the controlled transition using `requestId` and no DNI or certificate value

#### Scenario: Any other result is received
- **WHEN** availability is negative or cannot be confirmed
- **THEN** the frontend exposes no continuation control and does not navigate

### Requirement: Functional contract remains synchronized
The OpenAPI document SHALL describe initiation of a new request and certificate-existence query, the normalized availability response, numeric `requestId`, correlation header, and every expected common error. It MUST NOT expose `eligibilityResult`, a raw provider boolean, a certificate collection, count, order number, creation date or UUID. Frontend generated types SHALL be regenerated from that document and contract drift checks SHALL remain mandatory.

#### Scenario: Initial contract is inspected
- **WHEN** the OpenAPI operation and schemas are reviewed
- **THEN** they clearly identify existence-only semantics and contain no certificate-level response property

#### Scenario: Generated contract is stale
- **WHEN** the snapshot or TypeScript declarations retain the old eligibility field or detailed certificate data
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
