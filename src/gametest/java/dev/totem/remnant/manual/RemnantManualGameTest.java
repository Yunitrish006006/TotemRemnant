package dev.totem.remnant.manual;

import dev.totem.core.api.v1.manual.TotemManualAssembler;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Verifies the server operation used by the smithing-table manual source. */
public final class RemnantManualGameTest {
    @GameTest(maxTicks = 20)
    public void smithingTableSourceCreatesAndReusesCanonicalManual(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOOK));
            if (!RemnantManual.grant(player, InteractionHand.MAIN_HAND)
                    || !TotemManualAssembler.isCanonical(player.getMainHandItem())) {
                helper.fail("Plain book did not become a canonical Totem manual");
                return;
            }

            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BOOK));
            if (!RemnantManual.grant(player, InteractionHand.OFF_HAND)
                    || !player.getOffhandItem().is(Items.BOOK)
                    || !TotemManualAssembler.isCanonical(player.getMainHandItem())) {
                helper.fail("Existing manual was not reused without consuming the other-hand book");
                return;
            }
            helper.succeed();
        } finally {
            player.discard();
        }
    }
}
