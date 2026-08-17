package dev.totem.remnant.manual;

import dev.totem.remnant.network.RemnantManualRecipesPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/** Keeps manual recipe pictures synchronized with the server's live datapack recipes. */
public final class RemnantManualRecipeSync {
    public static final List<Identifier> SMITHING_RECIPE_IDS = List.of(
            Identifier.fromNamespaceAndPath("deadrecall", "backpack_basic"),
            Identifier.fromNamespaceAndPath("deadrecall", "backpack_standard_smithing"),
            Identifier.fromNamespaceAndPath("deadrecall", "backpack_advanced_smithing"),
            Identifier.fromNamespaceAndPath("deadrecall", "backpack_netherite_smithing")
    );
    public static final List<Identifier> MODULE_RECIPE_IDS = List.of(
            recipeId("upgrade_crafting"),
            recipeId("upgrade_compaction"),
            recipeId("upgrade_matching_pickup"),
            recipeId("upgrade_capacity"),
            recipeId("upgrade_soulbound_charge"),
            recipeId("upgrade_blast_protection"),
            recipeId("upgrade_fire_protection"),
            recipeId("upgrade_despawn_protection"),
            recipeId("upgrade_void_protection")
    );
    public static final List<Identifier> RECIPE_IDS = java.util.stream.Stream.concat(
            SMITHING_RECIPE_IDS.stream(),
            MODULE_RECIPE_IDS.stream()
    ).toList();

    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private RemnantManualRecipeSync() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        PayloadTypeRegistry.clientboundPlay().register(
                RemnantManualRecipesPayload.TYPE,
                RemnantManualRecipesPayload.CODEC
        );
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register(
                (player, joined) -> send(player)
        );
    }

    private static void send(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, RemnantManualRecipesPayload.TYPE)) {
            return;
        }
        ServerLevel level = player.level();
        ContextMap context = SlotDisplayContext.fromLevel(level);
        List<RemnantManualRecipesPayload.Entry> entries = RECIPE_IDS.stream()
                .map(id -> readRecipe(player, context, id))
                .toList();
        ServerPlayNetworking.send(player, new RemnantManualRecipesPayload(entries));
    }

    private static RemnantManualRecipesPayload.Entry readRecipe(
            ServerPlayer player,
            ContextMap context,
            Identifier id
    ) {
        ResourceKey<Recipe<?>> key = ResourceKey.create(
                net.minecraft.core.registries.Registries.RECIPE,
                id
        );
        Optional<RecipeHolder<?>> holder = player.level().getServer().getRecipeManager().byKey(key);
        if (holder.isEmpty()) {
            return RemnantManualRecipesPayload.Entry.unavailable(id.toString());
        }

        Optional<ShapedCraftingRecipeDisplay> shapedDisplay = holder.get().value().display().stream()
                .filter(ShapedCraftingRecipeDisplay.class::isInstance)
                .map(ShapedCraftingRecipeDisplay.class::cast)
                .findFirst();
        if (shapedDisplay.isPresent()) {
            ShapedCraftingRecipeDisplay shaped = shapedDisplay.get();
            List<ItemStack> ingredients = shaped.ingredients().stream()
                    .map(slot -> slot.resolveForFirstStack(context))
                    .toList();
            ItemStack result = shaped.result().resolveForFirstStack(context);
            if (result.isEmpty()) {
                return RemnantManualRecipesPayload.Entry.unavailable(id.toString());
            }
            return new RemnantManualRecipesPayload.Entry(
                    id.toString(),
                    true,
                    shaped.width(),
                    shaped.height(),
                    ingredients,
                    result
            );
        }

        Optional<SmithingRecipeDisplay> smithingDisplay = holder.get().value().display().stream()
                .filter(SmithingRecipeDisplay.class::isInstance)
                .map(SmithingRecipeDisplay.class::cast)
                .findFirst();
        if (smithingDisplay.isPresent()) {
            SmithingRecipeDisplay smithing = smithingDisplay.get();
            List<ItemStack> ingredients = List.of(
                    smithing.template().resolveForFirstStack(context),
                    smithing.base().resolveForFirstStack(context),
                    smithing.addition().resolveForFirstStack(context)
            );
            ItemStack result = smithing.result().resolveForFirstStack(context);
            if (result.isEmpty() || ingredients.stream().anyMatch(ItemStack::isEmpty)) {
                return RemnantManualRecipesPayload.Entry.unavailable(id.toString());
            }
            return new RemnantManualRecipesPayload.Entry(
                    id.toString(),
                    true,
                    3,
                    1,
                    ingredients,
                    result
            );
        }
        return RemnantManualRecipesPayload.Entry.unavailable(id.toString());
    }

    private static Identifier recipeId(String path) {
        return Identifier.fromNamespaceAndPath("totem", "remnant/" + path);
    }
}
