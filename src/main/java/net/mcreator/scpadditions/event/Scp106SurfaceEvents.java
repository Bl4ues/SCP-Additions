package net.mcreator.scpadditions.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.Scp106Entity;
import net.mcreator.scpadditions.init.ScpAdditionsModParticleTypes;
import net.mcreator.scpadditions.roamer.Scp106PhasePortalTracker;

import java.util.Map;
import java.util.WeakHashMap;

/** Server-authoritative SCP-106 surface portals and emergence alignment. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp106SurfaceEvents {
    private static final byte HUNTING = 0;
    private static final byte EMERGING_GROUND = 1;
    private static final byte EMERGING_WALL = 2;
    private static final byte PHASE_TRAVEL = 3;
    private static final byte VANISHING = 4;

    private static final Map<Scp106Entity, Byte> PREVIOUS_STATES =
            new WeakHashMap<>();

    private Scp106SurfaceEvents() {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Scp106Entity scp106)
                || scp106.level().isClientSide
                || !(scp106.level() instanceof ServerLevel level)) {
            return;
        }

        byte state = scp106.getEncounterState();
        Byte previousState = PREVIOUS_STATES.put(scp106, state);
        boolean changed = previousState == null
                || previousState.byteValue() != state;

        if (changed) {
            if (state == EMERGING_GROUND || state == VANISHING) {
                spawnGroundPortal(level, scp106);
            } else if (state == EMERGING_WALL) {
                spawnWallPortal(level, scp106, scp106.getYRot());
            }

            if (state == HUNTING && previousState != null
                    && (previousState == EMERGING_GROUND
                    || previousState == EMERGING_WALL)) {
                settleAfterEmergence(level, scp106);
            }
        }

        Scp106PhasePortalTracker.tick(scp106, state == PHASE_TRAVEL);
    }

    private static void spawnGroundPortal(ServerLevel level,
            Scp106Entity scp106) {
        double surfaceY = findGroundSurfaceY(level, scp106);
        level.sendParticles(
                ScpAdditionsModParticleTypes.SCP_106_PORTAL.get(),
                scp106.getX(), surfaceY + 0.065D, scp106.getZ(),
                0, 0.0D, 0.90D, 0.0D, 1.0D);
    }

    private static void spawnWallPortal(ServerLevel level,
            Scp106Entity scp106, float placementYaw) {
        float yaw = placementYaw * Mth.DEG_TO_RAD;
        Vec3 modelForward = new Vec3(-Mth.sin(yaw), 0.0D,
                Mth.cos(yaw));
        // The wall emergence animation moves opposite the model's ordinary
        // forward axis, so its real surface normal is the inverse direction.
        Vec3 outward = modelForward.scale(-1.0D);
        Vec3 position = scp106.position()
                .subtract(outward.scale(0.49D))
                .add(0.0D, 1.0D, 0.0D);
        Vec3 encodedNormal = outward.scale(0.90D);
        level.sendParticles(
                ScpAdditionsModParticleTypes.SCP_106_PORTAL.get(),
                position.x, position.y, position.z,
                0, encodedNormal.x, encodedNormal.y,
                encodedNormal.z, 1.0D);
    }

    private static void settleAfterEmergence(ServerLevel level,
            Scp106Entity scp106) {
        AABB currentBox = scp106.getBoundingBox().deflate(0.025D);
        if (!intersectsSolid(level, currentBox)) return;

        double surfaceY = findGroundSurfaceY(level, scp106);
        double preferredLift = surfaceY + 0.025D - scp106.getY();
        if (preferredLift > 0.02D && preferredLift <= 1.55D) {
            AABB moved = currentBox.move(0.0D, preferredLift, 0.0D);
            if (!intersectsSolid(level, moved)) {
                scp106.setPos(scp106.getX(),
                        scp106.getY() + preferredLift, scp106.getZ());
                Vec3 movement = scp106.getDeltaMovement();
                scp106.setDeltaMovement(movement.x,
                        Math.max(0.0D, movement.y), movement.z);
                return;
            }
        }

        for (double lift = 0.10D; lift <= 1.60D; lift += 0.10D) {
            AABB moved = currentBox.move(0.0D, lift, 0.0D);
            if (!intersectsSolid(level, moved)) {
                scp106.setPos(scp106.getX(),
                        scp106.getY() + lift, scp106.getZ());
                Vec3 movement = scp106.getDeltaMovement();
                scp106.setDeltaMovement(movement.x,
                        Math.max(0.0D, movement.y), movement.z);
                return;
            }
        }
    }

    private static double findGroundSurfaceY(ServerLevel level,
            Scp106Entity scp106) {
        AABB body = scp106.getBoundingBox().deflate(0.025D);
        boolean embedded = intersectsSolid(level, body);
        double referenceY = scp106.getY();
        double bestY = referenceY;
        double bestScore = Double.MAX_VALUE;
        int minX = Mth.floor(body.minX - 0.10D);
        int maxX = Mth.floor(body.maxX + 0.10D);
        int minZ = Mth.floor(body.minZ - 0.10D);
        int maxZ = Mth.floor(body.maxZ + 0.10D);
        int minY = Mth.floor(referenceY) - 2;
        int maxY = Mth.floor(referenceY) + 1;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    mutable.set(x, y, z);
                    BlockState state = level.getBlockState(mutable);
                    VoxelShape shape = state.getCollisionShape(level, mutable);
                    for (AABB local : shape.toAabbs()) {
                        AABB worldShape = local.move(x, y, z);
                        if (scp106.getX() < worldShape.minX - 0.08D
                                || scp106.getX() > worldShape.maxX + 0.08D
                                || scp106.getZ() < worldShape.minZ - 0.08D
                                || scp106.getZ() > worldShape.maxZ + 0.08D) {
                            continue;
                        }

                        double top = worldShape.maxY;
                        if (top < referenceY - 1.30D
                                || top > referenceY + 1.55D) {
                            continue;
                        }
                        if (embedded && top < referenceY + 0.12D) {
                            continue;
                        }

                        double score = Math.abs(top - referenceY);
                        if (score < bestScore) {
                            bestScore = score;
                            bestY = top;
                        }
                    }
                }
            }
        }
        return bestY;
    }

    private static boolean intersectsSolid(ServerLevel level, AABB box) {
        int minX = Mth.floor(box.minX);
        int minY = Mth.floor(box.minY);
        int minZ = Mth.floor(box.minZ);
        int maxX = Mth.floor(box.maxX - 1.0E-7D);
        int maxY = Mth.floor(box.maxY - 1.0E-7D);
        int maxZ = Mth.floor(box.maxZ - 1.0E-7D);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    mutable.set(x, y, z);
                    BlockState state = level.getBlockState(mutable);
                    VoxelShape shape = state.getCollisionShape(level, mutable);
                    for (AABB local : shape.toAabbs()) {
                        if (local.move(x, y, z).intersects(box)) return true;
                    }
                }
            }
        }
        return false;
    }
}
