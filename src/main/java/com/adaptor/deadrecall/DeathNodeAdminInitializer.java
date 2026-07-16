package com.adaptor.deadrecall;

import com.adaptor.deadrecall.network.DeathNodeAdminPayload;
import com.adaptor.deadrecall.network.ManageDeathNodeAdminPayload;
import com.adaptor.deadrecall.network.RequestDeathNodeAdminPayload;
import com.adaptor.deadrecall.space.DeathNodeAdminService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.server.permissions.Permissions;

public final class DeathNodeAdminInitializer implements ModInitializer {
    @Override
    public void onInitialize() {
        PayloadTypeRegistry.clientboundPlay().register(DeathNodeAdminPayload.TYPE, DeathNodeAdminPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RequestDeathNodeAdminPayload.TYPE, RequestDeathNodeAdminPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ManageDeathNodeAdminPayload.TYPE, ManageDeathNodeAdminPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(RequestDeathNodeAdminPayload.TYPE,
                (payload, context) -> context.server().execute(() ->
                        DeathNodeAdminService.sendSnapshot(context.player())));
        ServerPlayNetworking.registerGlobalReceiver(ManageDeathNodeAdminPayload.TYPE,
                (payload, context) -> context.server().execute(() ->
                        DeathNodeAdminService.handleAction(context.player(), payload.nodeId(), payload.action())));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("deadrecall")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                        .then(Commands.literal("deathnodes")
                                .executes(context -> open(context.getSource().getPlayerOrException())))
                        .then(Commands.literal("deathpoints")
                                .executes(context -> open(context.getSource().getPlayerOrException())))));
    }

    private static int open(net.minecraft.server.level.ServerPlayer player) {
        DeathNodeAdminService.sendSnapshot(player);
        return 1;
    }
}
