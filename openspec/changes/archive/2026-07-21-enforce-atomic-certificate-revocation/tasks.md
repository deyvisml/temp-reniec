## 1. Confirm the effective baseline and migration safety

- [x] 1.1 Confirm that `update-context-and-ui-reference-flow`, `extend-persistence-for-selectable-certificates`, `start-new-request-without-progress-recovery` and `add-spanish-database-comments` are synchronized or archived in dependency order before this change is archived.
- [x] 1.2 Inventory every current occurrence of individual, mixed or partial revocation semantics in active documentation, OpenSpec specifications, backend enums, entities, repositories, migrations and tests.
- [x] 1.3 Confirm that the selection step still permits one, several or all available certificates and record that atomicity applies only to the confirmed selected set.
- [x] 1.4 Verify that `certificate_revocation_result` contains no relevant environment data; if relevant data exists, stop destructive migration work and design an approved preservation path.
- [x] 1.5 Identify the V2 keys and indexes used exclusively by `certificate_revocation_result` so V4 removes no constraint required by another relationship.

## 2. Update the authoritative functional references

- [x] 2.1 Revise `docs/context/PROJECT_CONTEXT.md` throughout so the selected UUID list is atomic, successful means all selected certificates, failed means none, and uncertain remains unresolved.
- [x] 2.2 Remove or supersede every current context rule, scenario, state, glossary entry and pending question that permits `PARTIAL` or mixed per-certificate outcomes.
- [x] 2.3 Preserve the ability to select one, several or all certificates and state that unselected certificates remain unaffected.
- [x] 2.4 Document that selection becomes immutable after confirmation and that a provider without atomic list semantics is incompatible.
- [x] 2.5 Update `docs/TECHNICAL_DECISIONS.md` only where atomic revocation, idempotency, uncertainty or schema simplicity is affected.
- [x] 2.6 Update `docs/ui-reference/README.md` so confirmation and result/receipt references use one common outcome and explicitly flag contradictory text inside reused images without modifying the PNG files.
- [x] 2.7 Audit other non-archived documentation and current specifications for obsolete individual or partial semantics while leaving archived changes unchanged.

## 3. Simplify the MySQL persistence model

- [x] 3.1 Add forward-only Flyway V4 without changing V1, V2 or V3 checksums.
- [x] 3.2 Drop `certificate_revocation_result` and only the candidate keys or indexes proven to exist exclusively for its foreign keys.
- [x] 3.3 Preserve requests, eligibility checks, identity verifications, request certificates and selections, revocation operations, receipts and audit events during V3-to-V4 migration.
- [x] 3.4 Remove the individual-result JPA entity, repository and status enum without adding a replacement table or speculative snapshot structure.
- [x] 3.5 Remove the individual-result calculator and make `revocation_operation.normalized_result` the direct technical outcome source.
- [x] 3.6 Remove `PARTIAL`, `REVOCATION_PARTIAL` and equivalent request, final-outcome or audit values from active backend code.
- [x] 3.7 Update repository queries and relationships so no mapping, query or constraint references the removed table.
- [x] 3.8 Update native Spanish comment coverage and model assertions for the effective seven-table, 81-column schema.

## 4. Enforce atomic domain transitions

- [x] 4.1 Centralize the valid global outcomes as `SUCCEEDED`, `FAILED` and `OUTCOME_UNKNOWN` with no partial mapping.
- [x] 4.2 Enforce that selected certificate rows cannot be added, removed or deselected after the request is confirmed.
- [x] 4.3 Define the future revocation-port contract at the domain boundary only if a real interface already exists, requiring the complete UUID list and one idempotency key without inventing provider fields.
- [x] 4.4 Ensure a confirmed success updates every selected certificate consistently and a confirmed failure represents that none were revoked.
- [x] 4.5 Ensure `OUTCOME_UNKNOWN` retains the same operation and idempotency key, blocks incompatible initiation and makes no per-certificate success claim.
- [x] 4.6 Reject any attempted normalization of mixed provider results instead of translating it to a partial citizen outcome.
- [x] 4.7 Keep receipt-generation failure independent from a confirmed atomic revocation result.

## 5. Align documentation and contracts with the resulting implementation

- [x] 5.1 Update `docs/data-model/README.md`, its ER diagram, table count, state lists, migration history, queries and justification to the seven-table atomic model.
- [x] 5.2 Explain in the model documentation how selected certificate rows identify the atomic set and why no individual-result table is needed.
- [x] 5.3 Update OpenAPI annotations, schemas and generated TypeScript contracts only if current runtime contracts expose removed partial or individual-result concepts.
- [x] 5.4 Confirm that no new revocation endpoint, external mock, selection screen or receipt generator was introduced by this corrective change.

## 6. Verify clean and incremental database behavior

- [x] 6.1 Extend the empty-database Testcontainers suite to run V1 through V4, require seven domain tables, validate Hibernate and require complete Spanish comment coverage.
- [x] 6.2 Extend the incremental suite to create representative V3 requests, certificates, selections, operations, receipts and audit rows before applying V4.
- [x] 6.3 Prove after V4 that retained row counts and representative values are unchanged and that `certificate_revocation_result` plus its exclusive keys are absent.
- [x] 6.4 Verify one, several and all-certificate selections before confirmation and rejection of selection changes after confirmation.
- [x] 6.5 Verify successful, failed and uncertain atomic operations and rejection or absence of partial states.
- [x] 6.6 Verify idempotency and concurrent transitions do not create a replacement operation for an uncertain outcome.
- [x] 6.7 Start the backend against both a clean migrated schema and the local V3-to-V4 schema with `ddl-auto=validate`.

## 7. Run final consistency validation

- [x] 7.1 Run backend fast tests and the complete MySQL Testcontainers verification suite.
- [x] 7.2 Search active documentation, source, migrations and generated contracts for `PARTIAL`, `REVOCATION_PARTIAL`, mixed-result aggregation and stale eight-table claims; resolve every current contradiction.
- [x] 7.3 Inspect the final diff to confirm PNGs and archived changes are untouched and no unrelated frontend, dependency or functional feature was added.
- [x] 7.4 Query local MySQL metadata to confirm the effective seven tables and their Spanish table and column comments are readable.
- [x] 7.5 Run `openspec validate enforce-atomic-certificate-revocation --strict` and resolve every issue.
