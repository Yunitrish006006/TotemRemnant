package dev.totem.remnant.mixin.client;

import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

/** Read-only recipe-book UI state. Search text itself is deliberately never exposed. */
@Mixin(RecipeBookComponent.class)
public interface SidebarRecipeBookComponentAccessor {
    @Accessor("tabButtons") List<RecipeBookTabButton> remnant$getTabButtons();
    @Accessor("selectedTab") RecipeBookTabButton remnant$getSelectedTab();
    @Accessor("selectedTab") void remnant$setSelectedTab(RecipeBookTabButton value);
    @Accessor("filterButton") CycleButton<Boolean> remnant$getFilterButton();
    @Accessor("searchBox") EditBox remnant$getSearchBox();
    @Accessor("recipeBookPage") RecipeBookPage remnant$getRecipeBookPage();
    @Accessor("visible") void remnant$setVisible(boolean value);
    @Accessor("widthTooNarrow") void remnant$setWidthTooNarrow(boolean value);
    @Invoker("initVisuals") void remnant$initVisuals();
}
