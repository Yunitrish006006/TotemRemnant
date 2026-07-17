package com.adaptor.deadrecall;

import com.adaptor.deadrecall.inventory.ContainerNestingDiagnostics;
import com.adaptor.deadrecall.inventory.ContainerNestingDiagnostics.Finding;
import com.adaptor.deadrecall.inventory.ContainerNestingDiagnostics.ScanReport;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.ArrayList;
import java.util.List;

/** Registers the read-only legacy container-nesting report. */
public final class ContainerSafetyAdminInitializer implements ModInitializer {
    private static final int CHAT_FINDING_LIMIT = 20;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("deadrecall")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                        .then(Commands.literal("containers")
                                .then(Commands.literal("scan")
                                        .executes(context -> scanAll(context.getSource()))
                                        .then(Commands.argument("player", StringArgumentType.word())
                                                .executes(context -> scanPlayer(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "player")
                                                )))))));
    }

    private static int scanAll(CommandSourceStack source) {
        List<ScanReport> playerReports = new ArrayList<>();
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            playerReports.add(ContainerNestingDiagnostics.scanPlayer(player));
        }
        ScanReport report = ContainerNestingDiagnostics.merge(playerReports);
        sendReport(source, Component.translatable("message.deadrecall.container_scan.scope_all"), report);
        audit(source, report);
        return commandResult(report);
    }

    private static int scanPlayer(CommandSourceStack source, String playerName) {
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(playerName);
        if (player == null) {
            source.sendFailure(Component.translatable("message.deadrecall.container_scan.player_missing", playerName));
            return 0;
        }

        ScanReport report = ContainerNestingDiagnostics.scanPlayer(player);
        sendReport(source, Component.literal(player.getName().getString()), report);
        audit(source, report);
        return commandResult(report);
    }

    private static int commandResult(ScanReport report) {
        return Math.max(1, report.totalFindings());
    }

    private static void sendReport(CommandSourceStack source, Component scope, ScanReport report) {
        if (report.clean()) {
            source.sendSuccess(() -> Component.translatable(
                    "message.deadrecall.container_scan.clean",
                    scope,
                    report.scannedRoots(),
                    report.scannedStacks()
            ).withStyle(ChatFormatting.GREEN), false);
        } else {
            source.sendSuccess(() -> Component.translatable(
                    "message.deadrecall.container_scan.summary",
                    scope,
                    report.totalFindings(),
                    report.scannedRoots(),
                    report.scannedStacks()
            ).withStyle(ChatFormatting.YELLOW), false);
        }

        int displayed = Math.min(CHAT_FINDING_LIMIT, report.findings().size());
        for (int index = 0; index < displayed; index++) {
            Finding finding = report.findings().get(index);
            source.sendSuccess(() -> Component.translatable(
                    "message.deadrecall.container_scan.finding",
                    finding.owner(),
                    finding.path(),
                    finding.parentItemId(),
                    finding.childItemId(),
                    finding.depth(),
                    directionComponent(finding)
            ).withStyle(ChatFormatting.GOLD), false);
        }

        int hidden = report.totalFindings() - displayed;
        if (hidden > 0) {
            source.sendSuccess(() -> Component.translatable(
                    "message.deadrecall.container_scan.more",
                    hidden
            ).withStyle(ChatFormatting.GRAY), false);
        }
        if (report.truncated()) {
            source.sendSuccess(() -> Component.translatable(
                    "message.deadrecall.container_scan.truncated"
            ).withStyle(ChatFormatting.RED), false);
        }
    }

    private static Component directionComponent(Finding finding) {
        String suffix = switch (finding.direction()) {
            case RESTRICTED_CONTAINER_INSIDE_BACKPACK -> "inside_backpack";
            case BACKPACK_INSIDE_PORTABLE_CONTAINER -> "backpack_inside_container";
        };
        return Component.translatable("message.deadrecall.container_scan.direction." + suffix);
    }

    private static void audit(CommandSourceStack source, ScanReport report) {
        Deadrecall.LOGGER.info(
                "[ContainerSafety] {} ran a read-only nesting scan: roots={}, stacks={}, findings={}, truncated={}",
                source.getTextName(),
                report.scannedRoots(),
                report.scannedStacks(),
                report.totalFindings(),
                report.truncated()
        );
        for (Finding finding : report.findings()) {
            Deadrecall.LOGGER.warn(
                    "[ContainerSafety] invalid nesting owner={} path={} parent={} child={} depth={} direction={}",
                    finding.owner(),
                    finding.path(),
                    finding.parentItemId(),
                    finding.childItemId(),
                    finding.depth(),
                    finding.direction()
            );
        }
    }
}
