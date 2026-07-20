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


def patch_game_bridge(text: str) -> str:
    text = text.replace(
        "        ServerTickEvents.START_WORLD_TICK.register(level -> NeoForge.EVENT_BUS.post(new LevelTickEvent.Pre(level)));\n"
        "        ServerTickEvents.END_WORLD_TICK.register(level -> NeoForge.EVENT_BUS.post(new LevelTickEvent.Post(level)));",
        "        ServerTickEvents.START_WORLD_TICK.register(level -> {\n"
        "            NeoForge.EVENT_BUS.post(new LevelTickEvent.Pre(level));\n"
        "            level.players().forEach(player -> NeoForge.EVENT_BUS.post(new PlayerTickEvent.Pre(player)));\n"
        "        });\n"
        "        ServerTickEvents.END_WORLD_TICK.register(level -> {\n"
        "            level.players().forEach(player -> NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player)));\n"
        "            NeoForge.EVENT_BUS.post(new LevelTickEvent.Post(level));\n"
        "        });",
    )
    damage = '''        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            LivingIncomingDamageEvent event=new LivingIncomingDamageEvent(entity,source,amount);
            NeoForge.EVENT_BUS.post(event);
            return !event.isCanceled() && event.getAmount() > 0;
        });
'''
    text = text.replace(damage, "")
    text = text.replace(
        "        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> NeoForge.EVENT_BUS.post(new LivingDeathEvent(entity,source)));\n",
        "",
    )
    return text


edit(
    "src/main/java/net/mcreator/scpadditions/fabric/FabricGameEventBridge.java",
    patch_game_bridge,
)


def patch_client_bridge(text: str) -> str:
    text = text.replace(
        "        ClientTickEvents.START_CLIENT_TICK.register(client -> NeoForge.EVENT_BUS.post(new ClientTickEvent.Pre()));",
        "        ClientTickEvents.START_CLIENT_TICK.register(client -> {\n"
        "            NeoForge.EVENT_BUS.post(new ClientTickEvent.Pre());\n"
        "            if (client.player != null) {\n"
        "                NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre(client.player));\n"
        "            }\n"
        "        });",
    )
    text = text.replace(
        "        ClientTickEvents.END_CLIENT_TICK.register(client -> {\n"
        "            NeoForge.EVENT_BUS.post(new InputEvent.Key());",
        "        ClientTickEvents.END_CLIENT_TICK.register(client -> {\n"
        "            if (client.player != null) {\n"
        "                NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(client.player));\n"
        "            }\n"
        "            NeoForge.EVENT_BUS.post(new InputEvent.Key());",
    )
    return text


edit(
    "src/main/java/net/mcreator/scpadditions/fabric/FabricClientEventBridge.java",
    patch_client_bridge,
)

write(
    "src/main/java/net/mcreator/scpadditions/fabric/FabricLivingDropsCapture.java",
    '''package net.mcreator.scpadditions.fabric;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** Captures vanilla death item entities until the mutable NeoForge-style event is applied. */
public final class FabricLivingDropsCapture {
    private static final ThreadLocal<Deque<Context>> CONTEXTS =
            ThreadLocal.withInitial(ArrayDeque::new);

    private FabricLivingDropsCapture() {}

    public static void begin(LivingEntity entity, ServerLevel level) {
        CONTEXTS.get().push(new Context(entity, level, new ArrayList<>()));
    }

    public static boolean capture(ServerLevel level, Entity entity) {
        Deque<Context> stack = CONTEXTS.get();
        if (stack.isEmpty() || !(entity instanceof ItemEntity item)) {
            return false;
        }
        Context context = stack.peek();
        if (context.level != level) {
            return false;
        }
        context.drops.add(item);
        return true;
    }

    public static void finish(LivingEntity entity, ServerLevel level) {
        Deque<Context> stack = CONTEXTS.get();
        if (stack.isEmpty()) return;
        Context context = stack.pop();
        if (stack.isEmpty()) CONTEXTS.remove();
        if (context.entity != entity || context.level != level) {
            throw new IllegalStateException("Mismatched SCP Additions death-drop capture context");
        }
        LivingDropsEvent event = new LivingDropsEvent(entity, context.drops);
        NeoForge.EVENT_BUS.post(event);
        List<ItemEntity> finalDrops = List.copyOf(event.getDrops());
        for (ItemEntity drop : finalDrops) {
            if (drop != null && !drop.isRemoved() && !drop.getItem().isEmpty()) {
                level.addFreshEntity(drop);
            }
        }
    }

    private record Context(
            LivingEntity entity,
            ServerLevel level,
            List<ItemEntity> drops) {}
}
''',
)

write(
    "src/main/java/net/mcreator/scpadditions/fabric/mixin/LivingEntityEventMixin.java",
    '''package net.mcreator.scpadditions.fabric.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.mcreator.scpadditions.fabric.FabricLivingDropsCapture;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class LivingEntityEventMixin {
    @ModifyVariable(method = "heal", at = @At("HEAD"), argsOnly = true)
    private float scpAdditions$modifyHealAmount(float amount) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide()) return amount;
        LivingHealEvent event = new LivingHealEvent(entity, amount);
        NeoForge.EVENT_BUS.post(event);
        return event.isCanceled() ? 0.0F : Math.max(0.0F, event.getAmount());
    }

    @ModifyVariable(
            method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private float scpAdditions$modifyIncomingDamage(
            float amount,
            DamageSource source) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide()) return amount;
        LivingIncomingDamageEvent event =
                new LivingIncomingDamageEvent(entity, source, amount);
        NeoForge.EVENT_BUS.post(event);
        return event.isCanceled() ? 0.0F : Math.max(0.0F, event.getAmount());
    }

    @Inject(
            method = "startUsingItem(Lnet/minecraft/world/InteractionHand;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void scpAdditions$beforeUseItem(
            InteractionHand hand,
            CallbackInfo callback) {
        LivingEntity entity = (LivingEntity) (Object) this;
        ItemStack stack = entity.getItemInHand(hand);
        LivingEntityUseItemEvent.Start event =
                new LivingEntityUseItemEvent.Start(entity, stack);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) callback.cancel();
    }

    @Inject(method = "completeUsingItem", at = @At("HEAD"))
    private void scpAdditions$finishUseItem(CallbackInfo callback) {
        LivingEntity entity = (LivingEntity) (Object) this;
        ItemStack used = entity.getUseItem().copy();
        if (!used.isEmpty()) {
            NeoForge.EVENT_BUS.post(
                    new LivingEntityUseItemEvent.Finish(entity, used));
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void scpAdditions$afterLivingTick(CallbackInfo callback) {
        NeoForge.EVENT_BUS.post(
                new EntityTickEvent.Post((LivingEntity) (Object) this));
    }

    @Inject(
            method = "dropAllDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V",
            at = @At("HEAD"))
    private void scpAdditions$beginDeathDrops(
            ServerLevel level,
            DamageSource source,
            CallbackInfo callback) {
        FabricLivingDropsCapture.begin((LivingEntity) (Object) this, level);
    }

    @Inject(
            method = "dropAllDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V",
            at = @At("RETURN"))
    private void scpAdditions$finishDeathDrops(
            ServerLevel level,
            DamageSource source,
            CallbackInfo callback) {
        FabricLivingDropsCapture.finish((LivingEntity) (Object) this, level);
    }
}
''',
)

write(
    "src/main/java/net/mcreator/scpadditions/fabric/mixin/ServerLevelDropsMixin.java",
    '''package net.mcreator.scpadditions.fabric.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.mcreator.scpadditions.fabric.FabricLivingDropsCapture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
abstract class ServerLevelDropsMixin {
    @Inject(
            method = "addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void scpAdditions$captureDeathDrop(
            Entity entity,
            CallbackInfoReturnable<Boolean> callback) {
        if (FabricLivingDropsCapture.capture(
                (ServerLevel) (Object) this, entity)) {
            callback.setReturnValue(true);
        }
    }
}
''',
)

write(
    "src/main/java/net/mcreator/scpadditions/fabric/mixin/ProjectileImpactMixin.java",
    '''package net.mcreator.scpadditions.fabric.mixin;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
abstract class ProjectileImpactMixin {
    @Inject(
            method = "onHit(Lnet/minecraft/world/phys/HitResult;)V",
            at = @At("HEAD"))
    private void scpAdditions$projectileImpact(
            HitResult hit,
            CallbackInfo callback) {
        NeoForge.EVENT_BUS.post(new ProjectileImpactEvent(
                (Projectile) (Object) this, hit));
    }
}
''',
)

write(
    "src/main/java/net/mcreator/scpadditions/fabric/mixin/ItemEntityPickupMixin.java",
    '''package net.mcreator.scpadditions.fabric.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
abstract class ItemEntityPickupMixin {
    @Inject(
            method = "playerTouch(Lnet/minecraft/world/entity/player/Player;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void scpAdditions$beforePickup(
            Player player,
            CallbackInfo callback) {
        ItemEntityPickupEvent.Pre event = new ItemEntityPickupEvent.Pre(
                player, (ItemEntity) (Object) this);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled() || event.getCanPickup() == TriState.FALSE) {
            callback.cancel();
        }
    }
}
''',
)

write(
    "src/main/java/net/mcreator/scpadditions/fabric/mixin/PlayerEventMixin.java",
    '''package net.mcreator.scpadditions.fabric.mixin;

import java.util.Optional;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
abstract class PlayerEventMixin {
    @ModifyVariable(
            method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private ItemStack scpAdditions$beforeToss(ItemStack stack) {
        Player player = (Player) (Object) this;
        if (stack.isEmpty() || player.level().isClientSide()) return stack;
        ItemEntity candidate = new ItemEntity(
                player.level(), player.getX(), player.getEyeY() - 0.3D,
                player.getZ(), stack);
        ItemTossEvent event = new ItemTossEvent(candidate, player);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) return ItemStack.EMPTY;
        return candidate.getItem();
    }

    @Inject(
            method = "getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;)F",
            at = @At("RETURN"),
            cancellable = true)
    private void scpAdditions$modifyBreakSpeed(
            BlockState state,
            CallbackInfoReturnable<Float> callback) {
        Player player = (Player) (Object) this;
        float original = callback.getReturnValue();
        PlayerEvent.BreakSpeed event = new PlayerEvent.BreakSpeed(
                player, state, Optional.of(player.blockPosition()), original);
        NeoForge.EVENT_BUS.post(event);
        callback.setReturnValue(Math.max(0.0F, event.getNewSpeed()));
    }
}
''',
)

write(
    "src/main/java/net/mcreator/scpadditions/fabric/mixin/BlockItemPlaceMixin.java",
    '''package net.mcreator.scpadditions.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
abstract class BlockItemPlaceMixin {
    @Inject(
            method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;",
            at = @At("RETURN"))
    private void scpAdditions$afterBlockPlaced(
            BlockPlaceContext context,
            CallbackInfoReturnable<InteractionResult> callback) {
        if (!callback.getReturnValue().consumesAction()) return;
        Level level = context.getLevel();
        if (level.isClientSide()) return;
        BlockPos pos = context.getClickedPos();
        NeoForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(
                level, pos, level.getBlockState(pos), context.getPlayer()));
    }
}
''',
)

write(
    "src/main/java/net/mcreator/scpadditions/fabric/mixin/LevelNeighborNotifyMixin.java",
    '''package net.mcreator.scpadditions.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
abstract class LevelNeighborNotifyMixin {
    @Inject(
            method = "updateNeighborsAt(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;)V",
            at = @At("HEAD"))
    private void scpAdditions$neighborNotify(
            BlockPos pos,
            Block source,
            CallbackInfo callback) {
        Level level = (Level) (Object) this;
        NeoForge.EVENT_BUS.post(new BlockEvent.NeighborNotifyEvent(
                level, pos, level.getBlockState(pos)));
    }
}
''',
)

write(
    "src/main/java/net/mcreator/scpadditions/fabric/mixin/ResultSlotCraftedMixin.java",
    '''package net.mcreator.scpadditions.fabric.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
abstract class ResultSlotCraftedMixin {
    @Shadow private Container craftSlots;

    @Inject(
            method = "onTake(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"))
    private void scpAdditions$itemCrafted(
            Player player,
            ItemStack result,
            CallbackInfo callback) {
        NeoForge.EVENT_BUS.post(
                new PlayerEvent.ItemCraftedEvent(player, craftSlots));
    }
}
''',
)

write(
    "src/main/java/net/mcreator/scpadditions/fabric/mixin/client/MinecraftInputMixin.java",
    '''package net.mcreator.scpadditions.fabric.mixin.client;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftInputMixin {
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void scpAdditions$beforeUseKey(CallbackInfo callback) {
        InputEvent.InteractionKeyMappingTriggered event =
                new InputEvent.InteractionKeyMappingTriggered(true);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) callback.cancel();
    }
}
''',
)

write(
    "src/main/java/net/mcreator/scpadditions/fabric/mixin/client/MouseScrollInputMixin.java",
    '''package net.mcreator.scpadditions.fabric.mixin.client;

import net.minecraft.client.MouseHandler;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
abstract class MouseScrollInputMixin {
    @Inject(
            method = "onScroll(JDD)V",
            at = @At("HEAD"),
            cancellable = true)
    private void scpAdditions$mouseScroll(
            long window,
            double horizontal,
            double vertical,
            CallbackInfo callback) {
        InputEvent.MouseScrollingEvent event =
                new InputEvent.MouseScrollingEvent(horizontal, vertical);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) callback.cancel();
    }
}
''',
)

write(
    "src/main/java/net/mcreator/scpadditions/fabric/mixin/client/ScreenDragMixin.java",
    '''package net.mcreator.scpadditions.fabric.mixin.client;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
abstract class ScreenDragMixin {
    @Inject(
            method = "mouseDragged(DDIDD)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void scpAdditions$mouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY,
            CallbackInfoReturnable<Boolean> callback) {
        ScreenEvent.MouseDragged.Pre event = new ScreenEvent.MouseDragged.Pre(
                (Screen) (Object) this,
                mouseX, mouseY, button, dragX, dragY);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) callback.setReturnValue(true);
    }
}
''',
)

write(
    "src/main/java/net/mcreator/scpadditions/fabric/mixin/client/GuiHealthLayerMixin.java",
    '''package net.mcreator.scpadditions.fabric.mixin.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
abstract class GuiHealthLayerMixin {
    @Inject(method = "renderPlayerHealth", at = @At("HEAD"), cancellable = true)
    private void scpAdditions$beforePlayerHealth(
            GuiGraphics graphics,
            CallbackInfo callback) {
        RenderGuiLayerEvent.Pre event = new RenderGuiLayerEvent.Pre(
                VanillaGuiLayers.PLAYER_HEALTH);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) callback.cancel();
    }
}
''',
)

mixins_path = ROOT / "src/main/resources/scp_additions.mixins.json"
metadata = json.loads(mixins_path.read_text(encoding="utf-8"))
common = list(metadata.get("mixins", []))
for name in [
    "LivingEntityEventMixin",
    "ServerLevelDropsMixin",
    "ProjectileImpactMixin",
    "ItemEntityPickupMixin",
    "PlayerEventMixin",
    "BlockItemPlaceMixin",
    "LevelNeighborNotifyMixin",
    "ResultSlotCraftedMixin",
]:
    if name not in common:
        common.append(name)
metadata["mixins"] = common
client = list(metadata.get("client", []))
for name in [
    "client.MinecraftInputMixin",
    "client.MouseScrollInputMixin",
    "client.ScreenDragMixin",
    "client.GuiHealthLayerMixin",
]:
    if name not in client:
        client.append(name)
metadata["client"] = client
new_json = json.dumps(metadata, indent=2) + "\n"
old_json = mixins_path.read_text(encoding="utf-8")
if new_json != old_json:
    mixins_path.write_text(new_json, encoding="utf-8")
    changed.append("src/main/resources/scp_additions.mixins.json")

print(f"Fabric API round 9 changed {len(changed)} files")
for item in changed:
    print(item)
