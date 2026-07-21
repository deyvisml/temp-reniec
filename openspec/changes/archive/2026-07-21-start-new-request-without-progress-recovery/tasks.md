## 1. Confirm the effective baseline and archive order

- [x] 1.1 Confirm that the completed context-v2 and selectable-certificate changes are applied and will be archived or synchronized before this change so the effective model contains the six-step flow and eight persistence tables.
- [x] 1.2 Record the current recovery references in context, technical decisions, main specs, data-model documentation, backend DTO/coordinator/repository, OpenAPI artifact, generated TypeScript, frontend behavior, and tests.
- [x] 1.3 Confirm that this correction requires no Flyway migration, new table, new column, session storage, JWT implementation, provider-contract change, or new dependency.

## 2. Correct permanent domain and technical documentation

- [x] 2.1 Update `docs/context/PROJECT_CONTEXT.md` to state that every home-page entry is a new cancellation intention, previous progress and constancias are never reopened automatically, and historical records remain only for evidence and safety.
- [x] 2.2 Update `docs/TECHNICAL_DECISIONS.md` to remove indefinite progress recovery and distinguish continuation of the current journey from a later new initiation.
- [x] 2.3 Update `docs/data-model/README.md` to replace the recovery section with the new-request, abandonment, protected-operation, historical-retention, and no-session strategy.
- [x] 2.4 Search permanent documentation for contradictory promises such as “recuperar”, “retomar”, “crear o recuperar”, reuse indicators, recovery windows, or reopening a prior constancia, correcting only references affected by this decision.

## 3. Implement transactional new-request initiation

- [x] 3.1 Define explicit backend sets or predicates for replaceable pre-confirmation states, transient eligibility state, protected confirmed/in-progress/uncertain states, and terminal historical states while preserving stored legacy values.
- [x] 3.2 Replace resumable-status repository lookup with a pessimistically locked latest-request-by-DNI query that supports the new-journey decision without adding a custom generic repository or permanent unique-DNI guard.
- [x] 3.3 Change eligibility preparation so a replaceable previous request becomes `ABANDONED` and a different request with attempt number 1 is created and queried; never reuse its request, attempt, certificates, or selection.
- [x] 3.4 Keep a live `CHECKING_ELIGIBILITY` request protected, but close a stale submitted attempt, abandon its request, and create a different request instead of adding another attempt to the old request.
- [x] 3.5 Add a controlled protected-operation error for confirmed, revoking, or uncertain requests that blocks initiation without returning the previous request identifier, step, certificates, result, or constancia.
- [x] 3.6 Guard eligibility finalization so a failed, stale, completed, or abandoned attempt cannot be finalized again or reactivate historical progress when a provider response arrives late.
- [x] 3.7 Preserve all historical child rows and verify that terminal requests allow a new request without deletion or mutation of certificates, selections, operations, results, receipts, or audit events.

## 4. Remove recovery semantics from the API contract

- [x] 4.1 Remove `reused` and recovery descriptions from `CancellationRequestResponse`, preparation/result records, controller annotations, OpenAPI tags, summaries, DTO schemas, and service names where applicable.
- [x] 4.2 Document the new protected-operation conflict and existing in-progress/concurrency errors with stable codes, generic messages, correlation identifiers, and no historical data disclosure.
- [x] 4.3 Regenerate or update the committed OpenAPI JSON and TypeScript contract so the response has no `reused` property and the initiation operation is described only as creating a new request.
- [x] 4.4 Update backend and frontend contract fixtures and drift tests to reject reintroduction of `reused`, `publicReference`, or recovery wording.

## 5. Align frontend behavior without adding restoration

- [x] 5.1 Update the centralized API mapping and DNI form flow for the new response type while keeping duplicate-submit prevention and current eligible continuation intact.
- [x] 5.2 Map the protected-operation error to a generic accessible SweetAlert2 message that acknowledges the temporary block without linking to or exposing the earlier request.
- [x] 5.3 Verify that returning to the home page after a terminal result submits a fresh request and that no code reads DNI, request progress, request identifiers, or restoration tokens from `localStorage` or `sessionStorage`.
- [x] 5.4 Keep historical constancia consultation or re-download outside this change and avoid adding a provisional route, button, or API for it.

## 6. Verify new journeys, concurrency, and history

- [x] 6.1 Add backend unit tests showing that eligible, identity, selection, reason, and pending-confirmation history is abandoned and replaced with a different request and fresh attempt.
- [x] 6.2 Add tests showing that completed, failed, not-eligible, receipt-available, and abandoned history remains unchanged while a new request is created.
- [x] 6.3 Add tests showing that live eligibility, confirmed revocation, revocation in progress, and uncertain outcomes block safely without recovery or sensitive response data.
- [x] 6.4 Add stale-attempt and late-response tests proving that the old request remains abandoned and cannot overwrite the new request.
- [x] 6.5 Add MySQL Testcontainers concurrency tests proving that equivalent simultaneous initiations result in at most one new active request and one provider attempt.
- [x] 6.6 Add an end-to-end persistence test in which a prior completed selective cancellation is followed by a new consultation with a distinct request, while all old certificates, individual results, and constancia remain queryable.
- [x] 6.7 Update frontend tests for the contract, eligible continuation, protected-operation feedback, duplicate submission, and absence of browser-based restoration.

## 7. Run final validation

- [x] 7.1 Run backend fast tests and the complete MySQL Testcontainers verification suite.
- [x] 7.2 Regenerate OpenAPI and run backend API documentation, schema, and contract-drift tests.
- [x] 7.3 Run frontend type checking, linting, unit tests, production build, and real frontend-backend-MySQL integration checks used by the project.
- [x] 7.4 Inspect the final diff to confirm no Flyway migration, session table, recovery token, local/session storage, historical deletion, unrelated UI, external contract, or dependency was added.
- [x] 7.5 Run `openspec validate start-new-request-without-progress-recovery --strict` and resolve every specification consistency issue.
