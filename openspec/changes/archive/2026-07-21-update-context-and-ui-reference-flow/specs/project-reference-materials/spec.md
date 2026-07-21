## MODIFIED Requirements

### Requirement: Permanent functional context
The repository SHALL contain an exact-content copy of the supplied updated context at `docs/context/PROJECT_CONTEXT.md`, and this document SHALL be the sole current primary source for understanding the domain, scope, actors, business rules, states, and constraints. The repository MUST NOT retain a second active context document that could be interpreted as equally authoritative; historical material MAY remain only where it is clearly archival and non-current.

#### Scenario: Updated functional context is available from the repository
- **WHEN** a contributor needs current functional project context
- **THEN** the complete supplied v2 document is available at `docs/context/PROJECT_CONTEXT.md` without depending on an external attachment or conversation

#### Scenario: Functional context integrity is verified
- **WHEN** the incorporated document is compared with `PROJECT_CONTEXT_v2.md`
- **THEN** its content, byte size, and SHA-256 hash match the supplied source

#### Scenario: Contributor searches for the current source of truth
- **WHEN** current project documentation is inspected
- **THEN** only `docs/context/PROJECT_CONTEXT.md` is identified as the authoritative functional context and any historical copy is explicitly non-current

### Requirement: Permanent visual references
The repository SHALL contain the six supplied PNG files at `docs/ui-reference/home.png`, `docs/ui-reference/step-1.png`, `docs/ui-reference/step-2.png`, `docs/ui-reference/step-3.png`, `docs/ui-reference/step-4.png`, and `docs/ui-reference/step-5-final.png`. Each destination SHALL preserve the exact binary content of its mapped supplied image. The active reference directory MUST NOT retain `step-4-final.png` or another duplicate name from the previous ordering.

#### Scenario: All visual references are available
- **WHEN** a contributor accesses `docs/ui-reference/`
- **THEN** exactly the six expected flow PNG names are present and the new certificate-selection view can be opened at `step-2.png`

#### Scenario: Visual reference integrity is verified
- **WHEN** each destination image is compared with its corresponding supplied source
- **THEN** its byte size and SHA-256 hash match the source and the file decodes successfully as a PNG

#### Scenario: Previous ordering is inspected
- **WHEN** the active visual-reference paths are listed
- **THEN** reason, confirmation, and receipt use steps 3, 4, and 5 respectively and no active `step-4-final.png` remains

### Requirement: Visual flow index
The repository SHALL contain `docs/ui-reference/README.md` identifying `home.png` as DNI entry and the initial lookup of current certificate issues, `step-1.png` as identity verification through ID Perú, `step-2.png` as selection of one or more current digital certificates, `step-3.png` as cancellation-reason selection, `step-4.png` as review and confirmation, and `step-5-final.png` as per-certificate result and receipt. It SHALL describe the screen sequence as one home screen followed by five numbered steps.

#### Scenario: Contributor identifies a flow view
- **WHEN** a contributor reads the visual reference README
- **THEN** every supplied image is linked to its repository file and mapped to the correct role in the five-step citizen flow

#### Scenario: Certificate terminology is interpreted
- **WHEN** the README refers to current issues returned by the initial lookup
- **THEN** it explains that they are presented to citizens as current digital certificates after successful authentication

### Requirement: Reference authority and usage rules
The visual reference README SHALL state that `PROJECT_CONTEXT.md` is the primary functional source, the images are the primary visual references for view implementation, and designs MUST NOT be used to invent unconfirmed functional rules. It SHALL require the context to prevail over contradictory image text or numbering, require the difference to be recorded as pending validation or later UI correction, and require later domain or interface tasks to review both the context and the corresponding visual reference before implementation.

#### Scenario: Functional contradiction is discovered
- **WHEN** an image implies four steps, old numbering, cancellation of every certificate, or another behavior that conflicts with `PROJECT_CONTEXT.md`
- **THEN** the five-step selective-cancellation context prevails and the image remains only a composition reference for the affected elements

#### Scenario: A later domain or interface change begins
- **WHEN** a contributor starts a task related to the domain or a citizen-flow view
- **THEN** the contributor is directed to review the updated functional context and the visual reference for that view before implementing the change

### Requirement: Documentation-only change boundary
The change SHALL modify only reference assets, Markdown documentation, and OpenSpec planning or specification artifacts. It MUST NOT modify `/backend`, `/frontend`, database migrations, JPA entities, repositories, OpenAPI runtime contracts, mocks, JWT, external integrations, revocation behavior, receipt generation, dependencies, or executable configuration; alter or regenerate UI images; or claim that the existing implementation already supports the updated flow.

#### Scenario: Change contents are reviewed
- **WHEN** the completed implementation diff is inspected
- **THEN** it contains no executable application, infrastructure, schema, dependency, generated-contract, or functional citizen-flow change

## ADDED Requirements

### Requirement: Updated certificate-selection domain is documented
The current reference documentation SHALL state that the initial lookup returns a list of current certificate issues containing order number, creation date, and UUID; an empty list blocks continuation; the list remains linked to the cancellation request; certificate data is not shown before identity verification; the selection step is always displayed after authentication, including for one certificate; at least one certificate must be selected; and unselected certificates remain outside the operation. It SHALL state that revocation receives the selected UUID list, produces an individual result per certificate, supports overall successful, partial, failed, or uncertain outcomes, and that the receipt reflects actual per-certificate results.

#### Scenario: Contributor prepares a later functional change
- **WHEN** the contributor consults the permanent references for lookup, selection, revocation, or receipt behavior
- **THEN** the list-based selective-cancellation rules and individual-result semantics are available without relying on this conversation

### Requirement: Reused-design inconsistencies are explicit
The visual reference README SHALL identify every reused image whose internal stepper, numbering, or wording belongs to the previous four-step flow. It SHALL preserve those images unchanged, identify the elements that are not functionally current, and state that their composition remains useful while the five-step flow and selective behavior govern future implementation.

#### Scenario: Reused view displays old numbering
- **WHEN** a contributor opens `step-1.png`, `step-3.png`, `step-4.png`, or `step-5-final.png`
- **THEN** the README has already warned that any four-step stepper or displaced step number is not a current functional requirement

#### Scenario: Reused view implies cancellation of all certificates
- **WHEN** a contributor reads old all-certificates wording in confirmation or receipt imagery
- **THEN** the README directs the contributor to implement only selected-certificate behavior and record the visual wording for later correction

### Requirement: Affected current documentation is audited without rewriting history
Non-archived project documentation and current specifications SHALL be reviewed for references affected by the new flow, terminology, lookup response, selection behavior, individual revocation results, partial outcomes, and receipt. Functional reference documents SHALL be corrected; implementation-state documents that still describe unmodified code or schema SHALL explicitly identify their divergence and defer to `PROJECT_CONTEXT.md` for the target domain. Archived OpenSpec changes MUST remain unchanged as historical records.

#### Scenario: Current document describes the old behavior
- **WHEN** a non-archived document states binary-only eligibility, inseparable certificates, four steps, or obsolete view numbering
- **THEN** it is corrected if it is a functional reference or visibly marked as an implementation gap if it documents code that this change does not modify

#### Scenario: Historical change contains old terminology
- **WHEN** the audit encounters a file under `openspec/changes/archive/`
- **THEN** the file remains unchanged and is not treated as a current source of functional truth

