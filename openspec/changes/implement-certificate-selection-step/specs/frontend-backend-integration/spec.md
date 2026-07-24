## ADDED Requirements

### Requirement: Protected listing and selection contracts remain synchronized
The backend SHALL document the current-request certificate-listing and certificate-selection operations in OpenAPI, including cookie authentication, DTO fields, validation, success states, standard error bodies, correlation and all implemented HTTP codes. The frontend SHALL regenerate its snapshot and TypeScript declarations and SHALL use the central credentials-including client without handwritten duplicate contracts.

#### Scenario: Contract verification runs
- **WHEN** the backend document is compared with committed frontend artifacts
- **THEN** listing and selection operations, enums, required fields and errors are byte-for-byte synchronized after normalization

#### Scenario: Frontend consumes step 2 APIs
- **WHEN** listing or selection is requested
- **THEN** the central client propagates cookies and correlation and maps structured, network and timeout failures consistently

### Requirement: Certificate data respects the authenticated API boundary
The frontend-backend contract SHALL expose certificate details only through a valid internal flow session after identity verification. It MUST NOT place UUID, DNI, request identifiers or session tokens in URLs, browser storage or logs, and MUST NOT accept provider fields from the client as selection authority.

#### Scenario: Certificate selection is submitted
- **WHEN** the frontend sends the citizen's selected set
- **THEN** only UUIDs required for validation travel in the JSON body and the backend resolves all other data from persistence
