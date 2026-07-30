## MODIFIED Requirements

### Requirement: Certificate selection is stored without another table
Each request certificate SHALL continue to store whether it is selected and when it became selected. The database SHALL require `selected_at` to be present exactly when `selected` is true and SHALL enforce that at most one row is selected for a request through a unique conditional index. The entity SHALL use optimistic versioning, while application behavior SHALL serialize replacement through the request root. Selection SHALL require exactly one available certificate before advancing and MUST reject every selection change after citizen confirmation. No table dedicated exclusively to selection or snapshotting SHALL exist.

#### Scenario: One certificate is selected
- **WHEN** the citizen selects one row among several available certificates
- **THEN** only that row stores `selected=true` and a selection time while every other request row remains unselected

#### Scenario: Citizen replaces the selection
- **WHEN** another valid certificate is chosen before confirmation
- **THEN** the transaction deselects the previous row and selects the replacement without committing two selected rows

#### Scenario: A second selected row bypasses application validation
- **WHEN** a direct or concurrent database write attempts to leave two selected rows for the same request
- **THEN** MySQL rejects the write through the unique conditional index

#### Scenario: Selection timestamp is inconsistent
- **WHEN** a row is stored as selected without a time or unselected with a selection time
- **THEN** entity and database integrity validation reject the inconsistent state

#### Scenario: Selection is changed after confirmation
- **WHEN** application behavior attempts to select or deselect a row after `confirmed_at` exists
- **THEN** the change is rejected and the confirmed certificate remains immutable
