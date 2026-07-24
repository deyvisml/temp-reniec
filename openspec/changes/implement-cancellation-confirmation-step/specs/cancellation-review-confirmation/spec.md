## ADDED Requirements

### Requirement: Protected step-four access
The system SHALL render the cancellation review as step 4 of 5 only for the active session whose identity is verified and whose request has a valid reason and selected certificates. It MUST derive access and the current step from backend state, MUST NOT expose request identifiers or personal data in the URL, and MUST block direct access to future steps.

#### Scenario: Eligible request opens the review
- **WHEN** an authenticated citizen has a valid selected set and registered reason
- **THEN** the canonical internal route renders step 4 with steps 1 through 3 completed and step 4 current

#### Scenario: Session is absent or expired
- **WHEN** the review is requested without a valid active session
- **THEN** the API returns the standard unauthorized response and the frontend redirects to the public home

#### Scenario: Request is not ready for review
- **WHEN** identity, selection, reason, or allowed request state is missing
- **THEN** the backend denies the review and the flow resolves to the latest permitted earlier step

### Requirement: Authoritative minimized review summary
The backend SHALL build the review summary exclusively from the active request and its persisted selected certificates. The response SHALL include a masked DNI, order number and creation timestamp for each selected certificate, a presentation-safe abbreviated identifier, normalized reason, optional OTHER description, consequences, consent text, and consent version. It MUST NOT return the full DNI or full certificate UUID in this summary.

#### Scenario: Standard reason summary is loaded
- **WHEN** a ready request uses a predefined reason
- **THEN** the response contains its masked DNI, only its selected certificates and the normalized reason without an OTHER description

#### Scenario: OTHER reason summary is loaded
- **WHEN** a ready request uses OTHER with a valid description
- **THEN** the response includes that stored description exactly as the authoritative summary value

#### Scenario: Browser data is manipulated
- **WHEN** the browser holds stale or modified DNI, reason, order number, date, or certificate identifiers
- **THEN** none of those browser values influence the summary returned by the backend

### Requirement: Clear and accessible confirmation interface
The frontend SHALL follow the composition and hierarchy of `docs/ui-reference/step-4.png` while using the current five-step flow and referring only to the selected certificate set. It SHALL display the summary, immediate effect, future invalidity of cancelled certificates, and consent control with readable contrast, semantic structure, visible focus and responsive behavior.

#### Scenario: Review is displayed on a narrow viewport
- **WHEN** the citizen opens step 4 on a supported mobile viewport
- **THEN** all summary data, consequences, consent and actions remain readable and keyboard accessible without horizontal page overflow

#### Scenario: Consent is not selected
- **WHEN** the citizen has not affirmatively selected the consent control
- **THEN** the confirmation action remains disabled and no confirmation request can be submitted

#### Scenario: Confirmation is processing
- **WHEN** a valid confirmation request is in progress
- **THEN** the consent, back navigation and confirmation action are temporarily disabled and a non-color-only busy state is announced

### Requirement: Explicit versioned consent
The backend SHALL own the consent text and a stable version identifier. Confirmation SHALL require an explicit true acceptance and the exact version shown in the summary; it MUST reject absent, false, blank or obsolete consent evidence. The frontend MUST NOT invent or override the consent text.

#### Scenario: Current consent is accepted
- **WHEN** the citizen explicitly accepts the consent version returned by the current summary
- **THEN** the backend may proceed with the remaining confirmation validations

#### Scenario: Consent version changed
- **WHEN** the submitted version no longer matches the backend's current version
- **THEN** confirmation is rejected as a conflict and the citizen is required to reload and review the current text

#### Scenario: Crafted request claims no acceptance
- **WHEN** a client submits confirmation without `consentAccepted: true`
- **THEN** the backend rejects the request without changing the request status or confirmation time

### Requirement: Transactional confirmation validation
The backend SHALL resolve the request from the authenticated session and, inside one short transaction, revalidate verified identity, allowed state, valid reason, at least one selected certificate, ownership and availability of every selected certificate, and current consent. It MUST NOT trust a summary or certificate set submitted by the frontend.

#### Scenario: Valid request is confirmed
- **WHEN** every invariant is valid and current consent is accepted
- **THEN** the request atomically stores its confirmation time and consent version, transitions to `CONFIRMED`, and records one `CONSENT_CONFIRMED` audit event

#### Scenario: Selected certificate does not belong to the request
- **WHEN** persisted or manipulated selection data references a certificate outside the active request
- **THEN** confirmation is rejected and no request, certificate or audit row is partially updated

#### Scenario: Selection is empty or unavailable
- **WHEN** no selected certificate remains valid and available for the active request
- **THEN** confirmation is rejected without preparing revocation

#### Scenario: Reason is invalid
- **WHEN** the reason is missing, unsupported, or OTHER lacks its required valid description
- **THEN** confirmation is rejected without changing the request

#### Scenario: Request is no longer reversible
- **WHEN** the request is abandoned, revoking, revoked, failed, uncertain or final
- **THEN** confirmation is rejected with a controlled state conflict

### Requirement: Idempotent and concurrency-safe confirmation
The confirmation operation SHALL serialize concurrent changes to the active request. Repeating the same confirmation for an already `CONFIRMED` request with the persisted consent version SHALL return the persisted confirmation result without changing its timestamp or creating duplicate audit events; incompatible concurrent operations SHALL return a controlled conflict.

#### Scenario: Citizen double-submits confirmation
- **WHEN** two equivalent confirmation requests arrive for the same active request
- **THEN** both resolve to one persisted confirmation time and one consent audit event

#### Scenario: Another tab modifies reversible data first
- **WHEN** the selection or reason changes before the confirmation transaction acquires the request
- **THEN** confirmation validates the newest persisted state rather than stale browser data

### Requirement: Confirmation is the point of no return
After confirmation, the system MUST reject changes to the reason and selected certificate set. Confirmation SHALL leave the request in `CONFIRMED` and SHALL NOT invoke a revocation provider, create a revocation operation, transition to revocation in progress, or generate a receipt in this change.

#### Scenario: Reason edit is attempted after confirmation
- **WHEN** any application path attempts to replace the confirmed reason or OTHER description
- **THEN** the change is rejected and the confirmed values remain unchanged

#### Scenario: Selection edit is attempted after confirmation
- **WHEN** any application path attempts to select, deselect, add or remove certificates after confirmation
- **THEN** the change is rejected and the confirmed set remains unchanged

#### Scenario: Confirmation succeeds
- **WHEN** the request reaches `CONFIRMED`
- **THEN** no external revocation call, revocation-operation row or receipt is produced by this increment

### Requirement: Back navigation before confirmation
Before confirmation, the citizen SHALL be able to return from step 4 to step 3 through the back action and completed-step navigation. The flow MUST NOT allow navigation back to identity authentication or forward to step 5, and it MUST disable back navigation after confirmation has begun.

#### Scenario: Citizen returns to the reason step
- **WHEN** the ready citizen activates Regresar or completed step 3 before confirming
- **THEN** the flow renders step 3 within the same protected route and preserves the current unconfirmed request

#### Scenario: Citizen selects a disallowed step
- **WHEN** the citizen attempts to navigate to step 1 or step 5 from the review
- **THEN** navigation is blocked and the request remains unchanged

### Requirement: Documented confirmation contract and test coverage
OpenAPI SHALL document summary and confirmation operations, validation rules, schemas, success responses and principal errors, and generated TypeScript contracts SHALL remain synchronized. Automated tests SHALL cover authoritative summaries, mandatory consent, confirmation, double submission, manipulated certificates, expired sessions, back navigation, immutable data and absence of revocation side effects.

#### Scenario: OpenAPI contract is inspected
- **WHEN** the generated API document is compared with the implemented endpoints
- **THEN** the request, response, validation and error contracts match and contain no undocumented authentication mechanism or personal-data field

#### Scenario: Integrated confirmation is tested
- **WHEN** the protected frontend confirms a valid request against the backend and MySQL test environment
- **THEN** the UI reports confirmation and persistence contains one confirmed request with the accepted version and no revocation operation
