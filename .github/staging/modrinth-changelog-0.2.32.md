## 0.2.32

### Fixed
- Fixed backpack sidebar sorting using a cached mouse position instead of the originating click position when integrated with TotemVanillaTweaks. A click in the expanded sidebar now targets that backpack even if the cached pointer still refers to the player inventory.
- Preserved configurable sorting bindings and read-only Observer input protection.

### Validation
- Reproduced the incorrect player-inventory target before the fix. Native client regressions passed with and without TotemVanillaTweaks 0.1.31, checking sidebar targets, persisted contents, and unchanged player inventory.
- Existing scoped dedicated server + target + observer compatibility tests passed.
