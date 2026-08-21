package net.mcreator.scpadditions.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.Scp939Entity;

import java.util.Comparator;
import java.util.List;

/** Client presentation helpers for the SCP-939 pin/maul sequence. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class Scp939PinPresentationClient {
    private static final double PIN_SEARCH_RANGE = 3.25D;
    private static final double EXPECTED_PIN_FORWARD = 1.02D;
    private static final double REMOTE_PIN_TOLERANCE_SQR = 0.90D * 0.90D;

    private Scp939PinPresentationClient() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!Scp939ClientState.pinned() || minecraft.player == null
                || !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        Scp939Entity predator = findPinning939(minecraft.player);
        if (predator == null) return;

        Vec3 from = event.getCamera().getPosition();
        Vec3 to = headTarget(predator);
        Vec3 delta = to.subtract(from);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (horizontal < 1.0E-4D && Math.abs(delta.y) < 1.0E-4D) return;

        float yaw = (float) (Mth.atan2(delta.z, delta.x)
                * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(delta.y,
                Math.max(1.0E-4D, horizontal)) * Mth.RAD_TO_DEG);
        event.setYaw(yaw);
        event.setPitch(pitch);
        event.setRoll(0.0F);
    }

    /**
     * The sleeping-style rotation pivots the player model around its entity
     * origin. With the entity feet correctly anchored to the block surface the
     * old negative offset buried most of the horizontal model below the floor.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (!isPinnedVictim(player)) return;
        PoseStack pose = event.getPoseStack();
        pose.translate(0.0D, 0.24D, 0.0D);
    }

    public static Scp939Entity findPinning939(Player player) {
        if (player == null || player.level() == null) return null;
        AABB area = player.getBoundingBox().inflate(PIN_SEARCH_RANGE);
        List<Scp939Entity> nearby = player.level().getEntitiesOfClass(
                Scp939Entity.class, area,
                entity -> entity.isAlive() && !entity.isRemoved()
                        && (entity.getAction() == Scp939Entity.ACTION_PIN_LAND
                        || entity.getAction() == Scp939Entity.ACTION_MAUL));
        return nearby.stream()
                .min(Comparator.comparingDouble(player::distanceToSqr))
                .orElse(null);
    }

    public static Vec3 cameraPosition(Scp939Entity predator) {
        Vec3 forward = horizontalForward(predator);
        return predator.position().add(forward.scale(0.74D))
                .add(0.0D, 0.075D, 0.0D);
    }

    public static Vec3 headTarget(Scp939Entity predator) {
        Vec3 forward = horizontalForward(predator);
        return predator.position().add(forward.scale(0.50D))
                .add(0.0D, 0.94D, 0.0D);
    }

    public static boolean isPinnedVictim(Player player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null
                && player.getId() == minecraft.player.getId()
                && Scp939ClientState.pinned()) {
            return true;
        }

        Scp939Entity predator = findPinning939(player);
        if (predator == null) return false;
        Vec3 expected = predator.position().add(
                horizontalForward(predator).scale(EXPECTED_PIN_FORWARD));
        return player.position().distanceToSqr(expected)
                <= REMOTE_PIN_TOLERANCE_SQR;
    }

    public static Vec3 horizontalForward(Scp939Entity predator) {
        Vec3 look = predator.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0.0D, look.z);
        if (forward.lengthSqr() < 1.0E-4D) {
            float yaw = predator.getYRot() * Mth.DEG_TO_RAD;
            forward = new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
        }
        return forward.normalize();
    }
}
