package dev.totem.remnant;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.totem.remnant.death.DeathBackpackCaptureLifecycle;
import dev.totem.remnant.death.DeathBackpackFactory;
import dev.totem.remnant.death.DeathBackpackRecoveryService;
import dev.totem.remnant.registry.RemnantItemRegistration;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.lang.reflect.Proxy;

/** Entry point for the standalone death-backpack module. */
public final class TotemRemnant implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("TotemRemnant");

    @Override
    public void onInitialize() {
        RemnantItemRegistration.register();
        DeathBackpackFactory.register(contents -> {
            ItemStack backpack = new ItemStack(RemnantItemRegistration.DEATH_BACKPACK);
            backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
            return backpack;
        });
        installDeadRecallTransports();
        LOGGER.info("TotemRemnant initialized without Nexus dependency");
    }

    private static void installDeadRecallTransports() {
        installDeadRecallCaptureTransport();
        installDeadRecallRecoveryTransport();
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

    private static void installDeadRecallRecoveryTransport() {
        try {
            Class<?> transport = Class.forName("com.adaptor.deadrecall.core.api.DeathBackpackRecoveryTransport");
            Object adapter = Proxy.newProxyInstance(TotemRemnant.class.getClassLoader(), new Class<?>[] {transport},
                    (proxy, method, arguments) -> method.getName().equals("recover") && arguments != null && arguments.length == 2
                            ? DeathBackpackRecoveryService.recoverBoundNode(
                                    (net.minecraft.server.level.ServerPlayer) arguments[0],
                                    (net.minecraft.world.item.ItemStack) arguments[1])
                            : null);
            transport.getMethod("register", transport).invoke(null, adapter);
        } catch (ClassNotFoundException ignored) {
            // Standalone Remnant has no DeadRecall compatibility facade.
        } catch (ReflectiveOperationException exception) {
            LOGGER.warn("Unable to install DeadRecall recovery transport", exception);
        }
    }
}
