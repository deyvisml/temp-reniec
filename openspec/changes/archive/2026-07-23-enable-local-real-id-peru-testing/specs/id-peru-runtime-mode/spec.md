## ADDED Requirements

### Requirement: ID Perú mode is independent from the local environment
The backend SHALL allow the `local` profile to select `mock` or `real` ID Perú through `ID_PERU_MODE`, with `mock` as the local default. The `prod` profile MUST always select `real`, and tests MUST be able to select a controlled adapter without production credentials.

#### Scenario: Local mock is used by default
- **WHEN** the backend starts with the `local` profile and `ID_PERU_MODE` is absent
- **THEN** the mock ID Perú adapter is active and no institutional credentials are required

#### Scenario: Local real mode is selected
- **WHEN** the backend starts with the `local` profile and `ID_PERU_MODE=real`
- **THEN** the real ID Perú adapter is active using externally supplied development credentials

#### Scenario: Production starts
- **WHEN** the backend starts with the `prod` profile
- **THEN** only the real ID Perú adapter can be active

### Requirement: Real local configuration is external and validated
Real ID Perú mode SHALL obtain client identifier and client secret from external configuration, while the confirmed provider root SHALL remain fixed in versioned application configuration. It SHALL fail startup when a required credential is absent or malformed and MUST NOT expose credentials to the frontend or logs. The provider URI SHALL always use HTTPS; an HTTP referer SHALL be accepted only for localhost in local or test profiles, while production SHALL require HTTPS.

#### Scenario: Complete test credentials are provided
- **WHEN** local real mode starts with the required client credentials in the ignored `backend/.env`
- **THEN** the backend constructs the real authorization, token, userinfo and JWKS endpoints without source-code changes

#### Scenario: Provider root is resolved
- **WHEN** any real ID Perú mode starts
- **THEN** authorization, token, userinfo, JWKS and issuer are derived from the fixed HTTPS root `https://idaas2.reniec.gob.pe/` without requiring an environment variable

#### Scenario: A credential is missing
- **WHEN** local real mode starts without a required credential
- **THEN** startup fails with a controlled configuration error naming the missing property and without printing secret values

#### Scenario: Local application URLs are used
- **WHEN** local real mode uses absolute `http://localhost` frontend and backend URLs accepted by the registered test client
- **THEN** the callback is exactly `http://localhost:8080/api/v1/idperu/callback`, the registered return is `http://localhost:3000/autorizacion`, and both are accepted only for the local environment

#### Scenario: Production routes remain canonical
- **WHEN** real mode runs outside the local profile
- **THEN** it uses the uniform callback `/api/v1/idperu/callback` and the frontend return `/cancelacion` over configured HTTPS application bases

### Requirement: Local real verification is operationally documented
The repository SHALL document exact local mock and local real startup configurations, required credentials, callback registration, expected redirect flow and troubleshooting. A developer SHALL be able to switch modes by editing `.env` and restarting the backend, without modifying tracked source files.

#### Scenario: Developer follows the real-mode instructions
- **WHEN** authorized test credentials and callback registration are available
- **THEN** the developer can complete the initial query, enter step 1 at `/autorizacion`, leave the frontend for ID Perú, process the registered callback and remain at `/autorizacion` with the backend-validated result

### Requirement: The registered local route hosts identity step one
When `NEXT_PUBLIC_APP_ENV=local`, the frontend SHALL navigate from the successful initial query at `/cancelacion` to `/autorizacion`, SHALL render identity step one there, and SHALL keep that route after the ID Perú callback. The route SHALL reuse the shared cancellation-flow components and SHALL resolve callback state through the backend continuity cookie. Outside the local environment, direct access to `/autorizacion` SHALL redirect to `/cancelacion`.

#### Scenario: Local availability is confirmed
- **WHEN** a local citizen query returns `AVAILABLE`
- **THEN** the frontend navigates to `/autorizacion` and displays identity step one without placing DNI, request identifiers or state in the URL

#### Scenario: ID Perú returns in local mode
- **WHEN** the backend processes the local callback and redirects to `http://localhost:3000/autorizacion`
- **THEN** the frontend remains on `/autorizacion` and obtains the current identity result from the backend continuity context

#### Scenario: Authorization route is opened outside local mode
- **WHEN** `/autorizacion` is requested with a non-local frontend environment
- **THEN** the frontend redirects to `/cancelacion`

### Requirement: Authorization query values are encoded exactly once
The real ID Perú adapter SHALL construct the authorization request through encoded URI-variable expansion. Complete values such as `redirect_uri`, `state` and encrypted `vd` SHALL be percent-encoded exactly once as query parameter values, while PKCE URL-safe values SHALL preserve their meaning. The adapter MUST NOT concatenate pre-encoded or raw nested URLs into the outer authorization URL.

#### Scenario: Local callback is included in authorization
- **WHEN** the real adapter builds an authorization URL using `http://localhost:8080/api/v1/idperu/callback`
- **THEN** its raw query contains `redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Fapi%2Fv1%2Fidperu%2Fcallback` and does not contain the raw nested URL

#### Scenario: Encrypted DNI contains Base64 padding
- **WHEN** encrypted `vd` contains `=`, `+` or `/`
- **THEN** those characters are percent-encoded once in the raw authorization query and decode to the original encrypted value
