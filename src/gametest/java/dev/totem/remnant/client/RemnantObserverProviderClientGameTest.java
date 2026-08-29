package dev.totem.remnant.client;

import dev.totem.core.api.v1.client.observer.ObserverRemoteCursor;
import dev.totem.core.api.v1.client.observer.ObserverScreenContext;
import dev.totem.core.api.v1.client.observer.ObserverScreenHandle;
import dev.totem.core.api.v1.client.observer.ObserverScreenProvider;
import dev.totem.core.api.v1.client.observer.ObserverScreenSnapshot;
import dev.totem.remnant.client.screen.BackpackScreen;
import dev.totem.remnant.client.screen.RemnantBackpackObserverScreenProvider;
import dev.totem.remnant.inventory.BackpackMenu;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Owner-local runtime proof for the production Backpack Observer screen. */
@SuppressWarnings("UnstableApiUsage")
public final class RemnantObserverProviderClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientLevel().waitForChunksRender();
            context.getInput().resizeWindow(1280, 720);
            RemnantBackpackObserverScreenProvider provider = context.computeOnClient(client -> {
                boolean registered = FabricLoader.getInstance()
                        .getEntrypoints(ObserverScreenProvider.ENTRYPOINT, ObserverScreenProvider.class).stream()
                        .anyMatch(RemnantBackpackObserverScreenProvider.class::isInstance);
                if (!registered) throw new AssertionError("Remnant Observer provider entrypoint is missing");
                return new RemnantBackpackObserverScreenProvider();
            });
            ObserverScreenSnapshot initial = capture(context, provider, source(context, Items.IRON_INGOT, 1), 1);
            ObserverScreenSnapshot update = capture(context, provider, source(context, Items.GOLD_INGOT, 3), 2);
            AtomicInteger stops = new AtomicInteger();
            ObserverScreenHandle handle = context.computeOnClient(client -> provider.create(
                    new ObserverScreenContext(UUID.randomUUID(), "Target", stops::incrementAndGet), initial));
            context.runOnClient(client -> client.setScreenAndShow(handle.screen()));
            context.waitForScreen(BackpackScreen.class);
            context.runOnClient(client -> {
                BackpackScreen screen = (BackpackScreen) handle.screen();
                require(screen.totem$isObserverReadOnly(), "Backpack did not enter Observer mode");
                require(screen.getMenu().getSlot(0).getItem().is(Items.IRON_INGOT),
                        "Initial Backpack snapshot was not applied");
                handle.applySnapshot(foreign(update, "remnant_backpack", "wrong", 1, 90));
                handle.applySnapshot(foreign(update, "remnant_backpack", "", 2, 91));
                handle.applySnapshot(foreign(update, "foreign", "", 1, 92));
                handle.applySnapshot(update);
                handle.applySnapshot(initial);
                require(screen.getMenu().getSlot(0).getItem().is(Items.GOLD_INGOT)
                                && screen.getMenu().getSlot(0).getItem().getCount() == 3,
                        "Exact monotonic Backpack snapshot policy failed");
                ItemStack carried = new ItemStack(Items.DIAMOND, 2);
                handle.applyCursor(new ObserverRemoteCursor(2, 88, 83, 176, 166, carried));
                handle.applyCursor(new ObserverRemoteCursor(1, 0, 0, 176, 166, ItemStack.EMPTY));
                require(ItemStack.matches(carried, screen.getMenu().getCarried()),
                        "Stale Backpack cursor replaced the carried stack");
                ObserverPacketProbe.reset();
                require(screen.mouseClicked(new MouseButtonEvent(1, 1,
                                new MouseButtonInfo(0, 0)), false), "Observer mouse input was not consumed");
                require(screen.keyPressed(new KeyEvent(65, 0, 0)),
                        "Observer keyboard input was not consumed");
                require(ObserverPacketProbe.sends() == 0, "Backpack Observer input attempted a packet");
            });
            context.waitTicks(2);
            context.takeScreenshot("remnant-observer-owner-production-screen");
            context.runOnClient(client -> {
                ObserverPacketProbe.reset();
                require(handle.screen().keyPressed(new KeyEvent(256, 0, 0)), "Escape was not consumed");
                require(stops.get() == 1, "Escape did not request stop-observing exactly once");
                require(ObserverPacketProbe.sends() == 0, "Closing Observer mode attempted a packet");
                client.setScreenAndShow(null);
            });
            context.waitForScreen(null);
        }
    }

    private static BackpackScreen source(ClientGameTestContext context,
                                         net.minecraft.world.item.Item item, int count) {
        return context.computeOnClient(client -> {
            BackpackMenu menu = BackpackMenu.clientSide(count, client.player.getInventory(), 3, 0);
            menu.getSlot(0).set(new ItemStack(item, count));
            return new BackpackScreen(menu, client.player.getInventory(), Component.literal("Backpack"));
        });
    }

    private static ObserverScreenSnapshot capture(ClientGameTestContext context,
                                                  ObserverScreenProvider provider,
                                                  BackpackScreen screen, long sequence) {
        return context.computeOnClient(client -> provider.capture(screen, sequence).orElseThrow());
    }

    private static ObserverScreenSnapshot foreign(ObserverScreenSnapshot source, String family,
                                                   String variant, int protocol, long sequence) {
        return new ObserverScreenSnapshot(family, variant, protocol, sequence, source.title(), source.slots(),
                source.data(), source.metadata(), source.ownerPayload());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
