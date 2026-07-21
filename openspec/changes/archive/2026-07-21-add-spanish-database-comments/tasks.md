## 1. Confirm the effective schema baseline

- [x] 1.1 Confirm that `extend-persistence-for-selectable-certificates` and `start-new-request-without-progress-recovery` are applied and will be synchronized or archived before this change, so V1 and V2 produce the effective eight-table model.
- [x] 1.2 Inventory the eight domain tables and every column created by V1 and V2, including exact type, attributes, nullability, default, charset, collation and `AUTO_INCREMENT` metadata.
- [x] 1.3 Confirm that no current table or column already has a non-empty MySQL comment and record the expected coverage count for integration tests.
- [x] 1.4 Verify that this change requires no JPA mapping, dependency, API, frontend, domain-state, data-protection or functional-flow modification.

## 2. Define the Spanish metadata descriptions

- [x] 2.1 Write one concise Spanish responsibility description for each of the eight tables.
- [x] 2.2 Write one concise Spanish purpose description for every column in `certificate_cancellation_request`, `certificate_eligibility_check` and `identity_verification`.
- [x] 2.3 Write one concise Spanish purpose description for every column in `revocation_operation`, `cancellation_receipt` and `cancellation_audit_event`.
- [x] 2.4 Write one concise Spanish purpose description for every column in `cancellation_request_certificate` and `certificate_revocation_result`.
- [x] 2.5 Review all descriptions for consistent terminology, correct domain meaning, useful distinction between internal and external references, and absence of personal data, secrets, payloads or unimplemented claims.

## 3. Add the forward-only Flyway migration

- [x] 3.1 Create V3 after the existing migrations without modifying the contents or checksums of V1 and V2.
- [x] 3.2 Add native MySQL table comments for all eight domain tables.
- [x] 3.3 Add native MySQL column comments for all V1 columns using `MODIFY COLUMN` definitions that exactly preserve their existing structure.
- [x] 3.4 Add native MySQL column comments for all V2 columns using `MODIFY COLUMN` definitions that exactly preserve their existing structure.
- [x] 3.5 Review the migration to confirm that it contains no data update, rename, drop, new table, new column, changed key, changed index, changed constraint or changed relationship.

## 4. Verify clean and incremental migration behavior

- [x] 4.1 Extend the clean-database Testcontainers verification to require three successful Flyway migrations and complete non-empty `TABLE_COMMENT` and `COLUMN_COMMENT` coverage.
- [x] 4.2 Extend the incremental migration test to start at V2 with fictitious rows, run V3 and prove that row counts and representative values remain unchanged.
- [x] 4.3 Assert representative Spanish comments exactly for the request, DNI, certificate UUID, selection, idempotency, individual result, receipt and audit metadata.
- [x] 4.4 Verify representative structural metadata before and after V3, including column types, unsigned attributes, nullability, defaults, charset, collation and `AUTO_INCREMENT`.
- [x] 4.5 Start the backend against the migrated schema and confirm Hibernate `ddl-auto=validate` succeeds without entity changes.
- [x] 4.6 Add a regression assertion that reports every undocumented domain table or column by name when comment coverage is incomplete.

## 5. Update the permanent model documentation

- [x] 5.1 Update `docs/data-model/README.md` to list V3, explain that comments are native MySQL metadata and clarify that detailed domain rules remain in the context and specifications.
- [x] 5.2 Add a short convention requiring every future domain table or column to receive a concise Spanish comment in the migration that creates it.
- [x] 5.3 Add simple `INFORMATION_SCHEMA.TABLES` and `INFORMATION_SCHEMA.COLUMNS` queries for inspecting table and column descriptions.
- [x] 5.4 Confirm documentation does not duplicate obsolete recovery behavior or contradict the effective eight-table selective-cancellation model.

## 6. Run final validation

- [x] 6.1 Run backend fast tests and the complete MySQL Testcontainers verification suite from an empty database.
- [x] 6.2 Run the explicit V2-to-V3 incremental migration verification with preserved data.
- [x] 6.3 Inspect the final diff to confirm V1 and V2 are unchanged and no dependency, JPA entity, endpoint, frontend file or functional behavior was modified.
- [x] 6.4 Query the local MySQL schema after Flyway and manually verify that Workbench-compatible table and column descriptions are readable in Spanish.
- [x] 6.5 Run `openspec validate add-spanish-database-comments --strict` and resolve every consistency issue.
