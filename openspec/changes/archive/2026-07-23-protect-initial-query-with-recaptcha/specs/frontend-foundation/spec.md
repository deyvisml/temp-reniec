## MODIFIED Requirements

### Requirement: Minimal dependency and folder set
The frontend SHALL retain only `next`, `react`, `react-dom`, `sweetalert2`, and the maintained Google reCAPTCHA v2 Checkbox React integration as direct production dependencies. Development tooling SHALL remain limited to TypeScript/types, Tailwind/PostCSS, Vitest and `openapi-typescript` required by implemented behavior. It MUST NOT add UI kits, icon libraries, HTTP libraries, form libraries, runtime validation libraries, state managers, DOM test environments, E2E tools, SDK generators or preventive dependencies. Every dependency, source directory and contract artifact SHALL have a current purpose.

#### Scenario: Dependency and source tree are reviewed
- **WHEN** `package.json` and the source tree are inspected
- **THEN** each dependency supports the current shell, DNI flow, maintained result alert, reCAPTCHA widget, technical integration, contract synchronization or tests and no broad framework or unused abstraction was added

### Requirement: Explicit server and public environment variables
The frontend SHALL document `BACKEND_URL` as the server-only backend base URL; `NEXT_PUBLIC_BACKEND_URL`, `NEXT_PUBLIC_APP_ENV` and `NEXT_PUBLIC_RECAPTCHA_SITE_KEY` as non-sensitive browser-visible values; and the local backend URLs/environment label already established. `.env.example` SHALL contain only safe values or placeholders, `.env.local` SHALL be ignored, and documentation SHALL state that public variables are embedded at build time. No backend reCAPTCHA secret, production credential or test bypass SHALL be exposed through a `NEXT_PUBLIC_` variable.

#### Scenario: Local environment is configured
- **WHEN** a contributor copies `.env.example` to `.env.local` and supplies an approved development site key
- **THEN** backend addresses, environment label and widget configuration resolve without exposing the backend secret

#### Scenario: Public site key is absent
- **WHEN** the frontend builds or starts without `NEXT_PUBLIC_RECAPTCHA_SITE_KEY`
- **THEN** the application remains buildable but the form fails closed with a controlled unavailable state

#### Scenario: Browser bundle is reviewed
- **WHEN** frontend source, generated assets and browser configuration are inspected
- **THEN** they contain only the public site key, public backend address and environment label and no secret, verification token, database value or test-mode bypass
