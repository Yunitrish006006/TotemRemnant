package dev.totem.remnant.mixin.client;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(StonecutterScreen.class)
public interface SidebarStonecutterScreenAccessor {
    @Accessor("scrollOffs") float remnant$getScrollOffs();
    @Accessor("scrollOffs") void remnant$setScrollOffs(float value);
    @Accessor("startIndex") int remnant$getStartIndex();
    @Accessor("startIndex") void remnant$setStartIndex(int value);
}
