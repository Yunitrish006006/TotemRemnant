package com.adaptor.deadrecall.mixin.client;

import com.adaptor.deadrecall.client.DeadrecallClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.Arrays;

@Mixin(Options.class)
public abstract class OptionsMixin {

    @Mutable
    @Final
    @Shadow
    public KeyMapping[] keyMappings;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void deadrecall$injectDiscordConfigKey(net.minecraft.client.Minecraft minecraft, File file, CallbackInfo ci) {
        KeyMapping[] keys = new KeyMapping[] {
                DeadrecallClient.openDiscordConfigKey,
                DeadrecallClient.sortBackpackKey
        };
        for (KeyMapping key : keys) {
            if (key == null) {
                continue;
            }
            boolean exists = false;
            for (KeyMapping km : keyMappings) {
                if (km == key) {
                    exists = true;
                    break;
                }
            }
            if (exists) {
                continue;
            }
            keyMappings = Arrays.copyOf(keyMappings, keyMappings.length + 1);
            keyMappings[keyMappings.length - 1] = key;
        }
    }
}
