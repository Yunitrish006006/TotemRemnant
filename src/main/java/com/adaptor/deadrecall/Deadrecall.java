package com.adaptor.deadrecall;

import com.adaptor.deadrecall.alchemy.AlchemyHandler;
import com.adaptor.deadrecall.alchemy.CherryBrewInteractions;
import com.adaptor.deadrecall.alchemy.PigManureInteractions;
import com.adaptor.deadrecall.advancement.ModCriteriaTriggers;
import com.adaptor.deadrecall.block.ModBlocks;
import com.adaptor.deadrecall.block.entity.ModBlockEntities;
import com.adaptor.deadrecall.effect.ModMobEffects;
import com.adaptor.deadrecall.item.BackpackItemHelper;
import com.adaptor.deadrecall.item.ModItemGroups;
import com.adaptor.deadrecall.item.ModItems;
import com.adaptor.deadrecall.item.copper.CopperGolemLlmService;
import com.adaptor.deadrecall.item.copper.CopperGolemWrenchHandler;
import com.adaptor.deadrecall.network.CopperGolemOperationPayload;
import com.adaptor.deadrecall.network.CopperGolemFuelSlotPayload;
import com.adaptor.deadrecall.network.CopperWrenchBindingsPayload;
import com.adaptor.deadrecall.network.DiscordConfigSyncPayload;
import com.adaptor.deadrecall.network.ManageDiscordChannelPayload;
import com.adaptor.deadrecall.network.RequestDiscordConfigPayload;
import com.adaptor.deadrecall.network.SaveCopperGolemLlmConfigPayload;
import com.adaptor.deadrecall.network.SortBackpackPayload;
import com.adaptor.deadrecall.network.SaveDiscordConfigPayload;
import com.adaptor.deadrecall.network.TestCopperGolemLlmConnectionPayload;
import com.adaptor.deadrecall.network.UpdateCopperGolemBindingLlmPayload;
import com.adaptor.deadrecall.recipe.ModRecipes;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Deadrecall implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("DeadRecall");
    private static final int BOOKSHELF_REPLACE_INTERVAL_TICKS = 20;
    private static final int PLAYER_HOTBAR_SLOT_COUNT = 9;
    private static final double DEATH_BACKPACK_COLLECTION_RADIUS = 10.0D;
    private static final String TAG_DEATH_BACKPACK_ID = "deadrecall_death_backpack_id";
    private static final Map<UUID, PendingDeathCollection> pendingDeathCollections = new HashMap<>();
    private static final Set<UUID> scheduledDeathBackpackCollections = new HashSet<>();
    private static int bookshelfReplaceTicker = 0;
    private static MinecraftServer discordStatusOpenServer = null;

    @Override
    public void onInitialize() {
        // 註冊物品與方塊
        ModBlocks.registerModBlocks();
        ModBlockEntities.registerModBlockEntities();
        ModMobEffects.registerModEffects();
        ModCriteriaTriggers.registerModCriteriaTriggers();
        ModItems.registerModItems();
        ModItemGroups.registerModItemGroups();
        AlchemyHandler.register();
        CherryBrewInteractions.register();
        PigManureInteractions.register();
        CopperGolemWrenchHandler.register();
        ModRecipes.registerModRecipes();

        // 初始化 Discord 橋接
        DiscordBridge.init(FabricLoader.getInstance().getConfigDir());

        // 註冊自定義封包
        PayloadTypeRegistry.serverboundPlay().register(
                RequestDiscordConfigPayload.TYPE, RequestDiscordConfigPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                SaveDiscordConfigPayload.TYPE, SaveDiscordConfigPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                ManageDiscordChannelPayload.TYPE, ManageDiscordChannelPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                SortBackpackPayload.TYPE, SortBackpackPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                CopperGolemOperationPayload.TYPE, CopperGolemOperationPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                CopperGolemFuelSlotPayload.TYPE, CopperGolemFuelSlotPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                SaveCopperGolemLlmConfigPayload.TYPE, SaveCopperGolemLlmConfigPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                TestCopperGolemLlmConnectionPayload.TYPE, TestCopperGolemLlmConnectionPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                UpdateCopperGolemBindingLlmPayload.TYPE, UpdateCopperGolemBindingLlmPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
                DiscordConfigSyncPayload.TYPE, DiscordConfigSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
                CopperWrenchBindingsPayload.TYPE, CopperWrenchBindingsPayload.CODEC);

        // 收到客戶端請求時，回傳目前設定
        ServerPlayNetworking.registerGlobalReceiver(RequestDiscordConfigPayload.TYPE,
                (payload, context) -> {
                    ServerPlayer player = context.player();
                    var channels = DiscordBridge.getChannels();
                    var syncedChannels = new ArrayList<DiscordConfigSyncPayload.ChannelData>(channels.size());
                    for (var channel : channels) {
                        syncedChannels.add(new DiscordConfigSyncPayload.ChannelData(channel.id, channel.name));
                    }
                    ServerPlayNetworking.send(player, new DiscordConfigSyncPayload(
                            DiscordBridge.isEnabled(),
                            DiscordBridge.getWorkerUrl(),
                            DiscordBridge.getApiKey(),
                            syncedChannels
                    ));
                });

        // 收到客戶端儲存請求時，更新設定（需要 OP 權限）
        ServerPlayNetworking.registerGlobalReceiver(SaveDiscordConfigPayload.TYPE,
                (payload, context) -> {
                    ServerPlayer player = context.player();
                    
                    if (!canManageDiscordBridge(player)) {
                        player.sendSystemMessage(Component.literal("§c你沒有權限修改 Discord Bridge 設定！"));
                        LOGGER.warn("[DiscordBridge] 玩家 {} 嘗試未授權修改設定", player.getName().getString());
                        return;
                    }
                    
                    try {
                        DiscordBridge.updateConfig(payload.enabled(), payload.workerUrl(), payload.apiKey());
                        player.sendSystemMessage(Component.literal("§aDiscord Bridge 設定已更新"));
                    } catch (IllegalArgumentException e) {
                        player.sendSystemMessage(Component.literal("§c" + e.getMessage()));
                    } catch (Exception e) {
                        player.sendSystemMessage(Component.literal("§c更新失敗：" + e.getMessage()));
                        LOGGER.error("[DiscordBridge] 更新設定失敗", e);
                    }
                });

        // 收到頻道管理請求時，添加或移除頻道
        ServerPlayNetworking.registerGlobalReceiver(ManageDiscordChannelPayload.TYPE,
                (payload, context) -> {
                    ServerPlayer player = context.player();
                    
                    if (!canManageDiscordBridge(player)) {
                        player.sendSystemMessage(Component.literal("§c你沒有權限管理 Discord 頻道！"));
                        LOGGER.warn("[DiscordBridge] 玩家 {} 嘗試未授權管理頻道", player.getName().getString());
                        return;
                    }
                    
                    try {
                        if ("add".equals(payload.action())) {
                            DiscordBridge.addChannel(payload.channelId(), payload.channelName());
                            player.sendSystemMessage(Component.literal("§a已添加 Discord 頻道: " + payload.channelName()));
                        } else if ("remove".equals(payload.action())) {
                            DiscordBridge.removeChannel(payload.channelId());
                            player.sendSystemMessage(Component.literal("§a已移除 Discord 頻道: " + payload.channelId()));
                        }
                    } catch (IllegalArgumentException e) {
                        player.sendSystemMessage(Component.literal("§c" + e.getMessage()));
                    } catch (Exception e) {
                        player.sendSystemMessage(Component.literal("§c操作失敗：" + e.getMessage()));
                        LOGGER.error("[DiscordBridge] 管理頻道失敗", e);
                    }
                });

        ServerPlayNetworking.registerGlobalReceiver(SortBackpackPayload.TYPE,
                (payload, context) -> context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    sortOpenContainer(player, payload.target());
                }));

        ServerPlayNetworking.registerGlobalReceiver(CopperGolemOperationPayload.TYPE,
                (payload, context) -> context.server().execute(() ->
                        CopperGolemWrenchHandler.setTransportEnabledFromUi(context.player(), payload.golemId(), payload.running())));

        ServerPlayNetworking.registerGlobalReceiver(CopperGolemFuelSlotPayload.TYPE,
                (payload, context) -> context.server().execute(() ->
                        CopperGolemWrenchHandler.handleFuelSlotFromUi(context.player(), payload.golemId(), payload.action())));

        ServerPlayNetworking.registerGlobalReceiver(SaveCopperGolemLlmConfigPayload.TYPE,
                (payload, context) -> context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    if (!canManageDiscordBridge(player)) {
                        player.sendSystemMessage(Component.literal("§c你沒有權限修改 LLM API 設定！"));
                        LOGGER.warn("[CopperGolemLLM] 玩家 {} 嘗試未授權修改設定", player.getName().getString());
                        return;
                    }

                    CopperGolemWrenchHandler.setGolemLlmConfigFromUi(player, payload.golemId(), payload.apiUrl(), payload.apiKey(), payload.model());
                    player.sendSystemMessage(Component.literal("§a銅魁儡 LLM API 設定已更新"));
                }));

        ServerPlayNetworking.registerGlobalReceiver(TestCopperGolemLlmConnectionPayload.TYPE,
                (payload, context) -> context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    if (!canManageDiscordBridge(player)) {
                        player.sendSystemMessage(Component.literal("§c你沒有權限測試 LLM API 設定！"));
                        LOGGER.warn("[CopperGolemLLM] 玩家 {} 嘗試未授權測試連線", player.getName().getString());
                        return;
                    }

                    CopperGolemLlmService.testConnection(
                            context.server(),
                            player.getUUID(),
                            payload.apiUrl(),
                            payload.apiKey(),
                            payload.model()
                    );
                }));

        ServerPlayNetworking.registerGlobalReceiver(UpdateCopperGolemBindingLlmPayload.TYPE,
                (payload, context) -> context.server().execute(() ->
                        CopperGolemWrenchHandler.setBindingLlmFromUi(
                                context.player(),
                                payload.golemId(),
                                payload.dimension(),
                                payload.x(),
                                payload.y(),
                                payload.z(),
                                payload.enabled(),
                                payload.prompt()
                        )));

        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (entity instanceof ServerPlayer player) {
                rememberExistingDropsBeforeDeath(player);
            }
            return true;
        });

        // 註冊死亡背包功能 - 當玩家死亡時收集掉落物品
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayer player) {
                DeathLocationManager.setDeathLocation(player, player.blockPosition(), player.level());
                handlePlayerDeath(player);
            }
        });

        // 監聽玩家聊天訊息，轉發到 Discord
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String username = sender.getName().getString();
            String content = message.decoratedContent().getString();
            DiscordBridge.sendChatMessage(username, content);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (server.isDedicatedServer()) {
                notifyServerOpened(server, false);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server ->
                notifyServerClosed(server, true));

        // 清理銅魁儡失效綁定，並在整箱都無法分類時維持原地跳躍
        ServerTickEvents.END_SERVER_TICK.register(CopperGolemWrenchHandler::tickCopperGolemWrenchState);

        // 生存模式不允許持有一般書櫃：統一替換為書本（每個書櫃 3 本）
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            bookshelfReplaceTicker++;
            if (bookshelfReplaceTicker < BOOKSHELF_REPLACE_INTERVAL_TICKS) {
                return;
            }
            bookshelfReplaceTicker = 0;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.getAbilities().instabuild) {
                    continue;
                }
                replaceVanillaBookshelfInInventory(player);
            }
        });

        // 註冊 /back 指令
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("back")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    DeathLocationManager.DeathLocation loc = DeathLocationManager.getDeathLocation(player);
                    if (loc == null) {
                        player.sendSystemMessage(Component.literal("§c沒有死亡座標可傳送！"));
                        return 0;
                    }

                    ServerLevel world = context.getSource().getServer().getLevel(loc.dimension);
                    if (world == null) {
                        player.sendSystemMessage(Component.literal("§c找不到死亡世界！"));
                        return 0;
                    }
                    player.teleportTo(world, loc.pos.getX() + 0.5, loc.pos.getY(), loc.pos.getZ() + 0.5, Relative.DELTA, player.getYRot(), player.getXRot(), false);
                    player.sendSystemMessage(Component.literal("§a已傳送回死亡地點！"));
                    DeathLocationManager.clearDeathLocation(player);
                    return 1;
                })
            );

            dispatcher.register(
                    Commands.literal("discordbridge")
                            .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                            .then(Commands.literal("reload")
                                    .executes(context -> {
                                        DiscordBridge.reload();
                                        context.getSource().sendSuccess(() -> Component.literal("§aDiscord Bridge 設定已重新載入"), true);
                                        return 1;
                                    }))
                            .then(Commands.literal("set")
                                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                                            .then(Commands.argument("workerUrl", StringArgumentType.string())
                                                    .then(Commands.argument("apiKey", StringArgumentType.string())
                                                            .executes(context -> {
                                                                boolean enabled = BoolArgumentType.getBool(context, "enabled");
                                                                String workerUrl = StringArgumentType.getString(context, "workerUrl");
                                                                String apiKey = StringArgumentType.getString(context, "apiKey");
                                                                try {
                                                                    DiscordBridge.updateConfig(enabled, workerUrl, apiKey);
                                                                    context.getSource().sendSuccess(() -> Component.literal("§aDiscord Bridge 設定已更新"), true);
                                                                    return 1;
                                                                } catch (IllegalArgumentException e) {
                                                                    context.getSource().sendFailure(Component.literal("§c" + e.getMessage()));
                                                                    return 0;
                                                                } catch (Exception e) {
                                                                    context.getSource().sendFailure(Component.literal("§c更新失敗：" + e.getMessage()));
                                                                    LOGGER.error("[DiscordBridge] 更新設定失敗", e);
                                                                    return 0;
                                                                }
                                                            })))))
                            .then(Commands.literal("channel")
                                    .then(Commands.literal("add")
                                            .then(Commands.argument("channelId", StringArgumentType.string())
                                                    .then(Commands.argument("channelName", StringArgumentType.string())
                                                            .executes(context -> {
                                                               String channelId = StringArgumentType.getString(context, "channelId");
                                                               String channelName = StringArgumentType.getString(context, "channelName");
                                                               try {
                                                                   DiscordBridge.addChannel(channelId, channelName);
                                                                   context.getSource().sendSuccess(() -> Component.literal("§a已添加 Discord 頻道: " + channelName), true);
                                                                   return 1;
                                                               } catch (IllegalArgumentException e) {
                                                                   context.getSource().sendFailure(Component.literal("§c" + e.getMessage()));
                                                                   return 0;
                                                               } catch (Exception e) {
                                                                   context.getSource().sendFailure(Component.literal("§c添加頻道失敗：" + e.getMessage()));
                                                                   LOGGER.error("[DiscordBridge] 添加頻道失敗", e);
                                                                   return 0;
                                                               }
                                                            }))))
                                    .then(Commands.literal("remove")
                                            .then(Commands.argument("channelId", StringArgumentType.string())
                                                    .executes(context -> {
                                                        String channelId = StringArgumentType.getString(context, "channelId");
                                                        try {
                                                            DiscordBridge.removeChannel(channelId);
                                                            context.getSource().sendSuccess(() -> Component.literal("§a已移除 Discord 頻道: " + channelId), true);
                                                            return 1;
                                                        } catch (IllegalArgumentException e) {
                                                            context.getSource().sendFailure(Component.literal("§c" + e.getMessage()));
                                                            return 0;
                                                        } catch (Exception e) {
                                                            context.getSource().sendFailure(Component.literal("§c移除頻道失敗：" + e.getMessage()));
                                                            LOGGER.error("[DiscordBridge] 移除頻道失敗", e);
                                                            return 0;
                                                        }
                                                    })))
                                    .then(Commands.literal("list")
                                            .executes(context -> {
                                                var channels = DiscordBridge.getChannels();
                                                if (channels.isEmpty()) {
                                                    context.getSource().sendSuccess(() -> Component.literal("§c尚未配置任何 Discord 頻道"), true);
                                                } else {
                                                    context.getSource().sendSuccess(() -> Component.literal("§a已配置 " + channels.size() + " 個頻道:"), true);
                                                    for (var ch : channels) {
                                                        context.getSource().sendSuccess(() -> Component.literal("  - " + ch.name + " (" + ch.id + ")"), false);
                                                    }
                                                }
                                                return 1;
                                            })))
            );
        });
    }

    public static void notifyServerOpened(MinecraftServer server, boolean immediate) {
        if (server == null) {
            return;
        }

        synchronized (Deadrecall.class) {
            if (discordStatusOpenServer == server) {
                return;
            }
            discordStatusOpenServer = server;
        }

        sendDiscordServerStatus(server, "伺服器已開啟", 20.0D, immediate);
    }

    public static void notifyServerClosed(MinecraftServer server, boolean immediate) {
        if (server == null) {
            return;
        }

        synchronized (Deadrecall.class) {
            if (discordStatusOpenServer != server) {
                return;
            }
            discordStatusOpenServer = null;
        }

        sendDiscordServerStatus(server, "伺服器已關閉", 0.0D, immediate);
    }

    private static void sendDiscordServerStatus(MinecraftServer server, String status, double tps, boolean immediate) {
        int playersOnline = server.getPlayerList().getPlayerCount();
        int playersMax = server.getPlayerList().getMaxPlayers();
        String version = server.getServerVersion();

        if (immediate) {
            DiscordBridge.sendServerStatusImmediately(status, playersOnline, playersMax, version, tps);
        } else {
            DiscordBridge.sendServerStatus(status, playersOnline, playersMax, version, tps);
        }
    }

    private void handlePlayerDeath(ServerPlayer player) {
        ServerLevel world = (ServerLevel) player.level();
        BlockPos deathPos = player.blockPosition();
        UUID playerId = player.getUUID();
        PendingDeathCollection pendingCollection = pendingDeathCollections.remove(playerId);
        Set<UUID> existingDropIds = pendingCollection != null
                && pendingCollection.dimension().equals(world.dimension())
                ? pendingCollection.existingDropIds()
                : Set.of();

        if (!scheduledDeathBackpackCollections.add(playerId)) {
            LOGGER.warn("Death backpack collection already scheduled for player {}, skipping duplicate", player.getName().getString());
            return;
        }

        LOGGER.info("Player {} died at {}, starting death backpack collection", player.getName().getString(), deathPos);

        // 延到掉落物生成後再收集。
        world.getServer().execute(() -> {
            world.getServer().execute(() -> {
                try {
                    LOGGER.info("Collecting dropped items for player {} at {}", player.getName().getString(), deathPos);

                    // 收集本次死亡後新產生的掉落物，避免把死亡前地上的物品一起裝進死亡背包。
                    AABB searchBox = new AABB(deathPos).inflate(DEATH_BACKPACK_COLLECTION_RADIUS);
                    List<ItemEntity> droppedItems = world.getEntitiesOfClass(ItemEntity.class, searchBox,
                            entity -> entity.isAlive() && !existingDropIds.contains(entity.getUUID()));

                    LOGGER.info("Found {} new dropped items for player {}", droppedItems.size(), player.getName().getString());

                    if (!droppedItems.isEmpty()) {
                        // 創建死亡背包
                        ItemStack deathBackpack = new ItemStack(ModItems.DEATH_BACKPACK);
                        markDeathBackpackUnique(deathBackpack);
                        List<ItemStack> collectedItems = new ArrayList<>();

                        // 收集物品並移除實體
                        for (ItemEntity itemEntity : droppedItems) {
                            ItemStack droppedStack = itemEntity.getItem();
                            if (BackpackItemHelper.isBackpackItem(droppedStack)) {
                                LOGGER.info("Skipped backpack item from death backpack collection: {} x{}",
                                        droppedStack.getItem().getName(droppedStack).getString(),
                                        droppedStack.getCount());
                                continue;
                            }

                            if (droppedStack.isEmpty()) {
                                continue;
                            }

                            collectedItems.add(droppedStack.copy());
                            itemEntity.discard(); // 移除實體
                            LOGGER.info("Collected item: {} x{}", droppedStack.getItem().getName(droppedStack).getString(), droppedStack.getCount());
                        }

                        // 將物品存儲到背包中
                        if (!collectedItems.isEmpty()) {
                            deathBackpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(collectedItems));

                            // 在死亡地點生成背包實體
                            ItemEntity backpackEntity = new ItemEntity(world,
                                deathPos.getX() + 0.5, deathPos.getY() + 0.5, deathPos.getZ() + 0.5,
                                deathBackpack);

                            // 設置物品所有者，防止立即消失
                            backpackEntity.setPickUpDelay(40); // 2 秒拾取延遲
                            // 將死亡背包標記為不可被傷害/摧毀（例如仙人掌傷害）的一個額外保護
                            backpackEntity.setUnlimitedLifetime();
                            world.addFreshEntity(backpackEntity);

                            LOGGER.info("Created death backpack for player {} with {} items at {}",
                                player.getName().getString(), collectedItems.size(), deathPos);

                            // 通知玩家
                            player.sendSystemMessage(Component.literal("§e你的物品已被收集到死亡背包中！"));
                        }
                    } else {
                        LOGGER.info("No new dropped items found for player {} at {}", player.getName().getString(), deathPos);
                    }
                } finally {
                    scheduledDeathBackpackCollections.remove(playerId);
                }
            });
        });
    }

    private static void rememberExistingDropsBeforeDeath(ServerPlayer player) {
        ServerLevel world = (ServerLevel) player.level();
        BlockPos deathPos = player.blockPosition();
        AABB searchBox = new AABB(deathPos).inflate(DEATH_BACKPACK_COLLECTION_RADIUS);
        Set<UUID> existingDropIds = new HashSet<>();
        for (ItemEntity itemEntity : world.getEntitiesOfClass(ItemEntity.class, searchBox, ItemEntity::isAlive)) {
            existingDropIds.add(itemEntity.getUUID());
        }
        pendingDeathCollections.put(player.getUUID(), new PendingDeathCollection(world.dimension(), existingDropIds));
    }

    private static void markDeathBackpackUnique(ItemStack deathBackpack) {
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_DEATH_BACKPACK_ID, UUID.randomUUID().toString());
        deathBackpack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private record PendingDeathCollection(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, Set<UUID> existingDropIds) {
        private PendingDeathCollection {
            existingDropIds = Set.copyOf(existingDropIds);
        }
    }

    private void replaceVanillaBookshelfInInventory(ServerPlayer player) {
        boolean changed = false;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(Items.BOOKSHELF)) {
                continue;
            }
            int booksToGive = stack.getCount() * 3;
            player.getInventory().setItem(slot, ItemStack.EMPTY);
            while (booksToGive > 0) {
                int batch = Math.min(booksToGive, Items.BOOK.getDefaultMaxStackSize());
                ItemStack books = new ItemStack(Items.BOOK, batch);
                if (!player.getInventory().add(books)) {
                    player.drop(books, false);
                }
                booksToGive -= batch;
            }
            changed = true;
        }
        if (changed) {
            player.getInventory().setChanged();
        }
    }

    private boolean canManageDiscordBridge(ServerPlayer player) {
        if (player.getAbilities().instabuild || player.isCreative()) {
            return true;
        }

        var server = player.level().getServer();
        if (server == null) {
            return false;
        }

        if (server.isSingleplayer()) {
            var owner = server.getSingleplayerProfile();
            if (owner != null && owner.id().equals(player.getGameProfile().id())) {
                return true;
            }
        }

        return server.getPlayerList().isOp(new NameAndId(player.getGameProfile()));
    }

    private boolean sortOpenContainer(ServerPlayer player, SortBackpackPayload.Target target) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null) {
            return false;
        }

        if (target == SortBackpackPayload.Target.PLAYER) {
            return sortPlayerInventorySlots(menu, player);
        }

        boolean sorted;
        if (menu instanceof InventoryMenu) {
            sorted = sortSlotRange(menu, 0, InventoryMenu.INV_SLOT_START);
        } else {
            int topSlotCount = findTopSlotCount(menu, player);
            if (topSlotCount <= 0) {
                return false;
            }
            sorted = sortSlotRange(menu, 0, topSlotCount);
        }

        if (sorted) {
            menu.broadcastChanges();
        }
        return sorted;
    }

    private boolean sortPlayerInventorySlots(AbstractContainerMenu menu, ServerPlayer player) {
        List<Integer> playerSlots = new ArrayList<>();
        int nonEquipmentSlotCount = player.getInventory().getNonEquipmentItems().size();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            int containerSlot = slot.getContainerSlot();
            if (slot.container == player.getInventory()
                    && containerSlot >= PLAYER_HOTBAR_SLOT_COUNT
                    && containerSlot < nonEquipmentSlotCount) {
                playerSlots.add(i);
            }
        }

        if (playerSlots.isEmpty()) {
            return false;
        }

        List<ItemStack> stacks = new ArrayList<>(playerSlots.size());
        for (int slotIndex : playerSlots) {
            ItemStack stack = menu.getSlot(slotIndex).getItem();
            if (!stack.isEmpty()) {
                stacks.add(stack.copy());
            }
        }

        if (stacks.isEmpty()) {
            return false;
        }

        applySortedStacks(menu, playerSlots, stacks);
        menu.broadcastChanges();
        return true;
    }

    private int findTopSlotCount(AbstractContainerMenu menu, ServerPlayer player) {
        int count = 0;
        for (Slot slot : menu.slots) {
            if (slot.container == player.getInventory()) {
                break;
            }
            count++;
        }
        return count;
    }

    private boolean sortSlotRange(AbstractContainerMenu menu, int startInclusive, int endExclusive) {
        List<Integer> slotIndexes = new ArrayList<>();
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = startInclusive; i < endExclusive; i++) {
            slotIndexes.add(i);
            ItemStack stack = menu.getSlot(i).getItem();
            if (!stack.isEmpty()) {
                stacks.add(stack.copy());
            }
        }

        if (stacks.isEmpty()) {
            return false;
        }

        applySortedStacks(menu, slotIndexes, stacks);
        return true;
    }

    private void applySortedStacks(AbstractContainerMenu menu, List<Integer> targetSlots, List<ItemStack> stacks) {
        stacks.sort((left, right) -> {
            String leftId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(left.getItem()).toString();
            String rightId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(right.getItem()).toString();
            int compare = leftId.compareTo(rightId);
            if (compare != 0) {
                return compare;
            }
            return Integer.compare(right.getCount(), left.getCount());
        });

        List<ItemStack> compacted = compactStacks(stacks);
        for (int i = 0; i < targetSlots.size(); i++) {
            ItemStack stack = i < compacted.size() ? compacted.get(i).copy() : ItemStack.EMPTY;
            menu.getSlot(targetSlots.get(i)).setByPlayer(stack);
        }
    }

    private List<ItemStack> compactStacks(List<ItemStack> stacks) {
        List<ItemStack> compacted = new ArrayList<>();
        for (ItemStack stack : stacks) {
            ItemStack remaining = stack.copy();
            if (!compacted.isEmpty()) {
                ItemStack last = compacted.get(compacted.size() - 1);
                if (ItemStack.isSameItemSameComponents(last, remaining)) {
                    int movable = Math.min(last.getMaxStackSize() - last.getCount(), remaining.getCount());
                    if (movable > 0) {
                        last.grow(movable);
                        remaining.shrink(movable);
                    }
                }
            }
            while (!remaining.isEmpty()) {
                int count = remaining.getCount();
                int max = remaining.getMaxStackSize();
                int take = Math.min(count, max);
                ItemStack next = remaining.copy();
                next.setCount(take);
                compacted.add(next);
                remaining.shrink(take);
            }
        }
        return compacted;
    }

}
