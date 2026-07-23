## ADDED Requirements

### Requirement: ID Perú configuration is external, validated and environment-specific
The backend SHALL bind typed ID Perú and flow-authorization properties for mode, credentials, provider URIs, issuer, redirect/return URIs, authentication mechanism, max age, Referer, timeouts, artifact TTLs, allowed algorithms and internal cryptographic keys. Real mode SHALL fail closed on missing or unsafe values; examples and documentation SHALL contain placeholders only.

#### Scenario: Local mock starts with no institutional credentials
- **WHEN** the local profile selects mock mode
- **THEN** the backend starts with deterministic fictitious configuration and makes no call to ID Perú

#### Scenario: Real mode is incomplete
- **WHEN** a mandatory secret, registered URI, issuer, mechanism or cryptographic key is absent
- **THEN** startup or identity initialization fails with a controlled non-secret configuration message

#### Scenario: Production selects unsafe mode or URI
- **WHEN** production uses mock mode or a non-HTTPS provider endpoint
- **THEN** configuration is rejected

### Requirement: Maintained JOSE support is minimal and centralized
The backend SHALL use one maintained JOSE/JWT dependency compatible with its Spring Boot version and centralize ID Perú JWT validation and flow-token signing/verification. It MUST NOT add an OAuth authorization server, broad security framework features, multiple competing JWT libraries or custom RSA parsing when the chosen library supports the requirement.

#### Scenario: Dependencies are reviewed
- **WHEN** the backend dependency tree is inspected
- **THEN** it contains only the minimal JOSE/security modules justified by provider and cookie-token validation

#### Scenario: Invalid algorithm is received
- **WHEN** a JWT advertises `none` or an unapproved algorithm
- **THEN** the centralized validator rejects it before claim access

### Requirement: Security filters protect only declared flow boundaries
The backend SHALL leave public only the endpoints required for initial query, ID Perú initiation/callback, health and environment-appropriate API documentation, while requiring the proper continuation or verified-flow purpose for identity state and future post-authentication APIs. Cookie-authenticated mutations SHALL also validate an allowed request origin.

#### Scenario: Public callback arrives from ID Perú
- **WHEN** the provider posts to the registered callback without a browser continuation cookie
- **THEN** the callback can resolve and validate the attempt by state without opening other protected APIs

#### Scenario: Cross-origin mutation uses a valid cookie
- **WHEN** a disallowed Origin submits a cookie-authenticated mutation
- **THEN** the request is rejected despite the cookie

