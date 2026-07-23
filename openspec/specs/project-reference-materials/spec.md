# Project Reference Materials Specification

## Purpose

Define the permanent functional context, visual references, technical decisions, and documentation-only boundaries that guide subsequent project work.
## Requirements
### Requirement: Permanent functional context
The repository SHALL maintain `docs/context/PROJECT_CONTEXT.md` as the sole current primary source for understanding the domain, scope, actors, business rules, states, and constraints. It SHALL distinguish the initial DNI-based certificate-existence service from the post-authentication certificate-listing service throughout the flow, actors, rules, scenarios, states and diagrams. Superseded statements that attribute order number, creation date, UUID, count, list persistence or certificate creation to the initial query MUST be corrected so they cannot be interpreted as current. Historical material MAY remain only where it is clearly archival and non-current.

#### Scenario: Confirmed service separation is incorporated
- **WHEN** the updated context is reviewed
- **THEN** the home query returns only confirmed availability or non-availability while the detailed list belongs exclusively after successful identity authentication

#### Scenario: Contributor searches for the current source of truth
- **WHEN** current project documentation is inspected
- **THEN** only `docs/context/PROJECT_CONTEXT.md` is identified as authoritative and no current passage describes the initial service as a list provider

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

### Requirement: Reused-design inconsistencies are explicit
The visual reference README SHALL identify every reused image whose internal stepper, numbering, or wording belongs to the previous four-step flow. It SHALL preserve those images unchanged, identify the elements that are not functionally current, and state that their composition remains useful while the five-step flow and selective behavior govern future implementation.

#### Scenario: Reused view displays old numbering
- **WHEN** a contributor opens `step-1.png`, `step-3.png`, `step-4.png`, or `step-5-final.png`
- **THEN** the README has already warned that any four-step stepper or displaced step number is not a current functional requirement

#### Scenario: Reused view implies cancellation of all certificates
- **WHEN** a contributor reads old all-certificates wording in confirmation or receipt imagery
- **THEN** the README directs the contributor to implement only selected-certificate behavior and record the visual wording for later correction

### Requirement: Affected current documentation is audited without rewriting history
Non-archived project documentation and current specifications SHALL be reviewed for references affected by certificate existence, detailed listing, initial response fields, mock behavior, request states, persistence relationships, OpenAPI examples and test fixtures. Functional references and implementation-state documents SHALL be corrected to match the resulting code and schema. Archived OpenSpec changes MUST remain unchanged as historical records.

#### Scenario: Current document describes the initial service as a list provider
- **WHEN** a non-archived document attributes certificate details or rows to the initial query
- **THEN** it is corrected to describe the existence-only service and reserves detailed data for the post-authentication listing

#### Scenario: Historical change contains the superseded rule
- **WHEN** the audit encounters a file under `openspec/changes/archive/`
- **THEN** the file remains unchanged and is not treated as a current source of functional truth

### Requirement: ID Perú v1.2 technical reference is permanent
The repository SHALL contain the supplied PDF unchanged at `docs/integrations/id-peru/IDAAS-V2-Especificaciones-Tecnicas-v1.2.pdf` and a neighboring README identifying it as the primary technical reference, version 1.2, approved on 22/05/2026. The README SHALL identify dependent implementation areas, institutional configuration prerequisites and the prohibition on documenting credentials.

#### Scenario: Future authentication work begins
- **WHEN** a contributor changes ID Perú authentication, tokens, user data, logout or related security
- **THEN** project guidance requires consultation of the permanent PDF and its README first

#### Scenario: PDF integrity is verified
- **WHEN** the repository copy is compared with the supplied attachment
- **THEN** byte size and SHA-256 match and the 22-page document opens successfully

### Requirement: Reference-project decisions are documented critically
The integration documentation SHALL identify which decisions from `C:\FastFolder\sistema-autorizacion-certificados-reniec` were reused, adapted or discarded. It MUST NOT copy credentials, secrets, tokens, `.env` files, production configuration, obsolete v1 behavior or domain-specific session architecture.

#### Scenario: Contributor reviews implementation provenance
- **WHEN** the ID Perú README or architecture note is read
- **THEN** it explains the adopted provider separation, PKCE, vd, HTTP and JWKS lessons and the reasons deterministic verifier, callback GET, v1, circuit breaker and reference-domain sessions were not adopted

#### Scenario: Repository is scanned for borrowed credentials
- **WHEN** tracked documentation and configuration are inspected
- **THEN** no credential value from the reference project or PDF examples is present

### Requirement: Institutional unknowns remain explicit
Current documentation SHALL list as pending institutional confirmation the authorized client credentials, registered redirect URI, issuer, actual userinfo host, Referer, `acr_values`, optional max age, required claims and remote logout contract. It MUST NOT present a live institutional test as complete until authorized configuration has been exercised.

#### Scenario: Automated real-adapter tests pass
- **WHEN** tests against a controlled provider server succeed but institutional credentials are unavailable
- **THEN** documentation states that implementation is protocol-complete while live institutional validation remains pending
