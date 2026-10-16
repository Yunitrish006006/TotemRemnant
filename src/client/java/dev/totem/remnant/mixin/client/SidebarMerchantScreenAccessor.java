package dev.totem.remnant.mixin.client;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(MerchantScreen.class)
public interface SidebarMerchantScreenAccessor {
    @Accessor("shopItem") int remnant$getShopItem();
    @Accessor("shopItem") void remnant$setShopItem(int value);
    @Accessor("scrollOff") int remnant$getScrollOff();
    @Accessor("scrollOff") void remnant$setScrollOff(int value);
}
