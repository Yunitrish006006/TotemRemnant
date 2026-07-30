package dev.totem.remnant.gametest;

import dev.totem.remnant.inventory.ContainerNestingDiagnostics;
import dev.totem.remnant.inventory.ContainerNestingDiagnostics.Direction;
import dev.totem.remnant.inventory.ContainerNestingDiagnostics.ScanReport;
import dev.totem.remnant.registry.RemnantItemRegistration;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.List;

public final class ContainerNestingDiagnosticsGameTest {
    @GameTest(maxTicks = 20)
    public void detectsBothInvalidNestingDirectionsWithoutMutation(GameTestHelper helper) {
        ItemStack innerBackpack = new ItemStack(RemnantItemRegistration.BACKPACK_STANDARD);
        innerBackpack.set(DataComponents.CUSTOM_NAME, Component.literal("Legacy child"));

        ItemStack shulker = new ItemStack(Items.SHULKER_BOX);
        shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(innerBackpack)));

        ItemStack bundle = new ItemStack(Items.BUNDLE);
        bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(
                ItemStackTemplate.fromNonEmptyStack(
                        new ItemStack(RemnantItemRegistration.DEATH_BACKPACK)
                )
        )));

        ItemStack rootBackpack = new ItemStack(RemnantItemRegistration.BACKPACK_ADVANCED);
        rootBackpack.set(DataComponents.CUSTOM_NAME, Component.literal("Legacy root"));
        rootBackpack.set(
                DataComponents.CONTAINER,
                ItemContainerContents.fromItems(List.of(shulker, bundle))
        );
        ItemStack before = rootBackpack.copy();

        ScanReport report =
                ContainerNestingDiagnostics.scanItemStack("tester", "inventory[0]", rootBackpack);

        require(helper, report.totalFindings() == 4,
                "Expected four bidirectional invalid nesting findings, found "
                        + report.totalFindings());
        require(helper, report.findings().stream().filter(finding ->
                        finding.direction() == Direction.RESTRICTED_CONTAINER_INSIDE_BACKPACK).count() == 2,
                "Did not report both portable containers inside the root backpack");
        require(helper, report.findings().stream().filter(finding ->
                        finding.direction() == Direction.BACKPACK_INSIDE_PORTABLE_CONTAINER).count() == 2,
                "Did not report both backpacks inside portable containers");
        require(helper, ItemStack.isSameItemSameComponents(before, rootBackpack),
                "Read-only scan mutated the root backpack or nested components");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void ordinaryBackpackContentsProduceCleanReport(GameTestHelper helper) {
        ItemStack backpack = new ItemStack(RemnantItemRegistration.BACKPACK_BASIC);
        backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(
                new ItemStack(Items.DIRT, 32),
                new ItemStack(Items.DIAMOND, 3)
        )));

        ScanReport report =
                ContainerNestingDiagnostics.scanItemStack("tester", "inventory[1]", backpack);

        require(helper, report.clean(), "Ordinary contents were reported as invalid nesting");
        require(helper, !report.truncated(), "Small clean scan was unexpectedly truncated");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void depthLimitTruncatesPathologicalLegacyData(GameTestHelper helper) {
        ItemStack nested = new ItemStack(RemnantItemRegistration.BACKPACK_BASIC);
        for (int depth = 0; depth < 20; depth++) {
            ItemStack parent = new ItemStack(RemnantItemRegistration.BACKPACK_BASIC);
            parent.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(nested)));
            nested = parent;
        }

        ScanReport report =
                ContainerNestingDiagnostics.scanItemStack("tester", "inventory[2]", nested);

        require(helper, report.truncated(),
                "Deep legacy nesting did not activate the scanner depth guard");
        require(helper, report.scannedStacks() <= 18,
                "Depth guard scanned too many nested stacks");
        helper.succeed();
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }
}
