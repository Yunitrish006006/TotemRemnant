package dev.totem.remnant.death;

import dev.totem.core.api.v1.death.DeathRetainedItemPolicy;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Verifies bounded main-hand, offhand, hotbar, inventory death selection. */
public final class SoulboundDeathItemPriorityGameTest {
    @GameTest(maxTicks = 20)
    public void candidateOrderAndOffhandFallbackAreDeterministic(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            Inventory inventory = player.getInventory();
            inventory.setSelectedSlot(4);
            List<Integer> order = SoulboundDeathItemRetention.candidateSlots(inventory);
            if (order.size() != inventory.getContainerSize()
                    || order.get(0) != 4
                    || order.get(1) != Inventory.SLOT_OFFHAND
                    || order.get(2) != 0
                    || order.stream().distinct().count() != order.size()) {
                helper.fail("Soulbound candidate order was not main hand, offhand, then hotbar without duplicates");
                return;
            }

            inventory.setItem(4, new ItemStack(Items.DIAMOND));
            inventory.setItem(Inventory.SLOT_OFFHAND, new ItemStack(Items.BOOK));
            inventory.setItem(0, new ItemStack(Items.COMPASS));
            DeathRetainedItemPolicy.register((owner, stack) ->
                    stack.is(Items.BOOK) || stack.is(Items.COMPASS));

            if (!SoulboundDeathItemRetention.stageForDeath(player)
                    || !inventory.getItem(Inventory.SLOT_OFFHAND).isEmpty()
                    || !inventory.getItem(0).is(Items.COMPASS)) {
                helper.fail("Death staging did not choose the valid offhand item before the hotbar");
                return;
            }
            if (!SoulboundDeathItemRetention.restoreAfterRespawn(player)
                    || !inventory.getItem(Inventory.SLOT_OFFHAND).is(Items.BOOK)) {
                helper.fail("The automatically selected offhand interface did not restore to its slot");
                return;
            }
            helper.succeed();
        } finally {
            player.discard();
        }
    }
}
