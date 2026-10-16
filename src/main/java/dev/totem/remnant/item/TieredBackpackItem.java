package dev.totem.remnant.item;

import dev.totem.core.api.v1.migration.LegacyItemMigrationRegistry;
import dev.totem.remnant.inventory.BackpackMenu;
import dev.totem.remnant.inventory.DeathBackpackInventory;
import dev.totem.remnant.inventory.BackpackUpgradeInventory;
import dev.totem.remnant.registry.BackpackMenuRegistration;
import dev.totem.remnant.upgrade.BackpackCapacity;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Remnant-owned tier metadata for legacy portable backpack identifiers. */
public final class TieredBackpackItem extends AbstractBackpackItem {
    private final BackpackTier tier;
    public TieredBackpackItem(Properties properties, BackpackTier tier) { super(properties); this.tier = tier; }
    public BackpackTier tier() { return tier; }

    @Override public java.util.Optional<net.minecraft.world.inventory.tooltip.TooltipComponent> getTooltipImage(ItemStack bag) {
        return BackpackTooltip.from(bag);
    }

    @Override public boolean overrideStackedOnOther(ItemStack bag, net.minecraft.world.inventory.Slot slot,
            net.minecraft.world.inventory.ClickAction action, Player player) {
        return dev.totem.remnant.inventory.BackpackItemInteraction.onOther(bag, slot, action, player);
    }

    @Override public boolean overrideOtherStackedOnMe(ItemStack bag, ItemStack carried,
            net.minecraft.world.inventory.Slot slot, net.minecraft.world.inventory.ClickAction action,
            Player player, net.minecraft.world.entity.SlotAccess cursor) {
        return dev.totem.remnant.inventory.BackpackItemInteraction.onMe(bag, carried, slot, action, player, cursor);
    }

    @Override public void appendHoverText(ItemStack bag, Item.TooltipContext context,
            net.minecraft.world.item.component.TooltipDisplay display,
            java.util.function.Consumer<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(bag, context, display, tooltip, flag);
        ItemStack selected = dev.totem.remnant.inventory.BackpackItemInteraction.selectedStack(bag);
        if (!selected.isEmpty()) tooltip.accept(Component.translatable("tooltip.totem.backpack.selected",
                selected.getHoverName(), selected.getCount()).withStyle(net.minecraft.ChatFormatting.YELLOW));
        tooltip.accept(Component.translatable("tooltip.totem.backpack.bundle_controls").withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            ItemStack held = player.getItemInHand(hand);
            ItemStack migrated = LegacyItemMigrationRegistry.migrate(held);
            if (migrated != held) {
                player.setItemInHand(hand, migrated);
            }
            int storageSlots = BackpackCapacity.slots(migrated);
            int rows = storageSlots / 9;
            DeathBackpackInventory inventory = new DeathBackpackInventory(serverPlayer, hand, storageSlots);
            BackpackUpgradeInventory upgrades = new BackpackUpgradeInventory(serverPlayer, hand, tier.upgradeSlots());
            serverPlayer.openMenu(new ExtendedMenuProvider<Integer>() {
                @Override
                public Integer getScreenOpeningData(ServerPlayer player) {
                    return BackpackMenuRegistration.openingData(rows, tier.upgradeSlots());
                }

                @Override
                public Component getDisplayName() {
                    return Component.translatable("container.totem.backpack." + tier.name().toLowerCase());
                }

                @Override
                public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
                    return BackpackMenu.serverSide(syncId, playerInventory, inventory, upgrades, rows);
                }
            });
        }
        return InteractionResult.SUCCESS;
    }
    public enum BackpackTier {
        BASIC(9, 1), STANDARD(18, 2), ADVANCED(27, 3), NETHERITE(36, 4);
        private final int slots;
        private final int upgradeSlots;
        BackpackTier(int slots, int upgradeSlots) { this.slots = slots; this.upgradeSlots = upgradeSlots; }
        public int slots() { return slots; }
        public int upgradeSlots() { return upgradeSlots; }
    }
}
