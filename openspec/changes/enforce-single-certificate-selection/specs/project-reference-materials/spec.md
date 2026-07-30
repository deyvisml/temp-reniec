## MODIFIED Requirements

### Requirement: Visual flow index
The repository SHALL contain `docs/ui-reference/README.md` identifying `home.png` as DNI entry and the boolean existence query, `step-1.png` as identity verification, `step-2.png` as detailed listing and selection of exactly one current digital certificate, `step-3.png` as reason selection, `step-4.png` as review and confirmation of that certificate, and `step-5-final.png` as its result and receipt. It SHALL state that certificate order number, creation date and UUID first become available through the second service after authentication, not through the home query.

#### Scenario: Contributor identifies the two queries
- **WHEN** a contributor reads the visual reference README
- **THEN** the home and step-2 references identify different service responsibilities and no detailed certificate data is attributed to the home screen

#### Scenario: New step-2 image implies multiple selection
- **WHEN** the reference depicts multiple checked cards or plural selection text
- **THEN** the README identifies those details as superseded and requires an exclusive single-certificate control

### Requirement: Updated certificate-selection domain is documented
The current reference documentation SHALL state that the initial lookup receives DNI and confirms only whether at least one cancellable certificate exists, without returning a list, count, order number, creation date or UUID. It SHALL state that successful authentication is followed by the second service that obtains the complete detailed request-linked list, after which step 2 always appears and the citizen selects exactly one available certificate for that request. The confirmed certificate SHALL be the only target of the future revocation and receipt.

#### Scenario: Contributor prepares the initial query
- **WHEN** permanent references are consulted for the home flow
- **THEN** they require an existence-only response and prohibit certificate-row creation at that stage

#### Scenario: Contributor prepares certificate selection
- **WHEN** permanent references are consulted for step 2
- **THEN** they identify the second service as the source of the full list but require exactly one explicit selected certificate

#### Scenario: Availability changes between services
- **WHEN** the first service was positive but the second service returns an empty list
- **THEN** the documented flow blocks continuation, does not classify authentication as failed and invents no certificate

### Requirement: Reused-design inconsistencies are explicit
The visual reference README SHALL identify every reused image whose internal stepper, numbering or wording is obsolete. It SHALL preserve those images unchanged and state that their composition remains useful while the five-step flow and single-certificate rule govern implementation.

#### Scenario: Reused view displays old numbering
- **WHEN** a contributor opens a reference containing the previous four-step flow
- **THEN** the README warns that its stepper and displaced number are not current functional requirements

#### Scenario: Selection image shows multiple checked certificates
- **WHEN** the SPEC-16A image is consulted
- **THEN** the README directs the contributor to implement radio-style exclusive selection and singular text rather than reproduce multiple active cards

#### Scenario: Confirmation or receipt image implies several certificates
- **WHEN** old plural wording is visible in step 4 or step 5
- **THEN** the README directs the contributor to present only the one certificate belonging to the operation
