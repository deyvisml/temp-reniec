## 1. Dependency and integration boundary

- [x] 1.1 Confirm `redesign-eligibility-result-feedback` is complete and record that it must be archived before this corrective change is archived.
- [x] 1.2 Install the current compatible SweetAlert2 11.x package as the only new runtime dependency and update the frontend lockfile without adding a React adapter or UI kit.
- [x] 1.3 Import the supported SweetAlert2 stylesheet once from the Next.js application root and avoid adding SweetAlert2 internal selectors or component rules to `globals.css`.
- [x] 1.4 Define a small eligibility-specific integration boundary so SweetAlert2 does not leak into the HTTP client, backend contracts or domain result model.

## 2. Outcome presentation migration

- [x] 2.1 Preserve the exhaustive mapping for eligible, not-eligible, inconclusive and controlled-error outcomes, including correlation identifiers and safe actions.
- [x] 2.2 Implement the client-side SweetAlert2 presenter using `titleText`, safe text content, supported icons/options and one active popup per resolved consultation.
- [x] 2.3 Configure institutional appearance with `buttonsStyling: false` and literal Tailwind `customClass` values for a compact popup, visible focus and targets of at least 44 pixels.
- [x] 2.4 Disable backdrop dismissal and translate Escape only to the current outcome's safe reset behavior; continuation and retry must require explicit buttons.
- [x] 2.5 Respect `prefers-reduced-motion`, constrained viewport height and mobile action layout using supported SweetAlert2 configuration rather than internal DOM assumptions.
- [x] 2.6 Route confirmed eligible outcomes through the existing Next.js continuation path and preserve safe retry/reset callbacks, DNI cleanup and focus restoration.
- [x] 2.7 Prevent stale or superseded popup promises from executing navigation, retry or reset after state change or component unmount.

## 3. Remove the project-owned modal

- [x] 3.1 Replace `EligibilityOutcomeDialog` usage in the DNI form while keeping the form mounted and duplicate submission protection unchanged.
- [x] 3.2 Remove the native `<dialog>` component, local modal icon/action markup and tests tied only to its internal implementation.
- [x] 3.3 Confirm no result returns to the former embedded full-width result panel and no toast is used for an outcome requiring citizen action.

## 4. Tests and verification

- [x] 4.1 Add unit tests for exhaustive outcome-to-SweetAlert configuration mapping, safe text handling, action selection and correlation identifiers.
- [x] 4.2 Add interaction coverage for eligible continuation, terminal reset, retry, Escape, blocked backdrop dismissal, cleanup and stale resolution protection.
- [x] 4.3 Verify in a running browser that focus, Tab/Shift+Tab containment, focus restoration and visible focus indicators work with SweetAlert2.
- [x] 4.4 Verify desktop, intermediate, mobile, zoomed and height-constrained layouts without horizontal overflow or unreachable actions.
- [x] 4.5 Run `npm run typecheck`, `npm test`, `npm run api:check` and `npm run build`, and inspect the production bundle for an unexpected integration regression.
- [x] 4.6 Confirm the final diff contains one new runtime dependency and no backend, database, OpenAPI, route, business-rule or unrelated UI changes.
- [x] 4.7 Replace DNI-enumeration-prone reset copy with neutral finish/return actions and remove unrelated reassurance from the not-eligible result.
