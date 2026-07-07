package com.adaptor.deadrecall.block.entity;

import com.adaptor.deadrecall.alchemy.AlchemyHandler;
import com.adaptor.deadrecall.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class AlchemyCauldronBlockEntity extends BlockEntity {
    private static final int COOK_TICKS_PER_LAYER = 200;

    private boolean ashAdded;
    private boolean ashCooked;
    private boolean mushroomAdded;
    private boolean mushroomCooked;
    private boolean manureAdded;
    private boolean manureCooked;
    private int cookTime;

    public AlchemyCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALCHEMY_CAULDRON, pos, state);
    }

    public boolean addIngredient(AlchemyHandler.AlchemyIngredient ingredient) {
        if (!canAddIngredient(ingredient)) {
            return false;
        }

        boolean changed = switch (ingredient) {
            case WOOD_ASH -> addAsh();
            case MUSHROOM -> addMushroom();
            case PIG_MANURE -> addManure();
        };
        if (changed) {
            setChanged();
        }
        return changed;
    }

    public boolean canAddIngredient(AlchemyHandler.AlchemyIngredient ingredient) {
        return switch (ingredient) {
            case WOOD_ASH -> !ashAdded && !ashCooked;
            case MUSHROOM -> !mushroomAdded && !mushroomCooked;
            case PIG_MANURE -> !manureAdded && !manureCooked;
        };
    }

    private boolean addAsh() {
        if (ashAdded || ashCooked) {
            return false;
        }
        ashAdded = true;
        return true;
    }

    private boolean addMushroom() {
        if (mushroomAdded || mushroomCooked) {
            return false;
        }
        mushroomAdded = true;
        return true;
    }

    private boolean addManure() {
        if (manureAdded || manureCooked) {
            return false;
        }
        manureAdded = true;
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlchemyCauldronBlockEntity cauldron) {
        if (!AlchemyHandler.hasLitCampfireBelow(level, pos)) {
            cauldron.cookTime = 0;
            return;
        }

        AlchemyHandler.AlchemyIngredient next = cauldron.nextCookableIngredient();
        if (next == null) {
            cauldron.cookTime = 0;
            return;
        }

        cauldron.cookTime++;
        if (cauldron.cookTime < COOK_TICKS_PER_LAYER) {
            cauldron.setChanged();
            return;
        }

        cauldron.cookTime = 0;
        cauldron.markCooked(next);

        if (cauldron.isComplete()) {
            ItemEntity result = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5,
                    new ItemStack(ModItems.SALTPETER));
            result.setDeltaMovement(0.0, 0.05, 0.0);
            level.addFreshEntity(result);
            level.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), 3);
            return;
        }

        int waterLevel = state.getValue(LayeredCauldronBlock.LEVEL);
        if (waterLevel > LayeredCauldronBlock.MIN_FILL_LEVEL) {
            level.setBlock(pos, state.setValue(LayeredCauldronBlock.LEVEL, waterLevel - 1), 3);
        }
        cauldron.setChanged();
    }

    private AlchemyHandler.AlchemyIngredient nextCookableIngredient() {
        if (ashAdded && !ashCooked) {
            return AlchemyHandler.AlchemyIngredient.WOOD_ASH;
        }
        if (mushroomAdded && !mushroomCooked) {
            return AlchemyHandler.AlchemyIngredient.MUSHROOM;
        }
        if (manureAdded && !manureCooked) {
            return AlchemyHandler.AlchemyIngredient.PIG_MANURE;
        }
        return null;
    }

    private void markCooked(AlchemyHandler.AlchemyIngredient ingredient) {
        switch (ingredient) {
            case WOOD_ASH -> {
                ashAdded = false;
                ashCooked = true;
            }
            case MUSHROOM -> {
                mushroomAdded = false;
                mushroomCooked = true;
            }
            case PIG_MANURE -> {
                manureAdded = false;
                manureCooked = true;
            }
        }
    }

    private boolean isComplete() {
        return ashCooked && mushroomCooked && manureCooked;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("ash_added", ashAdded);
        output.putBoolean("ash_cooked", ashCooked);
        output.putBoolean("mushroom_added", mushroomAdded);
        output.putBoolean("mushroom_cooked", mushroomCooked);
        output.putBoolean("manure_added", manureAdded);
        output.putBoolean("manure_cooked", manureCooked);
        output.putInt("cook_time", cookTime);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ashAdded = input.getBooleanOr("ash_added", false);
        ashCooked = input.getBooleanOr("ash_cooked", false);
        mushroomAdded = input.getBooleanOr("mushroom_added", false);
        mushroomCooked = input.getBooleanOr("mushroom_cooked", false);
        manureAdded = input.getBooleanOr("manure_added", false);
        manureCooked = input.getBooleanOr("manure_cooked", false);
        cookTime = input.getIntOr("cook_time", 0);
    }
}
