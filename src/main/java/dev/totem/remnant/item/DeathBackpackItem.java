package dev.totem.remnant.item;

import dev.totem.core.api.v1.migration.LegacyItemMigrationRegistry;
import dev.totem.remnant.inventory.BackpackMenu;
import dev.totem.remnant.inventory.DeathBackpackInventory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Persistent Remnant death-backpack item. */
public final class DeathBackpackItem extends AbstractBackpackItem {
    public DeathBackpackItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            ItemStack held = player.getItemInHand(hand);
            ItemStack migrated = LegacyItemMigrationRegistry.migrate(held);
            if (migrated != held) {
                player.setItemInHand(hand, migrated);
            }
            int rows = Math.max(1, Math.min(6,
                    (BackpackItemHelper.storedSlotFootprint(migrated) + 8) / 9));
            DeathBackpackInventory inventory = new DeathBackpackInventory(serverPlayer, hand, rows * 9);
            serverPlayer.openMenu(new net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider<Integer>() {
                @Override public Integer getScreenOpeningData(ServerPlayer player) {
                    return dev.totem.remnant.registry.BackpackMenuRegistration.openingData(rows, 0) | 0x10000;
                }
                @Override public Component getDisplayName() { return Component.translatable("container.totem.death_backpack"); }
                @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                        int id, net.minecraft.world.entity.player.Inventory inv, Player player) {
                    return BackpackMenu.deathServerSide(id, inv, inventory, rows);
                }
            });
        }
        return InteractionResult.SUCCESS;
    }
}
