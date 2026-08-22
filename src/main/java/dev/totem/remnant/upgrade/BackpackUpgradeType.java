package dev.totem.remnant.upgrade;

/** Functional modules that can be installed in an ordinary Remnant backpack. */
public enum BackpackUpgradeType {
    CRAFTING,
    COMPACTION,
    MATCHING_PICKUP,
    CAPACITY,
    SOULBOUND_CHARGE,
    ENDER_ACCESS,
    BLAST_PROTECTION,
    FIRE_PROTECTION,
    DESPAWN_PROTECTION,
    VOID_PROTECTION,
    PERFECT_PRESERVATION;

    /** Capabilities folded into the one-slot Perfect Preservation module. */
    public boolean isPreservationProtection() {
        return this == BLAST_PROTECTION
                || this == FIRE_PROTECTION
                || this == DESPAWN_PROTECTION
                || this == VOID_PROTECTION;
    }
}
