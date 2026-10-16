package dev.totem.remnant.client;

import dev.totem.core.api.v1.client.observer.ObserverScreenProvider;
import dev.totem.remnant.inventory.BackpackPanelMenuAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.world.item.ItemStack;
import java.lang.reflect.Field;
import java.util.*;

/** Invokes real legacy adapter ticks on the client thread and restores all injected test state. */
final class SidebarOwnershipRuntimeAssertions {
    private SidebarOwnershipRuntimeAssertions() { }
    static void verify(AbstractContainerScreen<?> source,ObserverScreenProvider provider) throws ReflectiveOperationException {
        Minecraft client=Minecraft.getInstance();var previous=client.gui.screen();
        Class<?> nativeClient=Class.forName("dev.totem.observer.client.ObserverNativeClient");
        Class<?> transport=Class.forName("dev.totem.observer.client.ObserverOwnedScreenTransportClient");
        String variant=dev.totem.remnant.inventory.BackpackSidebarAdapters.find(source.getMenu()).variant();
        String adapterName=switch(variant) {
            case "inventory","crafting" -> "ObserverNativeCraftingScreenClient";
            case "enchanting" -> "ObserverNativeEnchantingScreenClient";
            case "chest","hopper","furnace","smoker","blast_furnace" -> "ObserverNativeScreenClient";
            case "anvil" -> "ObserverNativeAnvilScreenClient";
            case "merchant" -> "ObserverNativeMerchantScreenClient";
            case "cartography" -> "ObserverCartographyScreenClient";
            case "brewing" -> "ObserverBrewingScreenClient";
            default -> "Observer"+Character.toUpperCase(variant.charAt(0))+variant.substring(1)+"ScreenClient";
        };
        Class<?> adapter=Class.forName("dev.totem.observer.client."+adapterName);
        String openName=switch(variant) {
            case "chest","hopper" -> "targetContainerOpen";
            case "furnace","smoker","blast_furnace" -> "targetFurnaceOpen";
            default -> "targetOpen";
        };
        var state=new LinkedHashMap<Field,Object>();
        for(var pair:List.of(new Object[]{nativeClient,"targetStateEnabled"},new Object[]{nativeClient,"targetScreenCapabilities"},
                new Object[]{transport,"providers"},new Object[]{transport,"ownershipScreen"},new Object[]{transport,"ownershipTick"},
                new Object[]{transport,"ownershipClaim"},new Object[]{adapter,openName},new Object[]{adapter,"nextTargetSequence"},new Object[]{adapter,"lastSnapshotNanos"})) {
            Field f=field((Class<?>)pair[0],(String)pair[1]);state.put(f,f.get(null));
        }
        var panel=((BackpackPanelMenuAccess)source.getMenu()).totem$getBackpackPanel();
        int selected=panel.selectedInventorySlot();var bag=panel.playerInventory().getItem(selected);
        var tick=adapter.getDeclaredMethod("tickTarget",Minecraft.class);tick.setAccessible(true);
        long ownerCapability=field(Class.forName("dev.totem.observer.network.ObserverOwnedScreenCapability"),"CAPABILITY").getLong(null);
        try {
            client.setScreenAndShow(source);
            field(nativeClient,"targetStateEnabled").setBoolean(null,true);
            field(nativeClient,"targetScreenCapabilities").setLong(null,-1L & ~ownerCapability);
            field(transport,"providers").set(null,Map.of(provider.familyId(),provider));
            field(adapter,openName).setBoolean(null,true);invalidate(transport);
            ObserverPacketProbe.reset();tick.invoke(null,client);
            require(!field(adapter,openName).getBoolean(null)&&ObserverPacketProbe.sends()==1,"Owned screen did not close prior legacy state exactly once");
            tick.invoke(null,client);require(ObserverPacketProbe.sends()==1,"Repeated owned tick resent legacy close");
            panel.playerInventory().setItem(selected,ItemStack.EMPTY);panel.refreshAfterExternalCommit();invalidate(transport);
            tick.invoke(null,client);
            require(field(adapter,openName).getBoolean(null)&&ObserverPacketProbe.sends()==2,"No-bag screen failed to resume native capture");
            panel.playerInventory().setItem(selected,bag);((BackpackPanelMenuAccess)source.getMenu()).totem$selectBackpackSlot(selected);invalidate(transport);
            tick.invoke(null,client);
            require(!field(adapter,openName).getBoolean(null)&&ObserverPacketProbe.sends()==3,"Offhand bag appearance did not close native state");
        } finally {
            panel.playerInventory().setItem(selected,bag);((BackpackPanelMenuAccess)source.getMenu()).totem$selectBackpackSlot(selected);
            for(var entry:state.entrySet())entry.getKey().set(null,entry.getValue());
            client.setScreenAndShow(previous);ObserverPacketProbe.reset();
        }
    }
    private static void invalidate(Class<?> owner)throws ReflectiveOperationException {field(owner,"ownershipTick").setLong(null,Long.MIN_VALUE);}
    private static Field field(Class<?> owner,String name)throws ReflectiveOperationException {var f=owner.getDeclaredField(name);f.setAccessible(true);return f;}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message+": "+ObserverPacketProbe.packetTypes());}
}
