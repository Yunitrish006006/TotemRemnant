package dev.totem.remnant.gametest;

import dev.totem.remnant.inventory.PortableContainerPolicy;
import dev.totem.remnant.registry.RemnantItemRegistration;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;

import java.util.List;

public final class BackpackItemIdMigrationGameTest {
    private static final Component CUSTOM_NAME = Component.literal("legacy migration");
    private static final DyedItemColor COLOR = new DyedItemColor(0x2D6F9F);
    private static final CustomData CUSTOM_DATA = customData();

    @GameTest(maxTicks = 20)
    public void canonicalAndLegacyIdsAreBothRegistered(GameTestHelper helper) {
        for (Mapping mapping : mappings()) {
            require(helper, BuiltInRegistries.ITEM.getKey(mapping.canonical()).toString().equals(mapping.canonicalId()),
                    "Canonical item registered under the wrong ID: " + mapping.canonicalId());
            require(helper, BuiltInRegistries.ITEM.getKey(mapping.legacy()).toString().equals(mapping.legacyId()),
                    "Legacy item ID is no longer registered: " + mapping.legacyId());
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void migrationPreservesEveryBackpackComponent(GameTestHelper helper) {
        for (Mapping mapping : mappings()) {
            ItemStack legacy = populated(mapping.legacy());
            ItemStack migrated = RemnantItemRegistration.migrateLegacy(legacy);

            require(helper, migrated.is(mapping.canonical()),
                    "Legacy item did not migrate to " + mapping.canonicalId());
            require(helper, migrated.getCount() == legacy.getCount(), "Migration changed stack count");
            requirePreservedComponents(helper, migrated, "Migration");
            require(helper, legacy.is(mapping.legacy()), "Migration mutated the original stack in place");
            require(helper, RemnantItemRegistration.migrateLegacy(migrated) == migrated,
                    "Canonical item was unnecessarily copied");
        }
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 20)
    public void usingLegacyBackpacksMigratesTheHeldStackBeforeOpening(GameTestHelper helper) {
        for (Mapping mapping : mappings()) {
            ServerPlayer player = helper.makeMockServerPlayerInLevel();
            ItemStack legacy = populated(mapping.legacy());
            player.setItemInHand(InteractionHand.MAIN_HAND, legacy);
            try {
                legacy.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                ItemStack migrated = player.getMainHandItem();
                require(helper, migrated.is(mapping.canonical()),
                        "Using legacy backpack did not migrate " + mapping.legacyId());
                requirePreservedComponents(helper, migrated, "Use migration");
                player.closeContainer();
            } finally {
                player.discard();
            }
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void dyeingLegacyTierBackpackMigratesItToCanonicalId(GameTestHelper helper) {
        ItemStack legacy = populated(RemnantItemRegistration.LEGACY_BACKPACK_ADVANCED);
        CraftingInput input = CraftingInput.of(2, 1, List.of(
                legacy,
                new ItemStack(Items.DYE.pick(DyeColor.RED))
        ));
        RecipeHolder<CraftingRecipe> recipe = helper.getLevel().recipeAccess()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .orElseThrow(() -> helper.assertionException("Legacy backpack dye recipe is missing"));
        ItemStack result = recipe.value().assemble(input);

        require(helper, result.is(RemnantItemRegistration.BACKPACK_ADVANCED),
                "Legacy dye recipe did not output the canonical advanced backpack");
        require(helper, result.has(DataComponents.DYED_COLOR), "Legacy dye recipe did not apply color");
        require(helper, contents().equals(result.get(DataComponents.CONTAINER)),
                "Legacy dye migration changed stored contents");
        require(helper, CUSTOM_NAME.equals(result.get(DataComponents.CUSTOM_NAME)),
                "Legacy dye migration changed custom name");
        require(helper, CUSTOM_DATA.equals(result.get(DataComponents.CUSTOM_DATA)),
                "Legacy dye migration changed addon data");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void upgradeRecipeAcceptsBothIdGenerationsAndProducesCanonicalTier(GameTestHelper helper) {
        for (Item baseItem : List.of(
                RemnantItemRegistration.BACKPACK_BASIC,
                RemnantItemRegistration.LEGACY_BACKPACK_BASIC
        )) {
            ItemStack base = populated(baseItem);
            SmithingRecipeInput input = new SmithingRecipeInput(
                    new ItemStack(Items.BUNDLE),
                    base,
                    new ItemStack(Items.IRON_INGOT)
            );
            RecipeHolder<SmithingRecipe> recipe = helper.getLevel().recipeAccess()
                    .getRecipeFor(RecipeType.SMITHING, input, helper.getLevel())
                    .orElseThrow(() -> helper.assertionException(
                            "Standard upgrade recipe rejected " + BuiltInRegistries.ITEM.getKey(baseItem)));
            ItemStack result = recipe.value().assemble(input);

            require(helper, result.is(RemnantItemRegistration.BACKPACK_STANDARD),
                    "Standard upgrade did not output the canonical tier");
            requirePreservedComponents(helper, result, "Smithing upgrade");
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void canonicalAndLegacyBackpacksRemainPortableAndWashable(GameTestHelper helper) {
        for (Mapping mapping : mappings()) {
            ItemStack canonical = new ItemStack(mapping.canonical());
            ItemStack legacy = new ItemStack(mapping.legacy());
            require(helper, canonical.is(PortableContainerPolicy.PORTABLE_CONTAINERS),
                    "Canonical backpack is missing portable container tag: " + mapping.canonicalId());
            require(helper, legacy.is(PortableContainerPolicy.PORTABLE_CONTAINERS),
                    "Legacy backpack is missing portable container tag: " + mapping.legacyId());

            boolean dyeable = mapping.canonical() != RemnantItemRegistration.DEATH_BACKPACK;
            require(helper, canonical.is(ItemTags.CAULDRON_CAN_REMOVE_DYE) == dyeable,
                    "Canonical backpack has wrong cauldron dye tag state: " + mapping.canonicalId());
            require(helper, legacy.is(ItemTags.CAULDRON_CAN_REMOVE_DYE) == dyeable,
                    "Legacy backpack has wrong cauldron dye tag state: " + mapping.legacyId());
        }
        helper.succeed();
    }

    private static ItemStack populated(Item item) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CONTAINER, contents());
        stack.set(DataComponents.CUSTOM_NAME, CUSTOM_NAME);
        stack.set(DataComponents.CUSTOM_DATA, CUSTOM_DATA);
        if (item != RemnantItemRegistration.DEATH_BACKPACK
                && item != RemnantItemRegistration.LEGACY_DEATH_BACKPACK) {
            stack.set(DataComponents.DYED_COLOR, COLOR);
        }
        return stack;
    }

    private static void requirePreservedComponents(GameTestHelper helper, ItemStack stack, String operation) {
        require(helper, contents().equals(stack.get(DataComponents.CONTAINER)),
                operation + " changed stored contents");
        require(helper, CUSTOM_NAME.equals(stack.get(DataComponents.CUSTOM_NAME)),
                operation + " changed custom name");
        require(helper, CUSTOM_DATA.equals(stack.get(DataComponents.CUSTOM_DATA)),
                operation + " changed addon data");
        if (!stack.is(RemnantItemRegistration.DEATH_BACKPACK)) {
            require(helper, COLOR.equals(stack.get(DataComponents.DYED_COLOR)),
                    operation + " changed dyed color");
        }
    }

    private static CustomData customData() {
        CompoundTag tag = new CompoundTag();
        tag.putString("migration_owner", "totem-remnant");
        tag.putInt("migration_revision", 1);
        return CustomData.of(tag);
    }

    private static ItemContainerContents contents() {
        return ItemContainerContents.fromItems(
                List.of(new ItemStack(Items.DIAMOND, 4), new ItemStack(Items.TORCH, 12)));
    }

    private static List<Mapping> mappings() {
        return List.of(
                new Mapping(RemnantItemRegistration.BACKPACK_BASIC,
                        RemnantItemRegistration.LEGACY_BACKPACK_BASIC,
                        "totem:remnant/backpack_basic", "deadrecall:backpack_basic"),
                new Mapping(RemnantItemRegistration.BACKPACK_STANDARD,
                        RemnantItemRegistration.LEGACY_BACKPACK_STANDARD,
                        "totem:remnant/backpack_standard", "deadrecall:backpack_standard"),
                new Mapping(RemnantItemRegistration.BACKPACK_ADVANCED,
                        RemnantItemRegistration.LEGACY_BACKPACK_ADVANCED,
                        "totem:remnant/backpack_advanced", "deadrecall:backpack_advanced"),
                new Mapping(RemnantItemRegistration.BACKPACK_NETHERITE,
                        RemnantItemRegistration.LEGACY_BACKPACK_NETHERITE,
                        "totem:remnant/backpack_netherite", "deadrecall:backpack_netherite"),
                new Mapping(RemnantItemRegistration.DEATH_BACKPACK,
                        RemnantItemRegistration.LEGACY_DEATH_BACKPACK,
                        "totem:remnant/death_backpack", "deadrecall:death_backpack")
        );
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }

    private record Mapping(Item canonical, Item legacy, String canonicalId, String legacyId) {
    }
}
