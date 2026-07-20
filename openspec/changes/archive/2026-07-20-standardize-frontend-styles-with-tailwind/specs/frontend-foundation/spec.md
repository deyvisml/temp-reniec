## MODIFIED Requirements

### Requirement: Minimal Tailwind and global styling baseline
The frontend SHALL use Tailwind CSS 4.3.2 through `@tailwindcss/postcss` and one global CSS import. Layout, spacing, typography, colors, responsive behavior, interactive states, and component presentation SHALL be expressed primarily through Tailwind utility classes colocated with the TSX that owns the markup. `app/globals.css` SHALL be limited to the Tailwind import, a small justified theme configuration, and rules that are genuinely global and not reasonably represented by utilities. It MUST NOT contain component-structure selectors, use `@apply` to recreate component styles, introduce another global stylesheet, or rely on inline visual styles. Any custom CSS exception MUST be scoped, documented, and justified by a current need. The frontend MUST NOT add a design system, broad token catalog, class-composition dependency, CSS framework, or UI kit as part of this baseline.

#### Scenario: Tailwind and global styles compile
- **WHEN** the production build processes the global stylesheet and Tailwind utilities used by the application
- **THEN** CSS generation succeeds and the rendered application retains readable text, coherent spacing, responsive layout, and visible keyboard focus

#### Scenario: Contributor inspects component presentation
- **WHEN** a contributor reviews an implemented frontend component
- **THEN** its visual composition and responsive variants are visible through Tailwind utilities in that component rather than hidden in global component selectors or inline styles

#### Scenario: Global stylesheet is reviewed
- **WHEN** `app/globals.css` is inspected after the migration
- **THEN** it contains only the Tailwind import and justified global theme or base concerns, with no selectors coupled to page, form, header, footer, result, or benefit markup

#### Scenario: Tailwind cannot reasonably express a required style
- **WHEN** a current visual requirement needs custom CSS that is not reasonably represented by Tailwind utilities
- **THEN** the implementation adds the smallest scoped and documented exception without establishing a second styling strategy
