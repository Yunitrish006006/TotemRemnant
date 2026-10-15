package dev.totem.remnant.mixin.client;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;
@Mixin(AbstractContainerMenu.class)
public interface SidebarMenuDataAccessor {
    @Accessor("dataSlots") List<DataSlot> remnant$dataSlots();
}
