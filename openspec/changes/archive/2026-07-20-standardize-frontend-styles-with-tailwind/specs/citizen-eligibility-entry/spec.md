## MODIFIED Requirements

### Requirement: Citizen home communicates the service accurately
The `/` route SHALL present an accessible, responsive citizen-facing home page based on `docs/ui-reference/home.png` and SHALL implement its component presentation through the Tailwind-first styling baseline defined by `frontend-foundation`. The migration from global component selectors MUST preserve the approved institutional header, service purpose, supplied image assets, DNI entry area, primary action, trust information, functional states, responsive behavior, semantic structure, and visible focus. It MUST NOT state or imply that the DNI, civil identity, physical document, DNIe, or ID PerÃº account is cancelled, expose or allow selection of individual certificates, alter the original reference files, or redesign the flow as part of the styling refactor.

#### Scenario: Citizen opens the home page
- **WHEN** a visitor opens `/` on a supported desktop, intermediate, or mobile viewport
- **THEN** the page presents the institutional header, service purpose, supplied hero image, DNI entry area, primary action, and trust information with the approved responsive composition and no horizontal overflow

#### Scenario: Form state is rendered after migration
- **WHEN** the DNI form displays its initial, validation, loading, eligible, non-eligible, inconclusive, unavailable, timeout, network, or controlled error state
- **THEN** its existing behavior, accessible announcements, focus treatment, readable hierarchy, and safe actions remain available after replacing global component selectors with Tailwind utilities

#### Scenario: Reference and context differ functionally
- **WHEN** a visual detail in `home.png` implies behavior not confirmed by `PROJECT_CONTEXT.md`
- **THEN** the implementation follows the context, records the difference for validation, and does not invent the behavior

#### Scenario: Styling implementation is reviewed
- **WHEN** the home page, form, header, footer, benefits, and result components are inspected
- **THEN** their presentation uses colocated Tailwind utilities and contains neither visual inline styles nor dependencies on component selectors in `app/globals.css`
