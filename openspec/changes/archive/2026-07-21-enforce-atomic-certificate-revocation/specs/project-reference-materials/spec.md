## MODIFIED Requirements

### Requirement: Permanent functional context
The repository SHALL maintain `docs/context/PROJECT_CONTEXT.md` as the sole current primary source for understanding the domain, scope, actors, business rules, states, and constraints. Confirmed business decisions SHALL be incorporated into that document without requiring a replacement attachment, and superseded statements in the same document MUST be corrected so they cannot be interpreted as simultaneously current. Historical material MAY remain only where it is clearly archival and non-current.

#### Scenario: Confirmed decision is incorporated
- **WHEN** the all-or-none revocation rule is approved
- **THEN** `PROJECT_CONTEXT.md` consistently describes atomic cancellation and no longer presents partial cancellation as valid

#### Scenario: Contributor searches for the current source of truth
- **WHEN** current project documentation is inspected
- **THEN** only `docs/context/PROJECT_CONTEXT.md` is identified as the authoritative functional context and any historical copy is explicitly non-current

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

### Requirement: Updated certificate-selection domain is documented
The current reference documentation SHALL state that the initial lookup returns a request-linked list of current certificate issues; certificate data is hidden until identity verification; the selection step always appears; and the citizen selects at least one, several, or all available certificates. It SHALL state that unselected certificates remain outside the operation, the confirmed selected UUID list is processed atomically, all selected certificates are revoked together or none are revoked, and only successful, failed, or uncertain overall outcomes are valid. It MUST NOT describe a confirmed partial result or independent per-certificate result as current behavior. The receipt SHALL identify the selected set and its one common result.

#### Scenario: Contributor prepares a later functional change
- **WHEN** the contributor consults permanent references for selection, revocation or receipt behavior
- **THEN** flexible selection and all-or-none execution are available without relying on this conversation

#### Scenario: Partial behavior is searched
- **WHEN** current functional documentation is searched for partial cancellation semantics
- **THEN** any occurrence is either removed, explicitly superseded, or limited to historical material

### Requirement: Affected current documentation is audited without rewriting history
Non-archived project documentation and current specifications SHALL be reviewed for references affected by atomic execution, individual results, partial outcomes, states, schema cardinality and receipt content. Functional references and implementation-state documents SHALL be corrected to match the resulting code and schema. Archived OpenSpec changes MUST remain unchanged as historical records.

#### Scenario: Current document describes partial behavior
- **WHEN** a non-archived document states that selected certificates can receive mixed confirmed results
- **THEN** it is corrected to describe one atomic outcome or clearly identified as a pending implementation gap during the change

#### Scenario: Historical change contains old terminology
- **WHEN** the audit encounters a file under `openspec/changes/archive/`
- **THEN** the file remains unchanged and is not treated as a current source of functional truth

