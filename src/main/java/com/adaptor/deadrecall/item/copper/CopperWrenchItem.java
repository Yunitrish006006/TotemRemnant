package com.adaptor.deadrecall.item.copper;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.UUID;
import java.util.function.Consumer;

public class CopperWrenchItem extends Item {
    public CopperWrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, tooltipFlag);

        UUID selectedGolem = CopperGolemWrenchHandler.getSelectedGolem(stack);
        tooltipAdder.accept(Component.translatable("item.deadrecall.copper_wrench.tooltip.summary").withStyle(ChatFormatting.GRAY));
        tooltipAdder.accept(Component.translatable(selectedGolem == null
                ? "item.deadrecall.copper_wrench.tooltip.unbound"
                : "item.deadrecall.copper_wrench.tooltip.bound").withStyle(selectedGolem == null ? ChatFormatting.YELLOW : ChatFormatting.GREEN));
        tooltipAdder.accept(Component.translatable("item.deadrecall.copper_wrench.tooltip.bind").withStyle(ChatFormatting.BLUE));
        tooltipAdder.accept(Component.translatable("item.deadrecall.copper_wrench.tooltip.source").withStyle(ChatFormatting.DARK_AQUA));
        tooltipAdder.accept(Component.translatable("item.deadrecall.copper_wrench.tooltip.targets").withStyle(ChatFormatting.DARK_AQUA));
        tooltipAdder.accept(Component.translatable("item.deadrecall.copper_wrench.tooltip.slots").withStyle(ChatFormatting.DARK_GRAY));
    }
}
