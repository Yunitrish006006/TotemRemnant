package dev.totem.remnant.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes the menu primitives used by the InventoryMenu backpack extension. */
@Mixin(AbstractContainerMenu.class)
interface AbstractContainerMenuInvoker {
    @Invoker("addSlot")
    Slot totem$addSlot(Slot slot);

    @Invoker("moveItemStackTo")
    boolean totem$moveItemStackTo(ItemStack stack, int start, int end, boolean reverse);
}
