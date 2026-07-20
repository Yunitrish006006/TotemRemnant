package com.adaptor.deadrecall.mixin.client;

import com.adaptor.deadrecall.client.render.DeathBackpackBeamState;
import com.adaptor.deadrecall.registry.TotemRemnantItemRegistration;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {
    @Unique
    private static final int DEATH_BACKPACK_BEAM_COLOR = ARGB.color(255, 255, 64, 64);
    @Unique
    private static final int DEATH_BACKPACK_BEAM_GLOW_COLOR = ARGB.color(72, 255, 64, 64);
    @Unique
    private static final int DEATH_BACKPACK_BEAM_HEIGHT = 2048;
    @Unique
    private static final float DEATH_BACKPACK_SOLID_BEAM_RADIUS = 0.06F;
    @Unique
    private static final float DEATH_BACKPACK_GLOW_BEAM_RADIUS = 0.22F;

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V",
            at = @At("TAIL")
    )
    private void deadrecall$markDeathBackpackBeam(ItemEntity entity, ItemEntityRenderState state, float tickDelta, CallbackInfo ci) {
        ((DeathBackpackBeamState) state).deadrecall$setDeathBackpackBeam(entity.getItem().is(TotemRemnantItemRegistration.DEATH_BACKPACK));
    }

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("TAIL")
    )
    private void deadrecall$submitDeathBackpackBeam(ItemEntityRenderState state, PoseStack poseStack,
                                                    SubmitNodeCollector submitNodeCollector,
                                                    CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (!((DeathBackpackBeamState) state).deadrecall$hasDeathBackpackBeam()) {
            return;
        }

        poseStack.pushPose();
        submitNodeCollector.submitCustomGeometry(
                poseStack,
                RenderTypes.lightning(),
                (pose, vertexConsumer) -> {
                    deadrecall$renderBeamCross(
                            pose,
                            vertexConsumer,
                            DEATH_BACKPACK_GLOW_BEAM_RADIUS,
                            DEATH_BACKPACK_BEAM_HEIGHT,
                            DEATH_BACKPACK_BEAM_GLOW_COLOR
                    );
                    deadrecall$renderBeamCross(
                            pose,
                            vertexConsumer,
                            DEATH_BACKPACK_SOLID_BEAM_RADIUS,
                            DEATH_BACKPACK_BEAM_HEIGHT,
                            DEATH_BACKPACK_BEAM_COLOR
                    );
                }
        );
        poseStack.popPose();
    }

    @Unique
    private static void deadrecall$renderBeamCross(PoseStack.Pose pose, VertexConsumer vertexConsumer,
                                                   float radius, int height, int color) {
        deadrecall$renderQuad(pose, vertexConsumer, -radius, 0.0F, 0.0F, radius, height, 0.0F, color);
        deadrecall$renderQuad(pose, vertexConsumer, 0.0F, 0.0F, -radius, 0.0F, height, radius, color);
        deadrecall$renderQuad(pose, vertexConsumer, -radius * 0.7F, 0.0F, -radius * 0.7F,
                radius * 0.7F, height, radius * 0.7F, color);
        deadrecall$renderQuad(pose, vertexConsumer, -radius * 0.7F, 0.0F, radius * 0.7F,
                radius * 0.7F, height, -radius * 0.7F, color);
    }

    @Unique
    private static void deadrecall$renderQuad(PoseStack.Pose pose, VertexConsumer vertexConsumer,
                                              float x1, float y1, float z1,
                                              float x2, float y2, float z2,
                                              int color) {
        vertexConsumer.addVertex(pose, x1, y1, z1).setColor(color);
        vertexConsumer.addVertex(pose, x1, y2, z1).setColor(color);
        vertexConsumer.addVertex(pose, x2, y2, z2).setColor(color);
        vertexConsumer.addVertex(pose, x2, y1, z2).setColor(color);
    }
}
