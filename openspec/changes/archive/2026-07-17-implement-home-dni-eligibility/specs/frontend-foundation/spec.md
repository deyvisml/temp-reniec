## ADDED Requirements

### Requirement: Functional citizen home page
The `/` route SHALL render the real citizen-facing home and DNI eligibility form specified by `citizen-eligibility-entry`. It SHALL preserve the root App Router shell, semantic landmarks, Spanish language metadata, global error boundaries, keyboard access, responsive layout, and safe behavior when the backend is unavailable. It MUST NOT include identity verification, cancellation reason, confirmation, revocation, receipt, JWT session behavior, or later citizen-flow controls.

#### Scenario: Citizen home is rendered
- **WHEN** a visitor opens `/`
- **THEN** the page renders the service explanation and accessible DNI form within the existing root shell, with no temporary project-preparation content

#### Scenario: Backend is unavailable
- **WHEN** the visitor opens or uses the home page while the backend cannot be reached
- **THEN** the page remains renderable and usable and reports communication failure only after a submitted consultation

## REMOVED Requirements

### Requirement: Temporary non-functional home page
**Reason**: The first citizen functionality replaces the project-preparation page and its technical availability indicator.

**Migration**: Replace temporary content and `IntegrationStatusIndicator` usage on `/` with the home and DNI eligibility form; retain `/api/v1/system/status` and its non-UI integration tests for technical diagnostics.
