package dev.totem.remnant.client.screen;

import dev.totem.core.api.v1.client.observer.ObserverRemoteCursor;
import dev.totem.core.api.v1.client.observer.ObserverScreenContext;
import dev.totem.core.api.v1.client.observer.ObserverScreenHandle;
import dev.totem.core.api.v1.client.observer.ObserverScreenProvider;
import dev.totem.core.api.v1.client.observer.ObserverScreenSnapshot;
import dev.totem.remnant.inventory.BackpackMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.Set;
import java.util.Optional;
import java.util.Map;

/** Remnant-owned factory for the production backpack Screen. */
public final class RemnantBackpackObserverScreenProvider implements ObserverScreenProvider {
    @Override public String familyId() { return "remnant_backpack"; }
    @Override public int protocolVersion() { return 1; }
    @Override public Set<String> variants() { return Set.of(""); }

    @Override public Optional<ObserverScreenSnapshot> capture(Screen candidate, long sequence) {
        if (!(candidate instanceof BackpackScreen screen) || screen.totem$isObserverReadOnly()) return Optional.empty();
        BackpackMenu menu = screen.getMenu();
        return Optional.of(new ObserverScreenSnapshot(familyId(), "", protocolVersion(), sequence,
                screen.getTitle(), menu.getItems(), new int[]{menu.getRowCount(), menu.upgradeSlotCount()},
                Map.of(), new byte[0]));
    }

    @Override public ObserverScreenHandle create(ObserverScreenContext context, ObserverScreenSnapshot snapshot) {
        if (!supports(snapshot)) throw new IllegalArgumentException("Incompatible Remnant Observer snapshot");
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) throw new IllegalStateException("Observer player is unavailable");
        int[] data = snapshot.data();
        int rows = data.length > 0 ? Math.clamp(data[0], 1, 12) : 3;
        int upgradeSlots = data.length > 1 ? Math.clamp(data[1], 0, 4) : 0;
        Inventory detachedInventory = new Inventory(minecraft.player, new EntityEquipment());
        BackpackMenu menu = BackpackMenu.clientSide(-1, detachedInventory, rows, upgradeSlots);
        BackpackScreen screen = new BackpackScreen(menu, detachedInventory, snapshot.title(),
                true, context.stopObserving());
        return new Handle(screen, menu, snapshot);
    }

    private final class Handle implements ObserverScreenHandle {
        private final BackpackScreen screen;
        private final BackpackMenu menu;
        private long sequence = -1;
        private long cursorSequence = -1;
        private Handle(BackpackScreen screen, BackpackMenu menu, ObserverScreenSnapshot initial) {
            this.screen = screen; this.menu = menu; applySnapshot(initial);
        }
        @Override public Screen screen() { return screen; }
        @Override public void applySnapshot(ObserverScreenSnapshot snapshot) {
            if (!RemnantBackpackObserverScreenProvider.this.supports(snapshot)
                    || snapshot.sequence() <= sequence) return;
            var items = new ArrayList<ItemStack>(menu.slots.size());
            var remoteSlots = snapshot.slots();
            for (int i = 0; i < menu.slots.size(); i++)
                items.add(i < remoteSlots.size() ? remoteSlots.get(i).copy() : ItemStack.EMPTY);
            menu.initializeContents((int)Math.min(Integer.MAX_VALUE, snapshot.sequence()), items, menu.getCarried());
            sequence = snapshot.sequence();
        }
        @Override public void applyCursor(ObserverRemoteCursor cursor) {
            if (cursor.sequence() <= cursorSequence) return;
            cursorSequence = cursor.sequence();
            menu.setCarried(cursor.carriedStack());
        }
    }
}
