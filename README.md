# TotemRemnant

TotemRemnant is the optional death-backpack module for Totem.  It depends on
TotemCore, but not TotemNexus: a Nexus death node is supplied only through the
versioned optional lifecycle adapter when Nexus is installed.

`0.1.4` is the current candidate built against TotemCore `0.2.0`; it reports
the spawned death-backpack ItemEntity UUID through Core's optional reverse
binding callback, rolls back the transaction when persistence fails, and owns
the complete portable-container nesting policy and diagnostics. It also owns
the death-backpack beam, optional Trinkets inventory adapter and the preserved
`deadrecall:main` creative tab.
`0.1.1` remains the first immutable lockstep artifact and rollback baseline.

## Verification

The Java 25 build and unit suite pass. The Fabric runner reports all 23 required
GameTests passing, including backpack click routes, Shulker Box menu and sided
automation, Hopper, Hopper Minecart, Dropper, diagnostics and legacy-data
guards. A nine-module candidate Dedicated Server also reached `Done`, executed
the Remnant-owned `/deadrecall containers scan` command once and stopped
cleanly.
