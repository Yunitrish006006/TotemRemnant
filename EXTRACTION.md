# Extraction contract

This repository will own death backpack capture/recovery, backpack items and
inventory, addon API, Trinkets integration, Remnant payloads, Mixins, client UI
and tests.  It must not import Nexus internals.  The stable death-node binding
stays on the backpack; Nexus may implement the Core lifecycle adapter.

During the lockstep compatibility window, existing
`com.adaptor.deadrecall.api.death` addons remain supported through forwarding
types in the DeadRecall bundle for at least two releases.

## Immutable bundle artifact

The first bundle artifact is `totem-remnant-0.1.1.jar`. It is built against the
pinned `TotemCore 0.1.2` JAR and normalized after Loom packages it, so the
DeadRecall lockstep manifest can verify its SHA-512. The rollback graph omits
Remnant and activates DeadRecall's guarded legacy implementation instead.

## Extraction order

Portable-container policy must move together with the backpack item hierarchy
and `BackpackItemHelper`: legacy `isBackpack` semantics are based on the item
types, not merely the `deadrecall:portable_containers` tag. Capture/recovery
services may only switch to the Remnant policy after those owner types exist.
