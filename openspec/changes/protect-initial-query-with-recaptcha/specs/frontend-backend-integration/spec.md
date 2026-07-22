## MODIFIED Requirements

### Requirement: Functional eligibility contract uses the shared transport
The initial certificate-availability client SHALL use the centralized JSON transport, environment-based backend URL, credentials mode, timeout, abort handling, correlation propagation and common error mapping. Request and response types SHALL come from generated OpenAPI declarations. Each submission SHALL include the current in-memory `recaptchaToken` with the DNI, while the client MUST NOT persist or log that token, accept or synthesize certificate collections, or lose the distinction between CAPTCHA failure, confirmed certificate absence and transport/service failure.

#### Scenario: Availability request succeeds
- **WHEN** the frontend submits a valid DNI and current CAPTCHA token and the backend returns `AVAILABLE` or `NOT_AVAILABLE`
- **THEN** the client returns typed availability data and correlation without duplicating transport behavior, retaining the token or exposing certificate details

#### Scenario: CAPTCHA request fails
- **WHEN** the backend returns a controlled CAPTCHA rejection, expiration, timeout, unavailability or invalid-response error
- **THEN** the shared transport preserves the stable code and correlation so the feature resets the widget and does not invoke or infer an availability outcome

#### Scenario: Availability transport fails
- **WHEN** the backend returns another controlled error, times out, sends invalid JSON or cannot be reached
- **THEN** the shared transport produces the established typed error and the feature maps it neither to CAPTCHA success nor `NOT_AVAILABLE`
