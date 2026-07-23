## MODIFIED Requirements

### Requirement: Continuation is emitted only for eligible requests
The backend SHALL set `canContinue=true` and `nextStep=IDENTITY_VERIFICATION` only when certificate availability is positively confirmed for the current active request. Negative, inconclusive, unavailable, timeout and technical outcomes SHALL block continuation. The frontend SHALL render the identity-verification step within the canonical `/cancelacion` route and SHALL NOT place the request identifier, DNI, certificate data or step name in the URL.

#### Scenario: Positive availability authorizes transition
- **WHEN** the frontend receives the expected positive response
- **THEN** it renders identity verification within `/cancelacion` without changing the visible URL or exposing request data

#### Scenario: Any other result is received
- **WHEN** availability is negative or cannot be confirmed
- **THEN** the frontend exposes no continuation control, remains at `/cancelacion` and presents the controlled outcome
