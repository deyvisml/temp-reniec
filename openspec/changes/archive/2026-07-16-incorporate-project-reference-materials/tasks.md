## 1. Incorporate source materials

- [x] 1.1 Create `docs/context/` and `docs/ui-reference/` without creating application directories or runtime configuration.
- [x] 1.2 Copy `PROJECT_CONTEXT.md` unchanged to `docs/context/PROJECT_CONTEXT.md`.
- [x] 1.3 Copy `home.png`, `step-1.png`, `step-2.png`, and `step-3.png` unchanged to their matching names under `docs/ui-reference/`.
- [x] 1.4 Copy `step-4 (final).png` unchanged to `docs/ui-reference/step-4-final.png`, normalizing only its filename.

## 2. Document reference usage

- [x] 2.1 Create `docs/ui-reference/README.md` with links mapping all five images to the home, identity verification, cancellation reason, confirmation, and final receipt views.
- [x] 2.2 Document in the visual README that `PROJECT_CONTEXT.md` is the primary functional source and the images are the primary visual implementation references.
- [x] 2.3 Document the prohibition on inferring unconfirmed rules from designs, the precedence and pending-validation procedure for contradictions, and the required pre-implementation review by later domain and interface tasks.

## 3. Record technical decisions

- [x] 3.1 Create `docs/TECHNICAL_DECISIONS.md` recording Spring Boot, Next.js, MySQL, Tailwind CSS, and the future `/backend` and `/frontend` top-level structure.
- [x] 3.2 Record JWT-based authenticated communication, backend/MySQL progress persistence, and progress recovery from another browser or device after renewed identity verification.
- [x] 3.3 Record the simple incremental architecture and data-design constraints, including no table per screen, step, or state and no unjustified microservices, queues, event sourcing, CQRS, or complex patterns.
- [x] 3.4 Record replaceable interface-based external integrations pending official contracts and the exclusion of administrative modules and functionality outside the citizen flow.

## 4. Verify integrity and scope

- [x] 4.1 Compare source and destination byte sizes and SHA-256 hashes for the context document and all five images.
- [x] 4.2 Open or decode each PNG from its final repository path and verify all README links resolve.
- [x] 4.3 Review the resulting repository diff and confirm it contains only the requested documentation and unchanged source assets, with no functional implementation or out-of-scope scaffolding.
