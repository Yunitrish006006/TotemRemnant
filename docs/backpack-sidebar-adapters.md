# Backpack sidebar adapters

Remnant uses one `BackpackPanelAttachment` for selection, stable appended storage slots,
scrolling layout and transfers from a sidebar into the player's main inventory/hotbar.
`BackpackPanelContainer` remains responsible for persistence, ordinary/death storage,
external commits and close-time recovery. Sorting and transfer packets keep their existing
menu/state/selected-stack validation. The sidebar never remaps its 256 physical slot IDs.

`BackpackSidebarAdapters` is the immutable registration table shared by menu admission
and Observer variant discovery. It admits exact native classes only: InventoryMenu,
CraftingMenu, EnchantmentMenu, ChestMenu and twelve additional native workstation/container menus. ChestMenu covers vanilla chests, barrels and
ender chests. Subclasses, including Remnant's own standalone BackpackMenu, do not inherit
admission. A native slot-count mismatch fails closed before any sidebar slots are added.

Each native menu keeps a small constructor-tail / quickMoveStack adapter mixin. Attachment
must happen after all native slots exist; attaching in AbstractContainerMenu's constructor
would change functional slot indices. Preserve explicit constructor descriptors, especially
the direct ChestMenu constructor. Delegating tails must not append a second panel.
Only sidebar source slots use the shared quick-move operation. Native sources stay in vanilla
quickMoveStack; ChestMenu's separate clamp excludes appended slots from vanilla destinations.

## Adding another interface

This is an internal, source-level extension mechanism, not runtime third-party registration.
Add an exact menu class, unique Observer variant and its native slot-count policy to the table.
Add the two lifecycle hooks after checking every native constructor path and quick-move range.
Do not assume arbitrary menus use the same vanilla destination bounds.

Pair the registration with the actual native Screen class in the module-owned Observer
provider, and provide its production Screen/Menu read-only reconstruction, initial/update
snapshots, monotonic validation, cursor/carried stack, close handling and input/packet
suppression. Never admit a menu without that matching client and Observer implementation.
Use an exhaustive adapter switch for screen-class admission so new enum registrations require
a deliberate client pairing. Keep protocol compatibility or introduce a new protocol when the
snapshot contract changes. The sidebar extensions preserve `remnant_sidebar@1` unchanged.

Validate native indices and constructor paths, registered/unsupported classes, transfer and
sort item conservation, ordinary/death storage and recovery, and rendered production client
coverage. Run the owning module's Observer tests plus the relay's module-present and
module-absent coverage for any new family or protocol change.

## Registered workstation extensions

Furnace, smoker, blast furnace, anvil, stonecutter, smithing table, grindstone,
loom, cartography table, brewing stand, hopper and merchant screens share the
same attachment. Furnace and item-combiner constructors append only after their
native slots are complete. Hopper quick-move clamps its destination range like
ChestMenu. All admissions remain exact-class checks.

The Remnant provider carries native data slots and public scroll/selection state.
Merchant offers use a variant-specific version1 payload bounded to32 offers and
64KiB; malformed, oversized or trailing data is rejected. Anvil prospective-result
custom names are redacted, and its projection name responder cannot send packets.
The matching Observer changes suppress competing native senders and restrict
fallback snapshots to native slots; ship these consumer changes with the expansion.
