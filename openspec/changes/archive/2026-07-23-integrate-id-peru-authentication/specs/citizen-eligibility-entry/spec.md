## ADDED Requirements

### Requirement: Positive availability creates only a current-browser identity handoff
The initial query SHALL treat `AVAILABLE` as permission to begin identity verification, not as verified identity or authorization for certificate listing. Its successful response SHALL establish the short-lived current-browser continuation required by the identity step, and the frontend SHALL navigate to `/verificacion-identidad` without DNI, request ID or certificate data in the URL.

#### Scenario: Availability is positive
- **WHEN** the protected initial query returns `AVAILABLE`
- **THEN** the browser can enter the identity page but cannot access the post-authentication listing boundary

#### Scenario: Availability is not positive
- **WHEN** the initial result is negative, inconclusive or technical failure
- **THEN** no identity continuation is established and ID Perú cannot be started

#### Scenario: Continuation is copied to another browser
- **WHEN** a citizen opens the identity route elsewhere without the HttpOnly continuation
- **THEN** the prior request is not recovered and the initial query must be repeated

