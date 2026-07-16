# Tasks: Admin Death Node Manager

## Specification and data access

- [ ] Define a server-side death-node query service with pagination and stable sorting.
- [ ] Add diagnostic status calculation without mutating data during reads.
- [ ] Add permission checks for every query and mutation endpoint.

## GUI and networking

- [ ] Add the admin command or entry point that opens the manager.
- [ ] Implement player, dimension, status and time filters.
- [ ] Implement paginated result rows and detail inspection.
- [ ] Implement short-lived confirmation tokens for destructive operations.
- [ ] Localize every player-visible label, tooltip and error.

## Operations

- [ ] Implement safe teleport-to-node.
- [ ] Implement single disable and delete.
- [ ] Implement server-recomputed batch disable and delete.
- [ ] Make backpack recovery idempotent when the bound node no longer exists.
- [ ] Add audit logging and optional Discord Bridge reporting.

## Tests

- [ ] Permission denial and forged payload tests.
- [ ] Offline player filtering and old-name/UUID lookup tests.
- [ ] Pagination stability under concurrent node changes.
- [ ] Single and batch confirmation expiry tests.
- [ ] Delete-node-then-recover-backpack test.
- [ ] Duplicate binding and missing backpack diagnostic tests.
- [ ] Dedicated Server restart persistence test.