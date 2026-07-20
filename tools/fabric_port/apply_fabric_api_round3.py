from __future__ import annotations

from pathlib import Path
import re

ROOT = Path.cwd()
JAVA = ROOT / 'src/main/java'
if not JAVA.exists():
    ROOT = Path(__file__).resolve().parents[2]
    JAVA = ROOT / 'src/main/java'
RES = ROOT / 'src/main/resources'


def write(rel: str, content: str) -> None:
    path = JAVA / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    normalized = content.strip() + '\n'
    if not path.exists() or path.read_text(encoding='utf-8') != normalized:
        path.write_text(normalized, encoding='utf-8')


def transform(rel: str, fn) -> None:
    path = JAVA / rel
    source = path.read_text(encoding='utf-8')
    updated = fn(source)
    if updated != source:
        path.write_text(updated, encoding='utf-8')

# ---------------------------------------------------------------------------
# NeoForge client event compatibility types backed by Fabric callbacks.
# ---------------------------------------------------------------------------
write('net/neoforged/neoforge/client/event/ClientPlayerNetworkEvent.java', '''
package net.neoforged.neoforge.client.event;
import net.neoforged.bus.api.Event;
public class ClientPlayerNetworkEvent extends Event {
    public static final class LoggingIn extends ClientPlayerNetworkEvent {}
    public static final class LoggingOut extends ClientPlayerNetworkEvent {}
}
''')
write('net/neoforged/neoforge/client/event/ClientTickEvent.java', '''
package net.neoforged.neoforge.client.event;
import net.neoforged.bus.api.Event;
public class ClientTickEvent extends Event {
    public static final class Pre extends ClientTickEvent {}
    public static final class Post extends ClientTickEvent {}
}
''')
write('net/neoforged/neoforge/client/event/InputEvent.java', '''
package net.neoforged.neoforge.client.event;
import net.neoforged.bus.api.Event;
public class InputEvent extends Event {
    public static final class Key extends InputEvent {}
    public static final class InteractionKeyMappingTriggered extends InputEvent {
        private final boolean useItem;
        public InteractionKeyMappingTriggered(boolean useItem) { this.useItem = useItem; }
        public boolean isUseItem() { return useItem; }
    }
    public static final class MouseScrollingEvent extends InputEvent {
        private final double scrollDeltaX;
        private final double scrollDeltaY;
        public MouseScrollingEvent(double x, double y) { scrollDeltaX=x; scrollDeltaY=y; }
        public double getScrollDeltaX() { return scrollDeltaX; }
        public double getScrollDeltaY() { return scrollDeltaY; }
    }
}
''')
write('net/neoforged/neoforge/client/event/ScreenEvent.java', '''
package net.neoforged.neoforge.client.event;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.Event;

public class ScreenEvent extends Event {
    private final Screen screen;
    protected ScreenEvent(Screen screen) { this.screen=screen; }
    public Screen getScreen() { return screen; }

    public static final class Init {
        public static final class Post extends ScreenEvent {
            private final List<GuiEventListener> listeners;
            private final Consumer<GuiEventListener> add;
            private final Consumer<GuiEventListener> remove;
            public Post(Screen screen, List<GuiEventListener> listeners,
                        Consumer<GuiEventListener> add, Consumer<GuiEventListener> remove) {
                super(screen); this.listeners=listeners; this.add=add; this.remove=remove;
            }
            public List<GuiEventListener> getListenersList() { return List.copyOf(listeners); }
            public void addListener(GuiEventListener listener) { add.accept(listener); }
            public void removeListener(GuiEventListener listener) { remove.accept(listener); }
        }
    }
    public static final class Render {
        public static class Pre extends ScreenEvent {
            private final GuiGraphics graphics; private final int mouseX, mouseY;
            public Pre(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
                super(screen); this.graphics=graphics; this.mouseX=mouseX; this.mouseY=mouseY;
            }
            public GuiGraphics getGuiGraphics(){return graphics;}
            public int getMouseX(){return mouseX;} public int getMouseY(){return mouseY;}
        }
        public static final class Post extends Pre {
            public Post(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) { super(screen,graphics,mouseX,mouseY); }
        }
    }
    public abstract static class MouseButton extends ScreenEvent {
        private final double mouseX, mouseY; private final int button;
        protected MouseButton(Screen screen,double mouseX,double mouseY,int button){super(screen);this.mouseX=mouseX;this.mouseY=mouseY;this.button=button;}
        public double getMouseX(){return mouseX;} public double getMouseY(){return mouseY;} public int getButton(){return button;}
    }
    public static final class MouseButtonPressed { public static final class Pre extends MouseButton { public Pre(Screen s,double x,double y,int b){super(s,x,y,b);} } }
    public static final class MouseButtonReleased { public static final class Pre extends MouseButton { public Pre(Screen s,double x,double y,int b){super(s,x,y,b);} } }
    public static final class MouseDragged {
        public static final class Pre extends MouseButton {
            private final double dragX,dragY;
            public Pre(Screen s,double x,double y,int b,double dx,double dy){super(s,x,y,b);dragX=dx;dragY=dy;}
            public int getMouseButton(){return getButton();} public double getDragX(){return dragX;} public double getDragY(){return dragY;}
        }
    }
    public static final class MouseScrolled {
        public static final class Pre extends ScreenEvent {
            private final double mouseX,mouseY,deltaX,deltaY;
            public Pre(Screen s,double x,double y,double dx,double dy){super(s);mouseX=x;mouseY=y;deltaX=dx;deltaY=dy;}
            public double getMouseX(){return mouseX;} public double getMouseY(){return mouseY;}
            public double getScrollDeltaX(){return deltaX;} public double getScrollDeltaY(){return deltaY;}
        }
    }
    public static final class KeyPressed {
        public static final class Pre extends ScreenEvent {
            private final int keyCode,scanCode,modifiers;
            public Pre(Screen s,int key,int scan,int modifiers){super(s);keyCode=key;scanCode=scan;this.modifiers=modifiers;}
            public int getKeyCode(){return keyCode;} public int getScanCode(){return scanCode;} public int getModifiers(){return modifiers;}
        }
    }
}
''')
write('net/neoforged/neoforge/client/event/RegisterKeyMappingsEvent.java', '''
package net.neoforged.neoforge.client.event;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.Event;
public final class RegisterKeyMappingsEvent extends Event {
    public void register(KeyMapping mapping) { KeyBindingHelper.registerKeyBinding(mapping); }
}
''')
write('net/neoforged/neoforge/client/event/RegisterMenuScreensEvent.java', '''
package net.neoforged.neoforge.client.event;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.Event;
public final class RegisterMenuScreensEvent extends Event {
    public <M extends AbstractContainerMenu, U extends AbstractContainerScreen<M> & MenuAccess<M>>
    void register(MenuType<? extends M> type, MenuScreens.ScreenConstructor<M,U> factory) {
        MenuScreens.register(type, factory);
    }
}
''')
write('net/neoforged/neoforge/client/event/EntityRenderersEvent.java', '''
package net.neoforged.neoforge.client.event;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.Event;
public class EntityRenderersEvent extends Event {
    public static final class RegisterRenderers extends EntityRenderersEvent {
        public <T extends Entity> void registerEntityRenderer(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
            EntityRendererRegistry.register(type, provider);
        }
    }
}
''')
write('net/neoforged/neoforge/client/event/RegisterParticleProvidersEvent.java', '''
package net.neoforged.neoforge.client.event;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.neoforged.bus.api.Event;
public final class RegisterParticleProvidersEvent extends Event {
    public <T extends ParticleOptions> void registerSpriteSet(ParticleType<T> type,
            ParticleFactoryRegistry.PendingParticleFactory<T> factory) {
        ParticleFactoryRegistry.getInstance().register(type, factory);
    }
}
''')
write('net/neoforged/neoforge/client/event/RegisterColorHandlersEvent.java', '''
package net.neoforged.neoforge.client.event;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.Event;
public class RegisterColorHandlersEvent extends Event {
    public static final class Block extends RegisterColorHandlersEvent {
        public void register(BlockColor provider, net.minecraft.world.level.block.Block... blocks) { ColorProviderRegistry.BLOCK.register(provider, blocks); }
    }
    public static final class Item extends RegisterColorHandlersEvent {
        public void register(ItemColor provider, ItemLike... items) { ColorProviderRegistry.ITEM.register(provider, items); }
    }
}
''')
write('net/neoforged/neoforge/client/event/RegisterGuiLayersEvent.java', '''
package net.neoforged.neoforge.client.event;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.fabric.FabricHudLayerRegistry;
import net.neoforged.bus.api.Event;
public final class RegisterGuiLayersEvent extends Event {
    @FunctionalInterface public interface Layer { void render(GuiGraphics graphics, DeltaTracker tracker); }
    public void registerAboveAll(ResourceLocation id, Layer layer) { FabricHudLayerRegistry.registerAbove(id, layer); }
    public void registerBelowAll(ResourceLocation id, Layer layer) { FabricHudLayerRegistry.registerBelow(id, layer); }
}
''')
write('net/neoforged/neoforge/client/event/RenderLevelStageEvent.java', '''
package net.neoforged.neoforge.client.event;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.neoforged.bus.api.Event;
public final class RenderLevelStageEvent extends Event {
    public enum Stage { AFTER_SOLID_BLOCKS }
    private final Stage stage; private final PoseStack poseStack; private final Camera camera;
    public RenderLevelStageEvent(Stage stage, PoseStack poseStack, Camera camera){this.stage=stage;this.poseStack=poseStack;this.camera=camera;}
    public Stage getStage(){return stage;} public PoseStack getPoseStack(){return poseStack;} public Camera getCamera(){return camera;}
}
''')
write('net/neoforged/neoforge/client/event/ComputeFovModifierEvent.java', '''
package net.neoforged.neoforge.client.event;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
public final class ComputeFovModifierEvent extends Event {
    private final Player player; private final float fovModifier; private float newFovModifier;
    public ComputeFovModifierEvent(Player player,float modifier){this.player=player;fovModifier=modifier;newFovModifier=modifier;}
    public Player getPlayer(){return player;} public float getFovModifier(){return fovModifier;}
    public float getNewFovModifier(){return newFovModifier;} public void setNewFovModifier(float value){newFovModifier=value;}
}
''')
write('net/neoforged/neoforge/client/event/RenderGuiLayerEvent.java', '''
package net.neoforged.neoforge.client.event;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
public class RenderGuiLayerEvent extends Event {
    private final ResourceLocation name;
    protected RenderGuiLayerEvent(ResourceLocation name){this.name=name;}
    public ResourceLocation getName(){return name;}
    public static final class Pre extends RenderGuiLayerEvent { public Pre(ResourceLocation name){super(name);} }
}
''')
write('net/neoforged/neoforge/client/event/RenderPlayerEvent.java', '''
package net.neoforged.neoforge.client.event;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.neoforged.bus.api.Event;
public class RenderPlayerEvent extends Event {
    private final AbstractClientPlayer entity; private final PlayerRenderer renderer;
    private final float partialTick; private final PoseStack poseStack;
    private final MultiBufferSource buffers; private final int packedLight;
    protected RenderPlayerEvent(AbstractClientPlayer entity,PlayerRenderer renderer,float partialTick,
            PoseStack poseStack,MultiBufferSource buffers,int packedLight){this.entity=entity;this.renderer=renderer;this.partialTick=partialTick;this.poseStack=poseStack;this.buffers=buffers;this.packedLight=packedLight;}
    public AbstractClientPlayer getEntity(){return entity;} public PlayerRenderer getRenderer(){return renderer;}
    public float getPartialTick(){return partialTick;} public PoseStack getPoseStack(){return poseStack;}
    public MultiBufferSource getMultiBufferSource(){return buffers;} public int getPackedLight(){return packedLight;}
    public static final class Pre extends RenderPlayerEvent { public Pre(AbstractClientPlayer e,PlayerRenderer r,float p,PoseStack s,MultiBufferSource b,int l){super(e,r,p,s,b,l);} }
    public static final class Post extends RenderPlayerEvent { public Post(AbstractClientPlayer e,PlayerRenderer r,float p,PoseStack s,MultiBufferSource b,int l){super(e,r,p,s,b,l);} }
}
''')
write('net/neoforged/neoforge/client/event/sound/PlaySoundSourceEvent.java', '''
package net.neoforged.neoforge.client.event.sound;
import com.mojang.blaze3d.audio.Channel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.neoforged.bus.api.Event;
public final class PlaySoundSourceEvent extends Event {
    private final SoundEngine engine; private final Channel channel; private final SoundInstance sound;
    public PlaySoundSourceEvent(SoundEngine engine,Channel channel,SoundInstance sound){this.engine=engine;this.channel=channel;this.sound=sound;}
    public SoundEngine getEngine(){return engine;} public Channel getChannel(){return channel;} public SoundInstance getSound(){return sound;}
}
''')
write('net/neoforged/neoforge/client/event/sound/PlayStreamingSourceEvent.java', '''
package net.neoforged.neoforge.client.event.sound;
import com.mojang.blaze3d.audio.Channel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.neoforged.bus.api.Event;
public final class PlayStreamingSourceEvent extends Event {
    private final SoundEngine engine; private final Channel channel; private final SoundInstance sound;
    public PlayStreamingSourceEvent(SoundEngine engine,Channel channel,SoundInstance sound){this.engine=engine;this.channel=channel;this.sound=sound;}
    public SoundEngine getEngine(){return engine;} public Channel getChannel(){return channel;} public SoundInstance getSound(){return sound;}
}
''')

# HUD layer storage with deterministic below/above ordering.
write('net/mcreator/scpadditions/fabric/FabricHudLayerRegistry.java', '''
package net.mcreator.scpadditions.fabric;
import java.util.*;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
public final class FabricHudLayerRegistry {
    private record Entry(ResourceLocation id, RegisterGuiLayersEvent.Layer layer) {}
    private static final List<Entry> BELOW=new ArrayList<>(), ABOVE=new ArrayList<>();
    private FabricHudLayerRegistry(){}
    public static void registerBelow(ResourceLocation id, RegisterGuiLayersEvent.Layer layer){BELOW.add(new Entry(id,layer));}
    public static void registerAbove(ResourceLocation id, RegisterGuiLayersEvent.Layer layer){ABOVE.add(new Entry(id,layer));}
    static void render(GuiGraphics graphics, DeltaTracker tracker){
        for(Entry entry:BELOW) entry.layer.render(graphics,tracker);
        for(Entry entry:ABOVE) entry.layer.render(graphics,tracker);
    }
}
''')

# Straightforward Fabric callback wiring. Cancelable hard hooks are supplied by mixins in a later round.
write('net/mcreator/scpadditions/fabric/FabricClientEventBridge.java', '''
package net.mcreator.scpadditions.fabric;

import java.util.ArrayList;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.*;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.common.NeoForge;

final class FabricClientEventBridge {
    private FabricClientEventBridge() {}
    static void register() {
        var modBus=ScpAdditionsFabric.MOD_BUS;
        modBus.post(new RegisterKeyMappingsEvent());
        modBus.post(new RegisterMenuScreensEvent());
        modBus.post(new EntityRenderersEvent.RegisterRenderers());
        modBus.post(new RegisterParticleProvidersEvent());
        modBus.post(new RegisterColorHandlersEvent.Block());
        modBus.post(new RegisterColorHandlersEvent.Item());
        modBus.post(new RegisterGuiLayersEvent());

        ClientTickEvents.START_CLIENT_TICK.register(client -> NeoForge.EVENT_BUS.post(new ClientTickEvent.Pre()));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            NeoForge.EVENT_BUS.post(new InputEvent.Key());
            NeoForge.EVENT_BUS.post(new ClientTickEvent.Post());
        });
        ClientPlayConnectionEvents.JOIN.register((handler,sender,client) -> NeoForge.EVENT_BUS.post(new ClientPlayerNetworkEvent.LoggingIn()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client) -> NeoForge.EVENT_BUS.post(new ClientPlayerNetworkEvent.LoggingOut()));
        HudRenderCallback.EVENT.register(FabricHudLayerRegistry::render);
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if(context.matrixStack()!=null) NeoForge.EVENT_BUS.post(new RenderLevelStageEvent(RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS, context.matrixStack(), context.camera()));
        });

        ScreenEvents.AFTER_INIT.register((client,screen,width,height) -> {
            var listeners=new ArrayList<GuiEventListener>(screen.children());
            NeoForge.EVENT_BUS.post(new ScreenEvent.Init.Post(screen,listeners,
                    listener -> ((net.fabricmc.fabric.api.client.screen.v1.ScreensAccessor)(Object)screen).fabric$add(listener),
                    listener -> ((net.fabricmc.fabric.api.client.screen.v1.ScreensAccessor)(Object)screen).fabric$remove(listener)));
            ScreenMouseEvents.allowMouseClick(screen).register((s,x,y,b) -> !NeoForge.EVENT_BUS.post(new ScreenEvent.MouseButtonPressed.Pre(s,x,y,b)));
            ScreenMouseEvents.allowMouseRelease(screen).register((s,x,y,b) -> !NeoForge.EVENT_BUS.post(new ScreenEvent.MouseButtonReleased.Pre(s,x,y,b)));
            ScreenMouseEvents.allowMouseScroll(screen).register((s,x,y,dx,dy) -> !NeoForge.EVENT_BUS.post(new ScreenEvent.MouseScrolled.Pre(s,x,y,dx,dy)));
            ScreenKeyboardEvents.allowKeyPress(screen).register((s,key,scan,mods) -> !NeoForge.EVENT_BUS.post(new ScreenEvent.KeyPressed.Pre(s,key,scan,mods)));
            ScreenEvents.beforeRender(screen).register((s,g,x,y,t) -> NeoForge.EVENT_BUS.post(new ScreenEvent.Render.Pre(s,g,x,y)));
            ScreenEvents.afterRender(screen).register((s,g,x,y,t) -> NeoForge.EVENT_BUS.post(new ScreenEvent.Render.Post(s,g,x,y)));
        });
    }
}
''')

# Fabric API does not expose generic add/remove listeners through Screens in Mojang
# mappings, so a tiny interface is injected by the screen mixin in round 4. For now
# supply the compile-time contract.
write('net/fabricmc/fabric/api/client/screen/v1/ScreensAccessor.java', '''
package net.fabricmc.fabric.api.client.screen.v1;
import net.minecraft.client.gui.components.events.GuiEventListener;
public interface ScreensAccessor {
    void fabric$add(GuiEventListener listener);
    void fabric$remove(GuiEventListener listener);
}
''')

# ---------------------------------------------------------------------------
# Server/common API gaps.
# ---------------------------------------------------------------------------
write('net/mcreator/scpadditions/fabric/FabricPersistentData.java', '''
package net.mcreator.scpadditions.fabric;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
public final class FabricPersistentData {
    private static final AttachmentType<CompoundTag> DATA=AttachmentRegistry.create(
        ResourceLocation.fromNamespaceAndPath("scp_additions","persistent_entity_data"),
        builder -> builder.initializer(CompoundTag::new).persistent(CompoundTag.CODEC).copyOnDeath());
    private FabricPersistentData(){}
    public static CompoundTag get(Entity entity){return entity.getAttachedOrCreate(DATA);}
}
''')

# Add registry aliases through Fabric's injected FabricRegistry interface.
transform('net/neoforged/neoforge/registries/DeferredRegister.java', lambda s: s if 'void addAlias(' in s else s.replace(
    '    public Collection<Supplier<T>> getEntries() {',
    '''    public void addAlias(ResourceLocation oldId, ResourceLocation newId) {
        ((net.fabricmc.fabric.api.event.registry.FabricRegistry)(Object) registry).addAlias(oldId, newId);
    }

    public Collection<Supplier<T>> getEntries() {'''))

# Compatibility channel registration is native Fabric and does not need a registrar object.
transform('com/bl4ues/scpadditions/compat/network/SimpleChannel.java', lambda s: s if 'registerPayload(net.neoforged' in s else s.replace(
    '    public synchronized void registerCommon() {',
    '''    public void registerPayload(net.neoforged.neoforge.network.registration.PayloadRegistrar ignored) { registerCommon(); }
    public synchronized void registerCommon() {'''))

# Fix Minecraft/Fabric method changes without losing gameplay behavior.
def item_remainders(source: str) -> str:
    source = re.sub(r'\n\s*@Override\n\s*public boolean hasCraftingRemainingItem\(\) \{\n\s*return true;\n\s*\}\n\n\s*@Override\n\s*public ItemStack getCraftingRemainingItem\(ItemStack itemstack\) \{\n\s*return new ItemStack\(this\);\n\s*\}\n', '\n', source)
    return source
for rel in [
    'net/mcreator/scpadditions/item/Scp1176honeyItem.java',
    'net/mcreator/scpadditions/item/SecurityCredentialsItem.java',
    'net/mcreator/scpadditions/item/Level1KeycardItem.java',
    'net/mcreator/scpadditions/item/Level2KeycardItem.java',
    'net/mcreator/scpadditions/item/Level3KeycardItem.java',
    'net/mcreator/scpadditions/item/Level4KeycardItem.java',
    'net/mcreator/scpadditions/item/Level5KeycardItem.java',
    'net/mcreator/scpadditions/item/Level6KeycardItem.java',
]: transform(rel, item_remainders)

# SCP-1176 already manually returns/inserts the glass bottle in finishUsingItem.
# Keycards/credentials remain undamaged because crafting consumes a copy from the
# custom SCP inventory recipes; vanilla recipe remainder hooks were never needed.
transform('net/mcreator/scpadditions/item/SCP572Item.java', lambda s: s.replace(
    '\n\t@Override\n\tpublic boolean onEntitySwing(ItemStack itemstack, LivingEntity entity) {\n\t\tboolean retval = super.onEntitySwing(itemstack, entity);\n\t\tSCP572LivingEntityIsHitWithItemProcedure.execute(entity);\n\t\treturn retval;\n\t}\n', '\n'))
# Preserve SCP-572 swing behavior through a Fabric attack-entity callback later;
# inventory effects remain active through inventoryTick.

transform('net/mcreator/scpadditions/item/OffsetKeycardReaderItem.java', lambda s: re.sub(
    r'\n\s*@Override\n\s*public void removeFromBlockToItemMap\(Map<Block, Item> blockToItemMap, Item item\) \{.*?\n\s*\}\n\}',
    '\n}', s, flags=re.DOTALL))
transform('net/mcreator/scpadditions/procedures/TeslaTerminalController.java', lambda s: s.replace('player.closeContainer();','player.closeContainer();'))

# Persistent data calls become true Fabric persistent attachments.
for path in JAVA.rglob('*.java'):
    source=path.read_text(encoding='utf-8', errors='ignore')
    if '.getPersistentData()' not in source and 'Player.PERSISTED_NBT_TAG' not in source:
        continue
    updated=re.sub(r'([A-Za-z_][A-Za-z0-9_]*)\.getPersistentData\(\)', r'net.mcreator.scpadditions.fabric.FabricPersistentData.get(\1)', source)
    updated=updated.replace('Player.PERSISTED_NBT_TAG', '"PlayerPersisted"')
    if updated!=source: path.write_text(updated,encoding='utf-8')

# Build creative tab event only needs an accept sink; Fabric ItemGroupEvents will
# supply it in the next bridge pass.
write('net/neoforged/neoforge/event/BuildCreativeModeTabContentsEvent.java', '''
package net.neoforged.neoforge.event;
import java.util.function.Consumer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.Event;
public class BuildCreativeModeTabContentsEvent extends Event {
    private final CreativeModeTab tab; private final Consumer<ItemStack> output;
    public BuildCreativeModeTabContentsEvent(CreativeModeTab tab,Consumer<ItemStack> output){this.tab=tab;this.output=output;}
    public CreativeModeTab getTab(){return tab;}
    public void accept(ItemLike item){output.accept(new ItemStack(item));}
    public void accept(ItemStack stack){output.accept(stack);}
}
''')

# Remove stale imports and adapt the key comparison to the public 1.21.1 API.
for rel in ['net/mcreator/scpadditions/block/DeconClosedBlock.java','net/mcreator/scpadditions/block/DeconOpenReloadBlock.java']:
    transform(rel, lambda s: re.sub(r'^import org\.checkerframework\.checker\.units\.qual\.s;\s*\n','',s,flags=re.MULTILINE))
transform('com/bl4ues/scpinventory/client/ContextConfigClientEvents.java', lambda s: s.replace(
    'event.getKeyCode() != Keybinds.CONTEXT_CONFIG_SELECT.getKey().getValue()',
    '!Keybinds.CONTEXT_CONFIG_SELECT.matches(event.getKeyCode(), event.getScanCode())'))

print('Applied Fabric API round 3 client event and common compatibility layer')
