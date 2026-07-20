package com.adaptor.deadrecall.client;

import com.adaptor.deadrecall.registry.TotemAutomataItemRegistration;
import com.adaptor.deadrecall.item.copper.CopperGolemWrenchHandler;
import com.adaptor.deadrecall.network.CopperGolemVisualizationPayload;
import com.adaptor.deadrecall.network.RequestCopperGolemVisualizationPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class CopperGolemVisualizationClient {
    private static final int REQUEST_INTERVAL_TICKS = 40;
    private static final int DRAW_INTERVAL_TICKS = 8;
    private static final int MAX_LINE_PARTICLES = 48;

    private static UUID heldGolemId = null;
    private static String heldDimension = "";
    private static int requestCooldown = 0;
    private static int drawCooldown = 0;
    private static CopperGolemVisualizationPayload cachedPayload = null;

    private CopperGolemVisualizationClient() {
    }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(CopperGolemVisualizationClient::tick);
        ClientPlayNetworking.registerGlobalReceiver(CopperGolemVisualizationPayload.TYPE,
                (payload, context) -> context.client().execute(() -> accept(payload)));
    }

    private static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            clear();
            return;
        }

        UUID selected = selectedGolemFromHeldWrench(mc);
        String dimension = mc.level.dimension().identifier().toString();
        if (selected == null) {
            clear();
            return;
        }

        boolean changed = !selected.equals(heldGolemId) || !dimension.equals(heldDimension);
        heldGolemId = selected;
        heldDimension = dimension;
        if (changed) {
            cachedPayload = null;
            requestCooldown = 0;
            drawCooldown = 0;
        }

        if (requestCooldown-- <= 0) {
            requestCooldown = REQUEST_INTERVAL_TICKS;
            requestVisualization(selected);
        }

        if (drawCooldown-- <= 0) {
            drawCooldown = DRAW_INTERVAL_TICKS;
            drawCached(mc.level, selected, dimension);
        }
    }

    private static UUID selectedGolemFromHeldWrench(Minecraft mc) {
        UUID mainHand = selectedGolem(mc.player.getMainHandItem());
        if (mainHand != null) {
            return mainHand;
        }
        return selectedGolem(mc.player.getOffhandItem());
    }

    private static UUID selectedGolem(ItemStack stack) {
        if (!stack.is(TotemAutomataItemRegistration.COPPER_WRENCH)) {
            return null;
        }
        return CopperGolemWrenchHandler.getSelectedGolem(stack);
    }

    private static void requestVisualization(UUID golemId) {
        if (ClientPlayNetworking.canSend(RequestCopperGolemVisualizationPayload.TYPE)) {
            ClientPlayNetworking.send(new RequestCopperGolemVisualizationPayload(golemId));
        }
    }

    private static void accept(CopperGolemVisualizationPayload payload) {
        if (heldGolemId == null || !heldGolemId.equals(payload.golemId()) || !payload.valid()) {
            cachedPayload = null;
            return;
        }
        cachedPayload = payload;
    }

    private static void drawCached(ClientLevel level, UUID selected, String dimension) {
        CopperGolemVisualizationPayload payload = cachedPayload;
        if (payload == null
                || !payload.valid()
                || !selected.equals(payload.golemId())
                || !dimension.equals(payload.dimension())) {
            return;
        }

        Vec3 golemCenter = new Vec3(payload.golemX(), payload.golemY() + 0.9D, payload.golemZ());
        drawBlockedHint(level, payload, golemCenter);
        if ("gathering".equals(payload.mode())) {
            drawGathering(level, payload, golemCenter, dimension);
        } else {
            drawSorting(level, payload, golemCenter, dimension);
        }
    }

    private static void drawSorting(ClientLevel level, CopperGolemVisualizationPayload payload, Vec3 golemCenter, String dimension) {
        drawPosLine(level, golemCenter, payload.source(), dimension, ParticleTypes.WAX_ON);
        for (CopperGolemVisualizationPayload.PosEntry destination : payload.destinations()) {
            drawPosLine(level, golemCenter, destination, dimension,
                    destination.available() ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.SMOKE);
        }
    }

    private static void drawGathering(ClientLevel level, CopperGolemVisualizationPayload payload, Vec3 golemCenter, String dimension) {
        drawPosLine(level, golemCenter, payload.source(), dimension, ParticleTypes.WAX_ON);
        drawArea(level, payload.gatheringArea(), dimension);
        drawTarget(level, payload.gatheringTarget(), dimension);
    }

    private static void drawPosLine(ClientLevel level, Vec3 start, CopperGolemVisualizationPayload.PosEntry entry, String dimension, ParticleOptions particle) {
        if (entry == null || !dimension.equals(entry.dimension())) {
            return;
        }

        Vec3 end = Vec3.atCenterOf(new BlockPos(entry.x(), entry.y(), entry.z()));
        drawLine(level, start, end, entry.available() ? particle : ParticleTypes.SMOKE, 0.9D);
    }

    private static void drawArea(ClientLevel level, CopperGolemVisualizationPayload.AreaEntry area, String dimension) {
        if (area == null || !dimension.equals(area.dimension())) {
            return;
        }
        if (area.hasCornerA()) {
            drawCorner(level, new BlockPos(area.cornerAX(), area.cornerAY(), area.cornerAZ()));
        }
        if (area.hasCornerB()) {
            drawCorner(level, new BlockPos(area.cornerBX(), area.cornerBY(), area.cornerBZ()));
        }
        if (!area.hasCornerA() || !area.hasCornerB()) {
            return;
        }

        int minX = Math.min(area.cornerAX(), area.cornerBX());
        int minY = Math.min(area.cornerAY(), area.cornerBY());
        int minZ = Math.min(area.cornerAZ(), area.cornerBZ());
        int maxX = Math.max(area.cornerAX(), area.cornerBX()) + 1;
        int maxY = Math.max(area.cornerAY(), area.cornerBY()) + 1;
        int maxZ = Math.max(area.cornerAZ(), area.cornerBZ()) + 1;

        drawBoxEdge(level, minX, minY, minZ, maxX, minY, minZ);
        drawBoxEdge(level, minX, minY, maxZ, maxX, minY, maxZ);
        drawBoxEdge(level, minX, maxY, minZ, maxX, maxY, minZ);
        drawBoxEdge(level, minX, maxY, maxZ, maxX, maxY, maxZ);
        drawBoxEdge(level, minX, minY, minZ, minX, maxY, minZ);
        drawBoxEdge(level, maxX, minY, minZ, maxX, maxY, minZ);
        drawBoxEdge(level, minX, minY, maxZ, minX, maxY, maxZ);
        drawBoxEdge(level, maxX, minY, maxZ, maxX, maxY, maxZ);
        drawBoxEdge(level, minX, minY, minZ, minX, minY, maxZ);
        drawBoxEdge(level, maxX, minY, minZ, maxX, minY, maxZ);
        drawBoxEdge(level, minX, maxY, minZ, minX, maxY, maxZ);
        drawBoxEdge(level, maxX, maxY, minZ, maxX, maxY, maxZ);
    }

    private static void drawTarget(ClientLevel level, CopperGolemVisualizationPayload.PosEntry target, String dimension) {
        if (target == null || !dimension.equals(target.dimension())) {
            return;
        }

        Vec3 center = Vec3.atCenterOf(new BlockPos(target.x(), target.y(), target.z()));
        for (int i = 0; i < 5; i++) {
            level.addParticle(ParticleTypes.END_ROD, center.x, center.y - 0.35D + i * 0.25D, center.z, 0.0D, 0.01D, 0.0D);
        }
    }

    private static void drawCorner(ClientLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        level.addParticle(ParticleTypes.END_ROD, center.x, center.y, center.z, 0.0D, 0.01D, 0.0D);
        level.addParticle(ParticleTypes.WAX_ON, center.x, center.y + 0.25D, center.z, 0.0D, 0.0D, 0.0D);
    }

    private static void drawBoxEdge(ClientLevel level, double x1, double y1, double z1, double x2, double y2, double z2) {
        drawLine(level, new Vec3(x1, y1, z1), new Vec3(x2, y2, z2), ParticleTypes.ELECTRIC_SPARK, 1.2D);
    }

    private static void drawLine(ClientLevel level, Vec3 start, Vec3 end, ParticleOptions particle, double spacing) {
        double distance = start.distanceTo(end);
        int points = Math.max(2, Math.min(MAX_LINE_PARTICLES, (int) Math.ceil(distance / Math.max(0.25D, spacing))));
        for (int i = 0; i <= points; i++) {
            double t = i / (double) points;
            level.addParticle(
                    particle,
                    start.x + (end.x - start.x) * t,
                    start.y + (end.y - start.y) * t,
                    start.z + (end.z - start.z) * t,
                    0.0D,
                    0.0D,
                    0.0D
            );
        }
    }

    private static void drawBlockedHint(ClientLevel level, CopperGolemVisualizationPayload payload, Vec3 golemCenter) {
        if (payload.activity() == null || !payload.activity().startsWith("blocked_")) {
            return;
        }
        level.addParticle(ParticleTypes.SMOKE, golemCenter.x, golemCenter.y + 0.6D, golemCenter.z, 0.0D, 0.04D, 0.0D);
        level.addParticle(ParticleTypes.SMOKE, golemCenter.x + 0.12D, golemCenter.y + 0.45D, golemCenter.z - 0.12D, 0.0D, 0.03D, 0.0D);
    }

    private static void clear() {
        heldGolemId = null;
        heldDimension = "";
        requestCooldown = 0;
        drawCooldown = 0;
        cachedPayload = null;
    }
}
