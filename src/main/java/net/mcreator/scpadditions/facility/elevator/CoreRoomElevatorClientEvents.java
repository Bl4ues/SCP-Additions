package net.mcreator.scpadditions.facility.elevator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/** Client construction preview and facing synchronization for Core Room elevators. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CoreRoomElevatorClientEvents {
    private static final DustParticleOptions VALID_LINE =
            new DustParticleOptions(new Vector3f(0.18F, 1.0F, 0.30F), 0.75F);
    private static final DustParticleOptions INVALID_LINE =
            new DustParticleOptions(new Vector3f(1.0F, 0.16F, 0.12F), 0.75F);
    private static final double PARTICLE_SPACING = 0.50D;

    private CoreRoomElevatorClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) return;

        synchronizeCarriageFacing(level);
        if ((level.getGameTime() & 1L) != 0L) return;
        renderPlacementGuide(minecraft, level);
    }

    private static void synchronizeCarriageFacing(ClientLevel level) {
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof CoreRoomElevatorCarriageEntity carriage)) {
                continue;
            }
            float yaw = carriage.facing().toYRot();
            carriage.setYRot(yaw);
            carriage.yRotO = yaw;
        }
    }

    private static void renderPlacementGuide(Minecraft minecraft,
            ClientLevel level) {
        ItemStack main = minecraft.player.getMainHandItem();
        ItemStack off = minecraft.player.getOffhandItem();
        boolean placingPulley = main.is(CoreRoomElevatorModule.PULLEY_ITEM.get())
                || off.is(CoreRoomElevatorModule.PULLEY_ITEM.get());
        boolean placingStation = main.is(CoreRoomElevatorModule.STATION_ITEM.get())
                || off.is(CoreRoomElevatorModule.STATION_ITEM.get());
        if (!placingPulley && !placingStation) return;
        if (!(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos candidate = placementPos(level, hit);
        BlockPos anchor;
        Direction facing;
        boolean valid;

        if (placingPulley) {
            anchor = highestStationBelow(level, candidate);
            if (anchor == null) return;
            facing = level.getBlockState(anchor).getValue(
                    CoreRoomElevatorModule.FACING);
            if (hasBeamBetween(level, anchor, candidate.getY())) return;
            valid = CoreRoomElevatorManager.findPulleyFacing(level, candidate)
                    != null && isBeamPathClear(level, anchor, candidate.getY());
        } else {
            facing = minecraft.player.getDirection().getOpposite();
            candidate = CoreRoomElevatorManager.findStationSnap(
                    level, candidate, facing);
            anchor = nearestStation(level, candidate, facing);
            if (anchor == null) return;
            if (hasBeamBetween(level, anchor, candidate.getY())) return;
            valid = CoreRoomElevatorManager.isValidStationPlacement(
                    level, candidate, facing)
                    && isBeamPathClear(level, anchor, candidate.getY());
        }

        drawGuide(level, anchor, candidate, valid);
    }

    private static BlockPos placementPos(ClientLevel level,
            BlockHitResult hit) {
        BlockPos clicked = hit.getBlockPos();
        BlockState state = level.getBlockState(clicked);
        return state.canBeReplaced() ? clicked
                : clicked.relative(hit.getDirection());
    }

    @Nullable
    private static BlockPos highestStationBelow(ClientLevel level,
            BlockPos candidate) {
        BlockPos result = null;
        for (int y = level.getMinBuildHeight(); y < candidate.getY(); y++) {
            BlockPos pos = new BlockPos(candidate.getX(), y, candidate.getZ());
            if (level.getBlockState(pos).is(CoreRoomElevatorModule.STATION.get())) {
                result = pos;
            }
        }
        return result;
    }

    @Nullable
    private static BlockPos nearestStation(ClientLevel level,
            BlockPos candidate, Direction facing) {
        BlockPos best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
            if (y == candidate.getY()) continue;
            BlockPos pos = new BlockPos(candidate.getX(), y, candidate.getZ());
            BlockState state = level.getBlockState(pos);
            if (!state.is(CoreRoomElevatorModule.STATION.get())
                    || state.getValue(CoreRoomElevatorModule.FACING) != facing) {
                continue;
            }
            int distance = Math.abs(y - candidate.getY());
            if (distance < bestDistance) {
                best = pos;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static boolean hasBeamBetween(ClientLevel level, BlockPos station,
            int targetY) {
        int lower = Math.min(station.getY(), targetY);
        int upper = Math.max(station.getY(), targetY);
        int start = lower + CoreRoomElevatorModule.STATION_HEIGHT_BLOCKS;
        for (int y = start; y < upper; y++) {
            if (level.getBlockState(new BlockPos(station.getX(), y,
                    station.getZ())).is(CoreRoomElevatorModule.BEAMS.get())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBeamPathClear(ClientLevel level,
            BlockPos station, int targetY) {
        int lower = Math.min(station.getY(), targetY);
        int upper = Math.max(station.getY(), targetY);
        int start = lower + CoreRoomElevatorModule.STATION_HEIGHT_BLOCKS;
        if (start > upper) return false;

        for (int y = start; y < upper; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockState state = level.getBlockState(new BlockPos(
                            station.getX() + dx, y, station.getZ() + dz));
                    if (state.isAir() || state.canBeReplaced()) continue;
                    if (state.is(CoreRoomElevatorModule.BEAMS.get())
                            || state.is(CoreRoomElevatorModule.BEAM_STRUCTURE_PART.get())) {
                        continue;
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private static void drawGuide(ClientLevel level, BlockPos station,
            BlockPos candidate, boolean valid) {
        double x = station.getX() + 0.5D;
        double z = station.getZ() + 0.5D;
        double fromY = station.getY()
                + CoreRoomElevatorModule.STATION_HEIGHT_BLOCKS * 0.5D;
        double toY = candidate.getY() + 0.5D;
        double distance = Math.abs(toY - fromY);
        int steps = Math.max(1, (int) Math.ceil(distance / PARTICLE_SPACING));
        DustParticleOptions particle = valid ? VALID_LINE : INVALID_LINE;
        for (int step = 0; step <= steps; step++) {
            double progress = step / (double) steps;
            double y = fromY + (toY - fromY) * progress;
            level.addParticle(particle, x, y, z, 0.0D, 0.0D, 0.0D);
        }
    }
}
