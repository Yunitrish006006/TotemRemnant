package com.adaptor.deadrecall.item;

import com.adaptor.deadrecall.screen.BackpackScreenHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class BackpackItem extends Item {
    public BackpackItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient) {
            // 在伺服器端開啟背包介面，传递hand信息
            user.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, player) ->
                    new BackpackScreenHandler(syncId, playerInventory, player, hand, TieredBackpackItem.BackpackTier.STANDARD),
                Text.translatable("container.deadrecall.backpack")
            ));
        }

        return TypedActionResult.success(stack, world.isClient());
    }
}
