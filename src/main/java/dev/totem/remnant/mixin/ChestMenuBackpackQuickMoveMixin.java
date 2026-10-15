package dev.totem.remnant.mixin;

import dev.totem.remnant.inventory.BackpackPanelMenuAccess;
import net.minecraft.world.inventory.ChestMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Appended panel slots must not become native chest shift-click destinations. */
@Mixin(ChestMenu.class)
abstract class ChestMenuBackpackQuickMoveMixin {
    @ModifyArg(method = "quickMoveStack", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/inventory/ChestMenu;moveItemStackTo(Lnet/minecraft/world/item/ItemStack;IIZ)Z"), index = 2)
    private int totem$boundNativeDestinations(int end) {
        ChestMenu menu = (ChestMenu)(Object)this;
        if (BackpackPanelMenuAccess.supports(menu) && (Object)this instanceof BackpackPanelMenuAccess access)
            return Math.min(end, access.totem$getBackpackPanelSlotStart());
        return end;
    }
}
