package dev.totem.remnant.death;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Persists one staged soulbound item until it can be restored after respawn. */
final class SoulboundDeathItemSavedData extends SavedData {
    private static final int DATA_VERSION = 1;

    private static final Codec<PendingItem> PENDING_ITEM_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("player").forGetter(PendingItem::player),
            ItemStack.CODEC.fieldOf("stack").forGetter(PendingItem::stack),
            Codec.INT.optionalFieldOf("preferred_slot", -1).forGetter(PendingItem::preferredSlot)
    ).apply(instance, PendingItem::new));

    static final Codec<SoulboundDeathItemSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("data_version", DATA_VERSION)
                    .forGetter(SoulboundDeathItemSavedData::dataVersion),
            PENDING_ITEM_CODEC.listOf().optionalFieldOf("pending", List.of())
                    .forGetter(SoulboundDeathItemSavedData::pendingList)
    ).apply(instance, SoulboundDeathItemSavedData::new));

    static final SavedDataType<SoulboundDeathItemSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("totem", "remnant/soulbound_items"),
            SoulboundDeathItemSavedData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private final int dataVersion;
    private final Map<UUID, PendingItem> pendingByPlayer = new HashMap<>();

    SoulboundDeathItemSavedData() {
        this(DATA_VERSION, List.of());
    }

    private SoulboundDeathItemSavedData(int dataVersion, List<PendingItem> pending) {
        this.dataVersion = Math.max(dataVersion, DATA_VERSION);
        for (PendingItem item : pending) {
            if (!item.stack().isEmpty()) {
                this.pendingByPlayer.put(item.player(), item.copy());
            }
        }
    }

    boolean putIfAbsent(UUID playerId, ItemStack stack, int preferredSlot) {
        if (this.pendingByPlayer.containsKey(playerId) || stack.isEmpty()) {
            return false;
        }
        this.pendingByPlayer.put(playerId, new PendingItem(playerId, stack.copy(), preferredSlot));
        setDirty();
        return true;
    }

    Optional<PendingItem> pending(UUID playerId) {
        PendingItem item = this.pendingByPlayer.get(playerId);
        return item == null ? Optional.empty() : Optional.of(item.copy());
    }

    void remove(UUID playerId) {
        if (this.pendingByPlayer.remove(playerId) != null) {
            setDirty();
        }
    }

    private int dataVersion() {
        return this.dataVersion;
    }

    private List<PendingItem> pendingList() {
        List<PendingItem> pending = new ArrayList<>(this.pendingByPlayer.size());
        this.pendingByPlayer.values().stream()
                .map(PendingItem::copy)
                .forEach(pending::add);
        return pending;
    }

    record PendingItem(UUID player, ItemStack stack, int preferredSlot) {
        PendingItem {
            stack = stack.copy();
        }

        private PendingItem copy() {
            return new PendingItem(this.player, this.stack, this.preferredSlot);
        }
    }
}
