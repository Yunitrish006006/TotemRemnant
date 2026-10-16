package dev.totem.remnant.mixin.client;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(LoomScreen.class)
public interface SidebarLoomScreenAccessor {
    @Accessor("scrollOffs") float remnant$getScrollOffs();
    @Accessor("scrollOffs") void remnant$setScrollOffs(float value);
    @Accessor("startRow") int remnant$getStartRow();
    @Accessor("startRow") void remnant$setStartRow(int value);
}
