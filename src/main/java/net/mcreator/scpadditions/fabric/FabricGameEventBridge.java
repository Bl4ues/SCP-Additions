package net.mcreator.scpadditions.fabric;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.*;

final class FabricGameEventBridge {
    private FabricGameEventBridge() {}
    static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(FabricServerContext::set);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> FabricServerContext.set(null));
        ServerTickEvents.START_SERVER_TICK.register(server -> NeoForge.EVENT_BUS.post(new ServerTickEvent.Pre(server)));
        ServerTickEvents.END_SERVER_TICK.register(server -> NeoForge.EVENT_BUS.post(new ServerTickEvent.Post(server)));
        ServerTickEvents.START_WORLD_TICK.register(level -> NeoForge.EVENT_BUS.post(new LevelTickEvent.Pre(level)));
        ServerTickEvents.END_WORLD_TICK.register(level -> NeoForge.EVENT_BUS.post(new LevelTickEvent.Post(level)));
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(entity, level)));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(handler.player)));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(handler.player)));
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(newPlayer, oldPlayer, !alive)));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(newPlayer)));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerChangedDimensionEvent(player)));
        EntityTrackingEvents.START_TRACKING.register((entity, player) -> NeoForge.EVENT_BUS.post(new PlayerEvent.StartTracking(player, entity)));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> NeoForge.EVENT_BUS.post(new RegisterCommandsEvent(dispatcher)));
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> !NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level,pos,state,player)));
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {});
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            PlayerInteractEvent.RightClickBlock event=new PlayerInteractEvent.RightClickBlock(player,hand,hit);
            NeoForge.EVENT_BUS.post(event);
            return event.isCanceled()?event.getCancellationResult():InteractionResult.PASS;
        });
        UseItemCallback.EVENT.register((player, level, hand) -> {
            PlayerInteractEvent.RightClickItem event=new PlayerInteractEvent.RightClickItem(player,hand);
            NeoForge.EVENT_BUS.post(event);
            return event.isCanceled()?net.minecraft.world.InteractionResultHolder.success(player.getItemInHand(hand)):net.minecraft.world.InteractionResultHolder.pass(player.getItemInHand(hand));
        });
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            PlayerInteractEvent.EntityInteract event=new PlayerInteractEvent.EntityInteract(player,entity);
            NeoForge.EVENT_BUS.post(event);
            return event.isCanceled()?event.getCancellationResult():InteractionResult.PASS;
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            LivingIncomingDamageEvent event=new LivingIncomingDamageEvent(entity,source,amount);
            NeoForge.EVENT_BUS.post(event);
            return !event.isCanceled() && event.getAmount() > 0;
        });
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> !NeoForge.EVENT_BUS.post(new LivingDeathEvent(entity,source)));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> NeoForge.EVENT_BUS.post(new LivingDeathEvent(entity,source)));
    }
}
