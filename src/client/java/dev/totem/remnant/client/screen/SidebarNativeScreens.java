package dev.totem.remnant.client.screen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.input.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.network.chat.Component;
/** Read-only lifecycle wrappers; all visuals and slot rendering remain Mojang's production paths. */
final class SidebarNativeScreens {
    private SidebarNativeScreens() { }
    static final class InventoryView extends InventoryScreen implements SidebarProjection {
        private final RemotePlayer target;
        private final Runnable stop;
        private boolean stopped;
        private int[] data = new int[10];
        private java.util.Map<String,String> metadata = java.util.Map.of();
        @Override public void remnant$recipeState(int[] data, java.util.Map<String,String> metadata) {
            this.data=data.clone();this.metadata=java.util.Map.copyOf(metadata);
        }
        @Override protected void init() {
            super.init(); remnant$applyRecipeState();
            for(var child:children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)widget.active=false;
        }
        private void remnant$applyRecipeState() {
            RemnantSidebarObserverScreenProvider.recipeState(this,data,metadata);
            if ((Object)this instanceof AbstractRecipeBookScreen<?> recipe) {
                var access=(dev.totem.remnant.mixin.client.SidebarAbstractRecipeBookScreenAccessor)recipe;
                leftPos=access.remnant$getRecipeBookComponent().updateScreenPosition(width,imageWidth);
            }
        }
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int x,int y,float delta) {
            remnant$applyRecipeState();super.extractRenderState(g,x,y,delta);
        }
        InventoryView(RemotePlayer target, Runnable stop) { super(target); this.target = target; this.stop = stop; }

        @Override public RemotePlayer remnant$target() { return target; }
        @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubled) { return true; }
        @Override public boolean mouseReleased(MouseButtonEvent e) { return true; }
        @Override public boolean mouseDragged(MouseButtonEvent e, double x, double y) { return true; }
        @Override public boolean mouseScrolled(double x, double y, double dx, double dy) { return true; }
        @Override public boolean keyPressed(KeyEvent e) { if (e.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) onClose(); return true; }
        @Override public boolean keyReleased(KeyEvent e) { return true; }
        @Override public boolean charTyped(CharacterEvent e) { return true; }
        @Override public boolean preeditUpdated(PreeditEvent e) { return true; }
        @Override protected void slotClicked(Slot slot, int index, int button, ContainerInput input) { }
        @Override protected void handleSlotStateChanged(int a, int b, boolean c) { }
        @Override public void containerTick() { }
        @Override public void removed() { }
        @Override public void onClose() { if (!stopped) { stopped = true; stop.run(); } }
    }
    static final class CraftingView extends CraftingScreen implements SidebarProjection {
        private final RemotePlayer target;
        private final Runnable stop;
        private boolean stopped;
        private int[] data = new int[10];
        private java.util.Map<String,String> metadata = java.util.Map.of();
        @Override public void remnant$recipeState(int[] data, java.util.Map<String,String> metadata) {
            this.data=data.clone();this.metadata=java.util.Map.copyOf(metadata);
        }
        @Override protected void init() {
            super.init(); remnant$applyRecipeState();
            for(var child:children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)widget.active=false;
        }
        private void remnant$applyRecipeState() {
            RemnantSidebarObserverScreenProvider.recipeState(this,data,metadata);
            if ((Object)this instanceof AbstractRecipeBookScreen<?> recipe) {
                var access=(dev.totem.remnant.mixin.client.SidebarAbstractRecipeBookScreenAccessor)recipe;
                leftPos=access.remnant$getRecipeBookComponent().updateScreenPosition(width,imageWidth);
            }
        }
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int x,int y,float delta) {
            remnant$applyRecipeState();super.extractRenderState(g,x,y,delta);
        }
        CraftingView(CraftingMenu menu, Inventory inventory, Component title, RemotePlayer target, Runnable stop) { super(menu, inventory, title); this.target = target; this.stop = stop; }

        @Override public RemotePlayer remnant$target() { return target; }
        @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubled) { return true; }
        @Override public boolean mouseReleased(MouseButtonEvent e) { return true; }
        @Override public boolean mouseDragged(MouseButtonEvent e, double x, double y) { return true; }
        @Override public boolean mouseScrolled(double x, double y, double dx, double dy) { return true; }
        @Override public boolean keyPressed(KeyEvent e) { if (e.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) onClose(); return true; }
        @Override public boolean keyReleased(KeyEvent e) { return true; }
        @Override public boolean charTyped(CharacterEvent e) { return true; }
        @Override public boolean preeditUpdated(PreeditEvent e) { return true; }
        @Override protected void slotClicked(Slot slot, int index, int button, ContainerInput input) { }
        @Override protected void handleSlotStateChanged(int a, int b, boolean c) { }
        @Override public void containerTick() { }
        @Override public void removed() { }
        @Override public void onClose() { if (!stopped) { stopped = true; stop.run(); } }
    }
    static final class EnchantingView extends EnchantmentScreen implements SidebarProjection {
        private final RemotePlayer target;
        private final Runnable stop;
        private boolean stopped;
        private int[] data = new int[10];
        private java.util.Map<String,String> metadata = java.util.Map.of();
        @Override public void remnant$recipeState(int[] data, java.util.Map<String,String> metadata) {
            this.data=data.clone();this.metadata=java.util.Map.copyOf(metadata);
        }
        @Override protected void init() {
            super.init(); remnant$applyRecipeState();
            for(var child:children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)widget.active=false;
        }
        private void remnant$applyRecipeState() {
            RemnantSidebarObserverScreenProvider.recipeState(this,data,metadata);
            if ((Object)this instanceof AbstractRecipeBookScreen<?> recipe) {
                var access=(dev.totem.remnant.mixin.client.SidebarAbstractRecipeBookScreenAccessor)recipe;
                leftPos=access.remnant$getRecipeBookComponent().updateScreenPosition(width,imageWidth);
            }
        }
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int x,int y,float delta) {
            remnant$applyRecipeState();super.extractRenderState(g,x,y,delta);
        }
        EnchantingView(EnchantmentMenu menu, Inventory inventory, Component title, RemotePlayer target, Runnable stop) { super(menu, inventory, title); this.target = target; this.stop = stop; }

        @Override public RemotePlayer remnant$target() { return target; }
        @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubled) { return true; }
        @Override public boolean mouseReleased(MouseButtonEvent e) { return true; }
        @Override public boolean mouseDragged(MouseButtonEvent e, double x, double y) { return true; }
        @Override public boolean mouseScrolled(double x, double y, double dx, double dy) { return true; }
        @Override public boolean keyPressed(KeyEvent e) { if (e.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) onClose(); return true; }
        @Override public boolean keyReleased(KeyEvent e) { return true; }
        @Override public boolean charTyped(CharacterEvent e) { return true; }
        @Override public boolean preeditUpdated(PreeditEvent e) { return true; }
        @Override protected void slotClicked(Slot slot, int index, int button, ContainerInput input) { }
        @Override protected void handleSlotStateChanged(int a, int b, boolean c) { }
        @Override public void containerTick() { }
        @Override public void removed() { }
        @Override public void onClose() { if (!stopped) { stopped = true; stop.run(); } }
    }
    static final class ChestView extends ContainerScreen implements SidebarProjection {
        private final RemotePlayer target;
        private final Runnable stop;
        private boolean stopped;
        private int[] data = new int[10];
        private java.util.Map<String,String> metadata = java.util.Map.of();
        @Override public void remnant$recipeState(int[] data, java.util.Map<String,String> metadata) {
            this.data=data.clone();this.metadata=java.util.Map.copyOf(metadata);
        }
        @Override protected void init() {
            super.init(); remnant$applyRecipeState();
            for(var child:children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)widget.active=false;
        }
        private void remnant$applyRecipeState() {
            RemnantSidebarObserverScreenProvider.recipeState(this,data,metadata);
            if ((Object)this instanceof AbstractRecipeBookScreen<?> recipe) {
                var access=(dev.totem.remnant.mixin.client.SidebarAbstractRecipeBookScreenAccessor)recipe;
                leftPos=access.remnant$getRecipeBookComponent().updateScreenPosition(width,imageWidth);
            }
        }
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int x,int y,float delta) {
            remnant$applyRecipeState();super.extractRenderState(g,x,y,delta);
        }
        ChestView(ChestMenu menu, Inventory inventory, Component title, RemotePlayer target, Runnable stop) { super(menu, inventory, title); this.target = target; this.stop = stop; }

        @Override public RemotePlayer remnant$target() { return target; }
        @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubled) { return true; }
        @Override public boolean mouseReleased(MouseButtonEvent e) { return true; }
        @Override public boolean mouseDragged(MouseButtonEvent e, double x, double y) { return true; }
        @Override public boolean mouseScrolled(double x, double y, double dx, double dy) { return true; }
        @Override public boolean keyPressed(KeyEvent e) { if (e.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) onClose(); return true; }
        @Override public boolean keyReleased(KeyEvent e) { return true; }
        @Override public boolean charTyped(CharacterEvent e) { return true; }
        @Override public boolean preeditUpdated(PreeditEvent e) { return true; }
        @Override protected void slotClicked(Slot slot, int index, int button, ContainerInput input) { }
        @Override protected void handleSlotStateChanged(int a, int b, boolean c) { }
        @Override public void containerTick() { }
        @Override public void removed() { }
        @Override public void onClose() { if (!stopped) { stopped = true; stop.run(); } }
    }
    static final class FurnaceView extends FurnaceScreen implements SidebarProjection {
        private final RemotePlayer target;
        private final Runnable stop;
        private boolean stopped;
        private int[] data = new int[10];
        private java.util.Map<String,String> metadata = java.util.Map.of();
        @Override public void remnant$recipeState(int[] data, java.util.Map<String,String> metadata) {
            this.data=data.clone();this.metadata=java.util.Map.copyOf(metadata);
        }
        @Override public void init() {
            super.init(); remnant$applyRecipeState();
            for(var child:children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)widget.active=false;
        }
        private void remnant$applyRecipeState() {
            RemnantSidebarObserverScreenProvider.recipeState(this,data,metadata);
            if ((Object)this instanceof AbstractRecipeBookScreen<?> recipe) {
                var access=(dev.totem.remnant.mixin.client.SidebarAbstractRecipeBookScreenAccessor)recipe;
                leftPos=access.remnant$getRecipeBookComponent().updateScreenPosition(width,imageWidth);
            }
        }
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int x,int y,float delta) {
            remnant$applyRecipeState();super.extractRenderState(g,x,y,delta);
        }
        FurnaceView(FurnaceMenu menu, Inventory inventory, Component title, RemotePlayer target, Runnable stop) { super(menu, inventory, title); this.target = target; this.stop = stop; }

        @Override public RemotePlayer remnant$target() { return target; }
        @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubled) { return true; }
        @Override public boolean mouseReleased(MouseButtonEvent e) { return true; }
        @Override public boolean mouseDragged(MouseButtonEvent e, double x, double y) { return true; }
        @Override public boolean mouseScrolled(double x, double y, double dx, double dy) { return true; }
        @Override public boolean keyPressed(KeyEvent e) { if (e.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) onClose(); return true; }
        @Override public boolean keyReleased(KeyEvent e) { return true; }
        @Override public boolean charTyped(CharacterEvent e) { return true; }
        @Override public boolean preeditUpdated(PreeditEvent e) { return true; }
        @Override protected void slotClicked(Slot slot, int index, int button, ContainerInput input) { }
        @Override protected void handleSlotStateChanged(int a, int b, boolean c) { }
        @Override public void containerTick() { }
        @Override public void removed() { }
        @Override public void onClose() { if (!stopped) { stopped = true; stop.run(); } }
    }
    static final class SmokerView extends SmokerScreen implements SidebarProjection {
        private final RemotePlayer target;
        private final Runnable stop;
        private boolean stopped;
        private int[] data = new int[10];
        private java.util.Map<String,String> metadata = java.util.Map.of();
        @Override public void remnant$recipeState(int[] data, java.util.Map<String,String> metadata) {
            this.data=data.clone();this.metadata=java.util.Map.copyOf(metadata);
        }
        @Override public void init() {
            super.init(); remnant$applyRecipeState();
            for(var child:children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)widget.active=false;
        }
        private void remnant$applyRecipeState() {
            RemnantSidebarObserverScreenProvider.recipeState(this,data,metadata);
            if ((Object)this instanceof AbstractRecipeBookScreen<?> recipe) {
                var access=(dev.totem.remnant.mixin.client.SidebarAbstractRecipeBookScreenAccessor)recipe;
                leftPos=access.remnant$getRecipeBookComponent().updateScreenPosition(width,imageWidth);
            }
        }
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int x,int y,float delta) {
            remnant$applyRecipeState();super.extractRenderState(g,x,y,delta);
        }
        SmokerView(SmokerMenu menu, Inventory inventory, Component title, RemotePlayer target, Runnable stop) { super(menu, inventory, title); this.target = target; this.stop = stop; }

        @Override public RemotePlayer remnant$target() { return target; }
        @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubled) { return true; }
        @Override public boolean mouseReleased(MouseButtonEvent e) { return true; }
        @Override public boolean mouseDragged(MouseButtonEvent e, double x, double y) { return true; }
        @Override public boolean mouseScrolled(double x, double y, double dx, double dy) { return true; }
        @Override public boolean keyPressed(KeyEvent e) { if (e.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) onClose(); return true; }
        @Override public boolean keyReleased(KeyEvent e) { return true; }
        @Override public boolean charTyped(CharacterEvent e) { return true; }
        @Override public boolean preeditUpdated(PreeditEvent e) { return true; }
        @Override protected void slotClicked(Slot slot, int index, int button, ContainerInput input) { }
        @Override protected void handleSlotStateChanged(int a, int b, boolean c) { }
        @Override public void containerTick() { }
        @Override public void removed() { }
        @Override public void onClose() { if (!stopped) { stopped = true; stop.run(); } }
    }
    static final class BlastFurnaceView extends BlastFurnaceScreen implements SidebarProjection {
        private final RemotePlayer target;
        private final Runnable stop;
        private boolean stopped;
        private int[] data = new int[10];
        private java.util.Map<String,String> metadata = java.util.Map.of();
        @Override public void remnant$recipeState(int[] data, java.util.Map<String,String> metadata) {
            this.data=data.clone();this.metadata=java.util.Map.copyOf(metadata);
        }
        @Override public void init() {
            super.init(); remnant$applyRecipeState();
            for(var child:children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)widget.active=false;
        }
        private void remnant$applyRecipeState() {
            RemnantSidebarObserverScreenProvider.recipeState(this,data,metadata);
            if ((Object)this instanceof AbstractRecipeBookScreen<?> recipe) {
                var access=(dev.totem.remnant.mixin.client.SidebarAbstractRecipeBookScreenAccessor)recipe;
                leftPos=access.remnant$getRecipeBookComponent().updateScreenPosition(width,imageWidth);
            }
        }
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int x,int y,float delta) {
            remnant$applyRecipeState();super.extractRenderState(g,x,y,delta);
        }
        BlastFurnaceView(BlastFurnaceMenu menu, Inventory inventory, Component title, RemotePlayer target, Runnable stop) { super(menu, inventory, title); this.target = target; this.stop = stop; }

        @Override public RemotePlayer remnant$target() { return target; }
        @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubled) { return true; }
        @Override public boolean mouseReleased(MouseButtonEvent e) { return true; }
        @Override public boolean mouseDragged(MouseButtonEvent e, double x, double y) { return true; }
        @Override public boolean mouseScrolled(double x, double y, double dx, double dy) { return true; }
        @Override public boolean keyPressed(KeyEvent e) { if (e.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) onClose(); return true; }
        @Override public boolean keyReleased(KeyEvent e) { return true; }
        @Override public boolean charTyped(CharacterEvent e) { return true; }
        @Override public boolean preeditUpdated(PreeditEvent e) { return true; }
        @Override protected void slotClicked(Slot slot, int index, int button, ContainerInput input) { }
        @Override protected void handleSlotStateChanged(int a, int b, boolean c) { }
        @Override public void containerTick() { }
        @Override public void removed() { }
        @Override public void onClose() { if (!stopped) { stopped = true; stop.run(); } }
    }
    static final class AnvilView extends AnvilScreen implements SidebarProjection {
        private final RemotePlayer target;
        private final Runnable stop;
        private boolean stopped;
        private int[] data = new int[10];
        private java.util.Map<String,String> metadata = java.util.Map.of();
        @Override public void remnant$recipeState(int[] data, java.util.Map<String,String> metadata) {
            this.data=data.clone();this.metadata=java.util.Map.copyOf(metadata);
        }
        @Override public void init() {
            super.init(); remnant$applyRecipeState();
            for(var child:children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)widget.active=false;
        }
        private void remnant$applyRecipeState() {
            RemnantSidebarObserverScreenProvider.recipeState(this,data,metadata);
            if ((Object)this instanceof AbstractRecipeBookScreen<?> recipe) {
                var access=(dev.totem.remnant.mixin.client.SidebarAbstractRecipeBookScreenAccessor)recipe;
                leftPos=access.remnant$getRecipeBookComponent().updateScreenPosition(width,imageWidth);
            }
        }
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int x,int y,float delta) {
            remnant$applyRecipeState();super.extractRenderState(g,x,y,delta);
        }
        AnvilView(AnvilMenu menu, Inventory inventory, Component title, RemotePlayer target, Runnable stop) { super(menu, inventory, title); this.target = target; this.stop = stop; }

        @Override public RemotePlayer remnant$target() { return target; }
        @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubled) { return true; }
        @Override public boolean mouseReleased(MouseButtonEvent e) { return true; }
        @Override public boolean mouseDragged(MouseButtonEvent e, double x, double y) { return true; }
        @Override public boolean mouseScrolled(double x, double y, double dx, double dy) { return true; }
        @Override public boolean keyPressed(KeyEvent e) { if (e.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) onClose(); return true; }
        @Override public boolean keyReleased(KeyEvent e) { return true; }
        @Override public boolean charTyped(CharacterEvent e) { return true; }
        @Override public boolean preeditUpdated(PreeditEvent e) { return true; }
        @Override protected void slotClicked(Slot slot, int index, int button, ContainerInput input) { }
        @Override protected void handleSlotStateChanged(int a, int b, boolean c) { }
        @Override public void containerTick() { }
        @Override public void removed() { }
        @Override public void onClose() { if (!stopped) { stopped = true; stop.run(); } }
    }
    static final class StonecutterView extends StonecutterScreen implements SidebarProjection {
        private final RemotePlayer target;
        private final Runnable stop;
        private boolean stopped;
        private int[] data = new int[10];
        private java.util.Map<String,String> metadata = java.util.Map.of();
        @Override public void remnant$recipeState(int[] data, java.util.Map<String,String> metadata) {
            this.data=data.clone();this.metadata=java.util.Map.copyOf(metadata);
        }
        @Override public void init() {
            super.init(); remnant$applyRecipeState();
            for(var child:children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)widget.active=false;
        }
        private void remnant$applyRecipeState() {
            RemnantSidebarObserverScreenProvider.recipeState(this,data,metadata);
            if ((Object)this instanceof AbstractRecipeBookScreen<?> recipe) {
                var access=(dev.totem.remnant.mixin.client.SidebarAbstractRecipeBookScreenAccessor)recipe;
                leftPos=access.remnant$getRecipeBookComponent().updateScreenPosition(width,imageWidth);
            }
        }
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int x,int y,float delta) {
            remnant$applyRecipeState();super.extractRenderState(g,x,y,delta);
        }
        StonecutterView(StonecutterMenu menu, Inventory inventory, Component title, RemotePlayer target, Runnable stop) { super(menu, inventory, title); this.target = target; this.stop = stop; }

        @Override public RemotePlayer remnant$target() { return target; }
        @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubled) { return true; }
        @Override public boolean mouseReleased(MouseButtonEvent e) { return true; }
        @Override public boolean mouseDragged(MouseButtonEvent e, double x, double y) { return true; }
        @Override public boolean mouseScrolled(double x, double y, double dx, double dy) { return true; }
        @Override public boolean keyPressed(KeyEvent e) { if (e.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) onClose(); return true; }
        @Override public boolean keyReleased(KeyEvent e) { return true; }
        @Override public boolean charTyped(CharacterEvent e) { return true; }
        @Override public boolean preeditUpdated(PreeditEvent e) { return true; }
        @Override protected void slotClicked(Slot slot, int index, int button, ContainerInput input) { }
        @Override protected void handleSlotStateChanged(int a, int b, boolean c) { }
        @Override public void containerTick() { }
        @Override public void removed() { }
        @Override public void onClose() { if (!stopped) { stopped = true; stop.run(); } }
    }
    static final class SmithingView extends SmithingScreen implements SidebarProjection {
        private final RemotePlayer target;
        private final Runnable stop;
        private boolean stopped;
        private int[] data = new int[10];
        private java.util.Map<String,String> metadata = java.util.Map.of();
        @Override public void remnant$recipeState(int[] data, java.util.Map<String,String> metadata) {
            this.data=data.clone();this.metadata=java.util.Map.copyOf(metadata);
        }
        @Override public void init() {
            super.init(); remnant$applyRecipeState();
            for(var child:children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)widget.active=false;
        }
        private void remnant$applyRecipeState() {
            RemnantSidebarObserverScreenProvider.recipeState(this,data,metadata);
            if ((Object)this instanceof AbstractRecipeBookScreen<?> recipe) {
                var access=(dev.totem.remnant.mixin.client.SidebarAbstractRecipeBookScreenAccessor)recipe;
                leftPos=access.remnant$getRecipeBookComponent().updateScreenPosition(width,imageWidth);
            }
        }
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int x,int y,float delta) {
            remnant$applyRecipeState();super.extractRenderState(g,x,y,delta);
        }
        SmithingView(SmithingMenu menu, Inventory inventory, Component title, RemotePlayer target, Runnable stop) { super(menu, inventory, title); this.target = target; this.stop = stop; }

        @Override public RemotePlayer remnant$target() { return target; }
        @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubled) { return true; }
        @Override public boolean mouseReleased(MouseButtonEvent e) { return true; }
        @Override public boolean mouseDragged(MouseButtonEvent e, double x, double y) { return true; }
        @Override public boolean mouseScrolled(double x, double y, double dx, double dy) { return true; }
        @Override public boolean keyPressed(KeyEvent e) { if (e.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) onClose(); return true; }
        @Override public boolean keyReleased(KeyEvent e) { return true; }
        @Override public boolean charTyped(CharacterEvent e) { return true; }
        @Override public boolean preeditUpdated(PreeditEvent e) { return true; }
        @Override protected void slotClicked(Slot slot, int index, int button, ContainerInput input) { }
        @Override protected void handleSlotStateChanged(int a, int b, boolean c) { }
        @Override public void containerTick() { }
        @Override public void removed() { }
        @Override public void onClose() { if (!stopped) { stopped = true; stop.run(); } }
    }
    static final class GrindstoneView extends GrindstoneScreen implements SidebarProjection {
        private final RemotePlayer target;
        private final Runnable stop;
        private boolean stopped;
        private int[] data = new int[10];
        private java.util.Map<String,String> metadata = java.util.Map.of();
        @Override public void remnant$recipeState(int[] data, java.util.Map<String,String> metadata) {
            this.data=data.clone();this.metadata=java.util.Map.copyOf(metadata);
        }
        @Override public void init() {
            super.init(); remnant$applyRecipeState();
            for(var child:children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)widget.active=false;
        }
        private void remnant$applyRecipeState() {
            RemnantSidebarObserverScreenProvider.recipeState(this,data,metadata);
            if ((Object)this instanceof AbstractRecipeBookScreen<?> recipe) {
                var access=(dev.totem.remnant.mixin.client.SidebarAbstractRecipeBookScreenAccessor)recipe;
                leftPos=access.remnant$getRecipeBookComponent().updateScreenPosition(width,imageWidth);
            }
        }
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int x,int y,float delta) {
            remnant$applyRecipeState();super.extractRenderState(g,x,y,delta);
        }
        GrindstoneView(GrindstoneMenu menu, Inventory inventory, Component title, RemotePlayer target, Runnable stop) { super(menu, inventory, title); this.target = target; this.stop = stop; }

        @Override public RemotePlayer remnant$target() { return target; }
        @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubled) { return true; }
        @Override public boolean mouseReleased(MouseButtonEvent e) { return true; }
        @Override public boolean mouseDragged(MouseButtonEvent e, double x, double y) { return true; }
        @Override public boolean mouseScrolled(double x, double y, double dx, double dy) { return true; }
        @Override public boolean keyPressed(KeyEvent e) { if (e.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) onClose(); return true; }
        @Override public boolean keyReleased(KeyEvent e) { return true; }
        @Override public boolean charTyped(CharacterEvent e) { return true; }
        @Override public boolean preeditUpdated(PreeditEvent e) { return true; }
        @Override protected void slotClicked(Slot slot, int index, int button, ContainerInput input) { }
        @Override protected void handleSlotStateChanged(int a, int b, boolean c) { }
        @Override public void containerTick() { }
        @Override public void removed() { }
        @Override public void onClose() { if (!stopped) { stopped = true; stop.run(); } }
    }
    static final class LoomView extends LoomScreen implements SidebarProjection {
        private final RemotePlayer target;
        private final Runnable stop;
        private boolean stopped;
        private int[] data = new int[10];
        private java.util.Map<String,String> metadata = java.util.Map.of();
        @Override public void remnant$recipeState(int[] data, java.util.Map<String,String> metadata) {
            this.data=data.clone();this.metadata=java.util.Map.copyOf(metadata);
        }
        @Override public void init() {
            super.init(); remnant$applyRecipeState();
            for(var child:children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)widget.active=false;
        }
        private void remnant$applyRecipeState() {
            RemnantSidebarObserverScreenProvider.recipeState(this,data,metadata);
            if ((Object)this instanceof AbstractRecipeBookScreen<?> recipe) {
                var access=(dev.totem.remnant.mixin.client.SidebarAbstractRecipeBookScreenAccessor)recipe;
                leftPos=access.remnant$getRecipeBookComponent().updateScreenPosition(width,imageWidth);
            }
        }
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int x,int y,float delta) {
            remnant$applyRecipeState();super.extractRenderState(g,x,y,delta);
        }
        LoomView(LoomMenu menu, Inventory inventory, Component title, RemotePlayer target, Runnable stop) { super(menu, inventory, title); this.target = target; this.stop = stop; }

        @Override public RemotePlayer remnant$target() { return target; }
        @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubled) { return true; }
        @Override public boolean mouseReleased(MouseButtonEvent e) { return true; }
        @Override public boolean mouseDragged(MouseButtonEvent e, double x, double y) { return true; }
        @Override public boolean mouseScrolled(double x, double y, double dx, double dy) { return true; }
        @Override public boolean keyPressed(KeyEvent e) { if (e.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) onClose(); return true; }
        @Override public boolean keyReleased(KeyEvent e) { return true; }
        @Override public boolean charTyped(CharacterEvent e) { return true; }
        @Override public boolean preeditUpdated(PreeditEvent e) { return true; }
        @Override protected void slotClicked(Slot slot, int index, int button, ContainerInput input) { }
        @Override protected void handleSlotStateChanged(int a, int b, boolean c) { }
        @Override public void containerTick() { }
        @Override public void removed() { }
        @Override public void onClose() { if (!stopped) { stopped = true; stop.run(); } }
    }
    static final class CartographyView extends CartographyTableScreen implements SidebarProjection {
        private final RemotePlayer target;
        private final Runnable stop;
        private boolean stopped;
        private int[] data = new int[10];
        private java.util.Map<String,String> metadata = java.util.Map.of();
        @Override public void remnant$recipeState(int[] data, java.util.Map<String,String> metadata) {
            this.data=data.clone();this.metadata=java.util.Map.copyOf(metadata);
        }
        @Override public void init() {
            super.init(); remnant$applyRecipeState();
            for(var child:children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)widget.active=false;
        }
        private void remnant$applyRecipeState() {
            RemnantSidebarObserverScreenProvider.recipeState(this,data,metadata);
            if ((Object)this instanceof AbstractRecipeBookScreen<?> recipe) {
                var access=(dev.totem.remnant.mixin.client.SidebarAbstractRecipeBookScreenAccessor)recipe;
                leftPos=access.remnant$getRecipeBookComponent().updateScreenPosition(width,imageWidth);
            }
        }
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int x,int y,float delta) {
            remnant$applyRecipeState();super.extractRenderState(g,x,y,delta);
        }
        CartographyView(CartographyTableMenu menu, Inventory inventory, Component title, RemotePlayer target, Runnable stop) { super(menu, inventory, title); this.target = target; this.stop = stop; }

        @Override public RemotePlayer remnant$target() { return target; }
        @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubled) { return true; }
        @Override public boolean mouseReleased(MouseButtonEvent e) { return true; }
        @Override public boolean mouseDragged(MouseButtonEvent e, double x, double y) { return true; }
        @Override public boolean mouseScrolled(double x, double y, double dx, double dy) { return true; }
        @Override public boolean keyPressed(KeyEvent e) { if (e.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) onClose(); return true; }
        @Override public boolean keyReleased(KeyEvent e) { return true; }
        @Override public boolean charTyped(CharacterEvent e) { return true; }
        @Override public boolean preeditUpdated(PreeditEvent e) { return true; }
        @Override protected void slotClicked(Slot slot, int index, int button, ContainerInput input) { }
        @Override protected void handleSlotStateChanged(int a, int b, boolean c) { }
        @Override public void containerTick() { }
        @Override public void removed() { }
        @Override public void onClose() { if (!stopped) { stopped = true; stop.run(); } }
    }
    static final class BrewingView extends BrewingStandScreen implements SidebarProjection {
        private final RemotePlayer target;
        private final Runnable stop;
        private boolean stopped;
        private int[] data = new int[10];
        private java.util.Map<String,String> metadata = java.util.Map.of();
        @Override public void remnant$recipeState(int[] data, java.util.Map<String,String> metadata) {
            this.data=data.clone();this.metadata=java.util.Map.copyOf(metadata);
        }
        @Override public void init() {
            super.init(); remnant$applyRecipeState();
            for(var child:children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)widget.active=false;
        }
        private void remnant$applyRecipeState() {
            RemnantSidebarObserverScreenProvider.recipeState(this,data,metadata);
            if ((Object)this instanceof AbstractRecipeBookScreen<?> recipe) {
                var access=(dev.totem.remnant.mixin.client.SidebarAbstractRecipeBookScreenAccessor)recipe;
                leftPos=access.remnant$getRecipeBookComponent().updateScreenPosition(width,imageWidth);
            }
        }
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int x,int y,float delta) {
            remnant$applyRecipeState();super.extractRenderState(g,x,y,delta);
        }
        BrewingView(BrewingStandMenu menu, Inventory inventory, Component title, RemotePlayer target, Runnable stop) { super(menu, inventory, title); this.target = target; this.stop = stop; }

        @Override public RemotePlayer remnant$target() { return target; }
        @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubled) { return true; }
        @Override public boolean mouseReleased(MouseButtonEvent e) { return true; }
        @Override public boolean mouseDragged(MouseButtonEvent e, double x, double y) { return true; }
        @Override public boolean mouseScrolled(double x, double y, double dx, double dy) { return true; }
        @Override public boolean keyPressed(KeyEvent e) { if (e.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) onClose(); return true; }
        @Override public boolean keyReleased(KeyEvent e) { return true; }
        @Override public boolean charTyped(CharacterEvent e) { return true; }
        @Override public boolean preeditUpdated(PreeditEvent e) { return true; }
        @Override protected void slotClicked(Slot slot, int index, int button, ContainerInput input) { }
        @Override protected void handleSlotStateChanged(int a, int b, boolean c) { }
        @Override public void containerTick() { }
        @Override public void removed() { }
        @Override public void onClose() { if (!stopped) { stopped = true; stop.run(); } }
    }
    static final class HopperView extends HopperScreen implements SidebarProjection {
        private final RemotePlayer target;
        private final Runnable stop;
        private boolean stopped;
        private int[] data = new int[10];
        private java.util.Map<String,String> metadata = java.util.Map.of();
        @Override public void remnant$recipeState(int[] data, java.util.Map<String,String> metadata) {
            this.data=data.clone();this.metadata=java.util.Map.copyOf(metadata);
        }
        @Override public void init() {
            super.init(); remnant$applyRecipeState();
            for(var child:children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)widget.active=false;
        }
        private void remnant$applyRecipeState() {
            RemnantSidebarObserverScreenProvider.recipeState(this,data,metadata);
            if ((Object)this instanceof AbstractRecipeBookScreen<?> recipe) {
                var access=(dev.totem.remnant.mixin.client.SidebarAbstractRecipeBookScreenAccessor)recipe;
                leftPos=access.remnant$getRecipeBookComponent().updateScreenPosition(width,imageWidth);
            }
        }
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int x,int y,float delta) {
            remnant$applyRecipeState();super.extractRenderState(g,x,y,delta);
        }
        HopperView(HopperMenu menu, Inventory inventory, Component title, RemotePlayer target, Runnable stop) { super(menu, inventory, title); this.target = target; this.stop = stop; }

        @Override public RemotePlayer remnant$target() { return target; }
        @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubled) { return true; }
        @Override public boolean mouseReleased(MouseButtonEvent e) { return true; }
        @Override public boolean mouseDragged(MouseButtonEvent e, double x, double y) { return true; }
        @Override public boolean mouseScrolled(double x, double y, double dx, double dy) { return true; }
        @Override public boolean keyPressed(KeyEvent e) { if (e.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) onClose(); return true; }
        @Override public boolean keyReleased(KeyEvent e) { return true; }
        @Override public boolean charTyped(CharacterEvent e) { return true; }
        @Override public boolean preeditUpdated(PreeditEvent e) { return true; }
        @Override protected void slotClicked(Slot slot, int index, int button, ContainerInput input) { }
        @Override protected void handleSlotStateChanged(int a, int b, boolean c) { }
        @Override public void containerTick() { }
        @Override public void removed() { }
        @Override public void onClose() { if (!stopped) { stopped = true; stop.run(); } }
    }
    static final class MerchantView extends MerchantScreen implements SidebarProjection {
        private final RemotePlayer target;
        private final Runnable stop;
        private boolean stopped;
        private int[] data = new int[10];
        private java.util.Map<String,String> metadata = java.util.Map.of();
        @Override public void remnant$recipeState(int[] data, java.util.Map<String,String> metadata) {
            this.data=data.clone();this.metadata=java.util.Map.copyOf(metadata);
        }
        @Override public void init() {
            super.init(); remnant$applyRecipeState();
            for(var child:children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)widget.active=false;
        }
        private void remnant$applyRecipeState() {
            RemnantSidebarObserverScreenProvider.recipeState(this,data,metadata);
            if ((Object)this instanceof AbstractRecipeBookScreen<?> recipe) {
                var access=(dev.totem.remnant.mixin.client.SidebarAbstractRecipeBookScreenAccessor)recipe;
                leftPos=access.remnant$getRecipeBookComponent().updateScreenPosition(width,imageWidth);
            }
        }
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int x,int y,float delta) {
            remnant$applyRecipeState();super.extractRenderState(g,x,y,delta);
        }
        MerchantView(MerchantMenu menu, Inventory inventory, Component title, RemotePlayer target, Runnable stop) { super(menu, inventory, title); this.target = target; this.stop = stop; }

        @Override public RemotePlayer remnant$target() { return target; }
        @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubled) { return true; }
        @Override public boolean mouseReleased(MouseButtonEvent e) { return true; }
        @Override public boolean mouseDragged(MouseButtonEvent e, double x, double y) { return true; }
        @Override public boolean mouseScrolled(double x, double y, double dx, double dy) { return true; }
        @Override public boolean keyPressed(KeyEvent e) { if (e.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) onClose(); return true; }
        @Override public boolean keyReleased(KeyEvent e) { return true; }
        @Override public boolean charTyped(CharacterEvent e) { return true; }
        @Override public boolean preeditUpdated(PreeditEvent e) { return true; }
        @Override protected void slotClicked(Slot slot, int index, int button, ContainerInput input) { }
        @Override protected void handleSlotStateChanged(int a, int b, boolean c) { }
        @Override public void containerTick() { }
        @Override public void removed() { }
        @Override public void onClose() { if (!stopped) { stopped = true; stop.run(); } }
    }
}
