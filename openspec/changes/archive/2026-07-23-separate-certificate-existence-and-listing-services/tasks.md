## 1. Establish the corrected functional baseline

- [x] 1.1 Update `docs/context/PROJECT_CONTEXT.md` end to end so the home service returns only certificate existence and the authenticated step-2 service owns the detailed list, including the later positive-then-empty inconsistency scenario.
- [x] 1.2 Update `docs/ui-reference/README.md`, `docs/TECHNICAL_DECISIONS.md`, `docs/LOCAL_INTEGRATION.md` and other current documentation affected by service responsibility, states, terminology, mock fixtures or API behavior without modifying archived OpenSpec history or image binaries.
- [x] 1.3 Remove obsolete SPEC-08 alignment notices and audit current OpenSpec main specs so no active prose still attributes a certificate list, count, order number, creation date or UUID to the initial query.

## 2. Correct the schema and persistence vocabulary

- [x] 2.1 Add a forward-only Flyway migration after V4 that renames the initial check and request result from eligibility to availability, converts unambiguous legacy values and supplies current Spanish comments without editing prior migrations.
- [x] 2.2 In the same migration, remove the foreign key, source index and `eligibility_check_id` column from `cancellation_request_certificate` while preserving every retained request, attempt, certificate, selection, operation and receipt row.
- [x] 2.3 Rename the availability-check JPA entity, enums and repository and update the cancellation-request availability field so Hibernate matches the migrated schema.
- [x] 2.4 Decouple `CancellationRequestCertificateEntity` constructors and mappings from the initial check while retaining request ownership, certificate uniqueness, selection integrity and optimistic locking.
- [x] 2.5 Update the request status model to distinguish checking availability, absence confirmed, authentication pending, authentication completed with listing pending, detailed certificates available and certificates selected without a catalog table.
- [x] 2.6 Update `docs/data-model/README.md`, its ER diagram, table/column descriptions, state guide, migration history and inspection queries to explain the seven-table corrected model and the absence of certificate rows during the initial query.

## 3. Refactor the initial backend integration

- [x] 3.1 Replace the ambiguous eligibility gateway and result vocabulary with a certificate-availability port whose normalized outcomes are `AVAILABLE`, `NOT_AVAILABLE`, `INCONCLUSIVE`, `UNAVAILABLE` and `ERROR` and whose value contains no certificate collection.
- [x] 3.2 Update the deterministic local/test mock and backend README with fictitious fixtures for positive, negative, inconclusive, unavailable, timeout and technical-error scenarios, ensuring default behavior is documented and no fixture creates certificate objects.
- [x] 3.3 Update initiation preparation and finalization so the first service performs external I/O outside the transaction, persists only request and availability-attempt data, distinguishes every failure from confirmed absence and never calls the certificate repository.
- [x] 3.4 Update request transitions so only `AVAILABLE` reaches `PENDING_IDENTITY_VERIFICATION`, `NOT_AVAILABLE` reaches `NO_CERTIFICATES_AVAILABLE`, and all non-confirmed outcomes remain blocked and safely retryable.
- [x] 3.5 Update the success DTO to expose `availabilityResult` instead of `eligibilityResult`, preserve the established request metadata, and guarantee that the JSON contains no certificate count, collection, order number, creation date or UUID.
- [x] 3.6 Update controller annotations, uniform error mapping and privacy-conscious logging so unavailable, timeout, inconclusive and technical failures retain distinct behavior and logs contain correlation but no DNI or UUID.

## 4. Synchronize OpenAPI and the frontend

- [x] 4.1 Update Swagger/OpenAPI operation and schema documentation for the existence-only contract, every normalized result, correlation header and controlled error without documenting the second service or unimplemented security.
- [x] 4.2 Regenerate `frontend/openapi/backend-api.json` and `frontend/lib/api/generated.ts`, update contract aliases and make the contract check reject the former eligibility field or any initial-response certificate property.
- [x] 4.3 Update the centralized cancellation-request client and DNI form to interpret `availabilityResult`, permit continuation only for `AVAILABLE`, and preserve timeout, abort, network and common error handling.
- [x] 4.4 Update SweetAlert2 presentations and citizen copy so confirmed absence is clearly different from an unconfirmed or failed lookup and positive feedback does not imply the detailed list was already obtained.
- [x] 4.5 Verify that the frontend clears the submitted DNI when appropriate and never writes DNI, UUID, certificate data or request progress to URLs, `localStorage`, `sessionStorage` or cookies.

## 5. Verify backend behavior and migration safety

- [x] 5.1 Update gateway and mock unit tests for all deterministic availability outcomes and assert that error, timeout and invalid provider behavior never become `NOT_AVAILABLE`.
- [x] 5.2 Update coordinator and use-case tests for positive, negative, inconclusive, unavailable, timeout, technical error, stale response, duplicate submission and concurrency transitions.
- [x] 5.3 Add integration assertions that every initial-query scenario persists the correct request and attempt fields and leaves zero `cancellation_request_certificate` rows for that request.
- [x] 5.4 Extend Flyway integration tests for clean V1-to-current creation and V4-to-current upgrade with representative checks and certificates, verifying preservation, renamed metadata, removed relationship, constraints, comments and Hibernate validation.
- [x] 5.5 Keep isolated certificate-cardinality and selection tests passing by creating certificate fixtures independently of the initial availability check.
- [x] 5.6 Update OpenAPI documentation tests to verify the new field and values, complete error coverage and the absence of certificate-level data in the initial operation.

## 6. Verify frontend and integrated behavior

- [x] 6.1 Update frontend unit tests for DNI validation, positive continuation, confirmed absence, inconclusive, unavailable, timeout, technical error, network failure and duplicate submission using the generated contract.
- [x] 6.2 Add privacy and contract assertions that the initial response, browser navigation, storage and rendered output contain no UUID, order number, creation date, quantity or full DNI.
- [x] 6.3 Run the real frontend-backend-MySQL initial flow for positive and negative fictitious DNI values and confirm that only request and availability-attempt rows are written.
- [x] 6.4 Run backend fast tests and `mvnw verify`, frontend tests, type checking, build and `api:check`; resolve all regressions without adding the second service.
- [x] 6.5 Run `openspec validate separate-certificate-existence-and-listing-services --strict` and perform a final repository search confirming no current code, mock, test or documentation uses the superseded initial-list rule.
