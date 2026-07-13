package com.adaptor.deadrecall.mixin;

import com.adaptor.deadrecall.DiscordBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalInt;

@Mixin(Raids.class)
public abstract class RaidsMixin {
    @Inject(method = "createOrExtendRaid", at = @At("RETURN"))
    private void deadrecall$notifyRaidStarted(
            ServerPlayer player,
            BlockPos pos,
            CallbackInfoReturnable<Raid> cir
    ) {
        Raid raid = cir.getReturnValue();
        if (raid == null) {
            return;
        }

        OptionalInt id = ((Raids) (Object) this).getId(raid);
        String raidKey = player.level().dimension().identifier() + ":" + id.orElse(System.identityHashCode(raid));
        DiscordBridge.sendRaidStarted(raidKey, player.getName().getString());
    }
}
