package com.adaptor.deadrecall;

import com.adaptor.deadrecall.entity.DeathBackpackEntity;
import com.adaptor.deadrecall.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Deadrecall implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("DeadRecall");

    @Override
    public void onInitialize() {
        // 註冊物品
        ModItems.registerModItems();

        // 初始化 Discord 橋接
        DiscordBridge.init(FabricLoader.getInstance().getConfigDir());

        // 註冊死亡背包功能 - 當玩家死亡時收集掉落物品
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayerEntity player) {
                handlePlayerDeath(player);
            }
        });

        // 監聽玩家聊天訊息，轉發到 Discord
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String username = sender.getName().getString();
            String content = message.getContent().getString();
            DiscordBridge.sendChatMessage(username, content);
        });

        // 僅註冊 /back 指令，死亡座標記錄交由 Mixin 處理
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("back")
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    DeathLocationManager.DeathLocation loc = DeathLocationManager.getDeathLocation(player);
                    if (loc == null) {
                        player.sendMessage(Text.literal("§c沒有死亡座標可傳送！"), false);
                        return 0;
                    }

                    ServerWorld world = player.getServerWorld();
                    if (world == null) {
                        player.sendMessage(Text.literal("§c找不到死亡世界！"), false);
                        return 0;
                    }
                    player.teleport(world, loc.pos.getX() + 0.5, loc.pos.getY(), loc.pos.getZ() + 0.5, player.getYaw(), player.getPitch());
                    player.sendMessage(Text.literal("§a已傳送回死亡地點！"), false);
                    DeathLocationManager.clearDeathLocation(player);
                    return 1;
                })
            );
        });
    }

    private void handlePlayerDeath(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        BlockPos deathPos = player.getBlockPos();

        LOGGER.info("Player {} died at {}, starting death backpack collection", player.getName().getString(), deathPos);

        // 延遲 2 秒後收集掉落物品（給物品掉落時間）
        world.getServer().execute(() -> {
            world.getServer().execute(() -> {
                LOGGER.info("Collecting dropped items for player {} at {}", player.getName().getString(), deathPos);

                // 收集死亡地點附近的掉落物品
                Box searchBox = new Box(deathPos).expand(10.0); // 搜尋範圍 10 格
                List<ItemEntity> droppedItems = world.getEntitiesByClass(ItemEntity.class, searchBox, itemEntity -> {
                    LOGGER.debug("Found item entity: {} at {}, age: {}",
                        itemEntity.getStack().getItem().getName().getString(),
                        itemEntity.getBlockPos(),
                        itemEntity.age);

                    // 收集所有在搜尋區域內的物品（簡化邏輯）
                    return true;
                });

                LOGGER.info("Found {} dropped items for player {}", droppedItems.size(), player.getName().getString());

                if (!droppedItems.isEmpty()) {
                    // 創建死亡背包
                    ItemStack deathBackpack = new ItemStack(ModItems.DEATH_BACKPACK);
                    List<ItemStack> collectedItems = new ArrayList<>();

                    // 收集物品並移除實體
                    for (ItemEntity itemEntity : droppedItems) {
                        collectedItems.add(itemEntity.getStack().copy());
                        itemEntity.discard(); // 移除實體
                        LOGGER.info("Collected item: {} x{}", itemEntity.getStack().getItem().getName().getString(), itemEntity.getStack().getCount());
                    }

                    // 將物品存儲到背包中
                    if (!collectedItems.isEmpty()) {
                        // 創建容器組件來存儲物品
                        ContainerComponent container =
                            ContainerComponent.fromStacks(collectedItems);
                        deathBackpack.set(DataComponentTypes.CONTAINER, container);

                        // 在死亡地點生成背包實體
                        DeathBackpackEntity backpackEntity = new DeathBackpackEntity(world,
                            deathPos.getX() + 0.5, deathPos.getY() + 0.5, deathPos.getZ() + 0.5,
                            deathBackpack);

                        // 設置物品所有者，防止立即消失
                        backpackEntity.setOwner(player.getUuid());
                        backpackEntity.setPickupDelay(40); // 2 秒拾取延遲
                        // lifespan已在DeathBackpackEntity構造函數中設置為永久

                        world.spawnEntity(backpackEntity);

                        LOGGER.info("Created death backpack for player {} with {} items at {}",
                            player.getName().getString(), collectedItems.size(), deathPos);

                        // 通知玩家
                        player.sendMessage(Text.literal("§e你的物品已被收集到死亡背包中！"), false);
                    }
                } else {
                    LOGGER.info("No dropped items found for player {} at {}", player.getName().getString(), deathPos);
                }
            });
        });
    }
}
