package dev.totem.remnant.client;

import dev.totem.remnant.client.screen.BackpackSidebarLayout;
import dev.totem.remnant.inventory.BackpackPanelMenuAccess;
import dev.totem.remnant.registry.RemnantItemRegistration;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public final class BackpackWorkstationClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try(var world=context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();context.getInput().resizeWindow(1280,720);
            for(String variant:List.of("crafting","enchanting","chest")) for(boolean death:List.of(false,true)) {
                world.getServer().runOnServer(server->{
                    var player=server.getPlayerList().getPlayers().getFirst();player.closeContainer();
                    player.getInventory().clearContent();
                    var bag=new ItemStack(death?RemnantItemRegistration.DEATH_BACKPACK:RemnantItemRegistration.BACKPACK_BASIC);
                    bag.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(new ItemStack(Items.LAPIS_LAZULI,7))));
                    player.getInventory().setItem(0,bag);
                    player.openMenu(new SimpleMenuProvider((id,inv,p)->switch(variant) {
                        case "crafting"->new CraftingMenu(id,inv);
                        case "enchanting"->new EnchantmentMenu(id,inv);
                        default->ChestMenu.threeRows(id,inv);
                    },Component.literal(variant)));
                });
                context.waitFor(client->client.gui.screen() instanceof AbstractContainerScreen<?> screen
                        && BackpackPanelMenuAccess.supports(screen.getMenu()) && screen.getMenu().containerId>0);
                context.waitTicks(10);
                int start=context.computeOnClient(client->((BackpackPanelMenuAccess)((AbstractContainerScreen<?>)client.gui.screen()).getMenu()).totem$getBackpackPanelSlotStart());
                context.takeScreenshot("remnant-sidebar-before-"+variant+(death?"-death":"-ordinary"));
                System.out.println("SIDEBAR_BEFORE "+context.computeOnClient(client->state(((AbstractContainerScreen<?>)client.gui.screen()).getMenu())));
                click(context,start);context.waitTicks(10);
                System.out.println("SIDEBAR_AFTER_WITHDRAW "+context.computeOnClient(client->state(((AbstractContainerScreen<?>)client.gui.screen()).getMenu())));
                world.getServer().runOnServer(server->System.out.println("SIDEBAR_SERVER_WITHDRAW "+state(server.getPlayerList().getPlayers().getFirst().containerMenu)));
                click(context,variant.equals("chest")?0:1);context.waitTicks(10);
                world.getServer().runOnServer(server->{
                    var menu=server.getPlayerList().getPlayers().getFirst().containerMenu;
                    var panel=((BackpackPanelMenuAccess)menu).totem$getBackpackPanel();
                    require(menu.getSlot(variant.equals("chest")?0:1).getItem().getCount()==7 && menu.getCarried().isEmpty()
                            && panel.selectedBackpack().get(DataComponents.CONTAINER).nonEmptyItemCopyStream().findAny().isEmpty(),
                            "Native input withdrawal/functional placement diverged on server: "+variant+" death="+death+" "+state(menu));
                });
                click(context,variant.equals("chest")?0:1);context.waitTicks(5);click(context,start);context.waitTicks(10);
                world.getServer().runOnServer(server->{
                    var menu=server.getPlayerList().getPlayers().getFirst().containerMenu;
                    var panel=((BackpackPanelMenuAccess)menu).totem$getBackpackPanel();
                    require(menu.getSlot(variant.equals("chest")?0:1).getItem().isEmpty() && menu.getCarried().isEmpty()
                            && panel.getItem(0).getCount()==7,"Returning native input lost items on server: "+variant);
                });
                context.getInput().setCursorPos(10,10);context.waitTicks(2);
                context.takeScreenshot("remnant-sidebar-native-"+variant+(death?"-death":"-ordinary"));
                context.runOnClient(client->client.gui.screen().onClose());context.waitForScreen(null);
                var closed=new java.util.concurrent.atomic.AtomicBoolean();
                for(int attempt=0;attempt<40&&!closed.get();attempt++) {
                    context.waitTicks(1);
                    world.getServer().runOnServer(server->{var player=server.getPlayerList().getPlayers().getFirst();closed.set(player.containerMenu==player.inventoryMenu);});
                }
                require(closed.get(),"Server did not acknowledge closing the previous menu");
            }
        }
    }
    private static void click(ClientGameTestContext context,int index) {
        int[] point=context.computeOnClient(client->{
            var screen=(AbstractContainerScreen<?>)client.gui.screen();var layout=(BackpackSidebarLayout)screen;var slot=screen.getMenu().getSlot(index);
            double sx=(double)client.getWindow().getScreenWidth()/client.getWindow().getGuiScaledWidth();
            double sy=(double)client.getWindow().getScreenHeight()/client.getWindow().getGuiScaledHeight();
            return new int[]{(int)Math.round((layout.remnant$left()+slot.x+8)*sx),(int)Math.round((layout.remnant$top()+slot.y+8)*sy)};
        });
        context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(2);
        context.runOnClient(client->{
            System.out.println("SIDEBAR_POINTER requested="+point[0]+","+point[1]+" actual="
                    +client.mouseHandler.getScaledXPos(client.getWindow())+","+client.mouseHandler.getScaledYPos(client.getWindow())
                    +" screen="+client.gui.screen().getClass().getName()+" readonly="
                    +(client.gui.screen() instanceof dev.totem.core.api.v1.client.observer.ObserverReadOnlyScreen marker&&marker.totem$isObserverReadOnly()));
            ObserverPacketProbe.reset();
        });
        context.getInput().pressMouse(1);context.waitTicks(2);
        context.runOnClient(client->System.out.println("SIDEBAR_CLICK packets="+ObserverPacketProbe.packetTypes()));
    }
    private static String state(AbstractContainerMenu menu) {
        var access=(BackpackPanelMenuAccess)menu;var panel=access.totem$getBackpackPanel();
        var slot=menu.getSlot(access.totem$getBackpackPanelSlotStart());
        return "id="+menu.containerId+" state="+menu.getStateId()+" selected="+panel.selectedInventorySlot()
                +" panel="+panel.getItem(0).getCount()+" xy="+slot.x+","+slot.y+" carried="+menu.getCarried().getCount()
                +" functional="+menu.getSlot(menu instanceof ChestMenu?0:1).getItem().getCount();
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
