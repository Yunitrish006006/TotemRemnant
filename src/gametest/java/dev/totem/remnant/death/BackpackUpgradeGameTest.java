package dev.totem.remnant.death;

import dev.totem.remnant.item.BackpackItemHelper;
import dev.totem.remnant.registry.RemnantItemRegistration;
import dev.totem.remnant.upgrade.BackpackUpgradeData;
import dev.totem.remnant.upgrade.BackpackUpgradeType;
import dev.totem.remnant.upgrade.BackpackCapacity;
import dev.totem.remnant.upgrade.BackpackCompaction;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import dev.totem.remnant.inventory.DeathBackpackInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;

import java.util.ArrayList;
import java.util.List;

/** Covers persistence and all server-authoritative first-wave backpack modules. */
public final class BackpackUpgradeGameTest {
    @GameTest(maxTicks = 20)
    public void backpackRecipesRejectBundlesThatStillContainItems(GameTestHelper helper) {
        ItemStack emptyBundle = new ItemStack(Items.BUNDLE);
        ItemStack filledBundle = new ItemStack(Items.BUNDLE);
        filledBundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(
                ItemStackTemplate.fromNonEmptyStack(new ItemStack(Items.DIAMOND)))));

        SmithingRecipeInput validSmithing = new SmithingRecipeInput(
                emptyBundle.copy(), emptyBundle.copy(), new ItemStack(Items.LEATHER));
        ItemStack validBackpack = helper.getLevel().recipeAccess()
                .getRecipeFor(RecipeType.SMITHING, validSmithing, helper.getLevel())
                .map(holder -> holder.value().assemble(validSmithing)).orElse(ItemStack.EMPTY);
        require(helper, validBackpack.is(RemnantItemRegistration.BACKPACK_BASIC),
                "Empty Bundles no longer make the Basic Backpack");

        SmithingRecipeInput unsafeSmithing = new SmithingRecipeInput(
                filledBundle.copy(), emptyBundle.copy(), new ItemStack(Items.LEATHER));
        require(helper, helper.getLevel().recipeAccess()
                        .getRecipeFor(RecipeType.SMITHING, unsafeSmithing, helper.getLevel()).isEmpty(),
                "Smithing accepted a Bundle that still contained an item");

        List<ItemStack> capacityGrid = List.of(
                new ItemStack(Items.LEATHER), emptyBundle.copy(), new ItemStack(Items.LEATHER),
                new ItemStack(Items.IRON_INGOT), new ItemStack(Items.CHEST), new ItemStack(Items.IRON_INGOT),
                new ItemStack(Items.LEATHER), filledBundle.copy(), new ItemStack(Items.LEATHER));
        CraftingInput unsafeCapacity = CraftingInput.of(3, 3, capacityGrid);
        boolean matchesCapacity = helper.getLevel().recipeAccess()
                .getRecipeFor(RecipeType.CRAFTING, unsafeCapacity, helper.getLevel())
                .map(holder -> holder.value().assemble(unsafeCapacity)
                        .is(RemnantItemRegistration.UPGRADE_CAPACITY)).orElse(false);
        require(helper, !matchesCapacity,
                "Capacity-module recipe consumed a Bundle that still contained an item");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void netheriteIsIntrinsicallyFireproofAndModuleProtectsLowerTiers(GameTestHelper helper) {
        BlockPos netheritePos = helper.absolutePos(new BlockPos(1, 1, 1));
        ItemEntity netheriteEntity = new ItemEntity(helper.getLevel(),
                netheritePos.getX() + 0.5, netheritePos.getY(), netheritePos.getZ() + 0.5,
                new ItemStack(RemnantItemRegistration.BACKPACK_NETHERITE));
        require(helper, !netheriteEntity.hurtServer(
                        helper.getLevel(), helper.getLevel().damageSources().lava(), 10.0F),
                "Netherite backpack did not reject real ItemEntity lava damage");
        require(helper, netheriteEntity.isAlive(),
                "Netherite backpack lost its intrinsic fire protection");

        BlockPos basicPos = helper.absolutePos(new BlockPos(2, 1, 1));
        ItemEntity unprotectedBasic = new ItemEntity(helper.getLevel(),
                basicPos.getX() + 0.5, basicPos.getY(), basicPos.getZ() + 0.5,
                new ItemStack(RemnantItemRegistration.BACKPACK_BASIC));
        require(helper, unprotectedBasic.hurtServer(
                        helper.getLevel(), helper.getLevel().damageSources().lava(), 10.0F),
                "Basic backpack without a module rejected lava damage");
        require(helper, !unprotectedBasic.isAlive(),
                "Lava did not destroy the unprotected Basic Backpack");

        ItemStack protectedBackpack = new ItemStack(RemnantItemRegistration.BACKPACK_BASIC);
        install(protectedBackpack, RemnantItemRegistration.UPGRADE_FIRE_PROTECTION);
        ItemEntity protectedBasic = new ItemEntity(helper.getLevel(),
                basicPos.getX() + 0.5, basicPos.getY(), basicPos.getZ() + 0.5,
                protectedBackpack);
        require(helper, !protectedBasic.hurtServer(
                        helper.getLevel(), helper.getLevel().damageSources().lava(), 10.0F),
                "Fire-protection module did not cancel real ItemEntity lava damage");
        require(helper, protectedBasic.isAlive(),
                "Fire-protection module failed to keep the Basic Backpack alive");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void nonFireDroppedItemProtectionIsModuleDriven(GameTestHelper helper) {
        ItemStack unmodifiedNetherite = new ItemStack(RemnantItemRegistration.BACKPACK_NETHERITE);
        require(helper, unmodifiedNetherite.has(DataComponents.DAMAGE_RESISTANT),
                "Netherite backpack lacks its intrinsic fire-resistant item component");
        require(helper, !BackpackItemHelper.shouldProtectDroppedBackpackFromDamage(
                        unmodifiedNetherite, helper.getLevel().damageSources().cactus()),
                "Netherite backpack still has implicit cactus protection");
        require(helper, !BackpackItemHelper.shouldProtectDroppedBackpackFromDamage(
                        unmodifiedNetherite, helper.getLevel().damageSources().explosion(null, null)),
                "Netherite backpack still has implicit blast protection");
        require(helper, BackpackItemHelper.shouldProtectDroppedBackpackFromDamage(
                        unmodifiedNetherite, helper.getLevel().damageSources().lava()),
                "Netherite backpack lacks intrinsic fire protection");
        require(helper, !BackpackItemHelper.shouldPreventDroppedBackpackDespawn(unmodifiedNetherite),
                "Netherite backpack still has implicit despawn protection");

        ItemStack modularNetherite = new ItemStack(RemnantItemRegistration.BACKPACK_NETHERITE);
        install(modularNetherite,
                RemnantItemRegistration.UPGRADE_BLAST_PROTECTION,
                RemnantItemRegistration.UPGRADE_DESPAWN_PROTECTION,
                RemnantItemRegistration.UPGRADE_CRAFTING,
                RemnantItemRegistration.UPGRADE_COMPACTION);
        require(helper, BackpackItemHelper.shouldProtectDroppedBackpackFromDamage(
                        modularNetherite, helper.getLevel().damageSources().cactus()),
                "Combined impact-protection module did not protect against cactus damage");
        require(helper, BackpackItemHelper.shouldProtectDroppedBackpackFromDamage(
                        modularNetherite, helper.getLevel().damageSources().explosion(null, null)),
                "Combined impact-protection module did not protect against explosion damage");
        require(helper, BackpackItemHelper.shouldPreventDroppedBackpackDespawn(modularNetherite),
                "Despawn-protection module did not protect the dropped backpack");

        ItemStack voidProtected = new ItemStack(RemnantItemRegistration.BACKPACK_BASIC);
        install(voidProtected, RemnantItemRegistration.UPGRADE_VOID_PROTECTION);
        ItemEntity voidProtectedEntity = new ItemEntity(helper.getLevel(), 0.5,
                helper.getLevel().getMinY() - 65.0, 0.5, voidProtected);
        require(helper, BackpackItemHelper.shouldApplyBackpackVoidMomentum(voidProtectedEntity),
                "Void-protection module did not activate below the world's discard boundary");
        BackpackItemHelper.applyBackpackVoidMomentum(voidProtectedEntity);
        require(helper, voidProtectedEntity.getDeltaMovement().y > 0.0,
                "Void-protection module did not propel the backpack upward");
        require(helper, BackpackItemHelper.shouldProtectDroppedBackpackFromDamage(
                        voidProtected, helper.getLevel().damageSources().fellOutOfWorld()),
                "Void-protection module did not reject direct void damage");
        ItemEntity unprotectedVoidEntity = new ItemEntity(helper.getLevel(), 0.5,
                helper.getLevel().getMinY() - 65.0, 0.5,
                new ItemStack(RemnantItemRegistration.BACKPACK_NETHERITE));
        require(helper, !BackpackItemHelper.shouldApplyBackpackVoidMomentum(unprotectedVoidEntity),
                "Netherite backpack gained implicit void protection without a module");

        ItemStack legacyCactusModuleBackpack = new ItemStack(RemnantItemRegistration.BACKPACK_BASIC);
        install(legacyCactusModuleBackpack, RemnantItemRegistration.UPGRADE_CACTUS_PROTECTION);
        require(helper, BackpackItemHelper.shouldProtectDroppedBackpackFromDamage(
                        legacyCactusModuleBackpack, helper.getLevel().damageSources().cactus())
                        && BackpackItemHelper.shouldProtectDroppedBackpackFromDamage(
                        legacyCactusModuleBackpack,
                        helper.getLevel().damageSources().explosion(null, null)),
                "Legacy cactus module did not migrate to the combined protection behavior");

        ItemStack deathBackpack = new ItemStack(RemnantItemRegistration.DEATH_BACKPACK);
        require(helper, BackpackItemHelper.shouldProtectDroppedBackpackFromDamage(
                        deathBackpack, helper.getLevel().damageSources().lava()),
                "Death Backpack lost its system-level damage protection");
        require(helper, BackpackItemHelper.shouldPreventDroppedBackpackDespawn(deathBackpack),
                "Death Backpack lost its system-level despawn protection");
        ItemEntity deathBackpackEntity = new ItemEntity(helper.getLevel(), 0.5,
                helper.getLevel().getMinY() - 65.0, 0.5, deathBackpack);
        require(helper, BackpackItemHelper.shouldApplyBackpackVoidMomentum(deathBackpackEntity),
                "Death Backpack lost its system-level void rescue");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void modulesRoundTripOnBackpackData(GameTestHelper helper) {
        ItemStack backpack = new ItemStack(RemnantItemRegistration.BACKPACK_NETHERITE);
        install(backpack,
                RemnantItemRegistration.UPGRADE_CRAFTING,
                RemnantItemRegistration.UPGRADE_COMPACTION,
                RemnantItemRegistration.UPGRADE_MATCHING_PICKUP,
                RemnantItemRegistration.UPGRADE_SOULBOUND_CHARGE);

        require(helper, BackpackUpgradeData.has(backpack, BackpackUpgradeType.CRAFTING),
                "Crafting module did not persist");
        require(helper, BackpackUpgradeData.has(backpack, BackpackUpgradeType.COMPACTION),
                "Compaction module did not persist");
        require(helper, BackpackUpgradeData.has(backpack, BackpackUpgradeType.MATCHING_PICKUP),
                "Matching-pickup module did not persist");
        require(helper, BackpackUpgradeData.has(backpack, BackpackUpgradeType.SOULBOUND_CHARGE),
                "Soulbound charge did not persist");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 20)
    public void capacityModuleAddsOneRowAndCannotBeRemovedWhileOccupied(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            ItemStack backpack = new ItemStack(RemnantItemRegistration.BACKPACK_BASIC);
            install(backpack, RemnantItemRegistration.UPGRADE_CAPACITY);
            require(helper, BackpackCapacity.slots(backpack) == 18,
                    "Capacity module did not add exactly one nine-slot row");

            player.setItemInHand(InteractionHand.MAIN_HAND, backpack);
            backpack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            require(helper, player.containerMenu instanceof dev.totem.remnant.inventory.BackpackMenu,
                    "Capacity backpack did not open the extended backpack menu");
            dev.totem.remnant.inventory.BackpackMenu menu =
                    (dev.totem.remnant.inventory.BackpackMenu) player.containerMenu;
            require(helper, menu.getRowCount() == 2,
                    "Basic backpack with capacity module did not expose two rows");

            menu.slots.get(9).set(new ItemStack(Items.DIAMOND));
            int upgradeSlot = 18 + 36;
            require(helper, !menu.slots.get(upgradeSlot).mayPickup(player),
                    "Capacity module could be removed while its added row contained an item");
            menu.slots.get(9).set(ItemStack.EMPTY);
            require(helper, menu.slots.get(upgradeSlot).mayPickup(player),
                    "Capacity module remained locked after its added row was emptied");
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 20)
    public void fourCapacityModulesAddFourRowsAndOnlyFinalRowBlocksRemoval(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            ItemStack backpack = new ItemStack(RemnantItemRegistration.BACKPACK_NETHERITE);
            player.setItemInHand(InteractionHand.MAIN_HAND, backpack);
            dev.totem.remnant.inventory.BackpackUpgradeInventory upgrades =
                    new dev.totem.remnant.inventory.BackpackUpgradeInventory(
                            player, InteractionHand.MAIN_HAND, 4);
            for (int index = 0; index < 4; index++) {
                ItemStack capacity = new ItemStack(RemnantItemRegistration.UPGRADE_CAPACITY);
                require(helper, upgrades.canPlaceItem(index, capacity),
                        "Capacity module " + (index + 1) + " was incorrectly rejected");
                upgrades.setItem(index, capacity);
            }
            require(helper, BackpackUpgradeData.count(backpack, BackpackUpgradeType.CAPACITY) == 4,
                    "Four installed capacity modules did not persist");
            require(helper, BackpackCapacity.slots(backpack) == 72,
                    "Four capacity modules did not expand netherite backpack to 72 slots");

            backpack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            require(helper, player.containerMenu instanceof dev.totem.remnant.inventory.BackpackMenu,
                    "Four-module backpack did not open the extended backpack menu");
            dev.totem.remnant.inventory.BackpackMenu menu =
                    (dev.totem.remnant.inventory.BackpackMenu) player.containerMenu;
            require(helper, menu.getRowCount() == 8,
                    "Four-module netherite backpack did not expose eight rows");

            int upgradeSlot = 72 + 36;
            menu.slots.get(63).set(new ItemStack(Items.DIAMOND));
            require(helper, !menu.slots.get(upgradeSlot).mayPickup(player),
                    "Capacity module could be removed while the disappearing final row was occupied");
            menu.slots.get(63).set(ItemStack.EMPTY);
            menu.slots.get(54).set(new ItemStack(Items.EMERALD));
            require(helper, menu.slots.get(upgradeSlot).mayPickup(player),
                    "An occupied row that remains after removal incorrectly locked the capacity module");

            ItemStack removed = menu.slots.get(upgradeSlot).safeTake(1, 1, player);
            require(helper, removed.is(RemnantItemRegistration.UPGRADE_CAPACITY),
                    "First capacity module could not be removed after its final row was emptied");
            require(helper, !menu.slots.get(upgradeSlot + 1).mayPickup(player),
                    "Second capacity removal ignored items in its newly disappearing row");
            menu.slots.get(54).set(ItemStack.EMPTY);
            require(helper, menu.slots.get(upgradeSlot + 1).mayPickup(player),
                    "Second capacity module stayed locked after its disappearing row was emptied");

            menu.setCarried(new ItemStack(Items.DIAMOND));
            menu.clicked(63, 0, ContainerInput.PICKUP, player);
            require(helper, menu.getCarried().is(Items.DIAMOND) && !menu.slots.get(63).hasItem(),
                    "Removed capacity row still accepted cursor insertion");
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 20)
    public void backpackStoragePreservesIntentionalSlotPositions(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            ItemStack backpack = new ItemStack(RemnantItemRegistration.BACKPACK_NETHERITE);
            install(backpack, RemnantItemRegistration.UPGRADE_CAPACITY);
            List<ItemStack> contents = new ArrayList<>();
            for (int index = 0; index < 45; index++) contents.add(ItemStack.EMPTY);
            contents.set(10, new ItemStack(Items.EMERALD, 3));
            contents.set(44, new ItemStack(Items.DIAMOND, 2));
            backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
            player.setItemInHand(InteractionHand.MAIN_HAND, backpack);

            DeathBackpackInventory firstOpen = new DeathBackpackInventory(
                    player, InteractionHand.MAIN_HAND, BackpackCapacity.slots(backpack));
            require(helper, firstOpen.getItem(10).is(Items.EMERALD)
                            && firstOpen.getItem(44).is(Items.DIAMOND),
                    "Opening the backpack compacted items out of their saved slots");
            firstOpen.setChanged();
            DeathBackpackInventory secondOpen = new DeathBackpackInventory(
                    player, InteractionHand.MAIN_HAND, BackpackCapacity.slots(backpack));
            require(helper, secondOpen.getItem(10).is(Items.EMERALD)
                            && secondOpen.getItem(44).is(Items.DIAMOND),
                    "Saving and reopening the backpack changed item slot positions");
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    @GameTest(maxTicks = 20)
    public void fullBackpackCompactsAfterInputsFreeTheirOwnSlot(GameTestHelper helper) {
        ItemStack backpack = new ItemStack(RemnantItemRegistration.BACKPACK_BASIC);
        install(backpack, RemnantItemRegistration.UPGRADE_COMPACTION);
        List<ItemStack> contents = new ArrayList<>();
        contents.add(new ItemStack(Items.RAW_IRON, 9));
        for (int index = 1; index < 9; index++) contents.add(new ItemStack(Items.COBBLESTONE, 64));
        backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));

        require(helper, BackpackCompaction.compactIfEnabled(helper.getLevel(), backpack),
                "Full backpack refused compaction even though consuming inputs frees a slot");
        List<ItemStack> compacted = backpack.getOrDefault(
                DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItemCopyStream().toList();
        require(helper, count(compacted, Items.RAW_IRON_BLOCK) == 1
                        && count(compacted, Items.RAW_IRON) == 0,
                "Full-backpack compaction produced an incorrect result");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 20)
    public void matchingPickupUsesExistingVariantThenCompactsWithLiveRecipe(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            ItemStack backpack = new ItemStack(RemnantItemRegistration.BACKPACK_ADVANCED);
            install(backpack,
                    RemnantItemRegistration.UPGRADE_MATCHING_PICKUP,
                    RemnantItemRegistration.UPGRADE_COMPACTION);
            backpack.set(DataComponents.CONTAINER,
                    ItemContainerContents.fromItems(List.of(new ItemStack(Items.RAW_IRON, 8))));
            player.getInventory().setItem(0, backpack);

            ItemStack incoming = new ItemStack(Items.RAW_IRON, 10);
            require(helper, player.getInventory().add(incoming),
                    "Vanilla inventory add did not accept the incoming raw iron");
            require(helper, incoming.isEmpty(), "Matching pickup left accepted items outside the backpack");

            List<ItemStack> contents = backpack.getOrDefault(
                    DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItemCopyStream().toList();
            require(helper, count(contents, Items.RAW_IRON_BLOCK) == 2,
                    "Live 3x3 raw-iron recipe did not produce two raw-iron blocks");
            require(helper, count(contents, Items.RAW_IRON) == 0,
                    "Compaction left raw iron after two complete recipes");
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 20)
    public void installedCraftingModuleEmbedsPortableThreeByThreeGrid(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            ItemStack backpack = new ItemStack(RemnantItemRegistration.BACKPACK_BASIC);
            install(backpack, RemnantItemRegistration.UPGRADE_CRAFTING);
            player.setItemInHand(InteractionHand.MAIN_HAND, backpack);
            backpack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

            require(helper, player.containerMenu instanceof dev.totem.remnant.inventory.BackpackMenu,
                    "Using the backpack did not open the extended backpack menu");
            dev.totem.remnant.inventory.BackpackMenu menu =
                    (dev.totem.remnant.inventory.BackpackMenu) player.containerMenu;
            require(helper, menu.craftingInputSlots().size() == 9,
                    "Backpack menu did not expose a 3x3 crafting grid");
            menu.craftingInputSlots().get(0).set(new ItemStack(Items.OAK_PLANKS));
            menu.craftingInputSlots().get(1).set(new ItemStack(Items.OAK_PLANKS));
            menu.craftingInputSlots().get(3).set(new ItemStack(Items.OAK_PLANKS));
            menu.craftingInputSlots().get(4).set(new ItemStack(Items.OAK_PLANKS));
            require(helper, menu.craftingResultSlot().getItem().is(Items.CRAFTING_TABLE),
                    "Embedded 3x3 grid did not resolve the live crafting-table recipe");
            require(helper, player.containerMenu == menu,
                    "Embedded crafting unexpectedly replaced the backpack menu");
            ItemStack crafted = menu.craftingResultSlot().safeTake(1, 1, player);
            require(helper, crafted.is(Items.CRAFTING_TABLE),
                    "Embedded crafting result could not be taken");
            require(helper, menu.craftingInputSlots().stream().noneMatch(slot -> slot.hasItem()),
                    "Taking the embedded crafting result did not consume its ingredients");

            menu.craftingInputSlots().getFirst().set(new ItemStack(Items.OAK_PLANKS, 3));
            menu.removed(player);
            require(helper, countPlayerInventory(player, Items.OAK_PLANKS) == 3,
                    "Closing the backpack did not return unfinished crafting ingredients");
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 20)
    public void openBackpackViewStaysSynchronizedAfterCompaction(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            ItemStack backpack = new ItemStack(RemnantItemRegistration.BACKPACK_BASIC);
            install(backpack, RemnantItemRegistration.UPGRADE_COMPACTION);
            player.setItemInHand(InteractionHand.MAIN_HAND, backpack);
            DeathBackpackInventory storage = new DeathBackpackInventory(
                    player, InteractionHand.MAIN_HAND, 9);

            storage.setItem(0, new ItemStack(Items.RAW_GOLD, 9));

            require(helper, storage.getItem(0).is(Items.RAW_GOLD_BLOCK)
                            && storage.getItem(0).getCount() == 1,
                    "Open backpack view kept stale raw gold after component compaction");
            require(helper, backpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                            .nonEmptyItemCopyStream().anyMatch(stack -> stack.is(Items.RAW_GOLD_BLOCK)),
                    "Compacted open backpack did not persist its raw-gold block");
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 40)
    public void soulboundChargeRetainsBackpackOnceAndConsumesIt(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            ItemStack backpack = new ItemStack(RemnantItemRegistration.BACKPACK_BASIC);
            install(backpack, RemnantItemRegistration.UPGRADE_SOULBOUND_CHARGE);
            backpack.set(DataComponents.CONTAINER,
                    ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 5))));
            player.getInventory().setItem(0, backpack);
            player.getInventory().setItem(1, new ItemStack(Items.APPLE, 2));

            require(helper, DeathBackpackCaptureService.captureBeforeVanillaDrop(player, helper.getLevel()),
                    "Death capture did not commit around a charged backpack");
            require(helper, player.getInventory().isEmpty(),
                    "Charged backpack was not staged separately from death capture");
            require(helper, SoulboundDeathItemRetention.restoreAfterRespawn(player),
                    "Charged backpack was not restored");

            ItemStack restored = player.getInventory().getItem(0);
            require(helper, restored.is(RemnantItemRegistration.BACKPACK_BASIC),
                    "Restored item was not the protected backpack");
            require(helper, !BackpackUpgradeData.has(restored, BackpackUpgradeType.SOULBOUND_CHARGE),
                    "Single-use soulbound module was not consumed");
            require(helper, restored.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                            .nonEmptyItemCopyStream().anyMatch(stack -> stack.is(Items.DIAMOND)
                                    && stack.getCount() == 5),
                    "Protected backpack contents were not retained");
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    private static void install(ItemStack backpack, net.minecraft.world.item.Item... modules) {
        int capacity = ((dev.totem.remnant.item.TieredBackpackItem) backpack.getItem())
                .tier().upgradeSlots();
        List<ItemStack> installed = new ArrayList<>(capacity);
        for (net.minecraft.world.item.Item module : modules) installed.add(new ItemStack(module));
        while (installed.size() < capacity) installed.add(ItemStack.EMPTY);
        BackpackUpgradeData.write(backpack, installed, capacity);
    }

    private static int count(List<ItemStack> stacks, net.minecraft.world.item.Item item) {
        return stacks.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private static int countPlayerInventory(ServerPlayer player, net.minecraft.world.item.Item item) {
        int total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
