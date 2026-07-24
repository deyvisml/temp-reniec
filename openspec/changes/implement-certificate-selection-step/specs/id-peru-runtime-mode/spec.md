## MODIFIED Requirements

### Requirement: The registered local route hosts the authenticated internal flow
When `NEXT_PUBLIC_APP_ENV=local`, the frontend SHALL navigate from the successful initial query at `/cancelacion` to `/autorizacion`, SHALL render identity step one there, SHALL keep that route after the ID Perú callback and SHALL render step 2 there after successful identity. The route SHALL reuse the shared cancellation-flow components and SHALL resolve the current step through the backend flow session. Outside the local environment, direct access to `/autorizacion` SHALL redirect to `/cancelacion`.

#### Scenario: Local availability is confirmed
- **WHEN** a local citizen query returns `AVAILABLE`
- **THEN** the frontend navigates to `/autorizacion` and displays identity step one without placing DNI, request identifiers or state in the URL

#### Scenario: ID Perú returns in local mode
- **WHEN** the backend processes the local callback and redirects to `http://localhost:3000/autorizacion`
- **THEN** the frontend remains on `/autorizacion` and obtains the current session and identity result from the backend

#### Scenario: Local identity is verified
- **WHEN** the backend session authorizes certificate selection
- **THEN** `/autorizacion` displays the real step 2 without adding certificate or step data to the URL

#### Scenario: Authorization route is opened outside local mode
- **WHEN** `/autorizacion` is requested with a non-local frontend environment
- **THEN** the frontend redirects to `/cancelacion`
