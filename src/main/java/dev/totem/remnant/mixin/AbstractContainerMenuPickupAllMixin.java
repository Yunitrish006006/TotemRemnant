package dev.totem.remnant.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.totem.remnant.inventory.BackpackPanelSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Keeps one vanilla double-click collection on the surface where it started. */
@Mixin(AbstractContainerMenu.class)
abstract class AbstractContainerMenuPickupAllMixin {
    @WrapOperation(method = "doClick", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/inventory/AbstractContainerMenu;canTakeItemForPickAll(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/inventory/Slot;)Z"))
    private boolean totem$collectOnlyFromOriginSurface(
            AbstractContainerMenu menu, ItemStack carried, Slot candidate,
            Operation<Boolean> original, int slotId, int button, ContainerInput input, Player player
    ) {
        if (!(menu instanceof InventoryMenu)) return original.call(menu, carried, candidate);
        if (slotId < 0 || slotId >= menu.slots.size()) return false;
        Slot origin = menu.getSlot(slotId);
        if (!origin.isActive() || !candidate.isActive()
                || (origin instanceof BackpackPanelSlot) != (candidate instanceof BackpackPanelSlot)) {
            return false;
        }
        return original.call(menu, carried, candidate);
    }
}
