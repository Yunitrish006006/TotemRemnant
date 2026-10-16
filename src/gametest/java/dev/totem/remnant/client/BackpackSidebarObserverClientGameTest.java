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
            for(String variant:List.of("inventory","crafting","enchanting","chest","furnace","smoker","blast_furnace","anvil","stonecutter","smithing","grindstone","loom","cartography","brewing","hopper","merchant")) for(boolean death:List.of(false,true)) {
                var sourceRef=new java.util.concurrent.atomic.AtomicReference<AbstractContainerScreen<?>>();
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
                        case "furnace"->new FurnaceScreen(new FurnaceMenu(12,inv),inv,Component.literal("furnace"));
                        case "smoker"->new SmokerScreen(new SmokerMenu(12,inv),inv,Component.literal("smoker"));
                        case "blast_furnace"->new BlastFurnaceScreen(new BlastFurnaceMenu(12,inv),inv,Component.literal("blast_furnace"));
                        case "anvil"->new AnvilScreen(new AnvilMenu(12,inv),inv,Component.literal("anvil"));
                        case "stonecutter"->new StonecutterScreen(new StonecutterMenu(12,inv),inv,Component.literal("stonecutter"));
                        case "smithing"->new SmithingScreen(new SmithingMenu(12,inv),inv,Component.literal("smithing"));
                        case "grindstone"->new GrindstoneScreen(new GrindstoneMenu(12,inv),inv,Component.literal("grindstone"));
                        case "loom"->new LoomScreen(new LoomMenu(12,inv),inv,Component.literal("loom"));
                        case "cartography"->new CartographyTableScreen(new CartographyTableMenu(12,inv),inv,Component.literal("cartography"));
                        case "brewing"->new BrewingStandScreen(new BrewingStandMenu(12,inv),inv,Component.literal("brewing"));
                        case "hopper"->new HopperScreen(new HopperMenu(12,inv),inv,Component.literal("hopper"));
                        case "merchant"->new MerchantScreen(new MerchantMenu(12,inv),inv,Component.literal("merchant"));
                        default->new ContainerScreen(ChestMenu.sixRows(12,inv),inv,Component.literal("Chest"));
                    };
                    sourceRef.set(source);
                    if(source.getMenu() instanceof AnvilMenu anvil) {
                        anvil.getSlot(0).set(new ItemStack(Items.DIAMOND));
                        var draft=new ItemStack(Items.DIAMOND);draft.set(DataComponents.CUSTOM_NAME,Component.literal("PRIVATE_ANVIL_DRAFT"));
                        anvil.getSlot(AnvilMenu.RESULT_SLOT).set(draft);
                    }
                    if(source.getMenu() instanceof MerchantMenu merchant) {
                        var offers=new net.minecraft.world.item.trading.MerchantOffers();
                        for(int offer=0;offer<9;offer++)offers.add(new net.minecraft.world.item.trading.MerchantOffer(new net.minecraft.world.item.trading.ItemCost(Items.EMERALD,3),Optional.empty(),new ItemStack(Items.DIAMOND),2,7,3,0.2F));
                        merchant.setOffers(offers);merchant.setMerchantLevel(4);merchant.setXp(100);merchant.setShowProgressBar(true);merchant.setCanRestock(true);
                    }
                    if(source.getMenu() instanceof AbstractFurnaceMenu furnace) {
                        furnace.setData(0,10);furnace.setData(1,100);furnace.setData(2,20);furnace.setData(3,200);
                    }
                    if(source instanceof AbstractRecipeBookScreen<?> recipe) {
                        source.init(client.getWindow().getGuiScaledWidth(),client.getWindow().getGuiScaledHeight());
                        var ra=(dev.totem.remnant.mixin.client.SidebarAbstractRecipeBookScreenAccessor)recipe;
                        var ba=(dev.totem.remnant.mixin.client.SidebarRecipeBookComponentAccessor)ra.remnant$getRecipeBookComponent();
                        ba.remnant$initVisuals();ba.remnant$setVisible(true);
                        ba.remnant$getSearchBox().setValue("PRIVATE_UNSENT_SIDEBAR_QUERY");
                    }
                    if(source instanceof dev.totem.remnant.mixin.client.SidebarStonecutterScreenAccessor state) {
                        state.remnant$setStartIndex(4);state.remnant$setScrollOffs(0.5F);
                    }
                    if(source instanceof dev.totem.remnant.mixin.client.SidebarLoomScreenAccessor state) {
                        state.remnant$setStartRow(1);state.remnant$setScrollOffs(0.5F);
                    }
                    if(source instanceof dev.totem.remnant.mixin.client.SidebarMerchantScreenAccessor state) {
                        state.remnant$setShopItem(2);state.remnant$setScrollOff(1);
                    }
                    ((BackpackPanelMenuAccess)source.getMenu()).totem$selectBackpackSlot(40);
                    ((BackpackSidebarLayout)source).remnant$firstRow(death?255:0);
                    if(observerLoaded) verifyOwnership(source,provider);
                    sourceRef.set(source);
                    if(source.getMenu() instanceof AnvilMenu anvil) {
                        anvil.getSlot(0).set(new ItemStack(Items.DIAMOND));
                        var draft=new ItemStack(Items.DIAMOND);draft.set(DataComponents.CUSTOM_NAME,Component.literal("PRIVATE_ANVIL_DRAFT"));
                        anvil.getSlot(AnvilMenu.RESULT_SLOT).set(draft);
                    }
                    if(source.getMenu() instanceof MerchantMenu merchant) {
                        var offers=new net.minecraft.world.item.trading.MerchantOffers();
                        for(int offer=0;offer<9;offer++)offers.add(new net.minecraft.world.item.trading.MerchantOffer(new net.minecraft.world.item.trading.ItemCost(Items.EMERALD,3),Optional.empty(),new ItemStack(Items.DIAMOND),2,7,3,0.2F));
                        merchant.setOffers(offers);merchant.setMerchantLevel(4);merchant.setXp(100);merchant.setShowProgressBar(true);merchant.setCanRestock(true);
                    }
                    if(source.getMenu() instanceof AbstractFurnaceMenu furnace) {
                        furnace.setData(0,10);furnace.setData(1,100);furnace.setData(2,20);furnace.setData(3,200);
                    }
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
                if(death && List.of("merchant","stonecutter","loom").contains(variant)) {
                    context.runOnClient(client->{client.setScreenAndShow(sourceRef.get());((BackpackSidebarLayout)sourceRef.get()).remnant$firstRow(0);});
                    context.waitTicks(3);
                    context.runOnClient(client->{
                        var nativeScreen=sourceRef.get();var layout=(BackpackSidebarLayout)nativeScreen;
                        var panelAccess=(BackpackPanelMenuAccess)nativeScreen.getMenu();var slot=nativeScreen.getMenu().getSlot(panelAccess.totem$getBackpackPanelSlotStart());
                        nativeScreen.mouseScrolled(layout.remnant$left()+slot.x+8,layout.remnant$top()+slot.y+8,0,-1);
                        require(layout.remnant$firstRow()>0,"Native scroll override swallowed sidebar input: "+variant);
                        client.setScreenAndShow(null);
                    });
                }
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
                    if(menu instanceof MerchantMenu merchant) {
                        require(merchant.getOffers().size()==9 && merchant.getOffers().getFirst().getUses()==2 && merchant.getTraderLevel()==4 && merchant.getTraderXp()==100,"Merchant state missing");
                    }
                    if(menu instanceof AnvilMenu)require(initial.slots().get(AnvilMenu.RESULT_SLOT).get(DataComponents.CUSTOM_NAME)==null,"Anvil rename draft leaked");
                    if(screen instanceof dev.totem.remnant.mixin.client.SidebarStonecutterScreenAccessor state)
                        require(state.remnant$getStartIndex()==4 && state.remnant$getScrollOffs()==0.5F,"Stonecutter public scroll state missing");
                    if(screen instanceof dev.totem.remnant.mixin.client.SidebarLoomScreenAccessor state)
                        require(state.remnant$getStartRow()==1 && state.remnant$getScrollOffs()==0.5F,"Loom public scroll state missing");
                    if(screen instanceof dev.totem.remnant.mixin.client.SidebarMerchantScreenAccessor state)
                        require(state.remnant$getShopItem()==2 && state.remnant$getScrollOff()==1,"Merchant public selection missing");
                    var nativeData=((dev.totem.remnant.mixin.client.SidebarMenuDataAccessor)menu).remnant$dataSlots();
                    for(int i=0;i<nativeData.size();i++)require(nativeData.get(i).get()==initial.data()[10+i],"Native menu data missing: "+variant);
                    require(provider.capture(screen,2).isEmpty(),"Projection was rebroadcast");
                    var slots=new ArrayList<>(initial.slots());var bag=slots.getLast().copy();
                    var contents=new ArrayList<>(bag.get(DataComponents.CONTAINER).itemCopies().toList());
                    contents.set(death?255:0,new ItemStack(Items.EMERALD,11));bag.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(contents));slots.set(slots.size()-1,bag);
                    var update=new ObserverScreenSnapshot(provider.familyId(),variant,1,2,initial.title(),slots,initial.data(),initial.metadata(),initial.ownerPayload());
                    handle.applySnapshot(update);handle.applySnapshot(initial);
                    handle.applySnapshot(new ObserverScreenSnapshot(provider.familyId(),"wrong",1,3,initial.title(),slots,initial.data(),Map.of(),new byte[0]));
                    require(menu.getSlot(deep).getItem().is(Items.EMERALD)&&menu.getSlot(deep).getItem().getCount()==11,"Exact monotonic update policy failed");
                    if(menu instanceof MerchantMenu merchant) {
                        byte[] trailing=Arrays.copyOf(initial.ownerPayload(),initial.ownerPayload().length+1);
                        for(byte[] invalid:List.of(new byte[]{1},new byte[]{1,33},trailing, new byte[]{2,0})) {
                            handle.applySnapshot(new ObserverScreenSnapshot(provider.familyId(),variant,1,3,initial.title(),initial.slots(),initial.data(),initial.metadata(),invalid));
                            require(menu.getSlot(deep).getItem().getCount()==11 && merchant.getOffers().size()==9,"Invalid merchant payload mutated accepted state");
                        }
                        var recoverySlots=new ArrayList<>(slots);var recoveryBag=recoverySlots.getLast().copy();
                        var recoveryContents=new ArrayList<>(recoveryBag.get(DataComponents.CONTAINER).itemCopies().toList());
                        recoveryContents.set(death?255:0,new ItemStack(Items.EMERALD,13));
                        recoveryBag.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(recoveryContents));
                        recoverySlots.set(recoverySlots.size()-1,recoveryBag);
                        handle.applySnapshot(new ObserverScreenSnapshot(provider.familyId(),variant,1,3,initial.title(),recoverySlots,initial.data(),initial.metadata(),initial.ownerPayload()));
                        require(menu.getSlot(deep).getItem().getCount()==13,"Invalid payload consumed a valid update sequence");
                        handle.applySnapshot(new ObserverScreenSnapshot(provider.familyId(),variant,1,2,initial.title(),slots,initial.data(),initial.metadata(),initial.ownerPayload()));
                        require(menu.getSlot(deep).getItem().getCount()==13,"Stale merchant update restored the previous contents");
                    }
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
