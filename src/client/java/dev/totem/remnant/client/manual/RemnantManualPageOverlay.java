package dev.totem.remnant.client.manual;

import dev.totem.core.api.v1.client.manual.TotemManualPageOverlayRegistry;
import dev.totem.core.api.v1.client.manual.TotemManualPageRenderContext;
import dev.totem.remnant.manual.RemnantManualRecipeSync;
import dev.totem.remnant.network.RemnantManualRecipesPayload;
import dev.totem.remnant.registry.RemnantGameRules;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Adds Remnant's synchronized crafting and smithing diagrams to Core-owned manual pages. */
public final class RemnantManualPageOverlay {
    private static final String RECIPE_PAGE_PREFIX = "book.deadrecall.remnant.module_recipes.page.";
    private static final String SMITHING_PAGE_KEY = "book.deadrecall.remnant.basics.page.3";
    private static final String GAME_RULES_PAGE_KEY = "book.deadrecall.remnant.death_backpack.page.3";
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final Identifier CRAFTING_BACKGROUND =
            Identifier.withDefaultNamespace("textures/gui/container/crafting_table.png");

    private RemnantManualPageOverlay() {
    }

    public static void register() {
        TotemManualPageOverlayRegistry.register(
                Identifier.fromNamespaceAndPath("totem-remnant", "manual_recipes"),
                RemnantManualPageOverlay::render
        );
    }

    private static void render(TotemManualPageRenderContext context) {
        if (SMITHING_PAGE_KEY.equals(context.pageKey())) {
            renderSmithingRecipes(context);
        } else if (GAME_RULES_PAGE_KEY.equals(context.pageKey())) {
            renderGameRules(context);
        } else if (context.pageKey() != null && context.pageKey().startsWith(RECIPE_PAGE_PREFIX)) {
            renderCraftingRecipe(context);
        }
    }

    private static void renderGameRules(TotemManualPageRenderContext context) {
        if (!RemnantGameRules.clientRulesSynchronized()) {
            context.graphics().centeredText(context.font(),
                    Component.translatable("book.deadrecall.remnant.rules.syncing"),
                    context.pageLeft() + 93, context.pageTop() + 77, 0xFF9B2C20);
            return;
        }

        ruleRow(context, "book.deadrecall.remnant.rules.generate_death_backpacks",
                RemnantGameRules.clientGeneratesDeathBackpacks(), context.pageTop() + 53);
        ruleRow(context, "book.deadrecall.remnant.rules.owner_pickup_only",
                RemnantGameRules.clientDeathBackpackOwnerPickupOnly(), context.pageTop() + 79);
        ruleRow(context, "book.deadrecall.remnant.rules.prevent_nesting",
                RemnantGameRules.clientPreventsPortableContainerNesting(), context.pageTop() + 105);

        context.graphics().centeredText(context.font(),
                Component.translatable("book.deadrecall.remnant.rules.change_hint"),
                context.pageLeft() + 93, context.pageTop() + 137, 0xFF6F5637);
    }

    private static void ruleRow(
            TotemManualPageRenderContext context,
            String labelKey,
            boolean enabled,
            int y
    ) {
        Component label = Component.translatable(labelKey);
        Component state = Component.translatable(enabled
                ? "book.deadrecall.remnant.rules.enabled"
                : "book.deadrecall.remnant.rules.disabled");
        int left = context.pageLeft() + 37;
        int stateWidth = context.font().width(state);
        context.graphics().text(context.font(), label, left, y, 0xFF4B3826, false);
        context.graphics().text(context.font(), state,
                context.pageLeft() + 149 - stateWidth, y,
                enabled ? 0xFF287A45 : 0xFFA33A2B, false);
    }

    private static void renderCraftingRecipe(TotemManualPageRenderContext context) {
        int recipeIndex;
        try {
            recipeIndex = Integer.parseInt(context.pageKey().substring(RECIPE_PAGE_PREFIX.length())) - 1;
        } catch (NumberFormatException ignored) {
            return;
        }
        if (recipeIndex < 0 || recipeIndex >= RemnantManualRecipeSync.MODULE_RECIPE_IDS.size()) {
            return;
        }

        String recipeId = RemnantManualRecipeSync.MODULE_RECIPE_IDS.get(recipeIndex).toString();
        RemnantManualRecipesPayload.Entry recipe = RemnantManualRecipeCache.get(recipeId);
        if (recipe == null) {
            Component status = Component.translatable(
                    RemnantManualRecipeCache.isSynchronizedFromServer()
                            ? "book.deadrecall.remnant.recipe.unavailable"
                            : "book.deadrecall.remnant.recipe.loading"
            );
            context.graphics().centeredText(context.font(), status,
                    context.pageLeft() + 93, context.pageTop() + 82, 0xFF9B2C20);
            return;
        }
        if (!recipe.available()) {
            context.graphics().centeredText(context.font(),
                    Component.translatable("book.deadrecall.remnant.recipe.unavailable"),
                    context.pageLeft() + 93, context.pageTop() + 82, 0xFF9B2C20);
            return;
        }

        context.graphics().centeredText(context.font(),
                Component.translatable("book.deadrecall.remnant.recipe.shaped"),
                context.pageLeft() + 93, context.pageTop() + 45, 0xFF5B432A);

        int gridLeft = context.pageLeft() + 43;
        int gridTop = context.pageTop() + 61;
        for (int slot = 0; slot < 9; slot++) {
            int x = gridLeft + slot % 3 * 18;
            int y = gridTop + slot / 3 * 18;
            context.graphics().blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x, y, 18, 18);

            ItemStack ingredient = ingredientAt(recipe, slot);
            if (!ingredient.isEmpty()) {
                renderStack(context, ingredient, x, y);
            }
        }

        context.graphics().blit(RenderPipelines.GUI_TEXTURED, CRAFTING_BACKGROUND,
                gridLeft + 60, gridTop + 20, 89.0F, 34.0F, 15, 15, 256, 256);
        int resultX = gridLeft + 79;
        int resultY = gridTop + 18;
        context.graphics().blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE,
                resultX, resultY, 18, 18);
        renderStack(context, recipe.result(), resultX, resultY);
        context.graphics().itemDecorations(context.font(), recipe.result(), resultX + 1, resultY + 1);

        context.graphics().centeredText(context.font(),
                Component.translatable("book.deadrecall.remnant.recipe.live"),
                context.pageLeft() + 93, context.pageTop() + 127, 0xFF6F5637);
    }

    private static void renderSmithingRecipes(TotemManualPageRenderContext context) {
        if (!RemnantManualRecipeCache.isSynchronizedFromServer()) {
            context.graphics().centeredText(context.font(),
                    Component.translatable("book.deadrecall.remnant.recipe.loading"),
                    context.pageLeft() + 93, context.pageTop() + 86, 0xFF9B2C20);
            return;
        }

        int gridLeft = context.pageLeft() + 36;
        for (int recipeIndex = 0;
             recipeIndex < RemnantManualRecipeSync.SMITHING_RECIPE_IDS.size();
             recipeIndex++) {
            String recipeId = RemnantManualRecipeSync.SMITHING_RECIPE_IDS.get(recipeIndex).toString();
            RemnantManualRecipesPayload.Entry recipe = RemnantManualRecipeCache.get(recipeId);
            int rowTop = context.pageTop() + 46 + recipeIndex * 27;

            for (int slot = 0; slot < 3; slot++) {
                int x = gridLeft + slot * 18;
                context.graphics().blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE,
                        x, rowTop, 18, 18);
                if (recipe != null && recipe.available() && slot < recipe.ingredients().size()) {
                    renderStack(context, recipe.ingredients().get(slot), x, rowTop);
                }
            }

            context.graphics().blit(RenderPipelines.GUI_TEXTURED, CRAFTING_BACKGROUND,
                    gridLeft + 58, rowTop + 2, 89.0F, 34.0F, 15, 15, 256, 256);
            int resultX = gridLeft + 78;
            context.graphics().blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE,
                    resultX, rowTop, 18, 18);
            if (recipe != null && recipe.available()) {
                renderStack(context, recipe.result(), resultX, rowTop);
                context.graphics().itemDecorations(context.font(), recipe.result(), resultX + 1, rowTop + 1);
            } else {
                context.graphics().centeredText(context.font(), Component.literal("×"),
                        resultX + 9, rowTop + 5, 0xFF9B2C20);
            }
        }

        context.graphics().centeredText(context.font(),
                Component.translatable("book.deadrecall.remnant.recipe.live"),
                context.pageLeft() + 93, context.pageTop() + 158, 0xFF6F5637);
    }

    private static void renderStack(
            TotemManualPageRenderContext context,
            ItemStack stack,
            int x,
            int y
    ) {
        if (stack.isEmpty()) {
            return;
        }
        context.graphics().item(stack, x + 1, y + 1);
        if (inside(context.mouseX(), context.mouseY(), x, y, 18, 18)) {
            context.graphics().setTooltipForNextFrame(
                    context.font(), stack, context.mouseX(), context.mouseY());
        }
    }

    private static ItemStack ingredientAt(RemnantManualRecipesPayload.Entry recipe, int gridSlot) {
        int row = gridSlot / 3;
        int column = gridSlot % 3;
        if (row >= recipe.height() || column >= recipe.width()) {
            return ItemStack.EMPTY;
        }
        int recipeSlot = row * recipe.width() + column;
        return recipeSlot < recipe.ingredients().size()
                ? recipe.ingredients().get(recipeSlot)
                : ItemStack.EMPTY;
    }

    private static boolean inside(
            int mouseX,
            int mouseY,
            int x,
            int y,
            int width,
            int height
    ) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
