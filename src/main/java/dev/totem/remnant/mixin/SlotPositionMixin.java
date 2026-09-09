package dev.totem.remnant.mixin;

import dev.totem.remnant.inventory.MutableSlotPosition;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Allows the Remnant inventory side panel to move its existing panel slots
 * without replacing entries in AbstractContainerMenu.slots.
 */
@Mixin(Slot.class)
public abstract class SlotPositionMixin implements MutableSlotPosition {
    @Shadow @Final @Mutable public int x;
    @Shadow @Final @Mutable public int y;

    @Override
    public void totem$setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
