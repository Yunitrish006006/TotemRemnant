package dev.totem.remnant.client;

import dev.totem.remnant.registry.RemnantItemRegistration;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Captures and verifies Remnant's module-owned Creative tab. */
@SuppressWarnings("UnstableApiUsage")
public final class RemnantCreativeTabVisualGameTest implements FabricClientGameTest {
    private static Object creativeScreen;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getClientLevel().waitForChunksRender();
            singleplayer.getServer().runCommand("gamemode creative @a");
            context.waitFor(RemnantCreativeTabVisualGameTest::hasCreativeAbilities);
            context.runOnClient(RemnantCreativeTabVisualGameTest::openCreativeScreen);
            context.waitTicks(20);
            context.runOnClient(RemnantCreativeTabVisualGameTest::selectRemnantCreativeTab);
            context.waitTicks(2);
            context.takeScreenshot("totem-remnant-creative-showcase");
            context.runOnClient(RemnantCreativeTabVisualGameTest::closeScreen);
        }
    }

    private static void openCreativeScreen(Object client) {
        try {
            Object player = client.getClass().getField("player").get(client);
            Object level = client.getClass().getField("level").get(client);
            Object enabledFeatures = invoke(level, "enabledFeatures");
            Class<?> screenClass = Class.forName("net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen");
            Class<?> playerClass = Class.forName("net.minecraft.client.player.LocalPlayer");
            Class<?> featureFlagsClass = Class.forName("net.minecraft.world.flag.FeatureFlagSet");
            creativeScreen = screenClass
                    .getConstructor(playerClass, featureFlagsClass, boolean.class)
                    .newInstance(player, enabledFeatures, true);
            invoke(client, "setScreenAndShow", creativeScreen);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not open the Creative inventory", exception);
        }
    }

    private static void selectRemnantCreativeTab(Object client) {
        try {
            if (creativeScreen == null) {
                throw new IllegalStateException("Creative inventory was not opened");
            }
            Identifier tabId = Identifier.fromNamespaceAndPath("totem-remnant", "main");
            CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(tabId);
            if (tab == null) {
                throw new IllegalStateException("Missing module-owned Remnant Creative tab");
            }
            assertTabContents(tab);
            if (BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(
                    Identifier.fromNamespaceAndPath("deadrecall", "main"))) {
                throw new IllegalStateException("Standalone Remnant still registered the legacy DeadRecall tab");
            }
            Class<?> tabClass = Class.forName("net.minecraft.world.item.CreativeModeTab");
            Class<?> screenExtension = Class.forName(
                    "net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen");
            boolean selected = (Boolean) screenExtension
                    .getMethod("setSelectedTab", tabClass)
                    .invoke(creativeScreen, tab);
            if (!selected) {
                throw new IllegalStateException("Could not switch to the Remnant Creative tab");
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not select the Remnant Creative tab", exception);
        }
    }

    private static void assertTabContents(CreativeModeTab tab) {
        List<Item> expected = List.of(
                RemnantItemRegistration.BACKPACK_BASIC,
                RemnantItemRegistration.BACKPACK_STANDARD,
                RemnantItemRegistration.BACKPACK_ADVANCED,
                RemnantItemRegistration.BACKPACK_NETHERITE,
                RemnantItemRegistration.UPGRADE_CRAFTING,
                RemnantItemRegistration.UPGRADE_COMPACTION,
                RemnantItemRegistration.UPGRADE_MATCHING_PICKUP,
                RemnantItemRegistration.UPGRADE_CAPACITY,
                RemnantItemRegistration.UPGRADE_SOULBOUND_CHARGE,
                RemnantItemRegistration.UPGRADE_ENDER_ACCESS,
                RemnantItemRegistration.UPGRADE_BLAST_PROTECTION,
                RemnantItemRegistration.UPGRADE_FIRE_PROTECTION,
                RemnantItemRegistration.UPGRADE_DESPAWN_PROTECTION,
                RemnantItemRegistration.UPGRADE_VOID_PROTECTION,
                RemnantItemRegistration.UPGRADE_PERFECT_PRESERVATION,
                RemnantItemRegistration.DEATH_BACKPACK
        );
        List<Item> actual = tab.getDisplayItems().stream().map(ItemStack::getItem).toList();
        if (!actual.equals(expected)) {
            throw new IllegalStateException("Remnant Creative tab contents differ: " + actual);
        }
    }

    private static boolean hasCreativeAbilities(Object client) {
        try {
            Object player = client.getClass().getField("player").get(client);
            if (player == null) {
                return false;
            }
            Object abilities = invoke(player, "getAbilities");
            return abilities.getClass().getField("instabuild").getBoolean(abilities);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not verify the Creative-mode transition", exception);
        }
    }

    private static void closeScreen(Object client) {
        try {
            invoke(client, "setScreenAndShow", new Object[]{null});
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not close the Creative inventory", exception);
        }
    }

    private static Object invoke(Object target, String name, Object... arguments) throws ReflectiveOperationException {
        for (var method : target.getClass().getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == arguments.length) {
                return method.invoke(target, arguments);
            }
        }
        throw new NoSuchMethodException(target.getClass().getName() + "#" + name);
    }
}
