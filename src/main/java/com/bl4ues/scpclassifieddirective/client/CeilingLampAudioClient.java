package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

/** Maintains one positional electrical hum for the nearest powered ceiling lamp. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class CeilingLampAudioClient {
    private static final int DISCOVERY_INTERVAL_TICKS = 10;
    private static final int HORIZONTAL_DISCOVERY_RADIUS = 16;
    private static final int VERTICAL_DISCOVERY_RADIUS = 8;
    private static final double RETARGET_ADVANTAGE_SQ = 4.0D;

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
        if (activeLoop == null || activeLoop.isFinished()
                || activeLoop.level() != clientLevel) {
            startLoop(clientLevel, pos);
        }
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
                minecraft.player.getX(), minecraft.player.getY(),
                minecraft.player.getZ(), minecraft.player.blockPosition());
        if (nearest == null) stopLoop();
        else selectTarget(minecraft.level, nearest, minecraft.player.getX(),
                minecraft.player.getY(), minecraft.player.getZ());
    }

    private static void selectTarget(ClientLevel level, BlockPos candidate,
            double listenerX, double listenerY, double listenerZ) {
        if (activeLoop == null || activeLoop.isFinished()
                || activeLoop.level() != level) {
            startLoop(level, candidate);
            return;
        }

        BlockPos current = activeLoop.target();
        if (current.equals(candidate)) return;
        if (!CeilingLampLoopSound.shouldPlayFor(level.getBlockState(current))) {
            activeLoop.retarget(candidate);
            return;
        }

        double currentDistance = distanceToCenterSqr(current, listenerX,
                listenerY, listenerZ);
        double candidateDistance = distanceToCenterSqr(candidate, listenerX,
                listenerY, listenerZ);
        if (candidateDistance + RETARGET_ADVANTAGE_SQ < currentDistance) {
            activeLoop.retarget(candidate);
        }
    }

    private static void startLoop(ClientLevel level, BlockPos pos) {
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
            double listenerX, double listenerY, double listenerZ,
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
                    double distance = distanceToCenterSqr(cursor, listenerX,
                            listenerY, listenerZ);
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearest = cursor.immutable();
                    }
                }
            }
        }
        return nearest;
    }

    private static double distanceToCenterSqr(BlockPos pos, double x,
            double y, double z) {
        double dx = pos.getX() + 0.5D - x;
        double dy = pos.getY() + 0.5D - y;
        double dz = pos.getZ() + 0.5D - z;
        return dx * dx + dy * dy + dz * dz;
    }
}
