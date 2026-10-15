package dev.totem.remnant.client;

import com.mojang.authlib.GameProfile;
import dev.totem.core.api.v1.client.observer.*;
import dev.totem.remnant.client.screen.*;
import dev.totem.remnant.inventory.BackpackPanelMenuAccess;
import dev.totem.remnant.registry.RemnantItemRegistration;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.input.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("UnstableApiUsage")
public final class BackpackSidebarObserverClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try(var world=context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();context.getInput().resizeWindow(1280,720);
            var provider=new RemnantSidebarObserverScreenProvider();
            boolean observerLoaded=net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("totem-observer");
            require(observerLoaded==Boolean.getBoolean("totem.remnant.test.expectedObserver"),"Unexpected Observer runtime presence");
            for(String variant:List.of("inventory","crafting","enchanting","chest")) for(boolean death:List.of(false,true)) {
                var initial=context.computeOnClient(client->{
                    var target=new RemotePlayer(client.level,new GameProfile(UUID.randomUUID(),"Source"));
                    target.setId(-314159);target.experienceLevel=42;var inv=target.getInventory();
                    var bag=new ItemStack(death?RemnantItemRegistration.DEATH_BACKPACK:RemnantItemRegistration.BACKPACK_BASIC);
                    var contents=new ArrayList<ItemStack>();for(int i=0;i<(death?256:9);i++)contents.add(ItemStack.EMPTY);
                    contents.set(death?255:0,new ItemStack(Items.DIAMOND,7));
                    bag.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(contents));
                    inv.setItem(40,bag); // Offhand absent from workstation/chest native slots.
                    AbstractContainerScreen<?> source=switch(variant) {
                        case "inventory"->new InventoryScreen(target);
                        case "crafting"->new CraftingScreen(new CraftingMenu(12,inv),inv,Component.literal("Crafting"));
                        case "enchanting"->new EnchantmentScreen(new EnchantmentMenu(12,inv),inv,Component.literal("Enchanting"));
                        default->new ContainerScreen(ChestMenu.sixRows(12,inv),inv,Component.literal("Chest"));
                    };
                    if(source instanceof AbstractRecipeBookScreen<?> recipe) {
                        source.init(client.getWindow().getGuiScaledWidth(),client.getWindow().getGuiScaledHeight());
                        var ra=(dev.totem.remnant.mixin.client.SidebarAbstractRecipeBookScreenAccessor)recipe;
                        var ba=(dev.totem.remnant.mixin.client.SidebarRecipeBookComponentAccessor)ra.remnant$getRecipeBookComponent();
                        ba.remnant$initVisuals();ba.remnant$setVisible(true);
                        ba.remnant$getSearchBox().setValue("PRIVATE_UNSENT_SIDEBAR_QUERY");
                    }
                    ((BackpackPanelMenuAccess)source.getMenu()).totem$selectBackpackSlot(40);
                    ((BackpackSidebarLayout)source).remnant$firstRow(death?255:0);
                    if(observerLoaded) verifyOwnership(source,provider);
                    if(source instanceof AbstractRecipeBookScreen<?> recipe) {
                        var ra=(dev.totem.remnant.mixin.client.SidebarAbstractRecipeBookScreenAccessor)recipe;
                        var ba=(dev.totem.remnant.mixin.client.SidebarRecipeBookComponentAccessor)ra.remnant$getRecipeBookComponent();
                        ba.remnant$setVisible(true);ba.remnant$getSearchBox().setValue("PRIVATE_UNSENT_SIDEBAR_QUERY");
                    }
                    ((BackpackSidebarLayout)source).remnant$firstRow(death?255:0);
                    var snapshot=provider.capture(source,1).orElseThrow();
                    require(!snapshot.metadata().toString().contains("PRIVATE_UNSENT"),"Unsent recipe query leaked");
                    return snapshot;
                });
                var stops=new AtomicInteger();
                var handle=context.computeOnClient(client->provider.create(new ObserverScreenContext(UUID.randomUUID(),"Target",stops::incrementAndGet),initial));
                context.runOnClient(client->{
                    ObserverPacketProbe.reset();client.setScreenAndShow(handle.screen());
                    require(ObserverPacketProbe.sends()==0,"Readonly init sent packets: "+ObserverPacketProbe.packetTypes());
                });
                context.waitTicks(10);
                context.runOnClient(client->{
                    // Rendering spans live world ticks; allow only the two observed background transports.
                    require(ObserverPacketProbe.nonBackgroundSends()==0,"Readonly render sent unexpected packets: "+ObserverPacketProbe.packetTypes());
                    ObserverPacketProbe.reset();
                    var screen=(AbstractContainerScreen<?>)handle.screen();var menu=screen.getMenu();
                    var access=(BackpackPanelMenuAccess)menu;int deep=access.totem$getBackpackPanelSlotStart()+(death?255:0);
                    require(access.totem$getBackpackPanel().selectedInventorySlot()==40,"Explicit offhand selection was lost");
                    require(menu.getSlot(deep).getItem().getCount()==7,"Deep/ordinary sidebar snapshot missing");
                    require(menu.getSlot(deep).x > -1000 && menu.getSlot(deep).y > -1000,
                            "Sidebar viewport missing: "+variant+" death="+death+" x="+menu.getSlot(deep).x+" y="+menu.getSlot(deep).y+" firstRow="+((BackpackSidebarLayout)screen).remnant$firstRow()+" width="+screen.width);
                    var visible=menu.getSlot(deep);
                    require(visible.x>=189 || visible.x+16<=-13,"Sidebar overlapped native functional area");
                    require(provider.capture(screen,2).isEmpty(),"Projection was rebroadcast");
                    var slots=new ArrayList<>(initial.slots());var bag=slots.getLast().copy();
                    var contents=new ArrayList<>(bag.get(DataComponents.CONTAINER).itemCopies().toList());
                    contents.set(death?255:0,new ItemStack(Items.EMERALD,11));bag.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(contents));slots.set(slots.size()-1,bag);
                    var update=new ObserverScreenSnapshot(provider.familyId(),variant,1,2,initial.title(),slots,initial.data(),initial.metadata(),new byte[0]);
                    handle.applySnapshot(update);handle.applySnapshot(initial);
                    handle.applySnapshot(new ObserverScreenSnapshot(provider.familyId(),"wrong",1,3,initial.title(),slots,initial.data(),Map.of(),new byte[0]));
                    require(menu.getSlot(deep).getItem().is(Items.EMERALD)&&menu.getSlot(deep).getItem().getCount()==11,"Exact monotonic update policy failed");
                    handle.applyCursor(new ObserverRemoteCursor(2,88,83,176,166,new ItemStack(Items.GOLD_INGOT,3)));
                    handle.applyCursor(new ObserverRemoteCursor(1,0,0,176,166,ItemStack.EMPTY));
                    require(menu.getCarried().getCount()==3,"Stale cursor replaced carried item");
                    for(var child:screen.children())if(child instanceof net.minecraft.client.gui.components.Button button)
                        button.onPress(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_A,0,0));
                    screen.mouseClicked(new MouseButtonEvent(10,10,new MouseButtonInfo(1,0)),false);
                    screen.mouseReleased(new MouseButtonEvent(10,10,new MouseButtonInfo(1,0)));
                    screen.mouseDragged(new MouseButtonEvent(10,10,new MouseButtonInfo(1,0)),1,1);
                    screen.mouseScrolled(10,10,0,-1);screen.keyPressed(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_A,0,0));
                    require(ObserverPacketProbe.sends()==0,"Readonly native sidebar packet: "+variant+" death="+death+" types="+ObserverPacketProbe.packetTypes());
                });
                context.waitTicks(2);context.takeScreenshot("remnant-sidebar-observer-"+variant+(death?"-death":"-ordinary"));
                context.runOnClient(client->{
                    ObserverPacketProbe.reset();
                    handle.screen().keyPressed(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE,0,0));require(stops.get()==1,"Escape stop lifecycle failed");
                    client.setScreenAndShow(null);require(ObserverPacketProbe.sends()==0,"Readonly close sent inventory packet");
                });
            }
        }
    }
    private static void verifyOwnership(AbstractContainerScreen<?> source,RemnantSidebarObserverScreenProvider provider) {
        try {
            var policy=Class.forName("dev.totem.observer.client.ObserverScreenOwnership");
            var claims=policy.getMethod("claims",net.minecraft.client.gui.screens.Screen.class,Collection.class,long.class);
            require((Boolean)claims.invoke(null,source,List.of(provider),1L),"Owner provider did not suppress native capture");
            require((Boolean)claims.invoke(null,source,List.of(new FaultingProvider(provider)),1L),"Capture failure permitted legacy fallback");
            require(!(Boolean)claims.invoke(null,source,List.of(),1L),"Module-absent native fallback was suppressed");
            var access=(BackpackPanelMenuAccess)source.getMenu();var panel=access.totem$getBackpackPanel();
            var inventory=panel.playerInventory();int selected=panel.selectedInventorySlot();var bag=inventory.getItem(selected);
            try {
                inventory.setItem(selected,ItemStack.EMPTY);panel.refreshAfterExternalCommit();
                require(!(Boolean)claims.invoke(null,source,List.of(provider),1L),"No-bag native fallback was suppressed");
                var boundary=Class.forName("dev.totem.observer.client.ObserverVanillaSlotBoundary").getMethod("count",AbstractContainerMenu.class);
                require((Integer)boundary.invoke(null,source.getMenu())==access.totem$getBackpackPanelSlotStart(),"Legacy fallback included sidecar slots");
            } finally {inventory.setItem(selected,bag);access.totem$selectBackpackSlot(selected);}
            require((Boolean)claims.invoke(null,source,List.of(provider),1L),"Offhand bag reappearance failed to restore ownership");
            SidebarOwnershipRuntimeAssertions.verify(source,provider);
            // This policy has no negotiated-capability input, so absence cannot grant a fallback.
        } catch(ReflectiveOperationException error){throw new AssertionError("Owner arbitration runtime regression",error);}
    }
    private static final class FaultingProvider implements ObserverScreenProvider,java.util.function.Predicate<net.minecraft.client.gui.screens.Screen> {
        private final RemnantSidebarObserverScreenProvider owner;
        FaultingProvider(RemnantSidebarObserverScreenProvider owner){this.owner=owner;}
        public String familyId(){return owner.familyId();} public int protocolVersion(){return 1;}
        public Set<String> variants(){return owner.variants();}
        public boolean test(net.minecraft.client.gui.screens.Screen screen){return owner.test(screen);}
        public Optional<ObserverScreenSnapshot> capture(net.minecraft.client.gui.screens.Screen screen,long sequence){throw new IllegalStateException("Forced owner capture failure");}
        public ObserverScreenHandle create(ObserverScreenContext context,ObserverScreenSnapshot snapshot){throw new UnsupportedOperationException();}
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
