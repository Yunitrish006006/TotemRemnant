package dev.totem.remnant.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Server-authoritative crafting grids displayed in the Remnant manual. */
public record RemnantManualRecipesPayload(List<Entry> recipes) implements CustomPacketPayload {
    private static final int MAX_RECIPES = 32;
    private static final int MAX_SLOTS = 9;

    public static final Type<RemnantManualRecipesPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("totem", "remnant_manual_recipes")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, RemnantManualRecipesPayload> CODEC =
            CustomPacketPayload.codec(RemnantManualRecipesPayload::write, RemnantManualRecipesPayload::read);

    public RemnantManualRecipesPayload {
        recipes = List.copyOf(recipes);
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(recipes.size());
        for (Entry recipe : recipes) {
            buffer.writeUtf(recipe.id(), 128);
            buffer.writeBoolean(recipe.available());
            if (!recipe.available()) {
                continue;
            }
            buffer.writeVarInt(recipe.width());
            buffer.writeVarInt(recipe.height());
            buffer.writeVarInt(recipe.ingredients().size());
            recipe.ingredients().forEach(stack -> ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, stack));
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.result());
        }
    }

    private static RemnantManualRecipesPayload read(RegistryFriendlyByteBuf buffer) {
        int recipeCount = Math.min(buffer.readVarInt(), MAX_RECIPES);
        List<Entry> recipes = new ArrayList<>(recipeCount);
        for (int index = 0; index < recipeCount; index++) {
            String id = buffer.readUtf(128);
            if (!buffer.readBoolean()) {
                recipes.add(Entry.unavailable(id));
                continue;
            }
            int width = buffer.readVarInt();
            int height = buffer.readVarInt();
            int slotCount = Math.min(buffer.readVarInt(), MAX_SLOTS);
            List<ItemStack> ingredients = new ArrayList<>(slotCount);
            for (int slot = 0; slot < slotCount; slot++) {
                ingredients.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
            }
            ItemStack result = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
            recipes.add(new Entry(id, true, width, height, ingredients, result));
        }
        return new RemnantManualRecipesPayload(recipes);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(
            String id,
            boolean available,
            int width,
            int height,
            List<ItemStack> ingredients,
            ItemStack result
    ) {
        public Entry {
            ingredients = List.copyOf(ingredients);
            result = result.copy();
        }

        public static Entry unavailable(String id) {
            return new Entry(id, false, 0, 0, List.of(), ItemStack.EMPTY);
        }
    }
}
