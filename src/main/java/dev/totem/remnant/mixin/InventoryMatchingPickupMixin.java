package dev.totem.remnant.mixin;

import dev.totem.remnant.upgrade.BackpackMatchingPickup;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Routes ordinary incoming stacks through matching-pickup modules before vanilla placement. */
@Mixin(Inventory.class)
public abstract class InventoryMatchingPickupMixin {
    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void totemremnant$routeMatchingItems(int slot, ItemStack stack,
                                                 CallbackInfoReturnable<Boolean> callback) {
        if (slot == -1 && BackpackMatchingPickup.deposit((Inventory) (Object) this, stack)
                && stack.isEmpty()) {
            callback.setReturnValue(true);
        }
    }
}
