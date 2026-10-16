package dev.totem.remnant.item;

import dev.totem.remnant.inventory.BackpackItemInteraction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.List;
import java.util.Optional;

/** A bounded visual projection of real container slots, never a second contents store. */
public record BackpackTooltip(List<ItemStackTemplate> items, int selectedIndex) implements TooltipComponent {
    public static final int PAGE_SIZE = 12;

    public BackpackTooltip {
        items = List.copyOf(items);
        if (items.isEmpty() || selectedIndex < 0 || selectedIndex >= items.size())
            throw new IllegalArgumentException("Backpack tooltip needs a valid selected item");
    }

    public static Optional<TooltipComponent> from(ItemStack bag) {
        var choices = BackpackItemInteraction.choices(bag);
        if (choices.isEmpty()) return Optional.empty();
        var contents = bag.get(DataComponents.CONTAINER).itemCopies().limit(1025).toList();
        var items = choices.stream().map(index -> ItemStackTemplate.fromNonEmptyStack(contents.get(index))).toList();
        return Optional.of(new BackpackTooltip(items, choices.indexOf(BackpackItemInteraction.selected(bag))));
    }

    public int firstShownIndex() { return selectedIndex / PAGE_SIZE * PAGE_SIZE; }
    public List<ItemStackTemplate> shownItems() {
        return items.subList(firstShownIndex(), Math.min(items.size(), firstShownIndex() + PAGE_SIZE));
    }
}
