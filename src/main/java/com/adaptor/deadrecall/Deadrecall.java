package com.adaptor.deadrecall;

import com.adaptor.deadrecall.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Deadrecall implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("DeadRecall");

    @Override
    public void onInitialize() {
        // 註冊物品
        ModItems.registerModItems();

        // 初始化 Discord 橋接
        DiscordBridge.init(FabricLoader.getInstance().getConfigDir());

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
}
