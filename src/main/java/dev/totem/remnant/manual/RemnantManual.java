package dev.totem.remnant.manual;

import dev.totem.core.api.v1.manual.TotemManualLifecycle;
import dev.totem.core.api.v1.manual.TotemManualPlayerHelper;
import dev.totem.core.api.v1.manual.TotemManualRegistry;
import dev.totem.core.api.v1.manual.TotemManualSection;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

/** Continuous Remnant tutorial chapter and smithing-table acquisition source. */
public final class RemnantManual {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static final List<TotemManualSection> SECTIONS = List.of(manualSection());
    private static final Identifier MANUAL_ADVANCEMENT =
            Identifier.fromNamespaceAndPath("deadrecall", "remnant_manual");

    private RemnantManual() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        SECTIONS.forEach(TotemManualRegistry.global()::register);
        TotemManualLifecycle.registerLoginRefresh();

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (player.isSpectator()
                    || !world.getBlockState(hitResult.getBlockPos()).is(Blocks.SMITHING_TABLE)) {
                return InteractionResult.PASS;
            }
            ItemStack stack = player.getItemInHand(hand);
            if (!isManualRequest(stack)) {
                return InteractionResult.PASS;
            }
            if (world.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            return grant((ServerPlayer) player, hand)
                    ? InteractionResult.SUCCESS
                    : InteractionResult.PASS;
        });
    }

    public static boolean isManualRequest(ItemStack stack) {
        return TotemManualPlayerHelper.supportsSourceInteraction(stack, ignored -> false);
    }

    public static boolean grant(ServerPlayer player, InteractionHand hand) {
        if (player == null || hand == null) {
            return false;
        }
        return TotemManualPlayerHelper.acquireSections(
                player,
                hand,
                SECTIONS,
                MANUAL_ADVANCEMENT,
                ignored -> false
        ).handled();
    }

    static List<TotemManualSection> sections() {
        return SECTIONS;
    }

    private static TotemManualSection manualSection() {
        List<String> pageKeys = new java.util.ArrayList<>();
        pageKeys.add("book.deadrecall.remnant.basics.page.1");
        pageKeys.add("book.deadrecall.remnant.basics.page.2");
        pageKeys.add("book.deadrecall.remnant.basics.page.3");
        pageKeys.add("book.deadrecall.remnant.dyeing.page.1");
        pageKeys.add("book.deadrecall.remnant.echo_crystallization.page.1");
        pageKeys.add("book.deadrecall.remnant.module_recipes.overview");
        IntStream.rangeClosed(1, 11).forEach(page -> {
            pageKeys.add("book.deadrecall.remnant.module_recipes.description." + page);
            pageKeys.add("book.deadrecall.remnant.module_recipes.page." + page);
        });
        pageKeys.add("book.deadrecall.remnant.death_backpack.page.1");
        pageKeys.add("book.deadrecall.remnant.death_backpack.page.2");
        pageKeys.add("book.deadrecall.remnant.death_backpack.page.3");
        pageKeys.add("book.deadrecall.remnant.container_safety.page.1");

        Map<String, List<Component>> arguments = new LinkedHashMap<>();
        arguments.putAll(pageArguments("basics"));
        arguments.putAll(pageArguments("dyeing"));
        arguments.putAll(pageArguments("echo_crystallization"));
        arguments.putAll(pageArguments("module_recipes"));
        arguments.putAll(pageArguments("death_backpack"));
        arguments.putAll(pageArguments("container_safety"));
        return new TotemManualSection(
                Identifier.fromNamespaceAndPath("totem", "remnant/manual"),
                100,
                "book.deadrecall.remnant.manual.title",
                pageKeys,
                Map.copyOf(arguments)
        );
    }

    private static Map<String, List<Component>> pageArguments(String path) {
        String prefix = "book.deadrecall.remnant." + path + ".page.";
        return switch (path) {
            case "basics" -> Map.of(
                    prefix + "1", itemNames(
                            "item.minecraft.book",
                            "block.minecraft.smithing_table",
                            "item.totem.manual"
                    ),
                    prefix + "2", itemNames(
                            "item.totem.remnant.backpack_basic",
                            "item.totem.remnant.backpack_standard",
                            "item.totem.remnant.backpack_advanced",
                            "item.totem.remnant.backpack_netherite",
                            "item.minecraft.bundle",
                            "item.minecraft.leather",
                            "item.minecraft.iron_ingot",
                            "item.minecraft.diamond",
                            "item.minecraft.netherite_upgrade_smithing_template",
                            "item.minecraft.netherite_ingot"
                    ),
                    prefix + "3", itemNames(
                            "item.totem.remnant.upgrade_capacity",
                            "item.totem.remnant.upgrade_crafting",
                            "item.totem.remnant.upgrade_compaction",
                            "item.totem.remnant.upgrade_matching_pickup",
                            "item.totem.remnant.upgrade_soulbound_charge"
                    )
            );
            case "dyeing" -> Map.of(
                    prefix + "1", itemNames("item.totem.remnant.death_backpack")
            );
            case "echo_crystallization" -> Map.of(
                    prefix + "1", itemNames(
                            "item.minecraft.amethyst_shard",
                            "block.minecraft.sculk",
                            "item.minecraft.echo_shard",
                            "block.minecraft.deepslate"
                    )
            );
            case "module_recipes" -> moduleRecipePageArguments();
            case "death_backpack" -> Map.of(
                    prefix + "1", itemNames("item.totem.remnant.death_backpack"),
                    prefix + "2", itemNames("item.totem.remnant.death_backpack")
            );
            case "container_safety" -> Map.of(
                    prefix + "1", itemNames(
                            "item.minecraft.bundle",
                            "block.minecraft.shulker_box"
                    )
            );
            default -> Map.of();
        };
    }

    private static Map<String, List<Component>> moduleRecipePageArguments() {
        String prefix = "book.deadrecall.remnant.module_recipes.";
        String[] itemKeys = {
                "item.totem.remnant.upgrade_crafting",
                "item.totem.remnant.upgrade_compaction",
                "item.totem.remnant.upgrade_matching_pickup",
                "item.totem.remnant.upgrade_capacity",
                "item.totem.remnant.upgrade_soulbound_charge",
                "item.totem.remnant.upgrade_blast_protection",
                "item.totem.remnant.upgrade_fire_protection",
                "item.totem.remnant.upgrade_despawn_protection",
                "item.totem.remnant.upgrade_void_protection",
                "item.totem.remnant.upgrade_ender_access",
                "item.totem.remnant.upgrade_perfect_preservation"
        };
        Map<String, List<Component>> arguments = new LinkedHashMap<>();
        for (int index = 0; index < itemKeys.length; index++) {
            List<Component> itemName = itemNames(itemKeys[index]);
            arguments.put(prefix + "description." + (index + 1), itemName);
            arguments.put(prefix + "page." + (index + 1), itemName);
        }
        return Map.copyOf(arguments);
    }

    private static List<Component> itemNames(String... translationKeys) {
        return java.util.Arrays.stream(translationKeys)
                .<Component>map(Component::translatable)
                .toList();
    }
}
