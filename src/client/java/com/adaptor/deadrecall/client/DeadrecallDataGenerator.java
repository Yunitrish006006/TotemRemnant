package com.adaptor.deadrecall.client;

import com.adaptor.deadrecall.client.datagen.DeadRecallDataGenerationBootstrap;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class DeadrecallDataGenerator implements DataGeneratorEntrypoint {

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        DeadRecallDataGenerationBootstrap.register(pack);
    }
}
