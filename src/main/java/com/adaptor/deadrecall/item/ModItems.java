package com.adaptor.deadrecall.item;

import com.adaptor.deadrecall.Deadrecall;
import com.adaptor.deadrecall.effect.ModMobEffects;
import com.adaptor.deadrecall.item.copper.CopperWrenchItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.Foods;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

import java.util.function.Function;

public class ModItems {
    // 註冊不同等級的背包
    public static final Item BACKPACK_BASIC = registerItem("backpack_basic",
            props -> new TieredBackpackItem(props.stacksTo(1), TieredBackpackItem.BackpackTier.BASIC));

    public static final Item BACKPACK_STANDARD = registerItem("backpack_standard",
            props -> new TieredBackpackItem(props.stacksTo(1), TieredBackpackItem.BackpackTier.STANDARD));

    public static final Item BACKPACK_ADVANCED = registerItem("backpack_advanced",
            props -> new TieredBackpackItem(props.stacksTo(1), TieredBackpackItem.BackpackTier.ADVANCED));

    public static final Item BACKPACK_NETHERITE = registerItem("backpack_netherite",
            props -> new TieredBackpackItem(props.stacksTo(1).fireResistant(), TieredBackpackItem.BackpackTier.NETHERITE));

    // 死亡背包 - 特殊的死亡掉落物品收集器
    public static final Item DEATH_BACKPACK = registerItem("death_backpack",
            props -> new DeathBackpackItem(props.stacksTo(1).fireResistant()));

    public static final Item SALTPETER = registerItem("saltpeter",
            props -> new Item(props));

    public static final Item PIG_MANURE = registerItem("pig_manure",
            props -> new PigManureItem(props));

    public static final Item WOOD_ASH = registerItem("wood_ash",
            props -> new Item(props));

    public static final Item COCOA_POWDER = registerItem("cocoa_powder",
            props -> new Item(props.stacksTo(1)));

    public static final Item HOT_COCOA = registerItem("hot_cocoa",
            props -> new Item(props.stacksTo(16)
                    .food(Foods.HONEY_BOTTLE, Consumables.defaultDrink()
                            .sound(SoundEvents.GENERIC_DRINK)
                            .build())
                    .usingConvertsTo(Items.GLASS_BOTTLE)));

    public static final Item CHERRY_BREW = registerItem("cherry_brew",
            props -> new Item(props.stacksTo(16)
                    .food(new FoodProperties.Builder()
                                    .nutrition(4)
                                    .saturationModifier(0.4F)
                                    .alwaysEdible()
                                    .build(),
                            Consumables.defaultDrink()
                                    .sound(SoundEvents.GENERIC_DRINK)
                                    .onConsume(new ApplyStatusEffectsConsumeEffect(
                                            new MobEffectInstance(ModMobEffects.CHERRY_BLOOM, 20 * 180, 0),
                                            1.0F
                                    ))
                                    .build())
                    .usingConvertsTo(Items.GLASS_BOTTLE)));

    // 合成器皿（缽）：可對硫磺方塊右鍵填充
    public static final Item STONE_BOWL = registerItem("stone_bowl",
            props -> new StoneBowlItem(props.stacksTo(1)));

    // 帶硫磺的缽：作為火藥配方材料，合成後回傳缽
    public static final Item SULFUR_BOWL = registerItem("sulfur_bowl",
            props -> new Item(props.stacksTo(1).craftRemainder(STONE_BOWL)));

    public static final Item COPPER_WRENCH = registerItem("copper_wrench",
            props -> new CopperWrenchItem(props.stacksTo(1)));

    private static Item registerItem(String name, Function<Item.Properties, Item> itemFactory) {
        Identifier id = Identifier.fromNamespaceAndPath("deadrecall", name);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        Item.Properties props = new Item.Properties().setId(itemKey);
        Item item = itemFactory.apply(props);
        return Registry.register(BuiltInRegistries.ITEM, id, item);
    }

    public static void registerModItems() {
        Deadrecall.LOGGER.info("正在註冊模組物品...");
    }
}
