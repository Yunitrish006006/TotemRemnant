package dev.totem.remnant;

import com.adaptor.totem.api.death.DeathBackpackAddonInventoryProvider;
import com.adaptor.totem.api.death.DeathBackpackAddonInventoryRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.totem.remnant.death.DeathBackpackFactory;
import dev.totem.remnant.death.SoulboundDeathItemRetention;
import dev.totem.remnant.echo.EchoShardCrystallization;
import dev.totem.remnant.inventory.ContainerSafetyAdmin;
import dev.totem.remnant.manual.RemnantManual;
import dev.totem.remnant.manual.RemnantManualRecipeSync;
import dev.totem.remnant.network.BackpackPanelPayloadRegistration;
import dev.totem.remnant.registry.RemnantItemGroups;
import dev.totem.remnant.registry.RemnantItemRegistration;
import dev.totem.remnant.registry.BackpackMenuRegistration;
import dev.totem.remnant.registry.RemnantGameRules;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Entry point for the standalone death-backpack module. */
public final class TotemRemnant implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("TotemRemnant");
    private static final String TRINKETS_PROVIDER_CLASS =
            "dev.totem.remnant.integration.trinkets.TrinketsDeathBackpackInventoryProvider";

    @Override
    public void onInitialize() {
        RemnantGameRules.register();
        RemnantItemRegistration.register();
        BackpackMenuRegistration.register();
        BackpackPanelPayloadRegistration.register();
        RemnantItemGroups.register();
        RemnantManualRecipeSync.register();
        RemnantManual.register();
        EchoShardCrystallization.register();
        ContainerSafetyAdmin.register();
        installTrinketsIntegration();
        DeathBackpackFactory.register(contents -> {
            ItemStack backpack = new ItemStack(RemnantItemRegistration.DEATH_BACKPACK);
            backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
            return backpack;
        });
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) {
                SoulboundDeathItemRetention.restoreAfterRespawn(newPlayer);
            }
        });
        ServerPlayerEvents.JOIN.register(SoulboundDeathItemRetention::restoreAfterRespawn);
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
}
