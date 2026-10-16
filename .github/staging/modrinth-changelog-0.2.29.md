## 0.2.29

- Add backpack sidebars to furnaces, smokers, blast furnaces, anvils, stonecutters, smithing tables, grindstones, looms, cartography tables, brewing stands, hoppers and villager trading.
- Support ordinary and death backpacks through the shared native-menu adapter framework.
- Move materials manually between backpack storage and native workstation slots, preserving vanilla slot rules and item counts.
- Keep sidebar scrolling independent from trading, stonecutting and loom recipe scrolling.
- Add module-owned read-only Observer reconstruction for all 16 supported menu families, including merchant offers and native workstation state.
- Hide unsent anvil rename text and suppress observer input and item packets.

Minecraft 26.3 / Fabric; requires TotemCore 0.7.23. For Observer viewing of these additional sidebars, update the companion TotemObserver to 0.1.7.
