## Context

The current persistence foundation models the citizen journey through the generic tables `cancellation_process` and `cancellation_session`. That structure was intentionally minimal, but it cannot express the domain distinctions now confirmed in `PROJECT_CONTEXT.md`: the citizen owns a cancellation request, identity and eligibility may require controlled retries, confirmation immediately enables a technical revocation operation, and a receipt is evidence of an already-established result rather than the result itself.

The repository is still at an initial stage. The existing schema contains no relevant production information to preserve and its integration tests create disposable data in Testcontainers. This allows a clean replacement of the initial migration, provided local development databases are recreated. If relevant persistent data is discovered during implementation, the clean-replacement strategy must stop and be replaced by forward-only migration planning.

This change supersedes the persistence model proposed by `add-mysql-persistence-foundation`. That earlier change must not later be synchronized as the final persistence contract because doing so would reintroduce the obsolete generic model.

## Goals / Non-Goals

**Goals:**

- Make the certificate cancellation request the aggregate root and current-state source of truth for the complete citizen journey.
- Separate repeatable eligibility checks, identity verifications, request sessions, technical revocation operations, receipts, and audit events according to their real cardinality and lifecycle.
- Build the complete schema reproducibly from an empty MySQL database with Flyway and keep JPA in schema-validation mode.
- Provide database constraints and transactional strategies for one compatible active request per DNI, optimistic concurrency, revocation idempotency, and unique receipt codes.
- Store only protected DNI and free-text values, hashed token/session references, normalized external results, and minimal technical metadata.
- Provide the repositories and queries required by later use cases without adding endpoints, integrations, or empty service layers.
- Document the real schema, including an entity-relationship diagram, sensitive fields, integrity rules, recovery strategy, and unresolved external dependencies.

**Non-Goals:**

- Implement citizen-facing endpoints or workflow use cases.
- Integrate certificate eligibility services, ID Perú, revocation providers, or document storage.
- Issue JWTs, refresh tokens, or implement complete cross-device recovery.
- Implement institutional encryption or key management; this change defines protected storage boundaries only.
- Generate or store receipt PDFs.
- Implement event sourcing, administrative approval, administrative modules, or production retention policies.

## Decisions

### 1. Replace the initial schema instead of layering corrections over it

The existing `V1__create_cancellation_persistence.sql` will be replaced by one clean initial migration that creates the redesigned model. The obsolete `cancellation_process` and `cancellation_session` tables and their JPA classes will not remain. This keeps new installations free from contradictory history while no relevant data exists.

Changing an applied Flyway V1 checksum requires developers to recreate their local database. The documentation will state that clearly. Shared or non-disposable databases must never be repaired or cleaned automatically; discovering one is a reason to pause implementation and produce forward migrations instead.

### 2. Use seven tables because each represents a distinct domain lifecycle

The schema will contain:

- `certificate_cancellation_request`: the current state and root of the citizen request.
- `certificate_eligibility_check`: repeatable eligibility consultations.
- `identity_verification`: repeatable ID Perú verification attempts.
- `cancellation_request_session`: independently expiring and invalidatable sessions.
- `revocation_operation`: idempotent technical interactions with the revocation provider.
- `cancellation_receipt`: independently generated evidence associated with a successful revocation.
- `cancellation_audit_event`: append-only lifecycle trace, not a state source.

These are not tables per screen or per status. Each child exists because the domain allows several attempts or instances with independent dates, results, and technical references.

Identifiers will be application-generated UUIDs stored consistently as `BINARY(16)`. Sequential database identifiers will not be exposed. Child entities will hold unidirectional `request_id` references; the request entity will not eagerly map large child collections. Foreign keys will not cascade deletes because retention and erasure policies are not yet confirmed.

### 3. Keep a direct snapshot of the request's current state

`certificate_cancellation_request` will store:

- protected DNI representations: `dni_lookup_hash`, `dni_ciphertext`, `dni_key_version`, and `dni_last_four`;
- `request_status` using the confirmed backend-controlled values;
- a coarse `lifecycle_status`: `ACTIVE`, `FINALIZED`, `ABANDONED`, or `EXPIRED`;
- the current normalized eligibility result;
- reason code and protected OTHER description with its key version;
- consent text version and confirmation time;
- normalized final outcome;
- recovery and expiration timestamps; and
- creation, update, and optimistic-lock version fields.

The request status enum will support `STARTED`, `CHECKING_ELIGIBILITY`, `NOT_ELIGIBLE`, `ELIGIBLE`, `PENDING_IDENTITY_VERIFICATION`, `IDENTITY_VERIFIED`, `REASON_REGISTERED`, `PENDING_CONFIRMATION`, `CONFIRMED`, `REVOCATION_IN_PROGRESS`, `COMPLETED`, `FAILED`, `OUTCOME_UNKNOWN`, `RECEIPT_AVAILABLE`, `EXPIRED`, and `ABANDONED`. Values are stored as strings and do not use a catalog table.

The coarse lifecycle is intentionally separate from detailed workflow status. It supplies stable active/finalized semantics for uniqueness and recovery even as detailed states grow. `OUTCOME_UNKNOWN` remains active until reconciliation so an uncertain revocation cannot be bypassed by starting another request.

Database checks will enforce paired protected fields and coherent timestamps. JPA construction/update methods will additionally enforce that confirmation requires a reason and consent version, an OTHER reason requires protected description data, and a reason cannot change once confirmed. These are narrow persistence invariants, not a full workflow implementation.

### 4. Prevent incompatible concurrent active requests transactionally

MySQL cannot express a partial unique index directly. The request table will therefore expose a generated nullable guard derived from `dni_lookup_hash` only while `lifecycle_status = 'ACTIVE'`, with a unique index on that guard. MySQL permits multiple NULL values, so historical requests remain allowed while at most one active request for a DNI hash can exist.

The application must create requests in a transaction and translate a unique-constraint conflict into reuse/recovery behavior in a later use case. The DNI lookup value must be a deterministic keyed hash produced outside persistence, not a raw or unsalted public hash.

### 5. Define explicit attempt records without storing provider payloads

Eligibility checks will have a request-local attempt number, consultation status, normalized result (`ELIGIBLE`, `NOT_ELIGIBLE`, `UNAVAILABLE`, `INCONCLUSIVE`), optional external reference, request/response dates, technical error code, correlation ID, and creation time. `(request_id, attempt_number)` is unique.

Identity verifications will have a request-local attempt number, provider, status (`STARTED`, `VERIFIED`, `REJECTED`, `CANCELLED`, `IDENTITY_MISMATCH`, `ERROR`), optional external reference, secure verified-identity hash, normalized DNI match result, lifecycle dates, technical error/cancellation code, correlation ID, and creation time. `(request_id, attempt_number)` is unique.

Neither record stores complete provider responses. Identity records additionally exclude biometrics, photographs, and provider tokens.

### 6. Model sessions as recoverable, independently invalidatable references

`cancellation_request_session` will contain a unique hash of the session or refresh-token reference, a token-family identifier, expiry and activity dates, optional invalidation data, and an optional non-invasive client reference. Multiple sessions may belong to one request so a later use case can re-establish access after renewed identity verification.

No raw token is persisted. A client reference, if used, must be an application-issued opaque value or its hash and must not be derived through browser fingerprinting.

### 7. Make revocation idempotency and uncertainty explicit

`revocation_operation` will store a globally unique idempotency key, request-local technical attempt number, state (`PREPARED`, `SUBMITTED`, `SUCCEEDED`, `FAILED`, `OUTCOME_UNKNOWN`), external reference, preparation/submission/response/completion dates, normalized result, error code, next status-check time, correlation ID, timestamps, and an optimistic-lock version.

Both the idempotency key and `(request_id, attempt_number)` are unique. A generated nullable guard will also allow at most one open operation (`PREPARED`, `SUBMITTED`, or `OUTCOME_UNKNOWN`) per request. An uncertain outcome therefore blocks creation of another operation until the same operation is reconciled. Retrying network delivery must reuse the same idempotency key; this change stores and constrains the key but does not call an external service.

### 8. Keep receipt generation independent from revocation outcome

`cancellation_receipt` will reference both its request and the successful revocation operation. It will store a unique receipt code, generation state (`PENDING`, `GENERATING`, `AVAILABLE`, `FAILED`), an external storage reference, document hash, template version, generation/availability dates, error code, and timestamps. The relationship allows future regeneration without assuming one receipt forever.

MySQL cannot check the status of a referenced revocation row without a trigger. Because complex triggers are prohibited, an application-level factory/transaction and integration tests will enforce that receipts are created only for a `SUCCEEDED` operation belonging to the same request. Receipt failure never changes an already completed revocation result. The document itself is not stored in MySQL.

### 9. Keep audit append-only but not authoritative

`cancellation_audit_event` will store event type, previous and new request status, normalized result, correlation ID, optional external reference, a sanitized technical code/detail, origin, and occurrence time. The entity will be immutable after insertion and repository operations will focus on appending and ordered history lookup; application code will not expose update or delete behavior.

The request snapshot remains the source of truth. Audit events support traceability and are never replayed to reconstruct state, so this is not event sourcing.

### 10. Protect data before it reaches persistence

Persistence will accept `dni_ciphertext` and protected OTHER-description bytes plus explicit key-version metadata. It will also accept a deterministic keyed DNI lookup hash and only the last four presentation digits. It will not include a fallback plaintext column.

Institutional encryption, HMAC key management, rotation, and cryptographic ports remain future work. Tests will use synthetic protected byte arrays and non-real hashes. The schema and entities will reject incomplete ciphertext/key-version pairs. Logs and entity string representations must not disclose protected values.

Session/token references are hash-only. Provider payloads, credentials, secrets, biometrics, and PDFs have no storage columns.

### 11. Add only repositories with a concrete query need

Each of the seven persisted concepts requires its own repository because later use cases need entity-specific queries. The repositories will support:

- request lookup by ID, active DNI hash, most recent DNI history, and expiration candidates;
- active sessions for a request;
- latest eligibility attempt;
- latest valid identity verification;
- current/open revocation operation;
- latest available receipt; and
- audit history ordered by occurrence time.

Queries will be indexed on their actual predicates and ordering. There will be no generic custom base repository, empty service, or automatically loaded child collection.

### 12. Validate the schema and its behavior against real MySQL

Flyway remains the sole schema creator and Hibernate remains configured with `ddl-auto=validate`. MySQL check constraints, foreign keys, unique constraints, temporal rules, and generated guards will be exercised through the existing MySQL Testcontainers setup.

Tests will cover clean migration, root creation and state updates, optimistic locking, all repeatable child records, multiple sessions, reason and consent persistence, one-active-request enforcement, revocation idempotency and open-operation guards, receipt-code uniqueness, audit ordering, foreign-key integrity, expiration/abandonment queries, and rejection of duplicate controlled values.

### 13. Document the database as an implementation contract

`docs/data-model/README.md` will contain a Mermaid entity-relationship diagram and concise descriptions of entities, relationships, states, sensitive fields, indexes, integrity rules, idempotency, recovery, and unresolved external contracts. It will state explicitly:

> La solicitud de cancelación representa el trámite ciudadano completo. La revocación es una operación técnica ejecutada como consecuencia de la confirmación de dicha solicitud.

## Risks / Trade-offs

- **A larger initial schema than the previous foundation:** Seven tables add breadth, but each models confirmed one-to-many behavior or an independently meaningful result. Avoiding them would conflate retries, evidence, or technical operations in the request row.
- **Cryptography is not yet available:** The schema can guarantee that no plaintext fallback exists, but it cannot guarantee how callers produce protected values. Clear field contracts and synthetic tests mitigate this until institutional key management is selected.
- **Generated uniqueness guards depend on stable lifecycle values:** The coarse lifecycle enum is deliberately small and changes to active semantics will require an explicit migration and test update.
- **Some cross-row invariants cannot be expressed without triggers:** Receipt-to-successful-revocation validation is application-transactional and tested. This is preferred to hidden trigger logic.
- **Replacing V1 invalidates already-applied local checksums:** Local disposable databases must be recreated. The migration plan explicitly prohibits cleaning a database whose data matters.
- **Audit history can grow:** The request entity does not load audit collections, the history query is indexed and ordered, and later retention/pagination policy can be added without changing the root model.
- **Two active OpenSpec changes describe persistence:** This change is authoritative and supersedes `add-mysql-persistence-foundation`; archiving or syncing the older delta without that awareness would create a specification conflict.

## Migration Plan

1. Confirm once more that existing MySQL environments contain no relevant information requiring preservation.
2. Remove the obsolete JPA entities, repositories, tests, and original V1 migration.
3. Add the clean V1 schema, seven JPA entities/enums, concrete repositories, and integration tests.
4. Recreate disposable local databases so Flyway applies the new V1 from empty; never run `flyway clean` automatically.
5. Run the full backend test suite against MySQL Testcontainers and verify Hibernate schema validation.
6. Add the model documentation and confirm it matches the migration and entities.
7. Treat the earlier `add-mysql-persistence-foundation` change as superseded rather than synchronizing its obsolete delta into the main specification.

Rollback during this pre-production phase is source rollback plus recreation of the disposable local database. Once meaningful data exists, all future rollback or schema evolution must use forward migrations and explicit data-preservation plans.

## Open Questions

- Which institutional encryption/HMAC service, algorithms, key identifiers, and rotation procedure will protect DNI and OTHER-description values?
- What are the approved retention and erasure periods for requests, sessions, technical attempts, receipts, and audit events?
- What maximum length and content policy will be approved for the OTHER-description field?
- Which external reference formats, error codes, provider names, and normalized results will be confirmed by the eligibility, ID Perú, and revocation contracts?
- Which external document store will hold receipts and what form should its opaque storage reference take?
- What sanitized technical detail is institutionally permitted in audit events?
