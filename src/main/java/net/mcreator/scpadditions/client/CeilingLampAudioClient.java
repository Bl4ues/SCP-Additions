package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Maintains one positional electrical hum for the nearest powered ceiling lamp. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class CeilingLampAudioClient {
    private static final int DISCOVERY_INTERVAL_TICKS = 10;
    private static final int HORIZONTAL_DISCOVERY_RADIUS = 16;
    private static final int VERTICAL_DISCOVERY_RADIUS = 8;

    private static CeilingLampLoopSound activeLoop;
    private static int discoveryTicks;

    private CeilingLampAudioClient() {
    }

    public static void ensureLoop(Level level, BlockPos pos) {
        if (!(level instanceof ClientLevel clientLevel)
                || !CeilingLampLoopSound.shouldPlayFor(
                clientLevel.getBlockState(pos))) {
            return;
        }
        startOrRetarget(clientLevel, pos);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            stopLoop();
            discoveryTicks = 0;
            return;
        }

        if (activeLoop != null && activeLoop.isFinished()) activeLoop = null;

        discoveryTicks++;
        if (discoveryTicks < DISCOVERY_INTERVAL_TICKS) return;
        discoveryTicks = 0;

        BlockPos nearest = findNearestPoweredLamp(minecraft.level,
                minecraft.player.blockPosition());
        if (nearest == null) stopLoop();
        else startOrRetarget(minecraft.level, nearest);
    }

    private static void startOrRetarget(ClientLevel level, BlockPos pos) {
        if (activeLoop != null && !activeLoop.isFinished()
                && activeLoop.level() == level) {
            activeLoop.retarget(pos);
            return;
        }
        stopLoop();
        activeLoop = new CeilingLampLoopSound(level, pos);
        Minecraft.getInstance().getSoundManager().play(activeLoop);
    }

    private static void stopLoop() {
        if (activeLoop != null) {
            activeLoop.finish();
            activeLoop = null;
        }
    }

    private static BlockPos findNearestPoweredLamp(ClientLevel level,
            BlockPos center) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (int y = -VERTICAL_DISCOVERY_RADIUS;
                y <= VERTICAL_DISCOVERY_RADIUS; y++) {
            for (int x = -HORIZONTAL_DISCOVERY_RADIUS;
                    x <= HORIZONTAL_DISCOVERY_RADIUS; x++) {
                for (int z = -HORIZONTAL_DISCOVERY_RADIUS;
                        z <= HORIZONTAL_DISCOVERY_RADIUS; z++) {
                    cursor.set(center.getX() + x, center.getY() + y,
                            center.getZ() + z);
                    if (!level.hasChunkAt(cursor)
                            || !CeilingLampLoopSound.shouldPlayFor(
                            level.getBlockState(cursor))) continue;
                    double distance = cursor.distSqr(center);
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearest = cursor.immutable();
                    }
                }
            }
        }
        return nearest;
    }
}
