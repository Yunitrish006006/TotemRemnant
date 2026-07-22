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
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Remnant-owned tier metadata for legacy portable backpack identifiers. */
public final class TieredBackpackItem extends AbstractBackpackItem {
    private final BackpackTier tier;
    public TieredBackpackItem(Properties properties, BackpackTier tier) { super(properties); this.tier = tier; }
    public BackpackTier tier() { return tier; }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            int rows = tier.slots() / 9;
            DeathBackpackInventory inventory = new DeathBackpackInventory(serverPlayer, hand, tier.slots());
            serverPlayer.openMenu(new SimpleMenuProvider((syncId, playerInventory, ignored) -> switch (rows) {
                case 1 -> new ChestMenu(MenuType.GENERIC_9x1, syncId, playerInventory, inventory, 1);
                case 2 -> new ChestMenu(MenuType.GENERIC_9x2, syncId, playerInventory, inventory, 2);
                case 3 -> new ChestMenu(MenuType.GENERIC_9x3, syncId, playerInventory, inventory, 3);
                default -> new ChestMenu(MenuType.GENERIC_9x4, syncId, playerInventory, inventory, 4);
            }, Component.translatable("container.deadrecall.backpack." + tier.name().toLowerCase())));
        }
        return InteractionResult.SUCCESS;
    }
    public enum BackpackTier {
        BASIC(9), STANDARD(18), ADVANCED(27), NETHERITE(36);
        private final int slots;
        BackpackTier(int slots) { this.slots = slots; }
        public int slots() { return slots; }
    }
}
