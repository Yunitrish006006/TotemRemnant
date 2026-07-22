package dev.totem.remnant;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Entry point for the standalone death-backpack module. */
public final class TotemRemnant implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("TotemRemnant");

    @Override
    public void onInitialize() {
        LOGGER.info("TotemRemnant initialized without Nexus dependency");
    }
}
