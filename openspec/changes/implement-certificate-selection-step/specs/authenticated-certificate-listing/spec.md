## ADDED Requirements

### Requirement: Detailed certificates are loaded only for a verified active session
The backend SHALL obtain the current request exclusively from a valid persisted flow session and SHALL permit detailed listing only after matching ID Perú identity verification. It MUST NOT accept a client-selected DNI or request identifier and MUST reject expired, invalidated, unauthenticated or incorrectly staged sessions before invoking the provider.

#### Scenario: Verified citizen opens step 2
- **WHEN** a valid session in `IDENTITY_VERIFIED` requests the current certificate list
- **THEN** the backend resolves its own request and is allowed to invoke the second service

#### Scenario: Identity has not been verified
- **WHEN** a valid session still has `PENDING_IDENTITY`
- **THEN** the backend returns a controlled forbidden-state error and does not invoke the second service

#### Scenario: Session is unavailable
- **WHEN** the access token is absent, expired, invalid or linked to an invalidated session
- **THEN** the backend returns the standard unauthorized response without disclosing certificate data

### Requirement: The second-service result is normalized behind a replaceable port
The application SHALL define an internal certificate-listing port independent from HTTP and provider DTOs. It SHALL normalize a valid list, an empty list, timeout, unavailability and malformed response without converting technical failures into an empty list. The deterministic mock SHALL cover empty, one, several, duplicate UUID, invalid UUID, timeout, unavailable and malformed scenarios. A real adapter MUST NOT be implemented or enabled until an official contract is available and verified.

#### Scenario: Provider returns one or several valid certificates
- **WHEN** the configured adapter returns valid order number, creation date and canonical UUID values
- **THEN** the use case accepts the normalized collection without exposing provider-specific data

#### Scenario: Provider is unavailable
- **WHEN** the adapter times out or reports unavailability
- **THEN** the backend returns the corresponding controlled technical error and preserves a retryable request state

#### Scenario: Provider contract is not available
- **WHEN** the application is configured for real listing without a verified real adapter
- **THEN** startup fails with a concise non-secret configuration error instead of using a fictitious endpoint

### Requirement: A validated listing is persisted atomically as the request snapshot
The backend SHALL validate the complete provider collection before persistence and SHALL then store all certificates or none in `cancellation_request_certificate`. Each row SHALL belong to the active request and contain order number, creation time, canonical UUID, current availability, consultation time, selection state and technical audit fields. `(request_id, certificate_uuid)` SHALL remain unique, and repeated reads after a successful load SHALL use the persisted snapshot without another provider call.

#### Scenario: Valid list is loaded
- **WHEN** the second service returns several distinct valid certificates
- **THEN** all rows are persisted in one completion transaction and the request becomes `CERTIFICATES_AVAILABLE`

#### Scenario: Duplicate or malformed data is returned
- **WHEN** the response contains duplicate UUIDs, invalid UUIDs, missing order numbers or invalid dates
- **THEN** no row from that response is persisted and the backend returns a controlled invalid-provider-response error

#### Scenario: Persisted list is read again
- **WHEN** step 2 is reloaded during the same active operation after a successful load
- **THEN** the backend returns the request-owned rows without invoking the provider again

### Requirement: Empty post-authentication listing blocks continuation without invalidating identity
When the second service returns an empty list, the backend SHALL persist no certificate row, SHALL mark the request as having no currently available certificates and SHALL prevent selection and later steps. It MUST NOT classify the result as an identity failure or invent a certificate.

#### Scenario: Availability changed after the initial query
- **WHEN** the initial query was positive, identity succeeded and the second service returns an empty list
- **THEN** step 2 reports that no certificates are currently available and the operation cannot continue

### Requirement: Listing reservation controls concurrency and interrupted loads
The use case SHALL reserve listing through a short transaction and `CHECKING_CERTIFICATE_LIST`, perform external I/O outside that transaction and complete through a second transaction. It SHALL prevent duplicate concurrent persistence and SHALL recover a stale reservation after a configured threshold without automatic general retries.

#### Scenario: Two tabs request the first list concurrently
- **WHEN** both requests target the same verified operation before a list exists
- **THEN** at most one provider load owns completion and the other receives persisted data or a controlled in-progress response

#### Scenario: Previous listing was interrupted
- **WHEN** a request remains in `CHECKING_CERTIFICATE_LIST` beyond the configured stale threshold
- **THEN** a later authorized request can reserve a new controlled attempt without creating duplicate certificate rows

### Requirement: Certificate listing API is documented and privacy-limited
The backend SHALL expose a versioned protected current-request listing operation. Its OpenAPI contract SHALL document successful fields, cookie authentication, correlation and controlled 401, 403, 409, 422, 503 and 504 errors. Responses and logs MUST NOT expose DNI, provider payloads, credentials or certificate data belonging to another request, and UUIDs MUST NOT appear in URLs or logs.

#### Scenario: OpenAPI is generated
- **WHEN** the listing operation and schemas are inspected
- **THEN** they match runtime validation and contain only the citizen-facing certificate fields and standard errors
