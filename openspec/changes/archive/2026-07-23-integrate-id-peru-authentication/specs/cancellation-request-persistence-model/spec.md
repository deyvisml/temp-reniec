## ADDED Requirements

### Requirement: Identity attempts persist only necessary transient security state
`identity_verification` SHALL remain the persistence record for each ID Perú attempt and SHALL store request, attempt number, provider, real/mock mode, status, safe external reference, state hash, state expiry/consumption, protected PKCE verifier, session_state when valid, safe verified-subject reference, DNI match result, normalized error, correlation and technical timestamps. It SHALL NOT store provider tokens, authorization codes, plaintext state/verifier, client secret, biometric data, photographs or complete responses.

#### Scenario: Attempt starts
- **WHEN** identity verification is prepared
- **THEN** one attempt row with unique request attempt number, unique state hash, expiry and protected verifier is committed

#### Scenario: Attempt completes
- **WHEN** callback processing reaches a terminal result
- **THEN** status, completion and match/error fields are recorded and the protected verifier is cleared

#### Scenario: Persistence is inspected
- **WHEN** tables are queried after real or mock authentication
- **THEN** no prohibited provider artifact or unnecessary citizen claim is present

### Requirement: Callback consumption is enforced atomically
The persistence layer SHALL provide an atomic conditional operation that consumes only a started, unconsumed and unexpired attempt selected by state hash. A unique state constraint and unique `(request_id, attempt_number)` constraint SHALL prevent duplicates without adding an event-sourcing model or a session table.

#### Scenario: Concurrent callbacks arrive
- **WHEN** two transactions submit the same valid state simultaneously
- **THEN** exactly one consumes the attempt and the other observes a replay conflict

#### Scenario: Migration runs on existing data
- **WHEN** Flyway applies the identity extension over a schema through V5
- **THEN** existing rows remain valid and new uniqueness rules do not discard prior request history

### Requirement: Temporary flow authorization is represented on the verified attempt
The verified attempt SHALL store only the hash, validity and invalidation timestamp/reason needed to validate the current short-lived flow authorization. It MUST NOT create `cancellation_request_session` or any replacement session table.

#### Scenario: Authorization is issued
- **WHEN** an identity attempt becomes verified and matching
- **THEN** its authorization hash and expiry are stored in the same transaction as the request transition

#### Scenario: Authorization is invalidated
- **WHEN** logout, abandonment, expiration or completion occurs
- **THEN** the attempt records invalidation and no refresh or recovery record is created

### Requirement: Identity schema remains documented and reproducible
The incremental Flyway migration SHALL add Spanish table/column comments, foreign keys, uniqueness and search indexes matching JPA. It SHALL build correctly both from an empty database and from the current V5 schema, and Hibernate validation SHALL succeed.

#### Scenario: Empty database starts
- **WHEN** Flyway executes V1 through the new migration on MySQL 8.4
- **THEN** the resulting identity schema matches the entities and includes its Spanish descriptions

#### Scenario: Existing V5 database upgrades
- **WHEN** the new migration runs over the current development schema
- **THEN** no correctly modelled cancellation, certificate or revocation data is removed or redesigned

