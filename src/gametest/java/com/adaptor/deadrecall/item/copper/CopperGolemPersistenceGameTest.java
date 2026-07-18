package com.adaptor.deadrecall.item.copper;

import com.adaptor.deadrecall.item.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public final class CopperGolemPersistenceGameTest {
    private static final BlockPos GOLEM_POS = new BlockPos(2, 2, 2);
    private static final Component STORAGE_NAME = Component.literal("Persistent copper gathering storage");

    @GameTest(maxTicks = 40)
    public void entityNbtRoundTripPreservesGatheringStateAndComponents(GameTestHelper helper) {
        CopperGolem original = constructGolem(helper, GOLEM_POS, true);
        CopperGolem replacement = null;
        try {
            CopperGolemData.migrate(original);
            invokeWrench("setTransportEnabled", new Class<?>[]{CopperGolem.class, boolean.class}, original, false);
            invokeWrench("setMode", new Class<?>[]{CopperGolem.class, CopperGolemMode.class}, original, CopperGolemMode.GATHERING);

            ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
            tool.setDamageValue(7);
            CopperGolemWrenchHandler.setGatheringToolStackFromMenu(original, tool);

            ItemStack storage = new ItemStack(Items.COBBLESTONE, 12);
            storage.set(DataComponents.CUSTOM_NAME, STORAGE_NAME);
            CopperGolemWrenchHandler.setGatheringStorageStackFromMenu(original, storage);

            CompoundTag customData = CopperGolemData.readEntityTag(original);
            customData.putString("deadrecall_gathering_area_dim", Level.OVERWORLD.identifier().toString());
            customData.putInt("deadrecall_gathering_corner_a_x", 1);
            customData.putInt("deadrecall_gathering_corner_a_y", 2);
            customData.putInt("deadrecall_gathering_corner_a_z", 3);
            customData.putInt("deadrecall_gathering_corner_b_x", 8);
            customData.putInt("deadrecall_gathering_corner_b_y", 9);
            customData.putInt("deadrecall_gathering_corner_b_z", 10);
            CopperGolemData.writeStringList(
                    customData,
                    "deadrecall_gathering_manual_targets",
                    List.of("minecraft:stone", "minecraft:oak_log"),
                    64
            );
            CopperGolemData.writeEntityTag(original, customData);
            int expectedRevision = CopperGolemData.revision(original);

            TagValueOutput output = TagValueOutput.createWithContext(
                    ProblemReporter.DISCARDING,
                    helper.getLevel().registryAccess()
            );
            original.saveWithoutId(output);
            CompoundTag serialized = output.buildResult();
            original.discard();

            replacement = constructGolem(helper, GOLEM_POS.offset(1, 0, 0), false);
            replacement.load(TagValueInput.create(
                    ProblemReporter.DISCARDING,
                    helper.getLevel().registryAccess(),
                    serialized
            ));
            require(helper, helper.getLevel().addFreshEntity(replacement),
                    "Could not add the reloaded copper golem entity");

            require(helper, CopperGolemWrenchHandler.getMode(replacement) == CopperGolemMode.GATHERING,
                    "Entity NBT reload lost gathering mode");
            require(helper, !CopperGolemWrenchHandler.isTransportEnabled(replacement),
                    "Entity NBT reload changed the stopped state");
            require(helper, CopperGolemData.revision(replacement) == expectedRevision,
                    "Entity NBT reload changed the revision");

            ItemStack loadedTool = CopperGolemWrenchHandler.getGatheringToolStackForMenu(replacement);
            require(helper, loadedTool.is(Items.IRON_PICKAXE) && loadedTool.getDamageValue() == 7,
                    "Entity NBT reload lost gathering tool durability");

            ItemStack loadedStorage = CopperGolemWrenchHandler.getGatheringStorageStackForMenu(replacement);
            require(helper, loadedStorage.is(Items.COBBLESTONE) && loadedStorage.getCount() == 12,
                    "Entity NBT reload lost gathering storage quantity");
            require(helper, STORAGE_NAME.equals(loadedStorage.get(DataComponents.CUSTOM_NAME)),
                    "Entity NBT reload lost gathering storage components");

            CompoundTag loadedData = CopperGolemData.readEntityTag(replacement);
            require(helper, Level.OVERWORLD.identifier().toString().equals(
                            loadedData.getStringOr("deadrecall_gathering_area_dim", "")),
                    "Entity NBT reload lost gathering area dimension");
            require(helper, loadedData.getIntOr("deadrecall_gathering_corner_a_x", Integer.MIN_VALUE) == 1
                            && loadedData.getIntOr("deadrecall_gathering_corner_b_z", Integer.MIN_VALUE) == 10,
                    "Entity NBT reload lost gathering area corners");
            require(helper, CopperGolemData.readStringList(loadedData, "deadrecall_gathering_manual_targets")
                            .equals(List.of("minecraft:stone", "minecraft:oak_log")),
                    "Entity NBT reload lost manual gathering targets");
            helper.succeed();
        } finally {
            original.discard();
            if (replacement != null) {
                replacement.discard();
            }
        }
    }

    @GameTest(maxTicks = 30)
    public void multiplayerMenuAuthorityUsesEachPlayersOwnBoundWrench(GameTestHelper helper) {
        CopperGolem golem = constructGolem(helper, GOLEM_POS, true);
        ServerPlayer first = createPlayerNear(helper, GOLEM_POS.offset(1, 0, 0));
        ServerPlayer second = createPlayerNear(helper, GOLEM_POS.offset(-1, 0, 0));
        try {
            ItemStack firstWrench = new ItemStack(ModItems.COPPER_WRENCH);
            ItemStack secondWrench = new ItemStack(ModItems.COPPER_WRENCH);
            first.setItemInHand(InteractionHand.MAIN_HAND, firstWrench);
            second.setItemInHand(InteractionHand.MAIN_HAND, secondWrench);

            setSelectedGolem(firstWrench, golem.getUUID());
            require(helper, CopperGolemWrenchHandler.canUseMenu(first, golem.getUUID(), golem),
                    "First player's correctly bound wrench was rejected");
            require(helper, !CopperGolemWrenchHandler.canUseMenu(second, golem.getUUID(), golem),
                    "Second player's unbound wrench inherited the first player's authority");

            setSelectedGolem(secondWrench, golem.getUUID());
            require(helper, CopperGolemWrenchHandler.canUseMenu(first, golem.getUUID(), golem)
                            && CopperGolemWrenchHandler.canUseMenu(second, golem.getUUID(), golem),
                    "Two independently bound players could not manage the same golem");

            setSelectedGolem(secondWrench, UUID.randomUUID());
            require(helper, CopperGolemWrenchHandler.canUseMenu(first, golem.getUUID(), golem),
                    "Rebinding the second player changed the first player's wrench authority");
            require(helper, !CopperGolemWrenchHandler.canUseMenu(second, golem.getUUID(), golem),
                    "Rebound second-player wrench retained stale authority");
            helper.succeed();
        } finally {
            first.discard();
            second.discard();
            golem.discard();
        }
    }

    private static CopperGolem constructGolem(
            GameTestHelper helper,
            BlockPos relativePos,
            boolean addToLevel
    ) {
        Object entityType = BuiltInRegistries.ENTITY_TYPE.getValue(
                Identifier.fromNamespaceAndPath("minecraft", "copper_golem"));
        if (entityType == null) {
            throw helper.assertionException("Missing minecraft:copper_golem entity type");
        }

        try {
            for (Constructor<?> constructor : CopperGolem.class.getDeclaredConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length != 2
                        || !parameterTypes[0].isInstance(entityType)
                        || !parameterTypes[1].isInstance(helper.getLevel())) {
                    continue;
                }

                constructor.setAccessible(true);
                CopperGolem golem = (CopperGolem) constructor.newInstance(entityType, helper.getLevel());
                BlockPos absolutePos = helper.absolutePos(relativePos);
                golem.snapTo(
                        absolutePos.getX() + 0.5D,
                        absolutePos.getY(),
                        absolutePos.getZ() + 0.5D,
                        0.0F,
                        0.0F
                );
                if (addToLevel) {
                    require(helper, helper.getLevel().addFreshEntity(golem),
                            "Could not add copper golem to the GameTest level");
                }
                return golem;
            }
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Could not construct copper golem fixture", exception);
        }
        throw helper.assertionException("No compatible CopperGolem constructor was found");
    }

    private static ServerPlayer createPlayerNear(GameTestHelper helper, BlockPos relativePos) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos absolutePos = helper.absolutePos(relativePos);
        player.snapTo(
                absolutePos.getX() + 0.5D,
                absolutePos.getY(),
                absolutePos.getZ() + 0.5D,
                0.0F,
                0.0F
        );
        return player;
    }

    private static void setSelectedGolem(ItemStack wrench, UUID golemId) {
        invokeWrench("setSelectedGolem", new Class<?>[]{ItemStack.class, UUID.class}, wrench, golemId);
    }

    @SuppressWarnings("unchecked")
    private static <T> T invokeWrench(String name, Class<?>[] parameterTypes, Object... arguments) {
        try {
            Method method = CopperGolemWrenchHandler.class.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return (T) method.invoke(null, arguments);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Could not invoke CopperGolemWrenchHandler#" + name, exception);
        }
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }
}
