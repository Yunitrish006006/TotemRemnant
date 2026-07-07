package com.adaptor.deadrecall.block.entity;

import com.adaptor.deadrecall.Deadrecall;
import com.adaptor.deadrecall.block.ModBlocks;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

public final class ModBlockEntities {
    public static final BlockEntityType<AlchemyCauldronBlockEntity> ALCHEMY_CAULDRON = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath("deadrecall", "alchemy_cauldron"),
            new BlockEntityType<>(AlchemyCauldronBlockEntity::new, Set.of(ModBlocks.ALCHEMY_CAULDRON))
    );

    private ModBlockEntities() {
    }

    public static void registerModBlockEntities() {
        Deadrecall.LOGGER.info("正在註冊模組方塊實體...");
    }
}
