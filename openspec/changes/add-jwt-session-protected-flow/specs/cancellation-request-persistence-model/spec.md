## MODIFIED Requirements

### Requirement: Clean authoritative schema from an empty database
Flyway SHALL build the existing atomic cancellation model and SHALL apply forward-only migrations that preserve the current request, availability, identity, certificate, revocation, receipt and audit history while adding exactly one justified `cancellation_flow_session` table. The resulting schema SHALL match Hibernate mappings and contain no selection-only, status-catalog, generated-guard, per-access-token, per-tab, per-device or individual-revocation-result table.

#### Scenario: Empty database is initialized
- **WHEN** the backend starts against an empty MySQL database
- **THEN** Flyway applies the complete history and Hibernate validates the cancellation schema including the single flow-session table

#### Scenario: Existing V6 database is upgraded
- **WHEN** the session migration runs over representative requests and identity attempts
- **THEN** existing domain history survives and no historical request is automatically assigned a recoverable active session

#### Scenario: Migration history is inspected
- **WHEN** a contributor reviews Flyway files and checksums
- **THEN** prior migrations remain unchanged and the session change exists only in reproducible forward migrations

### Requirement: Only one incompatible active request and session per DNI
The model SHALL permit multiple historical requests for one DNI and SHALL provide indexes supporting lookup by DNI, status and recency. A new initiation SHALL never recover or return an earlier request as current progress. The use case SHALL serialize the decision by DNI, abandon a replaceable pre-confirmation request and invalidate its session before creating a new request, and block initiation while a confirmed, in-progress or uncertain operation could make another cancellation unsafe. Each request SHALL have at most one flow-session row and the model MUST NOT create recovery-window columns on the request, generated guard columns, complex triggers or multiple device/browser sessions.

#### Scenario: Previous pre-confirmation request exists
- **WHEN** the citizen initiates from home after the previous session ended and the latest request can be replaced safely
- **THEN** the previous request becomes `ABANDONED`, its session remains invalid, and a new request, availability check and session are created only after a new positive result

#### Scenario: Historical terminal request exists
- **WHEN** a completed, failed, not-eligible, abandoned or otherwise terminal request exists for the DNI
- **THEN** a new request can coexist and all historical rows remain queryable without restoring an old session

#### Scenario: Irreversible or uncertain operation exists
- **WHEN** the latest request is confirmed, revoking or has an uncertain revocation outcome
- **THEN** initiation is rejected without returning or reopening the previous request or session

#### Scenario: Concurrent new initiations are attempted
- **WHEN** concurrent transactions initiate a new journey for the same DNI
- **THEN** transaction and locking produce at most one new active request and, after positive availability, at most one session

### Requirement: Transactional flow session is represented independently from identity attempts
`cancellation_flow_session` SHALL store only the session identifier, unique request relation, lifecycle status, refresh family, current and bounded previous refresh hashes, concurrency-window timestamp, refresh expiration, last use, invalidation reason/time and technical timestamps. It SHALL use Spanish schema comments, foreign keys, uniqueness and lookup indexes aligned with actual queries. `identity_verification` SHALL retain only provider-attempt security artifacts and MUST NOT remain the source of truth for general flow authorization.

#### Scenario: Session is issued after availability
- **WHEN** a request becomes eligible to enter identity step 1
- **THEN** one session row is created independently of any later ID Perú attempt

#### Scenario: Refresh rotates
- **WHEN** session renewal succeeds
- **THEN** only hashed refresh references and bounded rotation metadata are updated under row lock

#### Scenario: ID Perú succeeds
- **WHEN** an identity attempt verifies a matching DNI
- **THEN** the request and existing session advance transactionally without creating another session row

#### Scenario: Session is invalidated
- **WHEN** logout, abandonment, expiration or completion occurs
- **THEN** invalidation is persisted and no raw token or historical recovery record is stored
