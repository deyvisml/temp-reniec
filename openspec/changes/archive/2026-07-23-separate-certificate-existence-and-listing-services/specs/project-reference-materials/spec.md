## MODIFIED Requirements

### Requirement: Permanent functional context
The repository SHALL maintain `docs/context/PROJECT_CONTEXT.md` as the sole current primary source for understanding the domain, scope, actors, business rules, states, and constraints. It SHALL distinguish the initial DNI-based certificate-existence service from the post-authentication certificate-listing service throughout the flow, actors, rules, scenarios, states and diagrams. Superseded statements that attribute order number, creation date, UUID, count, list persistence or certificate creation to the initial query MUST be corrected so they cannot be interpreted as current. Historical material MAY remain only where it is clearly archival and non-current.

#### Scenario: Confirmed service separation is incorporated
- **WHEN** the updated context is reviewed
- **THEN** the home query returns only confirmed availability or non-availability while the detailed list belongs exclusively after successful identity authentication

#### Scenario: Contributor searches for the current source of truth
- **WHEN** current project documentation is inspected
- **THEN** only `docs/context/PROJECT_CONTEXT.md` is identified as authoritative and no current passage describes the initial service as a list provider

### Requirement: Visual flow index
The repository SHALL contain `docs/ui-reference/README.md` identifying `home.png` as DNI entry and the boolean existence query, `step-1.png` as identity verification, `step-2.png` as the future detailed listing and selection of one or more current digital certificates, `step-3.png` as reason selection, `step-4.png` as review and atomic confirmation, and `step-5-final.png` as the common operation result and receipt. It SHALL state that certificate order number, creation date and UUID first become available to the application through the second service after authentication, not through the home query.

#### Scenario: Contributor identifies the two queries
- **WHEN** a contributor reads the visual reference README
- **THEN** the home and step-2 references identify different service responsibilities and no detailed certificate data is attributed to the home screen

#### Scenario: Reused image implies outdated behavior
- **WHEN** image text or numbering contradicts the confirmed two-service or atomic flow
- **THEN** the README identifies the inconsistency and directs implementation to the functional context

### Requirement: Reference authority and usage rules
The visual reference README SHALL state that `PROJECT_CONTEXT.md` is the primary functional source, the images are primary composition references, and designs MUST NOT be used to invent or preserve superseded rules. It SHALL require the context to prevail over image text that suggests the initial query returns certificate details, exposes a count, skips the authenticated listing, permits partial revocation, or uses obsolete numbering.

#### Scenario: Functional contradiction is discovered
- **WHEN** an image or prior note implies detailed certificates are obtained before authentication
- **THEN** the two-service context prevails and the image remains only a composition reference for unaffected elements

#### Scenario: A later domain or interface change begins
- **WHEN** a contributor starts work on the home, authentication or certificate-selection step
- **THEN** the contributor is directed to review which of the two certificate services belongs to that stage before implementation

### Requirement: Updated certificate-selection domain is documented
The current reference documentation SHALL state that the initial lookup receives DNI and confirms only whether at least one cancellable certificate currently exists, without returning a list, count, order number, creation date or UUID. It SHALL state that successful authentication is followed by a second service that obtains the detailed request-linked list, after which the selection step always appears and the citizen selects at least one, several, or all available certificates. The confirmed selected set SHALL remain governed by atomic revocation.

#### Scenario: Contributor prepares the initial query
- **WHEN** permanent references are consulted for the home flow
- **THEN** they require a boolean external response normalized into distinct functional and technical outcomes and prohibit certificate-row creation

#### Scenario: Contributor prepares certificate selection
- **WHEN** permanent references are consulted for step 2
- **THEN** they identify the second service as the source of order number, creation date and UUID and retain the existing selection and atomicity rules

#### Scenario: Availability changes between services
- **WHEN** the first service was positive but the second service later returns an empty list
- **THEN** the documented flow blocks continuation, reports that certificates are no longer available, does not classify authentication as failed and invents no certificate

### Requirement: Affected current documentation is audited without rewriting history
Non-archived project documentation and current specifications SHALL be reviewed for references affected by certificate existence, detailed listing, initial response fields, mock behavior, request states, persistence relationships, OpenAPI examples and test fixtures. Functional references and implementation-state documents SHALL be corrected to match the resulting code and schema. Archived OpenSpec changes MUST remain unchanged as historical records.

#### Scenario: Current document describes the initial service as a list provider
- **WHEN** a non-archived document attributes certificate details or rows to the initial query
- **THEN** it is corrected to describe the existence-only service and reserves detailed data for the post-authentication listing

#### Scenario: Historical change contains the superseded rule
- **WHEN** the audit encounters a file under `openspec/changes/archive/`
- **THEN** the file remains unchanged and is not treated as a current source of functional truth
