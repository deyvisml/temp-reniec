## MODIFIED Requirements

### Requirement: Step 1 presents an accessible five-step identity experience
The frontend SHALL render identity verification inside the configured citizen-flow route using the current five-step flow and the composition of `docs/ui-reference/step-1.png`. It SHALL explain that ID Perú verifies identity and that no certificate is cancelled in this step. After returning from the provider, it SHALL derive the outcome from backend-validated temporary context rather than browser parameters. A verified result SHALL activate the authenticated detailed certificate-list case and render the real step 2 selection experience; cancellation, rejection, mismatch, expiration and technical failures SHALL retain step 1 and present one accessible, non-technical SweetAlert2 notice with only valid recovery actions.

#### Scenario: Citizen opens the identity step
- **WHEN** a valid current-browser continuation is available
- **THEN** the page shows step 1 of 5 and one clear action to begin ID Perú authentication

#### Scenario: Citizen activates authentication twice
- **WHEN** the start control is triggered repeatedly while a request is pending
- **THEN** only one backend start call occurs and the busy state is announced

#### Scenario: Authentication returns successfully
- **WHEN** the callback has established a verified flow session
- **THEN** the frontend activates step 2 and obtains the detailed request-owned list through the protected backend API

#### Scenario: Authentication fails safely
- **WHEN** ID Perú is cancelled, rejects, expires, mismatches, times out or is unavailable
- **THEN** the frontend remains on step 1, shows exactly one citizen-facing alert for that outcome and permits only a controlled retry or clean restart

#### Scenario: Citizen refreshes after an alert
- **WHEN** an already presented callback failure state is rendered again without a new callback
- **THEN** the frontend does not open duplicate alerts or submit a new authentication automatically

#### Scenario: Browser parameters attempt to force success
- **WHEN** a citizen adds an apparent authentication outcome to the URL
- **THEN** the frontend ignores it and advances only when the backend confirms valid session and identity state
