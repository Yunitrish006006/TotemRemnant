package dev.totem.remnant.mixin.client;

import dev.totem.remnant.client.render.DeathBackpackBeamState;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemEntityRenderState.class)
public class ItemEntityRenderStateMixin implements DeathBackpackBeamState {
    @Unique
    private boolean totem$deathBackpackBeam;

    @Override
    public void totem$setDeathBackpackBeam(boolean value) {
        this.totem$deathBackpackBeam = value;
    }

    @Override
    public boolean totem$hasDeathBackpackBeam() {
        return this.totem$deathBackpackBeam;
    }
}
