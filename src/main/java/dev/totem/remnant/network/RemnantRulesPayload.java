package dev.totem.remnant.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client synchronization for Remnant rules used by inventory prediction and the live manual. */
public record RemnantRulesPayload(
        boolean generateDeathBackpacks,
        boolean deathBackpackOwnerPickupOnly,
        boolean preventPortableContainerNesting)
        implements CustomPacketPayload {
    public static final Type<RemnantRulesPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("totem", "remnant_rules")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, RemnantRulesPayload> CODEC =
            CustomPacketPayload.codec(RemnantRulesPayload::write, RemnantRulesPayload::read);

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(generateDeathBackpacks);
        buffer.writeBoolean(deathBackpackOwnerPickupOnly);
        buffer.writeBoolean(preventPortableContainerNesting);
    }

    private static RemnantRulesPayload read(RegistryFriendlyByteBuf buffer) {
        return new RemnantRulesPayload(
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
