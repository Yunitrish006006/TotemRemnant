# Death Node Administration Specification

## Requirement: Authorized access

The system SHALL permit only authorized server administrators to open or use death-node administration functions.

### Scenario: Unauthorized payload

- GIVEN a player without the required permission
- WHEN the player sends a query or mutation payload
- THEN the Server rejects it without returning private node data
- AND no node or index is changed

## Requirement: Player filtering

The system SHALL allow administrators to filter death nodes by player UUID or known player name, including offline players.

### Scenario: Offline player lookup

- GIVEN the target player is offline
- WHEN an administrator filters by the player's known name or UUID
- THEN the Server returns only nodes owned by that player

## Requirement: Server-authoritative pagination

The system SHALL paginate and sort results on the Server using stable keys.

### Scenario: Forged result list

- GIVEN a Client submits node UUIDs not present in the active server-side query
- WHEN a destructive operation is requested
- THEN the Server ignores the Client list and recomputes the target set from the confirmed query

## Requirement: Safe node inspection

The system SHALL expose node identity, owner, dimension, position, creation time, state, backpack binding and diagnostic flags to authorized administrators.

## Requirement: Node disable

The system SHALL allow an administrator to disable a death node while retaining its historical record.

### Scenario: Disable active node

- WHEN an authorized administrator disables an active death node
- THEN the node is removed from active Nexus indexes and normal player maps
- AND the historical record remains available to administrators

## Requirement: Node deletion

The system SHALL require a short-lived, operator-bound confirmation before permanently deleting one or more death nodes.

### Scenario: Expired confirmation

- GIVEN a confirmation token has expired or belongs to another administrator
- WHEN deletion is requested
- THEN the Server rejects the request
- AND no data is changed

## Requirement: Backpack independence

The system SHALL NOT automatically delete a death backpack entity when its death node is disabled or deleted.

### Scenario: Recover after node deletion

- GIVEN a death node was deleted by an administrator
- AND the bound death backpack still exists
- WHEN the backpack is recovered
- THEN recovery completes successfully
- AND the missing node deactivation step is treated as an idempotent success

## Requirement: Audit trail

The system SHALL record the operator, operation, affected count, filter summary, timestamp and result of every administrative mutation.