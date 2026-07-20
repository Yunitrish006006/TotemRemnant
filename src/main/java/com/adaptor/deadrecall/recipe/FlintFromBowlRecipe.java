package com.adaptor.deadrecall.recipe;

import com.adaptor.deadrecall.registry.LegacyGameplayItemRegistration;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class FlintFromBowlRecipe extends CustomRecipe {
    private static final FlintFromBowlRecipe INSTANCE = new FlintFromBowlRecipe();

    public static final MapCodec<FlintFromBowlRecipe> CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, FlintFromBowlRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<FlintFromBowlRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() != 2) {
            return false;
        }

        boolean hasGravel = false;
        boolean hasBowl = false;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(Items.GRAVEL)) {
                hasGravel = true;
                continue;
            }
            if (stack.is(LegacyGameplayItemRegistration.STONE_BOWL)) {
                hasBowl = true;
                continue;
            }
            return false;
        }

        return hasGravel && hasBowl;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return new ItemStack(Items.FLINT);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < input.size(); i++) {
            if (input.getItem(i).is(LegacyGameplayItemRegistration.STONE_BOWL)) {
                remaining.set(i, new ItemStack(LegacyGameplayItemRegistration.STONE_BOWL));
            }
        }
        return remaining;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return SERIALIZER;
    }
}
