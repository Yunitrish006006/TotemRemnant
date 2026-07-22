package dev.totem.remnant;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.totem.remnant.death.DeathBackpackCaptureLifecycle;

import java.lang.reflect.Proxy;

/** Entry point for the standalone death-backpack module. */
public final class TotemRemnant implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("TotemRemnant");

    @Override
    public void onInitialize() {
        installDeadRecallCaptureTransport();
        LOGGER.info("TotemRemnant initialized without Nexus dependency");
    }

    private static void installDeadRecallCaptureTransport() {
        try {
            Class<?> transport = Class.forName("com.adaptor.deadrecall.core.api.DeathBackpackCaptureTransport");
            Object adapter = Proxy.newProxyInstance(TotemRemnant.class.getClassLoader(), new Class<?>[] {transport},
                    (proxy, method, arguments) -> method.getName().equals("commit") && arguments != null && arguments.length == 4
                            ? DeathBackpackCaptureLifecycle.commit(
                                    (net.minecraft.server.level.ServerPlayer) arguments[0],
                                    (net.minecraft.server.level.ServerLevel) arguments[1],
                                    (net.minecraft.core.BlockPos) arguments[2],
                                    (java.util.List<net.minecraft.world.item.ItemStack>) arguments[3])
                            : null);
            transport.getMethod("register", transport).invoke(null, adapter);
        } catch (ClassNotFoundException ignored) {
            // Standalone Remnant has no DeadRecall compatibility facade.
        } catch (ReflectiveOperationException exception) {
            LOGGER.warn("Unable to install DeadRecall capture transport", exception);
        }
    }
}
