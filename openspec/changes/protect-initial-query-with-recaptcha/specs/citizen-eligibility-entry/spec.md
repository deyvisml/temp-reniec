## MODIFIED Requirements

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
