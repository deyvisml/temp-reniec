## ADDED Requirements

### Requirement: MySQL schema metadata is documented in Spanish
The MySQL persistence schema SHALL store a concise, non-empty Spanish comment for every domain table and every column belonging to those tables. Comments SHALL explain the stable domain or technical purpose of the element, SHALL be directly visible through `INFORMATION_SCHEMA`, and MUST NOT contain real personal data, credentials, secrets, provider payloads, or misleading behavior that is not implemented. Physical table and column names SHALL remain unchanged.

#### Scenario: Contributor inspects table metadata
- **WHEN** a contributor views any of the eight domain tables through MySQL metadata or a compatible database client
- **THEN** the table has a Spanish description that explains its responsibility in the cancellation-request model

#### Scenario: Contributor inspects column metadata
- **WHEN** a contributor views any column belonging to the eight domain tables
- **THEN** `COLUMN_COMMENT` contains a concise Spanish description of that field and no domain column has a blank description

#### Scenario: Sensitive-field description is reviewed
- **WHEN** comments for DNI, certificate UUID, external references, correlation identifiers, storage references, or technical errors are inspected
- **THEN** they describe the purpose of the field without embedding real values, secrets, credentials, or complete external payloads

### Requirement: Schema comments are delivered by forward-only Flyway migration
The comments SHALL be applied by a new incremental Flyway migration after the migrations that create the eight-table schema. Existing successful migrations MUST NOT be edited. The migration SHALL work both when Flyway builds an empty database and when it upgrades a database already at V2, and it MUST preserve all rows, table names, column names, types, attributes, nullability, defaults, keys, indexes, constraints, and relationships.

#### Scenario: Empty database is migrated
- **WHEN** Flyway runs all migrations against an empty MySQL database
- **THEN** the resulting eight-table schema contains complete Spanish table and column comments and Hibernate schema validation succeeds

#### Scenario: Existing V2 database is upgraded
- **WHEN** a MySQL database with the V2 structure and existing fictitious rows executes the comment migration
- **THEN** the rows and relational structure remain unchanged while comments become available

#### Scenario: Migration history is inspected
- **WHEN** checksums and migration files are reviewed after implementation
- **THEN** V1 and V2 remain unchanged and the comment metadata is introduced only by the new forward migration

### Requirement: Comment coverage remains mandatory for future schema changes
Repository documentation and persistence tests SHALL establish that every future domain table or column receives a concise Spanish comment in the migration that creates it. Automated integration verification SHALL fail when an expected domain table has an empty `TABLE_COMMENT` or an expected domain column has an empty `COLUMN_COMMENT`.

#### Scenario: A future migration adds a column
- **WHEN** a new domain column is introduced
- **THEN** the same migration defines its Spanish comment and the metadata coverage test includes the expanded schema

#### Scenario: A comment is accidentally omitted
- **WHEN** the integration suite finds a domain table or column with blank comment metadata
- **THEN** verification fails and identifies the undocumented table or column
