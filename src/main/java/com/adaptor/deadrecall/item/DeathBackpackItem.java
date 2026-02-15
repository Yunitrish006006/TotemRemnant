package com.adaptor.deadrecall.item;

import com.adaptor.deadrecall.screen.BackpackScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public class DeathBackpackItem extends Item {
    public DeathBackpackItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient) {
            // 在伺服器端開啟死亡背包介面，使用 ExtendedScreenHandlerFactory
            user.openHandledScreen(new ExtendedScreenHandlerFactory() {
                @Override
                public Text getDisplayName() {
                    return Text.translatable("container.deadrecall.death_backpack");
                }

                @Override
                public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
                    return new BackpackScreenHandler(syncId, playerInventory, player, hand, TieredBackpackItem.BackpackTier.ADVANCED);
                }

                @Override
                public Object getScreenOpeningData(ServerPlayerEntity player) {
                    return TieredBackpackItem.BackpackTier.ADVANCED.ordinal();
                }
            });
        }

        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, net.minecraft.item.tooltip.TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.literal("死亡背包 - 收集死亡掉落物品")
            .formatted(Formatting.RED));
        tooltip.add(Text.literal("容量: 27格 (3排)")
            .formatted(Formatting.GRAY));
        tooltip.add(Text.literal("防火保護")
            .formatted(Formatting.GOLD));
    }
}
