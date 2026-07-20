from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
changed: list[str] = []


def write(rel: str, text: str) -> None:
    path = ROOT / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    old = path.read_text(encoding="utf-8") if path.exists() else None
    if old != text:
        path.write_text(text, encoding="utf-8")
        changed.append(rel)


def edit(rel: str, transform) -> None:
    path = ROOT / rel
    old = path.read_text(encoding="utf-8")
    new = transform(old)
    if new != old:
        path.write_text(new, encoding="utf-8")
        changed.append(rel)


def patch_initialization_order(text: str) -> str:
    text = text.replace("        LegacyDrinkItemMappings.registerAliases();\n", "")
    text = text.replace("        FacilityLegacyMappings.registerAliases();\n", "")
    text = text.replace(
        "        ScpAdditionsModItems.REGISTRY.register(bus);\n",
        "        ScpAdditionsModItems.REGISTRY.register(bus);\n"
        "        LegacyDrinkItemMappings.registerAliases();\n",
        1,
    )
    text = text.replace(
        "        FacilityModule.register(bus);\n",
        "        FacilityModule.register(bus);\n"
        "        FacilityLegacyMappings.registerAliases();\n",
        1,
    )
    return text


edit(
    "src/main/java/net/mcreator/scpadditions/ScpAdditionsMod.java",
    patch_initialization_order,
)

write(
    "src/main/java/net/mcreator/scpadditions/fabric/mixin/Scp572SwingMixin.java",
    '''package net.mcreator.scpadditions.fabric.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.mcreator.scpadditions.item.SCP572Item;
import net.mcreator.scpadditions.procedures.SCP572LivingEntityIsHitWithItemProcedure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class Scp572SwingMixin {
    @Inject(
            method = "swing(Lnet/minecraft/world/InteractionHand;Z)V",
            at = @At("HEAD"))
    private void scpAdditions$scp572Swing(
            InteractionHand hand,
            boolean updateSelf,
            CallbackInfo callback) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!entity.level().isClientSide()
                && entity.getItemInHand(hand).getItem() instanceof SCP572Item) {
            SCP572LivingEntityIsHitWithItemProcedure.execute(entity);
        }
    }
}
''',
)

write(
    "src/main/java/net/mcreator/scpadditions/fabric/mixin/client/ScreenAccessorMixin.java",
    '''package net.mcreator.scpadditions.fabric.mixin.client;

import net.fabricmc.fabric.api.client.screen.v1.ScreensAccessor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Screen.class)
abstract class ScreenAccessorMixin implements ScreensAccessor {
    @Shadow
    protected abstract <T extends GuiEventListener & Renderable & NarratableEntry>
            T addRenderableWidget(T widget);

    @Shadow
    protected abstract void removeWidget(GuiEventListener listener);

    @Override
    public void fabric$add(GuiEventListener listener) {
        if (!(listener instanceof AbstractWidget widget)) {
            throw new IllegalArgumentException(
                    "SCP Additions can only add renderable screen widgets through the compatibility bridge");
        }
        addRenderableWidget(widget);
    }

    @Override
    public void fabric$remove(GuiEventListener listener) {
        removeWidget(listener);
    }
}
''',
)

write(
    "src/main/java/net/mcreator/scpadditions/fabric/mixin/client/AbstractClientPlayerFovMixin.java",
    '''package net.mcreator.scpadditions.fabric.mixin.client;

import net.minecraft.client.player.AbstractClientPlayer;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
abstract class AbstractClientPlayerFovMixin {
    @Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
    private void scpAdditions$computeFovModifier(
            CallbackInfoReturnable<Float> callback) {
        float original = callback.getReturnValue();
        ComputeFovModifierEvent event = new ComputeFovModifierEvent(
                (AbstractClientPlayer) (Object) this, original);
        NeoForge.EVENT_BUS.post(event);
        callback.setReturnValue(event.getNewFovModifier());
    }
}
''',
)

write(
    "src/main/java/net/mcreator/scpadditions/fabric/mixin/client/PlayerRendererMixin.java",
    '''package net.mcreator.scpadditions.fabric.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
abstract class PlayerRendererMixin {
    @Inject(
            method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            cancellable = true)
    private void scpAdditions$renderPlayerPre(
            AbstractClientPlayer player,
            float yaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            CallbackInfo callback) {
        PlayerRenderer renderer = (PlayerRenderer) (Object) this;
        RenderPlayerEvent.Pre event = new RenderPlayerEvent.Pre(
                player, renderer, partialTick, poseStack, buffers, packedLight);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            NeoForge.EVENT_BUS.post(new RenderPlayerEvent.Post(
                    player, renderer, partialTick, poseStack, buffers, packedLight));
            callback.cancel();
        }
    }

    @Inject(
            method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"))
    private void scpAdditions$renderPlayerPost(
            AbstractClientPlayer player,
            float yaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            CallbackInfo callback) {
        NeoForge.EVENT_BUS.post(new RenderPlayerEvent.Post(
                player,
                (PlayerRenderer) (Object) this,
                partialTick,
                poseStack,
                buffers,
                packedLight));
    }
}
''',
)

mixins_path = ROOT / "src/main/resources/scp_additions.mixins.json"
metadata = json.loads(mixins_path.read_text(encoding="utf-8"))
common = list(metadata.get("mixins", []))
for name in ["MobEffectApplicableMixin", "Scp572SwingMixin"]:
    if name not in common:
        common.append(name)
metadata["mixins"] = common
client = list(metadata.get("client", []))
for name in [
    "client.ScreenAccessorMixin",
    "client.AbstractClientPlayerFovMixin",
    "client.PlayerRendererMixin",
]:
    if name not in client:
        client.append(name)
metadata["client"] = client
new_json = json.dumps(metadata, indent=2) + "\n"
old_json = mixins_path.read_text(encoding="utf-8")
if new_json != old_json:
    mixins_path.write_text(new_json, encoding="utf-8")
    changed.append("src/main/resources/scp_additions.mixins.json")

print(f"Fabric API round 7 changed {len(changed)} files")
for item in changed:
    print(item)
