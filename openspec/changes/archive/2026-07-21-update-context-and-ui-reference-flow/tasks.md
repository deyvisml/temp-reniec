## 1. Verify supplied sources and current references

- [x] 1.1 Verify that `PROJECT_CONTEXT_v2.md` and all six supplied PNG files exist, record their byte sizes and SHA-256 hashes, and confirm every image decodes as PNG.
- [x] 1.2 Inventory the current files and hashes under `docs/context/` and `docs/ui-reference/`, confirming which supplied images already match existing binaries before any rename or replacement.
- [x] 1.3 Search non-archived documentation and current OpenSpec specifications for four-step numbering, obsolete image paths, binary-only eligibility, inseparable-certificate behavior, and all-certificates cancellation claims.

## 2. Replace the permanent functional context

- [x] 2.1 Replace `docs/context/PROJECT_CONTEXT.md` with an exact-content copy of `PROJECT_CONTEXT_v2.md` without normalization, editing, or a second active version.
- [x] 2.2 Verify the destination context byte size and SHA-256 against the supplied file and confirm current documentation identifies it as the sole authoritative functional source.

## 3. Reorganize the visual references

- [x] 3.1 Preserve or replace `home.png` and `step-1.png` from their mapped attachments, verifying exact binary identity at the permanent destinations.
- [x] 3.2 Replace `step-2.png` with the supplied new certificate-selection interface and verify that it is physically present and decodable.
- [x] 3.3 Reorganize the supplied reason and confirmation images as `step-3.png` and `step-4.png` without altering their binary content.
- [x] 3.4 Reorganize the supplied final-result image as `step-5-final.png`, then remove the active obsolete path `step-4-final.png` after verifying the new destination.
- [x] 3.5 Verify that `docs/ui-reference/` contains the six expected PNG names with source-matching sizes and SHA-256 hashes and contains no ambiguous old-order duplicate.

## 4. Update the visual and functional documentation

- [x] 4.1 Rewrite `docs/ui-reference/README.md` to link the six permanent images and describe the home screen followed by authentication, selection, reason, confirmation, and receipt as five numbered steps.
- [x] 4.2 Document that the lookup returns current certificate issues, that their citizen-facing representation appears only after authentication, that selection is mandatory even for one certificate, and that only selected UUIDs participate in revocation and per-certificate results.
- [x] 4.3 Add an explicit inconsistency register for the reused four-step steppers, displaced numbering, and all-certificates wording in confirmation and final imagery, preserving the images as composition references while giving the updated context precedence.
- [x] 4.4 State that every later domain or UI task must review `PROJECT_CONTEXT.md` and the corresponding image and must record unresolved functional contradictions instead of deriving rules from artwork.

## 5. Audit affected current documents

- [x] 5.1 Review `docs/TECHNICAL_DECISIONS.md`, `docs/data-model/README.md`, `docs/LOCAL_INTEGRATION.md`, and non-archived OpenSpec specifications, changing only references affected by the new context.
- [x] 5.2 Correct functional guidance that still presents four steps, old view numbering, boolean-only lookup, inseparable certificates, or cancellation of every certificate.
- [x] 5.3 Where a document accurately describes still-unmodified code, contracts, or schema, add a visible implementation-gap notice that defers target domain behavior to `PROJECT_CONTEXT.md` instead of pretending the new behavior is implemented.
- [x] 5.4 Leave `openspec/changes/archive/` unchanged as historical evidence and confirm it is not linked as a current functional authority.

## 6. Validate documentation-only completion

- [x] 6.1 Validate Markdown links from the visual README and confirm all six PNG files can be opened from their permanent repository paths.
- [x] 6.2 Search non-archived functional references again and confirm no unqualified current claim retains four-step numbering, obsolete paths, binary-only eligibility, or all-certificates behavior.
- [x] 6.3 Inspect `git diff` and confirm no file under `/backend` or `/frontend`, no migration, and no executable configuration or generated contract was modified.
- [x] 6.4 Run `openspec validate update-context-and-ui-reference-flow --strict` and resolve all proposal/spec consistency errors.
