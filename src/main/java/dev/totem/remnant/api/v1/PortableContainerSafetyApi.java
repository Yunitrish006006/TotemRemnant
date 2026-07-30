package dev.totem.remnant.api.v1;

import dev.totem.remnant.inventory.PortableContainerPolicy;
import dev.totem.remnant.inventory.PortableContainerDiagnostics;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

/**
 * Stable optional-integration facade for Remnant's portable-container policy.
 *
 * <p>Other feature modules may call this through an optional adapter without linking to Remnant
 * implementation classes.</p>
 */
public final class PortableContainerSafetyApi {
    private PortableContainerSafetyApi() {
    }

    public static boolean isRestrictedPortableContainer(ItemStack stack) {
        return PortableContainerPolicy.isRestrictedPortableContainer(stack);
    }

    public static boolean mayInsertIntoBackpack(ItemStack incoming) {
        return PortableContainerPolicy.mayInsertIntoBackpack(incoming);
    }

    public static boolean mayInsertIntoPortableContainer(ItemStack incoming) {
        return PortableContainerPolicy.mayInsertIntoPortableContainer(incoming);
    }

    public static boolean mayInsertIntoContainer(Container target, ItemStack incoming) {
        return PortableContainerPolicy.mayInsertIntoContainer(target, incoming);
    }

    /**
     * Records an automated insertion rejected by this policy.
     *
     * <p>The implementation is rate-limited and ignores non-block containers, so optional
     * automation modules can call it after a {@code false} policy result.</p>
     */
    public static void reportRejectedAutomation(
            Container target,
            ItemStack incoming,
            String route
    ) {
        if (target instanceof ShulkerBoxBlockEntity shulker) {
            PortableContainerDiagnostics.logRejectedAutomation(
                    shulker.getLevel(),
                    shulker.getBlockPos(),
                    incoming,
                    route
            );
        }
    }
}
