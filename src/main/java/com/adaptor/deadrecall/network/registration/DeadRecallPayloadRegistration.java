package com.adaptor.deadrecall.network.registration;

public final class DeadRecallPayloadRegistration {
    private DeadRecallPayloadRegistration() {
    }

    public static void register() {
        registerTypes();
        registerReceivers();
    }

    private static void registerTypes() {
        // Keep this sequence aligned with the legacy monolithic initializer.
        TotemDiscordBridgePayloadRegistration.registerServerboundTypes();
        LegacyContainerPayloadRegistration.registerServerboundTypes();
        TotemAutomataPayloadRegistration.registerServerboundTypes();
        TotemNexusPayloadRegistration.registerServerboundTypes();

        TotemDiscordBridgePayloadRegistration.registerClientboundTypes();
        TotemAutomataPayloadRegistration.registerClientboundTypes();
        TotemNexusPayloadRegistration.registerClientboundTypes();
    }

    private static void registerReceivers() {
        // Receiver order is also part of the compatibility-preserving extraction.
        TotemDiscordBridgePayloadRegistration.registerReceivers();
        LegacyContainerPayloadRegistration.registerReceivers();
        TotemAutomataPayloadRegistration.registerReceivers();
        TotemNexusPayloadRegistration.registerReceivers();
    }
}
