# Project Reference Materials Specification

## Purpose

Define the permanent functional context, visual references, technical decisions, and documentation-only boundaries that guide subsequent project work.
## Requirements
### Requirement: Permanent functional context
The repository SHALL maintain `docs/context/PROJECT_CONTEXT.md` as the sole current primary source for understanding the domain, scope, actors, business rules, states, and constraints. Confirmed business decisions SHALL be incorporated into that document without requiring a replacement attachment, and superseded statements in the same document MUST be corrected so they cannot be interpreted as simultaneously current. Historical material MAY remain only where it is clearly archival and non-current.

#### Scenario: Confirmed decision is incorporated
- **WHEN** the all-or-none revocation rule is approved
- **THEN** `PROJECT_CONTEXT.md` consistently describes atomic cancellation and no longer presents partial cancellation as valid

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
The repository SHALL contain `docs/ui-reference/README.md` identifying `home.png` as DNI entry and initial lookup, `step-1.png` as identity verification, `step-2.png` as selection of one or more current digital certificates, `step-3.png` as reason selection, `step-4.png` as review and atomic confirmation, and `step-5-final.png` as the common operation result and receipt. It SHALL describe the screen sequence as one home screen followed by five numbered steps and MUST state that per-certificate or partial wording visible inside reused images is not functionally current.

#### Scenario: Contributor identifies a flow view
- **WHEN** a contributor reads the visual reference README
- **THEN** every image is mapped to the correct role and confirmation/result views are interpreted using the atomic rule

#### Scenario: Reused image implies individual results
- **WHEN** a design contains wording that suggests some selected certificates can succeed while others fail
- **THEN** the README identifies the inconsistency and directs implementation to the all-or-none context

### Requirement: Reference authority and usage rules
The visual reference README SHALL state that `PROJECT_CONTEXT.md` is the primary functional source, the images are the primary composition references, and designs MUST NOT be used to invent or preserve superseded rules. It SHALL require the context to prevail over contradictory image text or numbering, require the difference to be recorded for later UI correction, and require later domain or interface tasks to review both the context and the corresponding visual reference before implementation.

#### Scenario: Functional contradiction is discovered
- **WHEN** an image implies partial cancellation, independent per-certificate outcomes, obsolete step numbering, or another behavior that conflicts with `PROJECT_CONTEXT.md`
- **THEN** the atomic five-step context prevails and the image remains only a composition reference for unaffected elements

#### Scenario: A later domain or interface change begins
- **WHEN** a contributor starts a task related to selection, confirmation, revocation, result or receipt
- **THEN** the contributor is directed to review the updated functional context and the visual reference before implementing it

### Requirement: Agreed technical decisions record
The repository SHALL contain a concise document under `docs/` recording Spring Boot for the backend, Next.js for the frontend, MySQL for the database, Tailwind CSS for styles, and future top-level `/backend` and `/frontend` directories. It SHALL also record JWT-based sessions and authenticated frontend-backend communication, backend and MySQL persistence of process progress, cross-browser or cross-device progress recovery after renewed citizen identity verification, and a simple, incremental, maintainable architecture.

#### Scenario: Contributor reviews the agreed foundation
- **WHEN** a contributor consults the technical decisions document
- **THEN** the selected stack, future directory layout, authentication approach, progress persistence, recovery behavior, and architectural direction are stated without requiring application scaffolding

### Requirement: Architectural constraints record
The technical decisions document SHALL require a database design without overengineering and SHALL prohibit creating a table for every screen, step, or state. It SHALL exclude microservices, queues, event sourcing, CQRS, and complex patterns unless a demonstrated need emerges; require external integrations to use interfaces and replaceable mocks until official contracts exist; and exclude administrative modules and functionality outside the citizen flow.

#### Scenario: Contributor evaluates a future architecture choice
- **WHEN** a contributor proposes a data model, architectural pattern, external integration, or module
- **THEN** the recorded constraints provide the baseline for rejecting unnecessary complexity and out-of-scope functionality

### Requirement: Documentation-only change boundary
The change SHALL modify only reference assets, Markdown documentation, and OpenSpec planning or specification artifacts. It MUST NOT modify `/backend`, `/frontend`, database migrations, JPA entities, repositories, OpenAPI runtime contracts, mocks, JWT, external integrations, revocation behavior, receipt generation, dependencies, or executable configuration; alter or regenerate UI images; or claim that the existing implementation already supports the updated flow.

#### Scenario: Change contents are reviewed
- **WHEN** the completed implementation diff is inspected
- **THEN** it contains no executable application, infrastructure, schema, dependency, generated-contract, or functional citizen-flow change

### Requirement: Updated certificate-selection domain is documented
The current reference documentation SHALL state that the initial lookup returns a request-linked list of current certificate issues; certificate data is hidden until identity verification; the selection step always appears; and the citizen selects at least one, several, or all available certificates. It SHALL state that unselected certificates remain outside the operation, the confirmed selected UUID list is processed atomically, all selected certificates are revoked together or none are revoked, and only successful, failed, or uncertain overall outcomes are valid. It MUST NOT describe a confirmed partial result or independent per-certificate result as current behavior. The receipt SHALL identify the selected set and its one common result.

#### Scenario: Contributor prepares a later functional change
- **WHEN** the contributor consults permanent references for selection, revocation or receipt behavior
- **THEN** flexible selection and all-or-none execution are available without relying on this conversation

#### Scenario: Partial behavior is searched
- **WHEN** current functional documentation is searched for partial cancellation semantics
- **THEN** any occurrence is either removed, explicitly superseded, or limited to historical material

### Requirement: Reused-design inconsistencies are explicit
The visual reference README SHALL identify every reused image whose internal stepper, numbering, or wording belongs to the previous four-step flow. It SHALL preserve those images unchanged, identify the elements that are not functionally current, and state that their composition remains useful while the five-step flow and selective behavior govern future implementation.

#### Scenario: Reused view displays old numbering
- **WHEN** a contributor opens `step-1.png`, `step-3.png`, `step-4.png`, or `step-5-final.png`
- **THEN** the README has already warned that any four-step stepper or displaced step number is not a current functional requirement

#### Scenario: Reused view implies cancellation of all certificates
- **WHEN** a contributor reads old all-certificates wording in confirmation or receipt imagery
- **THEN** the README directs the contributor to implement only selected-certificate behavior and record the visual wording for later correction

### Requirement: Affected current documentation is audited without rewriting history
Non-archived project documentation and current specifications SHALL be reviewed for references affected by atomic execution, individual results, partial outcomes, states, schema cardinality and receipt content. Functional references and implementation-state documents SHALL be corrected to match the resulting code and schema. Archived OpenSpec changes MUST remain unchanged as historical records.

#### Scenario: Current document describes partial behavior
- **WHEN** a non-archived document states that selected certificates can receive mixed confirmed results
- **THEN** it is corrected to describe one atomic outcome or clearly identified as a pending implementation gap during the change

#### Scenario: Historical change contains old terminology
- **WHEN** the audit encounters a file under `openspec/changes/archive/`
- **THEN** the file remains unchanged and is not treated as a current source of functional truth

