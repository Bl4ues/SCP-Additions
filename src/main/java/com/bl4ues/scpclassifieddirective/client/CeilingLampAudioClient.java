package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceGeometry;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState;
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

        Vec3 current = activeLoop == null || activeLoop.isFinished()
                ? null : activeLoop.target();
        Discovery discovery = findNearestPoweredLamp(minecraft.level,
                minecraft.player.getX(), minecraft.player.getY(),
                minecraft.player.getZ(), minecraft.player.blockPosition(),
                current);
        if (discovery.nearest() == null) stopLoop();
        else selectTarget(minecraft.level, discovery.nearest(),
                discovery.currentActive(), minecraft.player.getX(),
                minecraft.player.getY(), minecraft.player.getZ());
    }

    private static void selectTarget(ClientLevel level, Vec3 candidate,
            boolean currentActive, double listenerX, double listenerY,
            double listenerZ) {
        if (activeLoop == null || activeLoop.isFinished()
                || activeLoop.level() != level) {
            startLoop(level, candidate);
            return;
        }

        Vec3 current = activeLoop.target();
        if (current.distanceToSqr(candidate) < 1.0E-4D) return;
        if (!currentActive) {
            activeLoop.retarget(candidate);
            return;
        }

        double currentDistance = distanceToPointSqr(current, listenerX,
                listenerY, listenerZ);
        double candidateDistance = distanceToPointSqr(candidate, listenerX,
                listenerY, listenerZ);
        if (candidateDistance + RETARGET_ADVANTAGE_SQ < currentDistance) {
            activeLoop.retarget(candidate);
        }
    }

    private static void startLoop(ClientLevel level, BlockPos pos) {
        startLoop(level, Vec3.atCenterOf(pos));
    }

    private static void startLoop(ClientLevel level, Vec3 position) {
        stopLoop();
        activeLoop = new CeilingLampLoopSound(level, position);
        Minecraft.getInstance().getSoundManager().play(activeLoop);
    }

    private static void stopLoop() {
        if (activeLoop != null) {
            activeLoop.finish();
            activeLoop = null;
        }
    }

    private static Discovery findNearestPoweredLamp(ClientLevel level,
            double listenerX, double listenerY, double listenerZ,
            BlockPos center, Vec3 currentTarget) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        Vec3 nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        boolean currentActive = false;
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
                    Vec3 position = Vec3.atCenterOf(cursor);
                    if (currentTarget != null
                            && position.distanceToSqr(currentTarget) < 1.0E-4D) {
                        currentActive = true;
                    }
                    double distance = distanceToPointSqr(position, listenerX,
                            listenerY, listenerZ);
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearest = position;
                    }
                }
            }
        }

        for (ConstructionSurface surface : TransformConstructionClientState
                .surfaces(level.dimension().location())) {
            for (var entry : surface.attachments().entrySet()) {
                if (!CeilingLampLoopSound.shouldPlayFor(
                        entry.getValue().state())) continue;
                Vec3 position = TransformSurfaceGeometry.cellCenter(surface,
                        entry.getKey(), TransformSurfaceGeometry.MAIN_SIDE,
                        false);
                if (currentTarget != null
                        && position.distanceToSqr(currentTarget) < 1.0E-4D) {
                    currentActive = true;
                }
                double distance = distanceToPointSqr(position, listenerX,
                        listenerY, listenerZ);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = position;
                }
            }
            for (var entry : surface.overlays().entrySet()) {
                if (!CeilingLampLoopSound.shouldPlayFor(
                        entry.getValue().state())) continue;
                var key = entry.getKey();
                Vec3 position = TransformSurfaceGeometry.cellCenter(surface,
                        key.slot(), key.normalSign(), true);
                if (currentTarget != null
                        && position.distanceToSqr(currentTarget) < 1.0E-4D) {
                    currentActive = true;
                }
                double distance = distanceToPointSqr(position, listenerX,
                        listenerY, listenerZ);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = position;
                }
            }
        }
        return new Discovery(nearest, currentActive);
    }

    private static double distanceToPointSqr(Vec3 pos, double x,
            double y, double z) {
        double dx = pos.x - x;
        double dy = pos.y - y;
        double dz = pos.z - z;
        return dx * dx + dy * dy + dz * dz;
    }

    private record Discovery(Vec3 nearest, boolean currentActive) {
    }
}
