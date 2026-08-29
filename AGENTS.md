# TotemRemnant instructions

## Module-owned Observer UI

- Every new or modified player-facing `Screen`/`Menu` must provide a
  module-owned, read-only semantic Observer mode through TotemCore.
- TotemVanillaTweaks may coordinate relay and cursor state but must not copy the
  Backpack UI or draw a hand-built replacement screen.
- Remain framebuffer-free; never relay screenshots, pixels or video.
- Suppress all observer slot/button/edit/drag/scroll/key mutation and packet
  paths. Escape only stops observing; viewer authority never becomes target
  authority. Never relay secrets or unsent text.
- Require unit tests, native-scale Client GameTest screenshots, dedicated
  three-JVM E2E and Production Runtime checks for UI changes.
- Provider capture/create and handle methods are client-thread-only; GameTests
  must use their client-thread context helpers.
