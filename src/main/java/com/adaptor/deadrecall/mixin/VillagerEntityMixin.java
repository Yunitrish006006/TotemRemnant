package com.adaptor.deadrecall.mixin;

import com.adaptor.deadrecall.discord.DiscordEventNotifications;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerEntityMixin {

    @Unique
    private int deadrecall$previousVillagerLevel = -1;

    @Inject(method = "increaseMerchantCareer", at = @At("HEAD"))
    private void deadrecall$captureVillagerLevel(ServerLevel world, CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        this.deadrecall$previousVillagerLevel = self.getVillagerData().level();
    }

    @Inject(method = "increaseMerchantCareer", at = @At("TAIL"))
    private void deadrecall$notifyVillagerLevelUp(ServerLevel world, CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        int previousLevel = this.deadrecall$previousVillagerLevel;
        int currentLevel = self.getVillagerData().level();

        this.deadrecall$previousVillagerLevel = -1;

        if (previousLevel >= 0 && currentLevel > previousLevel) {
            String customName = self.hasCustomName() && self.getCustomName() != null
                    ? self.getCustomName().getString()
                    : "";
            String professionPath = self.getVillagerData().profession()
                    .unwrapKey()
                    .map(key -> key.identifier().getPath())
                    .orElse("none");
            DiscordEventNotifications.villagerLevelUp(
                    customName,
                    professionPath,
                    previousLevel,
                    currentLevel
            );
        }
    }
}
