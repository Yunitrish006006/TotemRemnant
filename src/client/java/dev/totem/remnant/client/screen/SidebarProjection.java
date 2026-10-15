package dev.totem.remnant.client.screen;
import dev.totem.core.api.v1.client.observer.ObserverReadOnlyScreen;
import net.minecraft.client.player.RemotePlayer;
/** Capability owned by Remnant's subclasses of the real Mojang Screens. */
public interface SidebarProjection extends ObserverReadOnlyScreen {
    RemotePlayer remnant$target();
    void remnant$recipeState(int[] data, java.util.Map<String,String> metadata);
    default boolean totem$isObserverReadOnly() { return true; }
}
