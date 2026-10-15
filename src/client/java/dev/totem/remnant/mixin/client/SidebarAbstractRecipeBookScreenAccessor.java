package dev.totem.remnant.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only semantic access to the recipe-book mode attached to a container screen. */
@Mixin(AbstractRecipeBookScreen.class)
public interface SidebarAbstractRecipeBookScreenAccessor {
    @Accessor("recipeBookComponent") RecipeBookComponent<?> remnant$getRecipeBookComponent();
    @Accessor("widthTooNarrow") boolean remnant$getWidthTooNarrow();
    @Accessor("widthTooNarrow") void remnant$setWidthTooNarrow(boolean value);
}
