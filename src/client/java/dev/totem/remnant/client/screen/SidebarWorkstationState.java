package dev.totem.remnant.client.screen;

import dev.totem.core.api.v1.client.observer.ObserverScreenSnapshot;
import dev.totem.remnant.mixin.client.*;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffers;
import java.util.Map;

/** Bounded public native state; never captures anvil edit text or other unsent input. */
final class SidebarWorkstationState {
    private static final int MAX_OFFERS = 32;
    private SidebarWorkstationState() { }

    static void capture(AbstractContainerScreen<?> screen, Map<String,String> metadata) {
        if (screen instanceof SidebarStonecutterScreenAccessor state) {
            metadata.put("stoneScroll", Float.toString(state.remnant$getScrollOffs()));
            metadata.put("stoneStart", Integer.toString(state.remnant$getStartIndex()));
        }
        if (screen instanceof SidebarLoomScreenAccessor state) {
            metadata.put("loomScroll", Float.toString(state.remnant$getScrollOffs()));
            metadata.put("loomStart", Integer.toString(state.remnant$getStartRow()));
        }
        if (screen.getMenu() instanceof MerchantMenu menu && screen instanceof SidebarMerchantScreenAccessor state) {
            metadata.put("tradeSelected", Integer.toString(state.remnant$getShopItem()));
            metadata.put("tradeScroll", Integer.toString(state.remnant$getScrollOff()));
            metadata.put("tradeLevel", Integer.toString(menu.getTraderLevel()));
            metadata.put("tradeXp", Integer.toString(menu.getTraderXp()));
            metadata.put("tradeProgress", Boolean.toString(menu.showProgressBar()));
            metadata.put("tradeRestock", Boolean.toString(menu.canRestock()));
        }
    }

    static byte[] capturePayload(AbstractContainerScreen<?> screen) {
        if (!(screen.getMenu() instanceof MerchantMenu menu)) return new byte[0];
        if (menu.getOffers().size() > MAX_OFFERS) return null;
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(256, ObserverScreenSnapshot.MAX_OWNER_PAYLOAD),
                Minecraft.getInstance().level.registryAccess());
        try {
            buffer.writeVarInt(1); // Merchant variant payload format, within remnant_sidebar@1.
            MerchantOffers.STREAM_CODEC.encode(buffer, menu.getOffers());
            byte[] result = new byte[buffer.readableBytes()]; buffer.readBytes(result); return result;
        } catch (RuntimeException invalid) { return null; }
        finally { buffer.release(); }
    }

    static boolean applyPayload(AbstractContainerScreen<?> screen, ObserverScreenSnapshot snapshot) {
        byte[] payload = snapshot.ownerPayload();
        if (!(screen.getMenu() instanceof MerchantMenu menu)) return payload.length == 0;
        if (payload.length == 0) return false;
        var buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload), Minecraft.getInstance().level.registryAccess());
        try {
            if (buffer.readVarInt() != 1) return false;
            int position = buffer.readerIndex(); int count = buffer.readVarInt();
            if (count < 0 || count > MAX_OFFERS) return false;
            buffer.readerIndex(position);
            MerchantOffers offers = MerchantOffers.STREAM_CODEC.decode(buffer);
            if (buffer.isReadable()) return false;
            menu.setOffers(offers);
            menu.setMerchantLevel(number(snapshot.metadata(), "tradeLevel", 5));
            menu.setXp(number(snapshot.metadata(), "tradeXp", Integer.MAX_VALUE));
            menu.setShowProgressBar(Boolean.parseBoolean(snapshot.metadata().get("tradeProgress")));
            menu.setCanRestock(Boolean.parseBoolean(snapshot.metadata().get("tradeRestock")));
            menu.setSelectionHint(number(snapshot.metadata(), "tradeSelected", Math.max(0, offers.size()-1)));
            return true;
        } catch (RuntimeException invalid) { return false; }
        finally { buffer.release(); }
    }

    static void apply(AbstractContainerScreen<?> screen, Map<String,String> metadata) {
        if (screen instanceof SidebarStonecutterScreenAccessor state) {
            state.remnant$setScrollOffs(fraction(metadata, "stoneScroll"));
            state.remnant$setStartIndex(number(metadata, "stoneStart", 4096));
        }
        if (screen instanceof SidebarLoomScreenAccessor state) {
            state.remnant$setScrollOffs(fraction(metadata, "loomScroll"));
            state.remnant$setStartRow(number(metadata, "loomStart", 4096));
        }
        if (screen.getMenu() instanceof MerchantMenu menu && screen instanceof SidebarMerchantScreenAccessor state) {
            state.remnant$setShopItem(number(metadata, "tradeSelected", Math.max(0,menu.getOffers().size()-1)));
            state.remnant$setScrollOff(number(metadata, "tradeScroll", Math.max(0,menu.getOffers().size()-7)));
        }
    }

    private static int number(Map<String,String> metadata, String key, int maximum) {
        try { return Math.clamp(Integer.parseInt(metadata.getOrDefault(key,"0")),0,maximum); }
        catch (NumberFormatException invalid) { return 0; }
    }
    private static float fraction(Map<String,String> metadata, String key) {
        try { float value=Float.parseFloat(metadata.getOrDefault(key,"0")); return Float.isFinite(value)?Math.clamp(value,0,1):0; }
        catch (NumberFormatException invalid) { return 0; }
    }
}
