## ADDED Requirements

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

