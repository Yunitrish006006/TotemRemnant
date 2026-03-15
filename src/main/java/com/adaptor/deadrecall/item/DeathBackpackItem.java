package com.adaptor.deadrecall.item;

import com.adaptor.deadrecall.screen.BackpackScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
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

    /**
     * 計算死亡背包實際需要的行數
     */
    private int calculateRows(ItemStack stack) {
        ContainerComponent container = stack.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT);
        int itemCount = 0;
        for (ItemStack s : container.iterateNonEmpty()) {
            itemCount++;
        }
        return Math.max(1, (int) Math.ceil(itemCount / 9.0));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient) {
            final int rows = calculateRows(stack);

            user.openHandledScreen(new ExtendedScreenHandlerFactory() {
                @Override
                public Text getDisplayName() {
                    return Text.translatable("container.deadrecall.death_backpack");
                }

                @Override
                public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
                    return new BackpackScreenHandler(syncId, playerInventory, player, hand, rows);
                }

                @Override
                public Object getScreenOpeningData(ServerPlayerEntity player) {
                    return rows;
                }
            });
        }

        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, net.minecraft.item.tooltip.TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        int rows = calculateRows(stack);
        int itemCount = 0;
        ContainerComponent container = stack.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT);
        for (ItemStack s : container.iterateNonEmpty()) {
            itemCount++;
        }
        tooltip.add(Text.literal("死亡背包 - 收集死亡掉落物品")
            .formatted(Formatting.RED));
        tooltip.add(Text.literal("物品數量: " + itemCount + " (" + rows + "排)")
            .formatted(Formatting.GRAY));
        tooltip.add(Text.literal("防火保護")
            .formatted(Formatting.GOLD));
    }
}
