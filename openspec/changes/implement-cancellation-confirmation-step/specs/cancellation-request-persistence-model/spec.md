## MODIFIED Requirements

### Requirement: Reason and consent integrity
The request SHALL store a cancellation-reason code controlled by the backend from `THEFT`, `LOSS`, `DEVICE_OR_NUMBER_CHANGE`, `SUSPECTED_UNAUTHORIZED_USE`, and `OTHER`. `OTHER` SHALL store its description as readable text of at most 300 characters; other codes SHALL leave that field null. Confirmation SHALL require a valid reason, confirmation time and the stable version of the backend-owned consent text shown to the citizen. Once confirmed, application behavior MUST NOT modify the reason, description, selected certificate set, confirmation time or consent version. The consent text itself MUST NOT be duplicated in each request row.

#### Scenario: OTHER reason is persisted
- **WHEN** a request selects `OTHER`
- **THEN** the request row stores a directly readable, length-limited description

#### Scenario: Confirmed reason is changed
- **WHEN** application behavior attempts to replace the reason of a confirmed request
- **THEN** the update is rejected before persistence

#### Scenario: Incomplete confirmation is attempted
- **WHEN** a request is confirmed without reason, confirmation time or current consent version
- **THEN** entity validation or application behavior rejects the inconsistent state

#### Scenario: New confirmation is persisted
- **WHEN** the citizen confirms the backend-owned consent text
- **THEN** the request stores its confirmation time and stable consent version without duplicating the full text

#### Scenario: Historical confirmation predates versioned consent
- **WHEN** an existing confirmed row is migrated from an earlier schema
- **THEN** its consent version may remain null without fabricating retroactive evidence, while all new confirmations require a value
