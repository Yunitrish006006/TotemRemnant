package com.adaptor.deadrecall.integration.trinkets;

import com.adaptor.deadrecall.api.death.DeathBackpackAddonInventoryProvider;
import com.adaptor.deadrecall.api.death.DeathBackpackAddonSlot;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Optional adapter for Trinkets Updated 4.x on Minecraft 26.2. */
public final class TrinketsDeathBackpackInventoryProvider implements DeathBackpackAddonInventoryProvider {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("deadrecall", "trinkets_updated");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public List<? extends DeathBackpackAddonSlot> collectDroppableSlots(ServerPlayer player) {
        List<DeathBackpackAddonSlot> slots = new ArrayList<>();
        TrinketsApi.getAttachment(player).forEachDroppable((access, stack) -> {
            if (!stack.isEmpty()) {
                slots.add(new Slot(access));
            }
        }, false);
        return List.copyOf(slots);
    }

    private record Slot(TrinketSlotAccess access) implements DeathBackpackAddonSlot {
        @Override
        public String sourceKey() {
            return access.getSerializedName();
        }

        @Override
        public ItemStack snapshot() {
            return access.get().copy();
        }

        @Override
        public boolean clearIfUnchanged(ItemStack expected) {
            ItemStack current = access.get();
            if (!sameExactStack(current, expected)) {
                return false;
            }
            return access.set(ItemStack.EMPTY);
        }

        @Override
        public boolean restoreIfEmpty(ItemStack stack) {
            return access.get().isEmpty() && access.set(stack.copy());
        }

        private static boolean sameExactStack(ItemStack first, ItemStack second) {
            return first.getCount() == second.getCount()
                    && ItemStack.isSameItemSameComponents(first, second);
        }
    }
}
