## ADDED Requirements

### Requirement: Step 2 requires one explicit certificate choice
The frontend SHALL always render step 2 after verified identity and a non-empty detailed listing, including when only one certificate exists. It SHALL use an accessible exclusive-selection control, SHALL leave the initial choice empty when no persisted selection exists, and MUST NOT allow more than one certificate to appear selected.

#### Scenario: Several certificates are available
- **WHEN** the citizen chooses one certificate and then chooses another
- **THEN** the second choice replaces the first and exactly one card and radio control remain selected

#### Scenario: One certificate is available
- **WHEN** step 2 renders a list containing one certificate
- **THEN** the citizen must explicitly select it before Continue becomes available

#### Scenario: No certificate is selected
- **WHEN** the citizen has not made a choice
- **THEN** Continue remains disabled and the interface explains that one certificate must be selected

### Requirement: Step 2 follows the supplied composition with singular behavior
The frontend SHALL use the supplied SPEC-16A image as a visual reference for hierarchy, certificate cards and actions, while replacing multiple-selection wording and behavior with singular wording. It MUST omit select-all behavior, MUST NOT show two selected cards, and SHALL communicate only zero or one selected certificate.

#### Scenario: Reference text contradicts the current rule
- **WHEN** the image says that one or several certificates can be marked or depicts multiple selected cards
- **THEN** the implementation uses singular citizen-facing text and an exclusive radio group while preserving only unaffected visual composition

#### Scenario: Step 2 is used with keyboard or mobile viewport
- **WHEN** the citizen navigates by keyboard or uses a supported narrow viewport
- **THEN** every radio control, certificate field and action remains labelled, focused, readable and operable without horizontal page overflow

### Requirement: Selection API accepts one authoritative UUID
The protected selection operation SHALL accept exactly one canonical `certificateUuid` in its JSON body and SHALL derive the request from the current authenticated session. It MUST reject missing, blank, malformed, array-valued, unknown, unavailable, cross-request or otherwise unauthorized certificate values without partially modifying persistence.

#### Scenario: Valid certificate is submitted
- **WHEN** the submitted UUID belongs to an available certificate of the active request
- **THEN** the backend persists it as the only selected certificate and returns a state that permits step 3

#### Scenario: Multiple UUIDs are submitted
- **WHEN** a client sends the former `certificateUuids` collection or otherwise attempts to submit more than one certificate
- **THEN** request validation rejects the payload and no selection flag or timestamp changes

#### Scenario: Arbitrary certificate is submitted
- **WHEN** the UUID is absent from the active request or no longer available
- **THEN** the entire operation is rejected through the standard controlled error contract

### Requirement: A new choice atomically replaces the previous choice
Before confirmation, the backend SHALL serialize selection changes for the active request, lock its certificate rows, select the submitted certificate and deselect every other row in one transaction. Repeating the same UUID SHALL be idempotent and SHALL preserve its existing selection timestamp.

#### Scenario: Citizen changes the selected certificate
- **WHEN** one certificate is already selected and the citizen submits another valid certificate
- **THEN** the former row becomes unselected, the new row becomes selected and no intermediate or committed state contains two selections

#### Scenario: Same selection is repeated
- **WHEN** the same UUID is submitted again
- **THEN** the operation succeeds without changing its original selection timestamp or adding duplicate audit evidence

#### Scenario: Conflicting tabs submit different certificates
- **WHEN** two concurrent transactions attempt to replace the selection
- **THEN** request locking and database uniqueness allow one coherent result and return a controlled conflict where applicable, never two selected rows

### Requirement: Step 4 reviews and confirms one persisted certificate
The backend SHALL build the review from exactly one selected, available, request-owned certificate and SHALL expose it as a singular `certificate` object. The frontend SHALL render one certificate summary. Review and confirmation MUST reject zero or multiple persisted selections and MUST NOT trust certificate details sent by the browser.

#### Scenario: Ready request opens step 4
- **WHEN** identity, reason and exactly one selected certificate are valid
- **THEN** the response and view show that certificate's order number, creation date and abbreviated UUID in singular form

#### Scenario: Persisted selection is inconsistent
- **WHEN** zero or more than one selected row is found during review or confirmation
- **THEN** the backend rejects the operation before confirmation and does not create consent or revocation evidence

#### Scenario: Citizen returns before confirmation
- **WHEN** the citizen returns to an editable prior step and chooses a different certificate
- **THEN** the next review is rebuilt from persistence and displays only the replacement certificate

### Requirement: Singular contracts and behavior remain synchronized
OpenAPI, generated TypeScript contracts, frontend API helpers, documentation and automated tests SHALL use singular selection semantics. Tests SHALL cover empty choice, one-item list, multi-item list with exclusive choice, replacement, crafted multiple payload, ownership, concurrency, database uniqueness and singular confirmation.

#### Scenario: Contract verification runs
- **WHEN** the committed OpenAPI snapshot and TypeScript declarations are checked
- **THEN** the request contains `certificateUuid`, the review contains `certificate`, and no active contract describes a selected UUID collection

#### Scenario: Complete regression suite runs
- **WHEN** backend, MySQL integration and frontend tests execute
- **THEN** they prove that no supported path can persist or confirm more than one selected certificate per request
