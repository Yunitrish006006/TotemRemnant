package com.adaptor.deadrecall.client.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

/**
 * Composes future resource-generation owners without changing the generated output.
 */
public final class DeadRecallDataGenerationBootstrap {
    private DeadRecallDataGenerationBootstrap() {
    }

    public static void register(FabricDataGenerator.Pack pack) {
        TotemDiscordBridgeDataGeneration.register(pack);
        TotemRemnantDataGeneration.register(pack);
        TotemAutomataDataGeneration.register(pack);
        TotemNexusDataGeneration.register(pack);
        LegacyGameplayDataGeneration.register(pack);
    }
}
