package com.bl4ues.scpclassifieddirective.hacking;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModItems;
import com.bl4ues.scpclassifieddirective.network.HackingDeviceNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Physical placement/removal interaction for Keycard Readers and OCUs. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID)
public final class HackingDeviceEvents {
    private HackingDeviceEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!HackingDeviceAttachmentManager.isCompatibleTarget(
                level, event.getPos())) {
            return;
        }

        if (HackingDeviceAttachmentManager.isAttached(level, event.getPos())) {
            if (HackingDeviceAttachmentManager.detach(player, event.getPos())) {
                consume(event);
            }
            return;
        }

        InteractionHand deviceHand = findDeviceHand(player);
        if (deviceHand != null
                && HackingDeviceAttachmentManager.attach(player,
                        event.getPos(), deviceHand)) {
            consume(event);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        sync(event.getEntity());
    }

    @SubscribeEvent
    public static void onChangedDimension(
            PlayerEvent.PlayerChangedDimensionEvent event) {
        sync(event.getEntity());
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.level instanceof ServerLevel level)
                || level.getGameTime() % 20L != 0L) {
            return;
        }
        HackingDeviceAttachmentManager.validate(level);
    }

    private static InteractionHand findDeviceHand(ServerPlayer player) {
        if (player.getMainHandItem().is(
                ScpClassifiedDirectiveModItems.HACKING_DEVICE.get())) {
            return InteractionHand.MAIN_HAND;
        }
        if (player.getOffhandItem().is(
                ScpClassifiedDirectiveModItems.HACKING_DEVICE.get())) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private static void sync(net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(serverPlayer.level() instanceof ServerLevel level)) {
            return;
        }
        HackingDeviceNetwork.sync(serverPlayer,
                HackingDeviceAttachmentManager.snapshot(level));
    }

    private static void consume(PlayerInteractEvent.RightClickBlock event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
