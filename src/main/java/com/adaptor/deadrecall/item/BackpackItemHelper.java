package com.adaptor.deadrecall.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class BackpackItemHelper {
    private static final double DROP_BASE_SPEED = 0.35;
    private static final double DROP_RANDOM_SPEED = 0.16;
    private static final double DROP_UPWARD_SPEED = 0.22;
    private static final double MIN_DIRECTION_LENGTH_SQUARED = 1.0E-6;

    private BackpackItemHelper() {
    }

    public static boolean isBackpackItem(ItemStack stack) {
        return stack.getItem() instanceof TieredBackpackItem || stack.getItem() instanceof DeathBackpackItem;
    }

    public static boolean dropStoredItems(ServerLevel level, Vec3 origin, ItemStack backpackStack, Vec3 direction) {
        if (!isBackpackItem(backpackStack)) {
            return false;
        }

        ItemContainerContents contents = backpackStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        List<ItemStack> drops = contents.nonEmptyItemCopyStream()
                .filter(stack -> !stack.isEmpty())
                .toList();
        if (drops.isEmpty()) {
            return false;
        }

        RandomSource random = level.getRandom();
        Vec3 ejectionDirection = normalizeHorizontalOrRandom(direction, random);
        for (ItemStack drop : drops) {
            ItemEntity itemEntity = new ItemEntity(level, origin.x, origin.y + 0.1, origin.z, drop.copy());
            itemEntity.setDeltaMovement(createDropVelocity(ejectionDirection, random));
            itemEntity.setDefaultPickUpDelay();
            level.addFreshEntity(itemEntity);
        }
        return true;
    }

    private static Vec3 createDropVelocity(Vec3 direction, RandomSource random) {
        double speed = DROP_BASE_SPEED + random.nextDouble() * DROP_RANDOM_SPEED;
        double spreadX = (random.nextDouble() - 0.5) * DROP_RANDOM_SPEED;
        double spreadZ = (random.nextDouble() - 0.5) * DROP_RANDOM_SPEED;
        double upward = DROP_UPWARD_SPEED + random.nextDouble() * DROP_RANDOM_SPEED;
        return direction.scale(speed).add(spreadX, upward, spreadZ);
    }

    private static Vec3 normalizeHorizontalOrRandom(Vec3 direction, RandomSource random) {
        Vec3 horizontal = direction == null ? Vec3.ZERO : direction.horizontal();
        if (horizontal.lengthSqr() < MIN_DIRECTION_LENGTH_SQUARED) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            return new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
        }
        return horizontal.normalize();
    }
}
