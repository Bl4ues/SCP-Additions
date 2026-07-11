package net.mcreator.scpadditions.event;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.AbstractScp131Entity;
import net.mcreator.scpadditions.entity.Scp173Entity;

@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp131WatchScp173Events {
    private static final double SCAN_RANGE = 15.0D;
    private static final double SCAN_RANGE_SQR = SCAN_RANGE * SCAN_RANGE;
    private static final double WATCH_DISTANCE = 1.2D;
    private static final double WATCH_DISTANCE_SQR = WATCH_DISTANCE * WATCH_DISTANCE;
    private static final double FAR_WATCH_DISTANCE_SQR = 1.8D * 1.8D;

    private Scp131WatchScp173Events() {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof AbstractScp131Entity scp131) || scp131.level().isClientSide) return;
        Scp173Entity scp173 = findNearest(scp131);
        if (scp173 == null) return;
        lookHardAt(scp131, scp173);
        double distanceSqr = scp131.distanceToSqr(scp173);
        if (!scp131.hasLineOfSight(scp173) || distanceSqr > FAR_WATCH_DISTANCE_SQR || distanceSqr < WATCH_DISTANCE_SQR) {
            Vec3 away = scp131.position().subtract(scp173.position());
            Vec3 horizontal = new Vec3(away.x, 0.0D, away.z);
            if (horizontal.lengthSqr() < 0.0001D) horizontal = new Vec3(1.0D, 0.0D, 0.0D);
            Vec3 spot = scp173.position().add(horizontal.normalize().scale(WATCH_DISTANCE));
            scp131.getNavigation().moveTo(spot.x, spot.y, spot.z, 1.45D);
        } else {
            scp131.getNavigation().stop();
            scp131.setDeltaMovement(Vec3.ZERO);
        }
        lookHardAt(scp131, scp173);
    }

    private static Scp173Entity findNearest(AbstractScp131Entity scp131) {
        AABB area = scp131.getBoundingBox().inflate(SCAN_RANGE);
        Scp173Entity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Scp173Entity scp173 : scp131.level().getEntitiesOfClass(Scp173Entity.class, area,
                entity -> entity.isAlive() && entity.distanceToSqr(scp131) <= SCAN_RANGE_SQR)) {
            double distance = scp131.distanceToSqr(scp173);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = scp173;
            }
        }
        return best;
    }

    private static void lookHardAt(AbstractScp131Entity scp131, LivingEntity target) {
        scp131.getLookControl().setLookAt(target, 90.0F, 90.0F);
        Vec3 toTarget = target.getEyePosition().subtract(scp131.getEyePosition());
        double horizontal = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        if (horizontal <= 0.0001D) return;
        float yaw = (float) (Mth.atan2(toTarget.z, toTarget.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) (-(Mth.atan2(toTarget.y, horizontal) * Mth.RAD_TO_DEG));
        scp131.setYRot(yaw);
        scp131.yRotO = yaw;
        scp131.yBodyRot = yaw;
        scp131.yBodyRotO = yaw;
        scp131.yHeadRot = yaw;
        scp131.yHeadRotO = yaw;
        scp131.setXRot(pitch);
        scp131.xRotO = pitch;
    }
}
