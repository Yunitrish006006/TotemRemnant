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

The next candidate is `totem-remnant-0.1.4.jar`, built against TotemCore
`0.2.0`. It does not replace the immutable `0.1.1` pin until its source commit,
artifact SHA-512 and assembled restart evidence are recorded in a new graph.

## Extraction order

Portable-container policy must move together with the backpack item hierarchy
and `BackpackItemHelper`: legacy `isBackpack` semantics are based on the item
types, not merely the `deadrecall:portable_containers` tag. Capture/recovery
services may only switch to the Remnant policy after those owner types exist.

The `0.1.3` candidate completes that policy ownership. It includes the stable
optional integration API, backpack-menu enforcement for every click route,
Shulker Box menu and sided-automation Mixins, rate-limited rejection logs and
the read-only `/deadrecall containers scan [player]` administrator report.
Existing invalid nesting remains readable and removable; the module never
rewrites legacy item components during load or diagnostics.

The `0.1.4` candidate completes the remaining Remnant surface: client-side
death-backpack beam rendering, the optional Trinkets Updated capture adapter,
and registration of the shared `deadrecall:main` creative tab.
