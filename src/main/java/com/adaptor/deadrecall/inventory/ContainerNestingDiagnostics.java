package com.adaptor.deadrecall.inventory;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Read-only diagnostics for legacy portable-container nesting.
 *
 * <p>The scanner never writes components, moves stacks or repairs data. Limits protect the server
 * from pathologically deep or oversized item-component graphs.</p>
 */
public final class ContainerNestingDiagnostics {
    static final int MAX_DEPTH = 16;
    static final int MAX_SCANNED_STACKS = 4_096;
    static final int MAX_RETAINED_FINDINGS = 256;

    private ContainerNestingDiagnostics() {
    }

    public static ScanReport scanPlayer(ServerPlayer player) {
        List<RootStack> roots = collectPlayerRoots(player);
        return scanRoots(player.getGameProfile().name(), roots);
    }

    public static ScanReport scanItemStack(String owner, String location, ItemStack stack) {
        return scanRoots(owner, List.of(new RootStack(location, stack)));
    }

    public static ScanReport scanRoots(String owner, List<RootStack> roots) {
        Accumulator accumulator = new Accumulator(owner, roots.size());
        for (RootStack root : roots) {
            scanStack(root.stack(), root.location(), 0, accumulator);
            if (accumulator.scannedStacks >= MAX_SCANNED_STACKS) {
                accumulator.truncated = true;
                break;
            }
        }
        return accumulator.toReport();
    }

    public static ScanReport merge(List<ScanReport> reports) {
        int roots = 0;
        int stacks = 0;
        int totalFindings = 0;
        boolean truncated = false;
        List<Finding> retained = new ArrayList<>();
        for (ScanReport report : reports) {
            roots += report.scannedRoots();
            stacks += report.scannedStacks();
            totalFindings += report.totalFindings();
            truncated |= report.truncated();
            int remaining = MAX_RETAINED_FINDINGS - retained.size();
            if (remaining > 0) {
                retained.addAll(report.findings().subList(0, Math.min(remaining, report.findings().size())));
            }
        }
        truncated |= totalFindings > retained.size();
        return new ScanReport("*", roots, stacks, totalFindings, List.copyOf(retained), truncated);
    }

    private static List<RootStack> collectPlayerRoots(ServerPlayer player) {
        List<RootStack> roots = new ArrayList<>();
        Set<ItemStack> seen = Collections.newSetFromMap(new IdentityHashMap<>());

        Container inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            addRoot(roots, seen, "inventory[" + slot + "]", inventory.getItem(slot));
        }

        Container crafting = player.inventoryMenu.getCraftSlots();
        for (int slot = 0; slot < crafting.getContainerSize(); slot++) {
            addRoot(roots, seen, "crafting[" + slot + "]", crafting.getItem(slot));
        }

        AbstractContainerMenu menu = player.containerMenu;
        addRoot(roots, seen, "cursor", menu.getCarried());
        for (int slotIndex = 0; slotIndex < menu.slots.size(); slotIndex++) {
            Slot slot = menu.slots.get(slotIndex);
            addRoot(roots, seen, "menu:" + menu.getClass().getSimpleName() + "[" + slotIndex + "]", slot.getItem());
        }
        return List.copyOf(roots);
    }

    private static void addRoot(List<RootStack> roots, Set<ItemStack> seen, String location, ItemStack stack) {
        if (stack == null || stack.isEmpty() || !seen.add(stack)) {
            return;
        }
        roots.add(new RootStack(location, stack));
    }

    private static void scanStack(ItemStack parent, String path, int depth, Accumulator accumulator) {
        if (parent == null || parent.isEmpty()) {
            return;
        }
        if (accumulator.scannedStacks >= MAX_SCANNED_STACKS) {
            accumulator.truncated = true;
            return;
        }
        accumulator.scannedStacks++;

        List<ChildStack> children = children(parent);
        if (children.isEmpty()) {
            return;
        }
        if (depth >= MAX_DEPTH) {
            accumulator.truncated = true;
            return;
        }

        boolean parentBackpack = PortableContainerPolicy.isBackpack(parent);
        boolean parentOtherPortable = PortableContainerPolicy.isRestrictedPortableContainer(parent) && !parentBackpack;
        for (ChildStack child : children) {
            ItemStack childStack = child.stack();
            if (childStack.isEmpty()) {
                continue;
            }

            Direction direction = null;
            if (parentBackpack && PortableContainerPolicy.isRestrictedPortableContainer(childStack)) {
                direction = Direction.RESTRICTED_CONTAINER_INSIDE_BACKPACK;
            } else if (parentOtherPortable && PortableContainerPolicy.isBackpack(childStack)) {
                direction = Direction.BACKPACK_INSIDE_PORTABLE_CONTAINER;
            }

            String childPath = path + "/" + child.component() + "[" + child.index() + "]";
            if (direction != null) {
                accumulator.addFinding(new Finding(
                        accumulator.owner,
                        childPath,
                        itemId(parent),
                        itemId(childStack),
                        depth + 1,
                        direction
                ));
            }
            scanStack(childStack, childPath, depth + 1, accumulator);
            if (accumulator.scannedStacks >= MAX_SCANNED_STACKS) {
                accumulator.truncated = true;
                return;
            }
        }
    }

    private static List<ChildStack> children(ItemStack stack) {
        List<ChildStack> children = new ArrayList<>();

        ItemContainerContents container = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        int containerIndex = 0;
        for (ItemStack child : container.nonEmptyItemCopyStream().toList()) {
            children.add(new ChildStack("container", containerIndex++, child));
        }

        BundleContents bundle = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        int bundleIndex = 0;
        for (ItemStack child : bundle.itemCopyStream().toList()) {
            children.add(new ChildStack("bundle", bundleIndex++, child));
        }
        return children;
    }

    private static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public enum Direction {
        RESTRICTED_CONTAINER_INSIDE_BACKPACK,
        BACKPACK_INSIDE_PORTABLE_CONTAINER
    }

    public record RootStack(String location, ItemStack stack) {
    }

    public record Finding(
            String owner,
            String path,
            String parentItemId,
            String childItemId,
            int depth,
            Direction direction
    ) {
    }

    public record ScanReport(
            String owner,
            int scannedRoots,
            int scannedStacks,
            int totalFindings,
            List<Finding> findings,
            boolean truncated
    ) {
        public boolean clean() {
            return totalFindings == 0;
        }
    }

    private record ChildStack(String component, int index, ItemStack stack) {
    }

    private static final class Accumulator {
        private final String owner;
        private final int scannedRoots;
        private int scannedStacks;
        private int totalFindings;
        private boolean truncated;
        private final List<Finding> findings = new ArrayList<>();

        private Accumulator(String owner, int scannedRoots) {
            this.owner = owner;
            this.scannedRoots = scannedRoots;
        }

        private void addFinding(Finding finding) {
            totalFindings++;
            if (findings.size() < MAX_RETAINED_FINDINGS) {
                findings.add(finding);
            } else {
                truncated = true;
            }
        }

        private ScanReport toReport() {
            return new ScanReport(owner, scannedRoots, scannedStacks, totalFindings, List.copyOf(findings), truncated);
        }
    }
}
