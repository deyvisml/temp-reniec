## MODIFIED Requirements

### Requirement: Minimal managed dependencies
The backend SHALL use Spring Boot's managed web MVC, validation, Actuator, Spring Data JPA, Flyway MySQL, and MySQL driver support; Springdoc OpenAPI's API-only WebMVC starter required to publish the implemented `/api/v1/**` contract; plus the minimum Spring Boot and MySQL Testcontainers test support required by the implemented foundation, cancellation-request persistence model, and technical integration. It MUST NOT add Swagger UI, security, messaging, cache, alternate database, external-integration, distributed-tracing, cryptographic, document-generation, SDK-generation, or other preventive dependencies.

#### Scenario: Dependency set is reviewed
- **WHEN** the Maven dependency declarations are inspected
- **THEN** every direct dependency is required for HTTP serving, validation, health, MySQL persistence and migrations, OpenAPI contract publication, or the specified tests and no out-of-scope starter is present
