package dev.totem.remnant.client;

/** Test-only counter for packets attempted by a read-only Observer screen. */
public final class ObserverPacketProbe {
    private static int sends;
    private static dev.totem.remnant.network.BackpackSortPayload lastSort;
    private ObserverPacketProbe() { }
    public static void reset() { sends = 0; lastSort = null; }
    public static void record() { sends++; }
    public static void record(net.minecraft.network.protocol.Packet<?> packet) {
        record();
        if (packet instanceof net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket custom
                && custom.payload() instanceof dev.totem.remnant.network.BackpackSortPayload sort) lastSort = sort;
    }
    public static dev.totem.remnant.network.BackpackSortPayload lastSort() { return lastSort; }
    public static int sends() { return sends; }
}
