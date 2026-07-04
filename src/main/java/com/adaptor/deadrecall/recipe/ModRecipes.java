package com.adaptor.deadrecall.recipe;

import com.adaptor.deadrecall.Deadrecall;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class ModRecipes {
    public static final RecipeSerializer<FlintFromBowlRecipe> FLINT_FROM_BOWL =
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
            Identifier.fromNamespaceAndPath("deadrecall", "flint_from_bowl"),
            FlintFromBowlRecipe.SERIALIZER);

    public static void registerModRecipes() {
        Deadrecall.LOGGER.info("正在註冊模組配方...");
    }
}
