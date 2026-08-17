package dev.totem.remnant.inventory;

import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/** Ensures the shared player-visible root does not expose Remnant administration. */
public final class ContainerSafetyCommandAuthorizationGameTest {
    @SuppressWarnings("removal")
    @GameTest(maxTicks = 20)
    public void sharedRootIsPublicButContainerScanRemainsAdministratorOnly(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            CommandSourceStack source = player.createCommandSourceStack();
            CommandNode<CommandSourceStack> root = helper.getLevel().getServer()
                    .getCommands()
                    .getDispatcher()
                    .getRoot()
                    .getChild("deadrecall");
            require(helper, root != null, "Missing shared /deadrecall command root");
            require(helper, root.canUse(source), "Normal player cannot use the shared /deadrecall root");

            CommandNode<CommandSourceStack> containers = root.getChild("containers");
            require(helper, containers != null, "Missing /deadrecall containers command child");
            require(helper, !containers.canUse(source),
                    "Normal player unexpectedly has access to container diagnostics");
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }
}

