package com.adaptor.deadrecall.mixin;

import com.adaptor.deadrecall.util.EnchantingPowerHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(EnchantingTableBlock.class)
public abstract class EnchantingTableBlockMixin {

    /**
     * 讓附魔台原版粒子判定把雕紋書櫃也當作有效書櫃。
     * 這樣粒子路徑與節奏完全沿用原版 animateTick 邏輯。
     */
    @Redirect(
            method = "animateTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/EnchantingTableBlock;isValidBookShelf(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)Z"
            )
    )
    private static boolean deadrecall$acceptChiseledBookshelf(Level level, BlockPos tablePos, BlockPos offset) {
        BlockEntity be = level.getBlockEntity(tablePos.offset(offset));
        return be instanceof ChiseledBookShelfBlockEntity;
    }

    @Inject(method = "animateTick", at = @At("TAIL"))
    private void deadrecall$boostParticleRate(
            BlockState state,
            Level level,
            BlockPos tablePos,
            RandomSource random,
            CallbackInfo ci
    ) {
        int normalBookCount = 0;
        int enchantedBookPower = 0;
        List<BlockPos> normalOffsets = new ArrayList<>();
        List<BlockPos> enchantedOffsets = new ArrayList<>();
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            BlockEntity be = level.getBlockEntity(tablePos.offset(offset));
            if (!(be instanceof ChiseledBookShelfBlockEntity shelf) || !hasVisiblePath(level, tablePos, offset)) {
                continue;
            }

            int shelfNormalCount = 0;
            int shelfEnchantedPower = 0;
            for (ItemStack stack : shelf.getItems()) {
                if (stack.is(Items.BOOK)) {
                    shelfNormalCount++;
                } else if (stack.is(Items.ENCHANTED_BOOK)) {
                    shelfEnchantedPower += EnchantingPowerHelper.getItemPower(stack);
                }
            }

            if (shelfNormalCount > 0) {
                normalOffsets.add(offset);
                normalBookCount += shelfNormalCount;
            }
            if (shelfEnchantedPower > 0) {
                enchantedOffsets.add(offset);
                enchantedBookPower += shelfEnchantedPower;
            }
        }

        if (normalOffsets.isEmpty() && enchantedOffsets.isEmpty()) {
            return;
        }

        normalBookCount = Math.min(normalBookCount, 96);
        enchantedBookPower = Math.min(enchantedBookPower, 192);

        int normalAttempts = 1 + (normalBookCount / 8);
        int normalChance = Math.max(2, 20 - (normalBookCount / 4));
        spawnScaledParticles(level, tablePos, random, normalOffsets, normalAttempts, normalChance, 1, 0.18D);

        int enchantedAttempts = 1 + (enchantedBookPower / 5);
        int enchantedChance = Math.max(1, 12 - (enchantedBookPower / 16));
        int enchantedBurst = 1 + (enchantedBookPower / 32);
        spawnScaledParticles(level, tablePos, random, enchantedOffsets, enchantedAttempts, enchantedChance, enchantedBurst, 0.30D);
    }

    private static boolean hasVisiblePath(BlockGetter level, BlockPos tablePos, BlockPos offset) {
        int stepX = Integer.signum(offset.getX());
        int stepZ = Integer.signum(offset.getZ());
        BlockPos between1 = tablePos.offset(stepX, 0, stepZ);
        BlockPos between2 = tablePos.offset(stepX, 1, stepZ);
        return level.getBlockState(between1).isAir() && level.getBlockState(between2).isAir();
    }

    private static void spawnScaledParticles(
            Level level,
            BlockPos tablePos,
            RandomSource random,
            List<BlockPos> sourceOffsets,
            int attempts,
            int triggerChance,
            int burst,
            double speed
    ) {
        if (sourceOffsets.isEmpty()) {
            return;
        }

        for (int i = 0; i < attempts; i++) {
            if (random.nextInt(triggerChance) != 0) {
                continue;
            }

            BlockPos offset = sourceOffsets.get(random.nextInt(sourceOffsets.size()));
            BlockPos shelfPos = tablePos.offset(offset);

            for (int j = 0; j < burst; j++) {
                double startX = shelfPos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.5D;
                double startY = shelfPos.getY() + 0.55D + random.nextDouble() * 0.35D;
                double startZ = shelfPos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.5D;
                double endX = tablePos.getX() + 0.5D;
                double endY = tablePos.getY() + 1.0D;
                double endZ = tablePos.getZ() + 0.5D;

                level.addParticle(
                        ParticleTypes.ENCHANT,
                        startX,
                        startY,
                        startZ,
                        (endX - startX) * speed,
                        (endY - startY) * speed,
                        (endZ - startZ) * speed
                );
            }
        }
    }
}
