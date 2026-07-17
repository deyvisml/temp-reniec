## 1. Migration safety and obsolete-model removal

- [x] 1.1 Verify and record that the current `cancellation_process` and `cancellation_session` environments contain no relevant information; stop and design forward migrations if preservation is required
- [x] 1.2 Remove the obsolete process/session JPA entities, enums, repositories, and persistence tests after the no-data precondition is satisfied
- [x] 1.3 Replace the old V1 migration file with a clearly named authoritative V1 for the cancellation-request model, without retaining obsolete tables or contradictory migrations

## 2. Cancellation request schema

- [x] 2.1 Create `certificate_cancellation_request` with UUID identity, protected DNI representations, detailed request status, coarse lifecycle, current eligibility, timestamps, and optimistic-lock version
- [x] 2.2 Add controlled reason, protected OTHER-description, consent, confirmation, final-outcome, recoverability, and expiration columns with coherent field-pair and date constraints
- [x] 2.3 Add the generated nullable active-DNI guard, its unique index, and query indexes for active, latest, and expiring request lookups

## 3. Repeatable attempt and session schema

- [x] 3.1 Create `certificate_eligibility_check` with normalized results, attempt uniqueness, correlation metadata, foreign key, and latest-attempt index
- [x] 3.2 Create `identity_verification` with privacy-limited identity references, normalized states and match result, attempt uniqueness, foreign key, and latest-valid index
- [x] 3.3 Create `cancellation_request_session` with hash-only session reference, token family, expiry/use/invalidation fields, non-invasive client reference, foreign key, uniqueness, and active-session index

## 4. Revocation, receipt, and audit schema

- [x] 4.1 Create `revocation_operation` with unique idempotency key, request-local attempt, controlled state, technical dates/results, correlation metadata, and optimistic-lock version
- [x] 4.2 Add the generated nullable open-operation guard and indexes that prevent bypassing submitted or uncertain operations while supporting current-operation lookup
- [x] 4.3 Create `cancellation_receipt` with request and revocation foreign keys, unique receipt code, generation state, external storage metadata, document hash, and no document BLOB
- [x] 4.4 Create `cancellation_audit_event` with immutable lifecycle-event fields and an index for ordered request history
- [x] 4.5 Review every foreign key, unique constraint, UTC timestamp, check constraint, and deletion rule across all seven tables and confirm that no cascading deletion, workflow trigger, stored procedure, secret, biometric, provider payload, or plaintext sensitive column is present

## 5. Request root JPA model

- [x] 5.1 Add backend-controlled enums for request status, lifecycle, current eligibility, cancellation reason, and normalized final outcome using stable string persistence
- [x] 5.2 Implement `CertificateCancellationRequestEntity` with UUID generation, UTC audit timestamps, `@Version`, protected-value fields, and mappings that exactly match Flyway
- [x] 5.3 Implement narrow request persistence invariants for protected field pairs, OTHER-description presence, complete consent confirmation, immutable confirmed reason, and coherent lifecycle dates
- [x] 5.4 Ensure entity diagnostics and string representations cannot reveal encrypted values, DNI hashes, last-four digits, reason descriptions, tokens, or unnecessary personal data

## 6. Child JPA models

- [x] 6.1 Implement eligibility-check enums and entity with a unidirectional lazy request reference and request-local attempt semantics
- [x] 6.2 Implement identity-verification enums and entity without biometric, photograph, provider-token, or complete-response fields
- [x] 6.3 Implement request-session entity with hash-only references, independent validity/invalidation behavior, and no browser fingerprinting fields
- [x] 6.4 Implement revocation-operation enums and entity with stable idempotency, uncertain-outcome representation, timestamps, and optimistic locking
- [x] 6.5 Implement cancellation-receipt enums and entity plus a persistence-level creation path that accepts only a successful operation belonging to the same request
- [x] 6.6 Implement an immutable cancellation-audit-event entity that supports append and read behavior without update/delete workflow methods
- [x] 6.7 Confirm all child mappings are unidirectional, lazy where applicable, and absent from automatically loaded request collections

## 7. Concrete repositories and indexed queries

- [x] 7.1 Implement the cancellation-request repository queries for ID, active secure-DNI hash, latest DNI history, and expiration candidates
- [x] 7.2 Implement the eligibility and identity repositories for latest attempt and latest valid verification queries
- [x] 7.3 Implement the session repository for active sessions of a request at a UTC cutoff
- [x] 7.4 Implement the revocation repository for current/open operation and idempotency-key lookup
- [x] 7.5 Implement the receipt repository for the latest available receipt and the audit repository for chronologically ordered history
- [x] 7.6 Verify that every custom query is backed by the migration indexes and that no generic base repository, empty service, or speculative interface was introduced

## 8. MySQL integration coverage

- [x] 8.1 Update the Testcontainers fixture so a clean MySQL instance runs the replacement V1 and Hibernate validates all seven tables
- [x] 8.2 Test request creation, state update, reason/consent persistence, protected-field constraints, expiration, abandonment, active lookup, and latest-history lookup
- [x] 8.3 Test optimistic-lock failure and the atomic one-active-request-per-DNI guard, including permitted historical requests
- [x] 8.4 Test multiple eligibility attempts and multiple identity-verification attempts, their uniqueness constraints, and latest-record queries
- [x] 8.5 Test multiple independently valid or invalidated sessions and active-session lookup
- [x] 8.6 Test revocation attempt uniqueness, global idempotency uniqueness, optimistic locking, open-operation guard, and preservation of `OUTCOME_UNKNOWN`
- [x] 8.7 Test successful-operation receipt association, failed receipt independence, unique receipt codes, available-receipt lookup, and absence of document storage
- [x] 8.8 Test append-only audit persistence, chronological history, foreign-key integrity, and rejection of orphan child records
- [x] 8.9 Retain and run the fast non-database backend foundation tests separately from the MySQL integration suite

## 9. Documentation and final verification

- [x] 9.1 Create `docs/data-model/README.md` with a Mermaid entity-relationship diagram that matches the seven-table migration and JPA model
- [x] 9.2 Document entity responsibilities, relationships, states, sensitive fields, indexes, constraints, idempotency, progress recovery, request-versus-revocation semantics, and external-contract open questions
- [x] 9.3 Update `backend/README.md` with the disposable-local-database recreation warning, Flyway ownership, MySQL variables, container-test requirements, and prohibition on cleaning databases containing relevant information
- [x] 9.4 Run the documented fast test command and full Maven verification with MySQL Testcontainers, then resolve every compilation, migration, schema-validation, and test failure
- [x] 9.5 Inspect the final dependency tree, runtime routes, schema, logs, and diff to confirm no functional endpoints, JWT behavior, external calls, PDF generation, extra database, administrative feature, plaintext protected data, or obsolete process model remains
- [x] 9.6 Record that `add-mysql-persistence-foundation` is superseded and must not synchronize its obsolete two-table persistence delta as the final contract
