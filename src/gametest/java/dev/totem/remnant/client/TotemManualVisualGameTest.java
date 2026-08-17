package dev.totem.remnant.client;

import dev.totem.core.api.v1.manual.TotemManualAssembler;
import dev.totem.remnant.registry.RemnantGameRules;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/** Captures the real Traditional Chinese Totem manual in the vanilla written-book screen. */
@SuppressWarnings("UnstableApiUsage")
public final class TotemManualVisualGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        AtomicReference<CompletableFuture<Void>> reload = new AtomicReference<>();
        context.runOnClient(client -> {
            client.options.languageCode = "zh_tw";
            client.getLanguageManager().setSelected("zh_tw");
            reload.set(client.reloadResourcePacks());
        });
        context.waitFor(client -> reload.get() != null && reload.get().isDone());
        context.runOnClient(client -> {
            if (!I18n.get("book.deadrecall.remnant.basics.title").equals("背包與升級")) {
                throw new AssertionError("Traditional Chinese manual resources were not loaded");
            }
            client.options.guiScale().set(3);
        });
        context.getInput().resizeWindow(1280, 720);

        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getClientLevel().waitForChunksRender();
            singleplayer.getServer().runOnServer(server -> server.getGameRules().set(
                    RemnantGameRules.DEATH_BACKPACK_OWNER_PICKUP_ONLY,
                    false,
                    server
            ));
            context.waitFor(client -> RemnantGameRules.clientRulesSynchronized()
                    && !RemnantGameRules.clientDeathBackpackOwnerPickupOnly());
            ItemStack manual = TotemManualAssembler.create();
            WrittenBookContent content = manual.get(DataComponents.WRITTEN_BOOK_CONTENT);
            if (content == null) {
                throw new AssertionError("Totem manual did not contain written-book pages");
            }
            int pageCount = content.pages().size();
            context.runOnClient(client -> client.setScreenAndShow(new BookViewScreen(
                    BookViewScreen.BookAccess.fromItem(manual)
            )));
            context.waitForScreen(BookViewScreen.class);
            context.waitTicks(10);

            for (int page = 0; page < pageCount; page += 2) {
                int capturedPage = page;
                context.runOnClient(client -> {
                    BookViewScreen screen = (BookViewScreen) client.gui.screen();
                    if (screen == null) {
                        throw new AssertionError("Totem manual screen closed before page " + (capturedPage + 1));
                    }
                    screen.setPage(capturedPage);
                });
                context.waitTicks(2);
                int rightPage = Math.min(page + 2, pageCount);
                context.takeScreenshot("totem-manual-spread-%02d-%02d".formatted(page + 1, rightPage));
            }

            context.runOnClient(client -> client.setScreenAndShow(null));
        }
    }
}
