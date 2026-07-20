## MODIFIED Requirements

### Requirement: Eligibility outcomes use accessible separated feedback
The frontend SHALL present every completed eligibility outcome through a compact, persistent and responsive SweetAlert2 modal that is visually and semantically separate from the DNI form. The form SHALL remain mounted as background context and MUST NOT be replaced by a full-width result panel inside the consultation card. The application MUST use SweetAlert2's supported public API and MUST NOT retain a project-owned modal implementation, depend on internal SweetAlert2 markup, or add component-specific global CSS.

#### Scenario: Citizen has no cancellable certificates
- **WHEN** the backend returns `NOT_ELIGIBLE`
- **THEN** a SweetAlert2 modal states concisely that no digital certificates are available for cancellation with the entered DNI, blocks continuation, avoids unrelated reassurance about the DNI or identity, and offers a conventional action to acknowledge the result

#### Scenario: Citizen receives an eligible result
- **WHEN** the backend returns `ELIGIBLE` with `canContinue=true`
- **THEN** the same SweetAlert2 presentation offers the authorized continuation action and an optional safe action to start another consultation

#### Scenario: Citizen receives a retryable result
- **WHEN** the result is inconclusive, unavailable, timed out, affected by network loss, or represented by a retryable stable error code
- **THEN** the modal explains the temporary condition without technical detail and provides an explicit safe retry plus an action to enter another DNI

#### Scenario: Outcome modal is reviewed visually
- **WHEN** an eligibility outcome is displayed on desktop, intermediate or mobile viewports
- **THEN** the modal remains compact, preserves at least 16 pixels of viewport clearance, keeps all actions reachable, avoids horizontal overflow, and does not recreate the oversized consultation card as an overlay

### Requirement: Eligibility outcome dialogs manage focus and dismissal safely
The SweetAlert2 integration SHALL expose an accessible name and description, open as a modal dialog, contain keyboard focus, and restore focus to the DNI field after a reset. Visual status MUST NOT rely only on color. Backdrop clicks MUST NOT dismiss the result, and Escape SHALL map to the safe non-continuing action for the current outcome. Retry and continuation SHALL occur only through explicit actions.

#### Scenario: Modal opens after a submitted consultation
- **WHEN** the pending consultation resolves to a functional result or controlled error
- **THEN** focus moves into the labeled SweetAlert2 modal and assistive technology can identify its result, explanation and available actions without duplicate announcements

#### Scenario: Citizen navigates with the keyboard
- **WHEN** the citizen uses Tab or Shift+Tab while the modal is open
- **THEN** focus remains inside the modal and every available action has a visible focus indicator and a target of at least 44 by 44 pixels

#### Scenario: Citizen dismisses with Escape
- **WHEN** the citizen presses Escape instead of selecting an action
- **THEN** the integration executes the variant's safe reset or return behavior, never continues the citizen flow, never retries automatically, and restores focus to the DNI field when the form is shown

#### Scenario: Citizen clicks the backdrop
- **WHEN** the citizen clicks outside the modal surface
- **THEN** the result remains open so that a terminal or retryable outcome cannot be lost accidentally

#### Scenario: Citizen prefers reduced motion
- **WHEN** the browser reports `prefers-reduced-motion: reduce`
- **THEN** the SweetAlert2 presentation does not use non-essential entrance or exit animation

### Requirement: Eligibility response semantics are explicit
The frontend SHALL distinguish eligible, not eligible, inconclusive, service unavailable, timeout, technical backend error, network loss, request-in-progress conflict, and concurrency conflict using typed results or stable API error codes. Citizen messages SHALL be understandable, non-technical, non-enumerating, and SHALL provide continuation, retry, restart, or return actions only when safe. Completed outcomes SHALL use the maintained SweetAlert2 modal integration instead of replacing the DNI form or invoking a project-owned modal.

#### Scenario: DNI is not eligible
- **WHEN** the backend returns `NOT_ELIGIBLE`
- **THEN** the modal blocks continuation, states that no digital certificates are available for cancellation without listing certificates or adding unrelated reassurance, and offers a conventional action to acknowledge the result

#### Scenario: Result is inconclusive
- **WHEN** the backend returns `INCONCLUSIVE`
- **THEN** the modal blocks continuation and offers a safe explicit retry against the same compatible request or a controlled restart with another DNI

#### Scenario: Service is unavailable or times out
- **WHEN** the client receives a stable unavailable or timeout error
- **THEN** the modal explains the temporary condition, preserves no browser-stored DNI, displays the correlation identifier when available, and offers only safe retry or restart actions

#### Scenario: Network connection is lost
- **WHEN** the browser cannot reach the backend
- **THEN** the modal shows a generic connection message without technical details and allows an explicit retry or controlled restart

#### Scenario: Eligible result authorizes continuation
- **WHEN** the backend returns `ELIGIBLE`, `canContinue=true`, and the next step
- **THEN** the modal offers continuation without placing the DNI in the URL and does not navigate until the citizen activates the explicit action
