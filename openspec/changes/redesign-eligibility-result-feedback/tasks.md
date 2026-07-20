## 1. Outcome model and component boundary

- [x] 1.1 Inventory the current eligible, not-eligible, inconclusive and controlled-error variants, including their safe primary and secondary actions.
- [x] 1.2 Introduce an exhaustive presentation mapping that converts each existing `ViewState` outcome into dialog tone, icon, title, description and actions without changing API semantics.
- [x] 1.3 Create the focused `EligibilityOutcomeDialog` component or equivalent local component using native `<dialog>`, React and literal Tailwind utilities.
- [x] 1.4 Keep the component specific to eligibility feedback and avoid creating a generic modal framework or unused variant system.

## 2. Accessible interaction behavior

- [x] 2.1 Open the dialog with `showModal()` when a consultation resolves and close it during state cleanup without accessing browser-only APIs during server rendering.
- [x] 2.2 Add stable `aria-labelledby` and `aria-describedby` relationships, meaningful non-color status icons and a single announcement strategy.
- [x] 2.3 Implement initial focus, native focus containment, visible focus indicators and restoration to the DNI field after reset.
- [x] 2.4 Prevent backdrop dismissal and map Escape to the outcome's safe return/reset action without triggering continuation or retry.
- [x] 2.5 Preserve minimum 44 px interactive targets, keyboard operation and reduced-motion behavior.

## 3. Result presentation and responsive design

- [x] 3.1 Keep the DNI form mounted as page context and remove the early return that replaces it with the current full-width `ResultPanel`.
- [x] 3.2 Implement the compact institutional dialog surface, restrained backdrop, status variants and consistent action hierarchy without adding component styles to `globals.css`.
- [x] 3.3 Implement `NOT_ELIGIBLE` copy and actions so the result is calm, terminal, non-enumerating and clearly states that the DNI and identity were not affected.
- [x] 3.4 Apply the same component vocabulary to eligible, inconclusive, unavailable, timeout, network, conflict and unexpected-error outcomes while preserving their distinct safe actions.
- [x] 3.5 Remove obsolete result-panel constants and markup after confirming that no state depends on them.
- [x] 3.6 Verify the dialog at desktop, intermediate and mobile widths, including zoom and constrained viewport height, with no horizontal overflow or unreachable actions.

## 4. Functional preservation and tests

- [x] 4.1 Add unit tests for exhaustive outcome-to-presentation mapping, citizen copy, correlation identifiers and safe action selection.
- [x] 4.2 Add render tests for native dialog markup, accessible name/description, action labels and absence of the old embedded full-width result panel.
- [x] 4.3 Verify eligible continuation, terminal reset, safe retry, duplicate-submit protection and DNI cleanup behavior remain unchanged.
- [x] 4.4 Review the running frontend with keyboard for focus entry, Tab/Shift+Tab containment, Escape behavior, backdrop behavior and focus restoration.
- [x] 4.5 Run `npm run typecheck`, `npm test`, `npm run api:check` and `npm run build`, resolving every regression.
- [x] 4.6 Confirm the final diff adds no runtime dependency, backend/API/database change, global component selector, unrelated route or out-of-scope citizen functionality.
