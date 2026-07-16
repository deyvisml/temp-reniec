# Project Reference Materials Specification

## Purpose

Define the permanent functional context, visual references, technical decisions, and documentation-only boundaries that guide subsequent project work.

## Requirements

### Requirement: Permanent functional context
The repository SHALL contain an exact-content copy of the supplied `PROJECT_CONTEXT.md` at `docs/context/PROJECT_CONTEXT.md`, and this document SHALL be identified as the primary source for understanding the domain, scope, actors, business rules, states, and constraints.

#### Scenario: Functional context is available from the repository
- **WHEN** a contributor needs functional project context
- **THEN** the complete supplied document is available at `docs/context/PROJECT_CONTEXT.md` without depending on an external attachment or conversation

#### Scenario: Functional context integrity is verified
- **WHEN** the incorporated document is compared with the supplied source file
- **THEN** its content, byte size, and SHA-256 hash match the source

### Requirement: Permanent visual references
The repository SHALL contain the five supplied PNG files at `docs/ui-reference/home.png`, `docs/ui-reference/step-1.png`, `docs/ui-reference/step-2.png`, `docs/ui-reference/step-3.png`, and `docs/ui-reference/step-4-final.png`. Each destination file SHALL preserve the exact binary content of its source; only `step-4 (final).png` SHALL be renamed to `step-4-final.png`.

#### Scenario: All visual references are available
- **WHEN** a contributor accesses `docs/ui-reference/`
- **THEN** all five expected PNG files exist at the specified permanent paths

#### Scenario: Visual reference integrity is verified
- **WHEN** each destination image is compared with its corresponding supplied source
- **THEN** its byte size and SHA-256 hash match the source and the file can be decoded as a PNG

### Requirement: Visual flow index
The repository SHALL contain `docs/ui-reference/README.md` identifying `home.png` as the home page for DNI entry and initial certificate lookup, `step-1.png` as identity verification through ID Perú, `step-2.png` as cancellation reason selection, `step-3.png` as review, consent, and confirmation, and `step-4-final.png` as the final result and receipt.

#### Scenario: Contributor identifies a flow view
- **WHEN** a contributor reads the visual reference README
- **THEN** the README maps every supplied image to its specified role in the citizen flow and links to its repository file

### Requirement: Reference authority and usage rules
The visual reference README SHALL state that `PROJECT_CONTEXT.md` is the primary functional source, the images are the primary visual references for view implementation, and designs MUST NOT be used to invent unconfirmed functional rules. It SHALL state that functional contradictions are resolved in favor of `PROJECT_CONTEXT.md`, with the difference recorded as pending validation, and that later domain or interface tasks MUST review these sources before implementation.

#### Scenario: Functional contradiction is discovered
- **WHEN** an image implies behavior that conflicts with `PROJECT_CONTEXT.md`
- **THEN** the documented context prevails and the difference is recorded as pending validation rather than implemented as a new rule

#### Scenario: A later domain or interface change begins
- **WHEN** a contributor starts a task related to the domain or user interface
- **THEN** the contributor is directed to review the functional context and visual references before implementing the change

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
The change SHALL add only the specified reference materials and documentation. It MUST NOT create or configure `/backend`, `/frontend`, Spring Boot, Next.js, MySQL, or Tailwind CSS; implement views, endpoints, JWT, schemas, tables, migrations, mocks, external services, receipt generation, or citizen-flow behavior; alter or redesign the UI images; define a final data model; or invent external contracts.

#### Scenario: Change contents are reviewed
- **WHEN** the completed change is inspected
- **THEN** it contains no executable application, infrastructure configuration, data schema, integration implementation, or functional citizen-flow code
