package dev.totem.remnant.mixin.client;

import dev.totem.remnant.client.render.DeathBackpackBeamState;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemEntityRenderState.class)
public class ItemEntityRenderStateMixin implements DeathBackpackBeamState {
    @Unique
    private boolean deadrecall$deathBackpackBeam;

    @Override
    public void deadrecall$setDeathBackpackBeam(boolean value) {
        this.deadrecall$deathBackpackBeam = value;
    }

    @Override
    public boolean deadrecall$hasDeathBackpackBeam() {
        return this.deadrecall$deathBackpackBeam;
    }
}
