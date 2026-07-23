## MODIFIED Requirements

### Requirement: Technical-foundation-only boundary
The backend foundation MAY contain MySQL, Flyway, Testcontainers, the corrected seven-table cancellation-request model, forward migrations through the availability correction, the versioned technical API, local MySQL Compose, Swagger/OpenAPI and the initial certificate-availability endpoint specified by `citizen-eligibility-entry`. It MUST NOT persist request sessions, recovery windows or speculative guards; containerize the applications; implement JWT, ID Perú, the post-authentication listing service, its contract or attempt table, selection UI or endpoint, real revocation execution, document generation, production deployment, administration, microservices, queues, event sourcing, CQRS, another database or unused layers.

#### Scenario: Completed correction is reviewed for scope
- **WHEN** migration history, JPA model, routes, gateway, OpenAPI, frontend contract and dependencies are inspected
- **THEN** the existing foundation contains an existence-only first service and preserves the certificate table for later listing without implementing that listing or adding a dependency

#### Scenario: Dependency set is reviewed
- **WHEN** Maven and npm dependency declarations are compared before and after the change
- **THEN** no new runtime or preventive dependency was added for the service separation

### Requirement: Concise local operation documentation
The backend SHALL retain concise local instructions for MySQL, build, tests, health, OpenAPI and Swagger and SHALL document the deterministic fictitious DNI fixtures for the certificate-availability mock. The fixture table SHALL identify positive, negative, inconclusive, unavailable, timeout and technical-error behavior and SHALL state that no fixture returns certificate objects. It SHALL not document a second-service URL, payload or mock before that integration exists.

#### Scenario: Contributor tests the initial flow locally
- **WHEN** a contributor follows the README with the local profile
- **THEN** the contributor can reproduce every normalized availability scenario and understands that detailed certificates are not obtained at this stage

#### Scenario: Contributor searches for the listing service
- **WHEN** local backend documentation is inspected
- **THEN** the second service is identified as future work and no invented endpoint or environment variable is presented as current
