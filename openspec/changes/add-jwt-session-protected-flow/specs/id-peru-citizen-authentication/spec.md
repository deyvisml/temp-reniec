## MODIFIED Requirements

### Requirement: Identity verification starts only from an eligible current request
The backend SHALL start ID Perú authentication only for the request represented by a valid active transactional session whose persisted availability is `AVAILABLE`, whose session phase permits identity verification and whose request status is `PENDING_IDENTITY_VERIFICATION`. It MUST build provider parameters in the backend and MUST NOT accept client-selected DNI, request ID, provider endpoints, `client_id`, `redirect_uri`, `acr_values`, state or PKCE values.

#### Scenario: Eligible citizen starts verification
- **WHEN** the active session identifies a request pending identity verification with confirmed availability
- **THEN** the backend creates a new identity attempt for that same request and returns its provider authorization URL

#### Scenario: Caller supplies only a request identifier
- **WHEN** a caller knows a numeric request ID but lacks the matching active session
- **THEN** the backend refuses to start ID Perú and reveals no DNI or request details

#### Scenario: Request cannot authenticate
- **WHEN** the session is invalid or availability, session phase, expiration or request status does not permit authentication
- **THEN** no identity attempt or provider URL is created

### Requirement: Authenticated identity must match the initiating DNI
After validating `/userinfo`, the backend SHALL compare its `doc` claim with the DNI held by the request linked to the active session. It SHALL require a consistent subject between validated ID token and userinfo data, persist only the necessary safe subject reference and match result, and MUST NOT expose the authenticated document or person data in mismatch responses. Only a match SHALL update that same session to its post-identity phase.

#### Scenario: Documents match
- **WHEN** validated userinfo contains the same eight-digit DNI as the session request
- **THEN** the attempt becomes `VERIFIED`, the request becomes `IDENTITY_VERIFIED` and the existing session is authorized for the certificate-selection boundary

#### Scenario: Documents differ
- **WHEN** validated userinfo identifies a different DNI
- **THEN** the attempt becomes `IDENTITY_MISMATCH`, the session remains blocked at step 1 and no data about the authenticated person is returned

#### Scenario: Required identity claim is absent
- **WHEN** userinfo omits or malforms `sub` or `doc`
- **THEN** the response is invalid, no comparison is inferred and the session is not elevated
