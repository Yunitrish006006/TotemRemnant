package com.adaptor.deadrecall.item.copper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

final class CopperGolemFuelService {
    private static final int FUEL_TICKS_PER_TRANSPORT = 200;

    private CopperGolemFuelService() {
    }

    static boolean hasFuelAvailable(CopperGolem golem, ServerLevel level) {
        return getFuelTicks(golem) > 0 || isFuel(level, getFuelStack(golem));
    }

    static ItemStack getFuelStack(CopperGolem golem) {
        return CopperGolemData.fuelStack(golem);
    }

    static int getFuelTicks(CopperGolem golem) {
        return CopperGolemData.fuelTicks(golem);
    }

    static void setFuelStack(CopperGolem golem, ItemStack fuelStack) {
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        writeFuelStack(tag, fuelStack);
        CopperGolemData.removeSortingBlockedTags(tag);
        CopperGolemData.bumpRevision(tag);
        CopperGolemData.writeEntityTag(golem, tag);
    }

    static ItemStack readFuelStack(CompoundTag tag) {
        return CopperGolemData.readItemStack(tag, CopperGolemData.TAG_FUEL_STACK);
    }

    static void writeFuelStack(CompoundTag tag, ItemStack fuelStack) {
        CopperGolemData.writeItemStack(tag, CopperGolemData.TAG_FUEL_STACK, fuelStack);
    }

    static boolean consumeForTransport(CopperGolem golem, ServerLevel level) {
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        if (!consumeForTransport(tag, level)) {
            return false;
        }

        CopperGolemData.writeEntityTag(golem, tag);
        return true;
    }

    static boolean consumeForTransport(CompoundTag tag, ServerLevel level) {
        int fuelTicks = tag.getIntOr(CopperGolemData.TAG_FUEL_TICKS, 0);
        if (fuelTicks <= 0) {
            ItemStack fuelStack = readFuelStack(tag);
            if (!isFuel(level, fuelStack)) {
                return false;
            }

            fuelTicks = Math.max(1, level.fuelValues().burnDuration(fuelStack));
            writeFuelStack(tag, consumeOneFuelItem(fuelStack));
        }

        fuelTicks = Math.max(0, fuelTicks - FUEL_TICKS_PER_TRANSPORT);
        if (fuelTicks > 0) {
            tag.putInt(CopperGolemData.TAG_FUEL_TICKS, fuelTicks);
        } else {
            tag.remove(CopperGolemData.TAG_FUEL_TICKS);
        }
        CopperGolemData.removeSortingBlockedTags(tag);
        return true;
    }

    static boolean isFuel(ServerLevel level, ItemStack stack) {
        return !stack.isEmpty() && level.fuelValues().isFuel(stack);
    }

    private static ItemStack consumeOneFuelItem(ItemStack fuelStack) {
        Item item = fuelStack.getItem();
        fuelStack.shrink(1);
        if (!fuelStack.isEmpty()) {
            return fuelStack;
        }

        var craftingRemainder = item.getCraftingRemainder();
        return craftingRemainder == null ? ItemStack.EMPTY : craftingRemainder.create();
    }
}
