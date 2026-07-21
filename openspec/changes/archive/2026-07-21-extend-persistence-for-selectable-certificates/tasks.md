## 1. Confirm the persistence baseline

- [x] 1.1 Record the current V1 checksum and verify the six existing tables, JPA mappings, repositories, tests, and SPEC-04R decisions before editing.
- [x] 1.2 Confirm that no dependency, endpoint, frontend file, external adapter, or current API contract needs to change for this persistence-only increment.

## 2. Add the incremental Flyway migration

- [x] 2.1 Create `V2__add_request_certificates_and_revocation_results.sql` without modifying V1.
- [x] 2.2 Add the minimal candidate keys required on `certificate_eligibility_check` and `revocation_operation` for same-request composite foreign keys.
- [x] 2.3 Create `cancellation_request_certificate` with request and source-attempt relationships, order number, emission and consultation timestamps, canonical UUID, availability, selection fields, optimistic version, technical timestamps, and coherent checks.
- [x] 2.4 Add uniqueness and indexes for request UUID, request certificate identity, source-attempt integrity, request listing, availability, and selected-certificate lookup.
- [x] 2.5 Create `certificate_revocation_result` with operation, certificate, request, submitted UUID, normalized individual result fields, correlation, optimistic version, and technical timestamps.
- [x] 2.6 Add composite foreign keys and uniqueness that reject cross-request certificates, mismatched submitted UUIDs, orphan rows, and duplicate operation-certificate results without cascade deletion or triggers.

## 3. Implement request-certificate persistence

- [x] 3.1 Add controlled availability and related enums required by the certificate model without adding status catalog tables.
- [x] 3.2 Implement `CancellationRequestCertificateEntity` with canonical UUID and bounded order-number validation, UTC timestamps, consistent selection methods, `@Version`, and no bidirectional request collection.
- [x] 3.3 Implement `CancellationRequestCertificateRepository` queries for request listing, request-and-UUID lookup, selected rows, counts, and any lock required by demonstrated concurrent selection.
- [x] 3.4 Verify that zero, one, and many certificate rows can belong to a request and that a source eligibility attempt from another request is rejected.

## 4. Implement individual revocation-result persistence

- [x] 4.1 Add the individual status enum `PENDING`, `SUCCEEDED`, `FAILED`, and `OUTCOME_UNKNOWN`, and extend the overall revocation result with `PARTIAL`.
- [x] 4.2 Implement `CertificateRevocationResultEntity` with same-request associations, submitted UUID snapshot, normalized bounded metadata, processing and technical timestamps, correlation, state transitions, and `@Version`.
- [x] 4.3 Implement `CertificateRevocationResultRepository` queries for operation history, operation-and-certificate lookup, status counts, and aggregation inputs.
- [x] 4.4 Implement a small deterministic overall-result calculator covering pending, successful, partial, failed, and uncertain combinations without external I/O.

## 5. Extend request state representation

- [x] 5.1 Add request states for no certificates, certificates available, authenticated pending selection, certificates selected, revocation in progress, and successful, partial, failed, or uncertain revocation while preserving legacy values needed by existing rows and code.
- [x] 5.2 Extend the request final-outcome representation with partial revocation and verify the request entity can persist each new state without changing the current eligibility endpoint behavior.
- [x] 5.3 Update affected audit event values only where needed to record certificate availability, selection, and individual-result lifecycle without implementing event sourcing.

## 6. Verify migration and certificate scenarios

- [x] 6.1 Add a Testcontainers migration test that initializes an empty MySQL database through V1 and V2 and validates all eight tables with Hibernate.
- [x] 6.2 Add an upgrade test that applies V1, inserts representative existing data, applies V2, and proves that all legacy rows remain unchanged.
- [x] 6.3 Add persistence tests for requests with zero, one, and several certificates, including source-attempt linkage and ordered retrieval.
- [x] 6.4 Test selecting one, several, and all certificates; distinguishing unselected rows; and rejecting inconsistent selection timestamps.
- [x] 6.5 Test rejection of duplicate canonical UUIDs inside one request while allowing the same UUID in a different historical request.
- [x] 6.6 Test optimistic-lock conflicts for concurrent selection or availability changes and verify controlled retry behavior at the repository boundary.

## 7. Verify revocation-result scenarios and integrity

- [x] 7.1 Test pending, successful, failed, and uncertain individual results with normalized codes, messages, references, processing times, and correlation.
- [x] 7.2 Test rejection of duplicate results for the same operation and certificate during retries.
- [x] 7.3 Test rejection of a result using a certificate from another request or a submitted UUID different from the referenced certificate.
- [x] 7.4 Test overall calculation for all-success, all-failure, partial, uncertain, pending, and empty-result scenarios.
- [x] 7.5 Test that finalizing a request preserves certificates, selections, operations, and results and that physical deletion is restricted while related history exists.

## 8. Update documentation and run verification

- [x] 8.1 Update `docs/data-model/README.md` with the eight-table ER diagram, table justifications, columns, states, indexes, constraints, UUID protection, selection, result aggregation, concurrency, migration path, and inspection queries.
- [x] 8.2 Remove the temporary SPEC-08 persistence-gap notice once the documented schema matches the implementation, while retaining explicit notes for still-unimplemented API and UI behavior.
- [x] 8.3 Run the fast backend tests and MySQL Testcontainers integration suite, including Flyway validation from empty and V1 upgrade.
- [x] 8.4 Run the backend build and existing API/OpenAPI tests to confirm this persistence increment does not change current runtime contracts.
- [x] 8.5 Inspect the final diff to confirm V1 is unchanged, no frontend or functional endpoint was modified, no new dependency or speculative table was added, and all scope restrictions hold.
- [x] 8.6 Run `openspec validate extend-persistence-for-selectable-certificates --strict` and resolve all specification consistency errors.
