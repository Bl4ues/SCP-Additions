package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Maintains one seamless positional loop for the nearest powered auxiliary unit. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class AuxiliaryGeneratorAudioClient {
    private static final int DISCOVERY_INTERVAL_TICKS = 10;
    private static final int HORIZONTAL_DISCOVERY_RADIUS = 16;
    private static final int VERTICAL_DISCOVERY_RADIUS = 8;

    private static AuxiliaryGeneratorLoopSound activeLoop;
    private static int discoveryTicks;

    private AuxiliaryGeneratorAudioClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            stopImmediately();
            discoveryTicks = 0;
            return;
        }

        if (activeLoop != null && activeLoop.isFinished()) activeLoop = null;
        discoveryTicks++;
        if (discoveryTicks < DISCOVERY_INTERVAL_TICKS) return;
        discoveryTicks = 0;

        BlockPos nearest = findNearestPoweredUnit(minecraft.level,
                minecraft.player.getX(), minecraft.player.getY(),
                minecraft.player.getZ(), minecraft.player.blockPosition());
        if (nearest == null) {
            if (activeLoop != null) activeLoop.requestStop();
            return;
        }

        if (activeLoop == null || activeLoop.isFinished()
                || activeLoop.level() != minecraft.level) {
            startLoop(minecraft.level, nearest);
        } else {
            activeLoop.retarget(nearest);
        }
    }

    private static void startLoop(ClientLevel level, BlockPos pos) {
        stopImmediately();
        activeLoop = new AuxiliaryGeneratorLoopSound(level, pos);
        Minecraft.getInstance().getSoundManager().play(activeLoop);
    }

    private static void stopImmediately() {
        if (activeLoop != null) {
            activeLoop.finish();
            activeLoop = null;
        }
    }

    private static BlockPos findNearestPoweredUnit(ClientLevel level,
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
                            || !AuxiliaryGeneratorLoopSound.shouldPlayFor(
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
