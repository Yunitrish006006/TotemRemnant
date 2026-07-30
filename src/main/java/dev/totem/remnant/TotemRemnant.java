package dev.totem.remnant;

import com.adaptor.deadrecall.api.death.DeathBackpackAddonInventoryProvider;
import com.adaptor.deadrecall.api.death.DeathBackpackAddonInventoryRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.totem.remnant.death.DeathBackpackCaptureLifecycle;
import dev.totem.remnant.death.DeathBackpackFactory;
import dev.totem.remnant.death.DeathBackpackRecoveryService;
import dev.totem.remnant.inventory.ContainerSafetyAdmin;
import dev.totem.remnant.registry.RemnantItemGroups;
import dev.totem.remnant.registry.RemnantItemRegistration;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.lang.reflect.Proxy;

/** Entry point for the standalone death-backpack module. */
public final class TotemRemnant implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("TotemRemnant");
    private static final String TRINKETS_PROVIDER_CLASS =
            "dev.totem.remnant.integration.trinkets.TrinketsDeathBackpackInventoryProvider";

    @Override
    public void onInitialize() {
        RemnantItemRegistration.register();
        RemnantItemGroups.register();
        ContainerSafetyAdmin.register();
        installTrinketsIntegration();
        DeathBackpackFactory.register(contents -> {
            ItemStack backpack = new ItemStack(RemnantItemRegistration.DEATH_BACKPACK);
            backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
            return backpack;
        });
        installDeadRecallTransports();
        LOGGER.info("TotemRemnant initialized without Nexus dependency");
    }

    private static void installTrinketsIntegration() {
        if (!FabricLoader.getInstance().isModLoaded("trinkets_updated")) {
            return;
        }

        try {
            Class<?> providerClass = Class.forName(TRINKETS_PROVIDER_CLASS);
            DeathBackpackAddonInventoryProvider provider =
                    (DeathBackpackAddonInventoryProvider) providerClass.getDeclaredConstructor().newInstance();
            DeathBackpackAddonInventoryRegistry.register(provider);
            LOGGER.info("Enabled death-backpack inventory integration for Trinkets Updated");
        } catch (ReflectiveOperationException | LinkageError exception) {
            throw new IllegalStateException(
                    "Could not initialize Trinkets Updated death-backpack integration",
                    exception
            );
        }
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
