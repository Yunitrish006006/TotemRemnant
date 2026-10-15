package dev.totem.remnant.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only recipe-book page position for Observer reconstruction. */
@Mixin(RecipeBookPage.class)
public interface SidebarRecipeBookPageAccessor {
    @Accessor("minecraft") Minecraft remnant$getMinecraft();
    @Accessor("currentPage") int remnant$getCurrentPage();
    @Accessor("currentPage") void remnant$setCurrentPage(int value);
    @Accessor("totalPages") int remnant$getTotalPages();
    @Accessor("totalPages") void remnant$setTotalPages(int value);
}
