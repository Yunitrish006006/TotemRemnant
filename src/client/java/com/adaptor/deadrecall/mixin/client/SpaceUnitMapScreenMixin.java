package com.adaptor.deadrecall.mixin.client;

import com.adaptor.deadrecall.client.SpaceUnitMapScreen;
import com.adaptor.deadrecall.network.RefreshSpaceUnitQuotePayload;
import com.adaptor.deadrecall.network.SpaceUnitMapPayload;
import com.adaptor.deadrecall.network.StartSpaceUnitTeleportPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;
import java.util.UUID;

@Mixin(SpaceUnitMapScreen.class)
public abstract class SpaceUnitMapScreenMixin {
    @Shadow
    private SpaceUnitMapPayload payload;

    @Shadow
    private UUID selectedUnitId;

    @Shadow
    private Button teleportButton;

    @Shadow
    private SpaceUnitMapPayload.Entry selectedEntry() {
        throw new AssertionError();
    }

    @Unique
    private UUID deadrecall$selectionBeforeClick;

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void deadrecall$captureSelection(
            MouseButtonEvent event,
            boolean doubleClick,
            CallbackInfoReturnable<Boolean> cir
    ) {
        this.deadrecall$selectionBeforeClick = this.selectedUnitId;
    }

    @Inject(method = "mouseClicked", at = @At("RETURN"))
    private void deadrecall$refreshSelectedQuote(
            MouseButtonEvent event,
            boolean doubleClick,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!cir.getReturnValue() || Objects.equals(this.deadrecall$selectionBeforeClick, this.selectedUnitId)) {
            return;
        }
        deadrecall$requestSelectedQuoteRefresh();
    }

    @Inject(method = "requestRefresh", at = @At("HEAD"), cancellable = true)
    private void deadrecall$refreshCurrentSelection(CallbackInfo ci) {
        SpaceUnitMapPayload.Entry selected = selectedEntry();
        if (selected == null || selected.id().equals(this.payload.sourceUnitId())) {
            return;
        }
        if (deadrecall$sendQuoteRefresh(selected.id())) {
            ci.cancel();
        }
    }

    @Inject(method = "updateButtonLayout", at = @At("TAIL"))
    private void deadrecall$allowAuthoritativeTeleportCheck(CallbackInfo ci) {
        if (this.teleportButton == null) {
            return;
        }
        SpaceUnitMapPayload.Entry selected = selectedEntry();
        this.teleportButton.active = selected != null && !selected.id().equals(this.payload.sourceUnitId());
    }

    @Inject(method = "requestTeleport", at = @At("HEAD"), cancellable = true)
    private void deadrecall$startWithFreshStructureCheck(CallbackInfo ci) {
        SpaceUnitMapPayload.Entry selected = selectedEntry();
        if (selected == null || selected.id().equals(this.payload.sourceUnitId())) {
            ci.cancel();
            return;
        }

        if (ClientPlayNetworking.canSend(StartSpaceUnitTeleportPayload.TYPE)) {
            ClientPlayNetworking.send(new StartSpaceUnitTeleportPayload(
                    this.payload.sourceType(),
                    this.payload.sourceUnitId(),
                    selected.id()
            ));
            ((SpaceUnitMapScreen) (Object) this).onClose();
        }
        ci.cancel();
    }

    @Unique
    private void deadrecall$requestSelectedQuoteRefresh() {
        SpaceUnitMapPayload.Entry selected = selectedEntry();
        if (selected != null && !selected.id().equals(this.payload.sourceUnitId())) {
            deadrecall$sendQuoteRefresh(selected.id());
        }
    }

    @Unique
    private boolean deadrecall$sendQuoteRefresh(UUID targetUnitId) {
        if (!ClientPlayNetworking.canSend(RefreshSpaceUnitQuotePayload.TYPE)) {
            return false;
        }
        ClientPlayNetworking.send(new RefreshSpaceUnitQuotePayload(
                this.payload.sourceType(),
                this.payload.sourceUnitId(),
                targetUnitId
        ));
        return true;
    }
}
