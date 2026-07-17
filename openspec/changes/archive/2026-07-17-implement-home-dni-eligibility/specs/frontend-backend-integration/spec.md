## ADDED Requirements

### Requirement: Functional eligibility contract uses the shared transport
The citizen eligibility client SHALL use the existing centralized JSON transport, environment-based backend URL, credentials mode, timeout, abort handling, correlation propagation, and common error mapping. Its request and response types SHALL come from the generated OpenAPI declarations rather than handwritten duplicate DTOs.

#### Scenario: Eligibility request succeeds
- **WHEN** the frontend submits a valid DNI and the backend returns a functional outcome
- **THEN** the client returns typed outcome data and the response correlation identifier to the feature without duplicating transport behavior

#### Scenario: Eligibility request fails
- **WHEN** the backend returns a common API error, times out, sends invalid JSON, or cannot be reached
- **THEN** the shared transport produces the established typed client error and the feature maps it to the appropriate citizen state

## REMOVED Requirements

### Requirement: Temporary visible integration indicator
**Reason**: The citizen home must no longer display project diagnostics; real eligibility submission becomes the visible consumer of the completed integration.

**Migration**: Remove the indicator from `/` while preserving the versioned system-status endpoint, centralized HTTP transport, correlation, OpenAPI synchronization and dedicated technical integration tests.
