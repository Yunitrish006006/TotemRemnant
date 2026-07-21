# Extraction contract

This repository will own death backpack capture/recovery, backpack items and
inventory, addon API, Trinkets integration, Remnant payloads, Mixins, client UI
and tests.  It must not import Nexus internals.  The stable death-node binding
stays on the backpack; Nexus may implement the Core lifecycle adapter.

During the lockstep compatibility window, existing
`com.adaptor.deadrecall.api.death` addons remain supported through forwarding
types in the DeadRecall bundle for at least two releases.
