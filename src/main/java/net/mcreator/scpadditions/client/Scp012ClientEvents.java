package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Applies a smooth but authoritative visual lock toward the active SCP-012. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Scp012ClientEvents {
    private static final float CAMERA_LOCK_SPEED = 0.42F;

    private Scp012ClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            Scp012ClientState.clear();
            return;
        }

        Scp012ClientState.tick();
        if (!Scp012ClientState.isActive()) return;

        Player player = minecraft.player;
        Vec3 target = Vec3.atCenterOf(Scp012ClientState.target());
        Vec3 delta = target.subtract(player.getEyePosition());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float targetYaw = (float) (Mth.atan2(delta.z, delta.x)
                * Mth.RAD_TO_DEG) - 90.0F;
        float targetPitch = (float) -(Mth.atan2(delta.y, horizontal)
                * Mth.RAD_TO_DEG);

        float yaw = Mth.rotLerp(CAMERA_LOCK_SPEED, player.getYRot(), targetYaw);
        float pitch = Mth.lerp(CAMERA_LOCK_SPEED, player.getXRot(),
                Mth.clamp(targetPitch, -90.0F, 90.0F));
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yHeadRot = yaw;
        player.yHeadRotO = yaw;
    }
}
