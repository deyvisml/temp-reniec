## MODIFIED Requirements

### Requirement: Updated certificate-selection domain is documented
The current reference documentation SHALL state that the initial lookup receives DNI and confirms only whether at least one cancellable certificate currently exists, without returning a list, count, order number, creation date or UUID. It SHALL state that successful authentication is followed by the implemented second-service boundary that obtains the detailed request-linked list, after which the selection step always appears and the citizen selects at least one, several, or all available certificates. The documentation SHALL identify deterministic local fixtures and SHALL distinguish the internal port and mock from a real adapter that requires an official contract. The confirmed selected set SHALL remain governed by atomic revocation.

#### Scenario: Contributor prepares the initial query
- **WHEN** permanent references are consulted for the home flow
- **THEN** they require a boolean external response normalized into distinct functional and technical outcomes and prohibit certificate-row creation

#### Scenario: Contributor prepares certificate selection
- **WHEN** permanent references are consulted for step 2
- **THEN** they identify the second service as the source of order number, creation date and UUID and explain the implemented persistence, selection and atomicity rules

#### Scenario: Availability changes between services
- **WHEN** the first service was positive but the second service later returns an empty list
- **THEN** the documented flow blocks continuation, reports that certificates are no longer available, does not classify authentication as failed and invents no certificate

#### Scenario: Real provider contract is unavailable
- **WHEN** a contributor configures or extends the second-service integration
- **THEN** the documentation prohibits fictitious production endpoints and directs them to the deterministic mock until the official contract is validated
