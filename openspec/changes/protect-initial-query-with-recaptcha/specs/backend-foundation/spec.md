## MODIFIED Requirements

### Requirement: Technical-foundation-only boundary
The backend foundation MAY contain MySQL, Flyway, Testcontainers, the corrected seven-table cancellation-request model, forward migrations through the availability correction, the versioned technical API, local MySQL Compose, Swagger/OpenAPI, the initial certificate-availability endpoint specified by `citizen-eligibility-entry`, and the Google reCAPTCHA v2 verification adapter specified by `initial-query-recaptcha-protection`. It MUST NOT persist CAPTCHA evidence or request sessions, add recovery windows or speculative guards, containerize the applications, implement JWT, ID Perú, the post-authentication listing service, its contract or attempt table, selection UI or endpoint, real revocation execution, document generation, production deployment, administration, microservices, queues, event sourcing, CQRS, another database or unused layers.

#### Scenario: Protected foundation is reviewed for scope
- **WHEN** routes, configuration, ports, adapters, OpenAPI, frontend contract and dependencies are inspected
- **THEN** the foundation validates CAPTCHA before the existence-only first service and preserves the certificate table for later listing without implementing any later flow

#### Scenario: Backend dependency set is reviewed
- **WHEN** Maven dependencies are compared before and after the change
- **THEN** Google verification uses Spring HTTP functionality already provided by the web starter and adds no preventive resilience, security or CAPTCHA SDK dependency

#### Scenario: Persistence schema is reviewed
- **WHEN** Flyway migrations and JPA entities are compared before and after the change
- **THEN** no table or column was added for CAPTCHA tokens, responses, IP addresses or challenge metadata
