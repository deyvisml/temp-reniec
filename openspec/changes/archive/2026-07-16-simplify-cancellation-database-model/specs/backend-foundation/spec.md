## MODIFIED Requirements

### Requirement: Technical-foundation-only boundary
The backend foundation MAY contain MySQL, Flyway, Testcontainers, the simplified seven-table cancellation-request persistence model specified by `cancellation-request-persistence-model`, the versioned technical integration API, and a development-only Docker Compose service containing MySQL 8.4. It MUST NOT containerize backend or frontend, add application Dockerfiles or PowerShell startup scripts, or introduce citizen-flow endpoints or use cases; functional JWT or refresh-token behavior; real ID Perú, certificate-lookup, revocation, document-storage, or other external integrations; complete progress recovery; PDF generation; production deployment; administrative modules; or citizen UI behavior. It MUST NOT introduce microservices, queues, event sourcing, CQRS, a multi-module Maven build, another database, Redis, workflow stored procedures, complex triggers, generated guard columns, unassessed tables, or unused layers.

#### Scenario: Completed change is reviewed for scope
- **WHEN** the implementation diff, Compose model, runtime routes, migration and JPA model are inspected
- **THEN** they contain the technical backend foundation, seven justified and simplified persistence tables, the technical integration, a single local MySQL service, tests and documentation, with no functional citizen flow, external integration, production deployment configuration or administrative capability
