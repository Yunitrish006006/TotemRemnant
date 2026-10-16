## 0.2.30

- Show backpack contents in the hover tooltip, with a highlighted wheel selection, item counts and the selected position. Larger contents page around the selected item.
- Fix middle-clicking a backpack item to sort and merge its internal contents while preserving its location, surrounding items and overflow storage.
- Support hovered backpack sorting in player inventory, chests and existing captured backpacks inside death-backpack storage.
- Enable native middle-click backpack sorting when TotemVanillaTweaks is absent; retain its configurable sort shortcut when installed.
- Reject stale container/root-backpack requests and preserve read-only Observer input suppression.

Minecraft 26.3 / Fabric; requires TotemCore 0.7.23. Update both client and server to use hovered-backpack sorting.
