package com.adaptor.deadrecall.alchemy;

import com.adaptor.deadrecall.block.ModBlocks;
import com.adaptor.deadrecall.block.entity.AlchemyCauldronBlockEntity;
import com.adaptor.deadrecall.item.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class AlchemyHandler {
    private static final int DROPPED_ITEM_SCAN_INTERVAL_TICKS = 5;

    private AlchemyHandler() {
    }

    public enum AlchemyIngredient {
        WOOD_ASH,
        MUSHROOM,
        PIG_MANURE
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (player.isSpectator()) {
                return InteractionResult.PASS;
            }

            BlockPos pos = hitResult.getBlockPos();
            ItemStack stack = player.getItemInHand(hand);

            InteractionResult manureResult = tryHarvestPigManure(player, world, hand, stack, pos);
            if (manureResult != InteractionResult.PASS) {
                return manureResult;
            }

            AlchemyIngredient ingredient = getIngredient(stack);
            if (ingredient == null) {
                return InteractionResult.PASS;
            }

            if (world.isClientSide()) {
                return canAcceptIngredient(world, pos, ingredient) ? InteractionResult.SUCCESS : InteractionResult.PASS;
            }

            if (tryAddIngredient((ServerLevel) world, pos, ingredient)) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                player.sendOverlayMessage(Component.translatable("message.deadrecall.alchemy.ingredient_added"));
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(AlchemyHandler::tickDroppedIngredients);
    }

    public static boolean hasLitCampfireBelow(Level level, BlockPos cauldronPos) {
        BlockState below = level.getBlockState(cauldronPos.below());
        return CampfireBlock.isLitCampfire(below);
    }

    private static InteractionResult tryHarvestPigManure(Player player, Level world, net.minecraft.world.InteractionHand hand,
                                                        ItemStack stack, BlockPos pos) {
        BlockState cleanState = ModBlocks.getCleanState(world.getBlockState(pos));
        if (cleanState == null || !stack.is(ItemTags.SHOVELS)) {
            return InteractionResult.PASS;
        }

        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        world.setBlock(pos, cleanState, 3);
        ItemStack manure = new ItemStack(ModItems.PIG_MANURE);
        if (!player.addItem(manure)) {
            player.drop(manure, false);
        }
        if (!player.getAbilities().instabuild) {
            stack.hurtAndBreak(1, player, hand);
        }
        world.playSound(null, pos, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    private static void tickDroppedIngredients(MinecraftServer server) {
        if (server.getTickCount() % DROPPED_ITEM_SCAN_INTERVAL_TICKS != 0) {
            return;
        }

        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof ItemEntity itemEntity) || !itemEntity.isAlive()) {
                    continue;
                }

                ItemStack stack = itemEntity.getItem();
                AlchemyIngredient ingredient = getIngredient(stack);
                if (ingredient == null) {
                    continue;
                }

                BlockPos cauldronPos = findCauldronForDroppedItem(level, itemEntity);
                if (cauldronPos == null || !tryAddIngredient(level, cauldronPos, ingredient)) {
                    continue;
                }

                stack.shrink(1);
                if (stack.isEmpty()) {
                    itemEntity.discard();
                } else {
                    itemEntity.setItem(stack);
                    itemEntity.setPickUpDelay(20);
                }
            }
        }
    }

    private static BlockPos findCauldronForDroppedItem(ServerLevel level, ItemEntity itemEntity) {
        BlockPos pos = itemEntity.blockPosition();
        if (canAcceptAnyIngredient(level, pos)) {
            return pos;
        }

        BlockPos below = pos.below();
        if (canAcceptAnyIngredient(level, below)) {
            return below;
        }
        return null;
    }

    private static boolean canAcceptAnyIngredient(Level level, BlockPos pos) {
        return canAcceptIngredient(level, pos, AlchemyIngredient.WOOD_ASH)
                || canAcceptIngredient(level, pos, AlchemyIngredient.MUSHROOM)
                || canAcceptIngredient(level, pos, AlchemyIngredient.PIG_MANURE);
    }

    private static boolean canAcceptIngredient(Level level, BlockPos pos, AlchemyIngredient ingredient) {
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.WATER_CAULDRON)) {
            return state.getValue(LayeredCauldronBlock.LEVEL) == LayeredCauldronBlock.MAX_FILL_LEVEL
                    && hasLitCampfireBelow(level, pos);
        }
        return state.is(ModBlocks.ALCHEMY_CAULDRON)
                && hasLitCampfireBelow(level, pos)
                && level.getBlockEntity(pos) instanceof AlchemyCauldronBlockEntity cauldron
                && cauldron.canAddIngredient(ingredient);
    }

    private static boolean tryAddIngredient(ServerLevel level, BlockPos pos, AlchemyIngredient ingredient) {
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.WATER_CAULDRON)) {
            if (state.getValue(LayeredCauldronBlock.LEVEL) != LayeredCauldronBlock.MAX_FILL_LEVEL
                    || !hasLitCampfireBelow(level, pos)) {
                return false;
            }
            BlockState alchemyState = ModBlocks.ALCHEMY_CAULDRON.defaultBlockState()
                    .setValue(LayeredCauldronBlock.LEVEL, state.getValue(LayeredCauldronBlock.LEVEL));
            level.setBlock(pos, alchemyState, 3);
        } else if (!state.is(ModBlocks.ALCHEMY_CAULDRON) || !hasLitCampfireBelow(level, pos)) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof AlchemyCauldronBlockEntity cauldron)) {
            return false;
        }

        boolean added = cauldron.addIngredient(ingredient);
        if (added) {
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.7F, 1.0F);
        }
        return added;
    }

    private static AlchemyIngredient getIngredient(ItemStack stack) {
        if (stack.is(ModItems.WOOD_ASH)) {
            return AlchemyIngredient.WOOD_ASH;
        }
        if (stack.is(ModItems.PIG_MANURE)) {
            return AlchemyIngredient.PIG_MANURE;
        }
        if (stack.is(Items.BROWN_MUSHROOM) || stack.is(Items.RED_MUSHROOM)) {
            return AlchemyIngredient.MUSHROOM;
        }
        return null;
    }
}
