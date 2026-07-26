# TotemRemnant

TotemRemnant is the optional death-backpack module for Totem.  It depends on
TotemCore, but not TotemNexus: a Nexus death node is supplied only through the
versioned optional lifecycle adapter when Nexus is installed.

`0.1.0` is the first immutable lockstep artifact. It retains DeadRecall's addon
API compatibility and is assembled with TotemCore by the DeadRecall compatibility
bundle. The legacy implementation remains available in DeadRecall during the
two-release observation window so the bundle can roll back without data loss.
