package dev.totem.remnant.item;

import dev.totem.remnant.inventory.DeathBackpackInventory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;

/** Persistent Remnant death-backpack item. */
public final class DeathBackpackItem extends AbstractBackpackItem {
    public DeathBackpackItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            int rows = Math.max(1, Math.min(6, (int) Math.ceil(BackpackItemHelper.countStoredStacks(player.getItemInHand(hand)) / 9.0D)));
            DeathBackpackInventory inventory = new DeathBackpackInventory(serverPlayer, hand, rows * 9);
            serverPlayer.openMenu(new SimpleMenuProvider((syncId, playerInventory, ignored) -> switch (rows) {
                case 1 -> new ChestMenu(MenuType.GENERIC_9x1, syncId, playerInventory, inventory, 1);
                case 2 -> new ChestMenu(MenuType.GENERIC_9x2, syncId, playerInventory, inventory, 2);
                case 3 -> new ChestMenu(MenuType.GENERIC_9x3, syncId, playerInventory, inventory, 3);
                case 4 -> new ChestMenu(MenuType.GENERIC_9x4, syncId, playerInventory, inventory, 4);
                case 5 -> new ChestMenu(MenuType.GENERIC_9x5, syncId, playerInventory, inventory, 5);
                default -> new ChestMenu(MenuType.GENERIC_9x6, syncId, playerInventory, inventory, 6);
            }, Component.translatable("container.deadrecall.death_backpack")));
        }
        return InteractionResult.SUCCESS;
    }
}
