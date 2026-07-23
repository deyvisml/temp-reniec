## MODIFIED Requirements

### Requirement: Local eligibility mock is deterministic
Local and test profiles SHALL provide a deterministic availability mock whose normal result for any valid DNI not reserved as a special fixture is `AVAILABLE`. Documented fictitious fixtures SHALL produce `NOT_AVAILABLE`, `INCONCLUSIVE`, `UNAVAILABLE`, timeout and technical error. The mock MUST NOT use randomness, obtain real citizen data, produce certificate counts or return certificate objects, and it MUST NOT be active outside local or test profiles.

#### Scenario: Normal valid DNI is submitted
- **WHEN** the local mock receives a valid DNI that is not a documented special fixture
- **THEN** it deterministically returns `AVAILABLE` so the same DNI can be verified by the selected ID Perú adapter

#### Scenario: Documented alternative fixture is used repeatedly
- **WHEN** the same fictitious DNI reserved for a non-success scenario is submitted more than once under equivalent state
- **THEN** the mock produces the same documented outcome each time

#### Scenario: Positive result is inspected
- **WHEN** availability is confirmed for a normal valid DNI
- **THEN** the result contains no order number, creation date, UUID or certificate collection
