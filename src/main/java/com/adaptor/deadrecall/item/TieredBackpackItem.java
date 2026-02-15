package com.adaptor.deadrecall.item;

import com.adaptor.deadrecall.screen.BackpackScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public class TieredBackpackItem extends Item {
    private final BackpackTier tier;

    public TieredBackpackItem(Settings settings, BackpackTier tier) {
        super(settings);
        this.tier = tier;
    }

    public BackpackTier getTier() {
        return tier;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient) {
            // 在伺服器端開啟背包介面，使用 ExtendedScreenHandlerFactory 傳遞等級信息
            user.openHandledScreen(new ExtendedScreenHandlerFactory() {
                @Override
                public Text getDisplayName() {
                    return Text.translatable("container.deadrecall.backpack." + tier.getName());
                }

                @Override
                public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
                    return new BackpackScreenHandler(syncId, playerInventory, player, hand, tier);
                }

                @Override
                public Object getScreenOpeningData(ServerPlayerEntity player) {
                    return tier.ordinal();
                }
            });
        }

        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, net.minecraft.item.tooltip.TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.literal("等級: " + tier.getDisplayName())
            .formatted(Formatting.GRAY));
        tooltip.add(Text.literal("槽位: " + tier.getSlots())
            .formatted(Formatting.BLUE));
    }

    public enum BackpackTier {
        BASIC("basic", "基礎", 9, 1),           // 1排 9格
        STANDARD("standard", "標準", 18, 2),    // 2排 18格
        ADVANCED("advanced", "進階", 27, 3),    // 3排 27格
        NETHERITE("netherite", "獄髓", 36, 4); // 4排 36格

        private final String name;
        private final String displayName;
        private final int slots;
        private final int rows;

        BackpackTier(String name, String displayName, int slots, int rows) {
            this.name = name;
            this.displayName = displayName;
            this.slots = slots;
            this.rows = rows;
        }

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getSlots() {
            return slots;
        }

        public int getRows() {
            return rows;
        }
    }
}
