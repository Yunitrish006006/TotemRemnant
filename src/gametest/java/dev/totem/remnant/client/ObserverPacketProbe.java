package dev.totem.remnant.client;

/** Test-only counter for packets attempted by a read-only Observer screen. */
public final class ObserverPacketProbe {
    private static int sends;
    private static int nonBackgroundSends;
    private static final java.util.List<String> packetTypes = new java.util.ArrayList<>();
    private static dev.totem.remnant.network.BackpackSortPayload lastSort;
    private static dev.totem.remnant.network.SortHoveredBackpackPayload lastHoveredSort;
    private ObserverPacketProbe() { }
    public static void reset() { sends = 0; nonBackgroundSends = 0; lastSort = null; lastHoveredSort = null; packetTypes.clear(); }
    public static void record() { sends++; }
    public static void record(net.minecraft.network.protocol.Packet<?> packet) {
        record();
        if (!(packet instanceof net.minecraft.network.protocol.game.ServerboundClientTickEndPacket)
                && !(packet instanceof net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket)
                && !(packet instanceof net.minecraft.network.protocol.game.ServerboundMovePlayerPacket)) nonBackgroundSends++;
        if (packetTypes.size() < 32) packetTypes.add(packet.getClass().getName());
        if (packet instanceof net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket custom
                && custom.payload() instanceof dev.totem.remnant.network.BackpackSortPayload sort) lastSort = sort;
        if (packet instanceof net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket custom
                && custom.payload() instanceof dev.totem.remnant.network.SortHoveredBackpackPayload sort) lastHoveredSort = sort;
    }
    public static dev.totem.remnant.network.BackpackSortPayload lastSort() { return lastSort; }
    public static dev.totem.remnant.network.SortHoveredBackpackPayload lastHoveredSort() { return lastHoveredSort; }
    public static java.util.List<String> packetTypes() { return java.util.List.copyOf(packetTypes); }
    public static int nonBackgroundSends() { return nonBackgroundSends; }
    public static int sends() { return sends; }
}
