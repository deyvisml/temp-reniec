## 1. Styling baseline and safeguards

- [x] 1.1 Inventory every custom class selector and inline visual style used by `frontend/app` and `frontend/components`, mapping each one to its owning component and rendered states.
- [x] 1.2 Record the current desktop, intermediate, and mobile layout measurements and capture the initial, validation, loading, success, negative, and retryable form states needed for visual comparison.
- [x] 1.3 Add a focused Vitest convention test that permits the Tailwind import and justified global theme/base rules but rejects component selectors, `@apply`, additional global stylesheets, and visual `style` props in application components.
- [x] 1.4 Document the Tailwind-first rule, allowed global CSS boundary, exception process, and literal-class requirement in `frontend/README.md`.

## 2. Global foundation and application shell

- [x] 2.1 Reduce `app/globals.css` to `@import "tailwindcss"` plus only the minimal repeated theme values and genuinely global rules justified by the current UI.
- [x] 2.2 Move body background, typography, minimum height, text rendering, selection/tap behavior, and focus presentation to appropriate Tailwind utilities in the root layout or owning controls.
- [x] 2.3 Migrate the skip link, main landmark, global message region, footer, loading state, not-found state, route error, and global error presentation to colocated Tailwind utilities.
- [x] 2.4 Migrate `SiteHeader` and `SiteFooter` to Tailwind utilities, remove the logo's inline dimensions, and preserve the approved logo sizing and responsive header behavior.

## 3. Citizen home presentation

- [x] 3.1 Migrate the home shell background, decorative shapes, content width, spacing, and overflow behavior from global component selectors to Tailwind utilities and pseudo-element variants.
- [x] 3.2 Migrate the hero copy, badge, title, accent, description, responsive grid, and supplied hero image sizing to Tailwind utilities without changing content or image files.
- [x] 3.3 Migrate the consultation card container and benefits section to Tailwind utilities while preserving their desktop and stacked responsive compositions.
- [x] 3.4 Migrate the shared benefit markup and inline SVG presentation without creating an unused generic design-system layer.

## 4. DNI form and result states

- [x] 4.1 Migrate the initial DNI form heading, emblem, field, help text, validation text, input shell, primary action, spinner, and security note to Tailwind utilities.
- [x] 4.2 Implement local literal class composition for invalid, focused, disabled, busy, hover, and reduced-motion states without adding a class-composition dependency or dynamically constructed Tailwind fragments.
- [x] 4.3 Migrate eligible, non-eligible, inconclusive, unavailable, timeout, network, conflict, and unexpected-error result panels and their primary or secondary actions.
- [x] 4.4 Confirm that labels, descriptions, `aria-*` state, focus movement, busy announcements, keyboard operation, retry behavior, and duplicate-submit protection remain unchanged.

## 5. Cleanup and verification

- [x] 5.1 Remove every obsolete global component selector and verify that no TSX class references depend on the deleted styling names.
- [x] 5.2 Verify that the final source contains no visual inline styles, `@apply`, additional CSS strategy, new UI dependency, modified reference image, or unrelated functional change.
- [x] 5.3 Run `npm run typecheck`, `npm test`, `npm run api:check`, and `npm run build` from `/frontend` and resolve all failures.
- [x] 5.4 Review the running page at representative desktop, intermediate, and mobile viewports for the initial and controlled result states, confirming visual equivalence, visible focus, readable contrast, correct supplied images, and absence of horizontal overflow or console errors.
- [x] 5.5 Compare the completed implementation with both delta specifications and mark every task complete only after the Tailwind-first boundary and citizen-home behavior are demonstrably satisfied.
