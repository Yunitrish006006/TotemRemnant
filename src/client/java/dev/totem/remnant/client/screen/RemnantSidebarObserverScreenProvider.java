package dev.totem.remnant.client.screen;

import com.mojang.authlib.GameProfile;
import dev.totem.core.api.v1.client.observer.*;
import dev.totem.remnant.inventory.BackpackPanelMenuAccess;
import dev.totem.remnant.mixin.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.gui.screens.recipebook.*;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Native vanilla screen reconstruction owned by the module that adds the sidebar. */
public final class RemnantSidebarObserverScreenProvider implements ObserverScreenProvider, java.util.function.Predicate<Screen> {
    @Override public String familyId() { return "remnant_sidebar"; }
    @Override public int protocolVersion() { return 1; }
    @Override public Set<String> variants() { return Set.of("inventory", "crafting", "enchanting", "chest"); }
    private static void clientThread() {
        if (!Minecraft.getInstance().isSameThread()) throw new IllegalStateException("Sidebar Observer requires client thread");
    }
    private static String variant(AbstractContainerScreen<?> screen) {
        if (screen.getClass() == InventoryScreen.class) return "inventory";
        if (screen.getClass() == CraftingScreen.class) return "crafting";
        if (screen.getClass() == EnchantmentScreen.class) return "enchanting";
        if (screen.getClass() == ContainerScreen.class) return "chest";
        return "";
    }
    /** Local ownership survives missing transport capability and capture/encoding failure. */
    @Override public boolean test(Screen candidate) {
        if (!(candidate instanceof AbstractContainerScreen<?> screen) || variant(screen).isEmpty()
                || !BackpackPanelMenuAccess.supports(screen.getMenu())) return false;
        var inventory=((BackpackPanelMenuAccess)screen.getMenu()).totem$getBackpackPanel().playerInventory();
        for (int i=0;i<inventory.getContainerSize();i++)
            if (dev.totem.remnant.inventory.BackpackPanelContainer.isBackpack(inventory.getItem(i))) return true;
        return false;
    }
    @Override public Optional<ObserverScreenSnapshot> capture(Screen candidate, long sequence) {
        clientThread();
        if (!test(candidate)) return Optional.empty();
        if (!(candidate instanceof AbstractContainerScreen<?> screen) || variant(screen).isEmpty()
                || !BackpackPanelMenuAccess.supports(screen.getMenu())) return Optional.empty();
        var menu = screen.getMenu();
        var access = (BackpackPanelMenuAccess)menu;
        var panel = access.totem$getBackpackPanel();
        int nativeCount = access.totem$getBackpackPanelSlotStart();
        var slots = new ArrayList<ItemStack>();
        for (int i = 0; i < nativeCount; i++) slots.add(menu.getSlot(i).getItem().copy());
        // Offhand bags are selectable but are absent from most vanilla menus.
        slots.add(panel.selectedBackpack().copy());
        var player = panel.playerInventory().player;
        var nativeData = ((SidebarMenuDataAccessor)menu).remnant$dataSlots();
        int[] data = new int[10 + nativeData.size()];
        data[0] = panel.selectedInventorySlot();
        data[1] = ((BackpackSidebarLayout)screen).remnant$firstRow();
        data[2] = player.experienceLevel; data[3] = player.hasInfiniteMaterials() ? 1 : 0;
        data[4] = menu instanceof ChestMenu chest ? chest.getRowCount() : 0;
        data[5] = nativeData.size();
        var metadata = new HashMap<String,String>();
        if (screen instanceof AbstractRecipeBookScreen<?> recipeScreen) {
            var ra = (SidebarAbstractRecipeBookScreenAccessor)recipeScreen;
            var book = ra.remnant$getRecipeBookComponent();
            var ba = (SidebarRecipeBookComponentAccessor)book;
            data[6] = book.isVisible() ? 1 : 0;
            data[7] = ra.remnant$getWidthTooNarrow() ? 1 : 0;
            data[8] = ba.remnant$getFilterButton() != null && Boolean.TRUE.equals(ba.remnant$getFilterButton().getValue()) ? 1 : 0;
            data[9] = ba.remnant$getSearchBox() != null && !ba.remnant$getSearchBox().getValue().isBlank() ? 1 : 0;
            if (ba.remnant$getSelectedTab() != null) metadata.put("tab", tabId(ba.remnant$getSelectedTab()));
            var page = (SidebarRecipeBookPageAccessor)ba.remnant$getRecipeBookPage();
            metadata.put("page", Integer.toString(page.remnant$getCurrentPage()));
            metadata.put("pages", Integer.toString(page.remnant$getTotalPages()));
        }
        int effectIndex=0;
        for(var effect:player.getActiveEffects()) {
            if(effectIndex>=32) break;
            metadata.put("effect"+(effectIndex++), BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value())+","+effect.getDuration()+","+effect.getAmplifier()+","+effect.isAmbient()+","+effect.isVisible()+","+effect.showIcon());
        }
        for (int i = 0; i < nativeData.size(); i++) data[10+i] = nativeData.get(i).get();
        return Optional.of(new ObserverScreenSnapshot(familyId(), variant(screen), protocolVersion(), sequence,
                screen.getTitle(), slots, data, metadata, new byte[0]));
    }
    private static String tabId(RecipeBookTabButton tab) {
        var category = tab.getCategory();
        if (category instanceof SearchRecipeBookCategory search) return "search:" + search.name();
        if (category instanceof net.minecraft.world.item.crafting.RecipeBookCategory recipe)
            return BuiltInRegistries.RECIPE_BOOK_CATEGORY.getKey(recipe).toString();
        return "";
    }
    @Override public ObserverScreenHandle create(ObserverScreenContext context, ObserverScreenSnapshot snapshot) {
        clientThread();
        if (!supports(snapshot)) throw new IllegalArgumentException("Unsupported Remnant sidebar snapshot");
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) throw new IllegalStateException("No client level");
        RemotePlayer target = new RemotePlayer(minecraft.level, new GameProfile(context.targetId(), context.targetName()));
        target.setId(-1 - (context.targetId().hashCode() & 0x7ffffffe));
        var inventory = target.getInventory();
        int rows = snapshot.data().length > 4 ? snapshot.data()[4] : 0;
        AbstractContainerScreen<?> screen = switch(snapshot.variant()) {
            case "inventory" -> new SidebarNativeScreens.InventoryView(target, context.stopObserving());
            case "crafting" -> new SidebarNativeScreens.CraftingView(new CraftingMenu(-1,inventory), inventory, snapshot.title(), target, context.stopObserving());
            case "enchanting" -> new SidebarNativeScreens.EnchantingView(new EnchantmentMenu(-1,inventory), inventory, snapshot.title(), target, context.stopObserving());
            case "chest" -> new SidebarNativeScreens.ChestView(new ChestMenu(chestType(rows),-1,inventory,
                    new net.minecraft.world.SimpleContainer(Math.clamp(rows,1,6)*9),Math.clamp(rows,1,6)), inventory, snapshot.title(), target, context.stopObserving());
            default -> throw new IllegalArgumentException("Unsupported native sidebar variant");
        };
        return new Handle(screen, target, snapshot);
    }
    private static MenuType<?> chestType(int rows) {
        return switch(Math.clamp(rows,1,6)) {
            case 1 -> MenuType.GENERIC_9x1; case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3; case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5; default -> MenuType.GENERIC_9x6;
        };
    }
    private final class Handle implements ObserverScreenHandle {
        private final AbstractContainerScreen<?> screen;
        private final RemotePlayer target;
        private final String variant;
        private long sequence = -1, cursorSequence = -1;
        Handle(AbstractContainerScreen<?> screen, RemotePlayer target, ObserverScreenSnapshot initial) {
            this.screen=screen;this.target=target;this.variant=initial.variant();applySnapshot(initial);
        }
        @Override public Screen screen() { clientThread(); return screen; }
        @Override public void applySnapshot(ObserverScreenSnapshot snapshot) {
            clientThread();
            if (!supports(snapshot) || !variant.equals(snapshot.variant()) || snapshot.sequence() <= sequence) return;
            var menu=screen.getMenu();var access=(BackpackPanelMenuAccess)menu;
            int nativeCount=access.totem$getBackpackPanelSlotStart();
            int[] data=snapshot.data();var slots=snapshot.slots();
            if (data.length < 10 || data[0]<-1 || data[0]>=target.getInventory().getContainerSize()
                    || data[5] != ((SidebarMenuDataAccessor)menu).remnant$dataSlots().size()
                    || data.length != 10+data[5] || slots.size()!=nativeCount+1
                    || variant.equals("chest") && data[4] != ((ChestMenu)menu).getRowCount()) return;
            for (int i=0;i<nativeCount;i++) menu.getSlot(i).set(slots.get(i).copy());
            if(data[0]>=0) target.getInventory().setItem(data[0], slots.get(nativeCount).copy());
            access.totem$selectBackpackSlot(data[0]);access.totem$getBackpackPanel().refreshAfterExternalCommit();
            ((BackpackSidebarLayout)screen).remnant$firstRow(data[1]);
            target.experienceLevel=Math.max(0,data[2]);target.getAbilities().instabuild=data[3]!=0;
            for(int i=0;i<data[5];i++) menu.setData(i,data[10+i]);
            target.removeAllEffects();
            for(int i=0;i<32;i++) {
                String effect=snapshot.metadata().get("effect"+i);if(effect==null)break;
                try {
                    String[] fields=effect.split(",");if(fields.length!=6)continue;
                    var holder=BuiltInRegistries.MOB_EFFECT.get(net.minecraft.resources.Identifier.parse(fields[0])).orElseThrow();
                    target.addEffect(new net.minecraft.world.effect.MobEffectInstance(holder,Integer.parseInt(fields[1]),Math.clamp(Integer.parseInt(fields[2]),0,255),Boolean.parseBoolean(fields[3]),Boolean.parseBoolean(fields[4]),Boolean.parseBoolean(fields[5])));
                } catch(RuntimeException ignored) { /* Unrecognized effect metadata cannot mutate a live entity. */ }
            }
            ((SidebarProjection)screen).remnant$recipeState(data,snapshot.metadata());
            sequence=snapshot.sequence();
        }
        @Override public void applyCursor(ObserverRemoteCursor cursor) {
            clientThread();if(cursor.sequence()<=cursorSequence)return;
            screen.getMenu().setCarried(cursor.carriedStack());cursorSequence=cursor.sequence();
        }
    }
    static void recipeState(AbstractContainerScreen<?> screen, int[] data, Map<String,String> metadata) {
        if (!(screen instanceof AbstractRecipeBookScreen<?> recipeScreen)) return;
        var ra=(SidebarAbstractRecipeBookScreenAccessor)recipeScreen;
        var book=ra.remnant$getRecipeBookComponent();var ba=(SidebarRecipeBookComponentAccessor)book;
        ra.remnant$setWidthTooNarrow(data[7]!=0);ba.remnant$setWidthTooNarrow(data[7]!=0);ba.remnant$setVisible(data[6]!=0);
        if(data[6]==0)return;
        if(((SidebarRecipeBookPageAccessor)ba.remnant$getRecipeBookPage()).remnant$getMinecraft()==null)ba.remnant$initVisuals();
        if(ba.remnant$getFilterButton()!=null)ba.remnant$getFilterButton().setValue(data[8]!=0);
        if(ba.remnant$getSearchBox()!=null)ba.remnant$getSearchBox().setValue(data[9]!=0?"…":"");
        for(var tab:ba.remnant$getTabButtons()) {
            if(tabId(tab).equals(metadata.get("tab"))){tab.select();ba.remnant$setSelectedTab(tab);}else tab.unselect();
        }
        var page=ba.remnant$getRecipeBookPage();page.updateCollections(List.of(),data[8]!=0,false);
        var pa=(SidebarRecipeBookPageAccessor)page;
        try {pa.remnant$setCurrentPage(Math.clamp(Integer.parseInt(metadata.getOrDefault("page","0")),0,4096));
            pa.remnant$setTotalPages(Math.clamp(Integer.parseInt(metadata.getOrDefault("pages","0")),0,4096));}
        catch(NumberFormatException ignored){pa.remnant$setCurrentPage(0);pa.remnant$setTotalPages(0);}
    }
}
