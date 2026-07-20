package net.mcreator.scpadditions.client;

import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.scp012.Scp012InfluenceEvents;

/** Applies a proximity-scaled camera influence that the player can resist. */
@EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class Scp012ClientEvents {
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

        if (!Scp012ClientState.isActive()) {
            Scp012ClientState.setInfluenceProximity(0.0F);
            Scp012ClientState.tick();
            return;
        }

        Player player = minecraft.player;
        Vec3 target = Vec3.atCenterOf(Scp012ClientState.target());
        Vec3 delta = target.subtract(player.getEyePosition());
        double distance = delta.length();
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float proximity = Mth.clamp(1.0F - (float) (distance
                / Scp012InfluenceEvents.INFLUENCE_RADIUS), 0.0F, 1.0F);

        // Feed the whole influence range into the psychosis renderer. Contact
        // progress still takes over near the composition and during damage.
        Scp012ClientState.setInfluenceProximity(proximity);
        Scp012ClientState.tick();

        float curved = (float) Math.pow(proximity, 1.45D);
        float cameraStrength = Mth.lerp(curved, 0.025F, 0.255F);
        if (Scp012ClientState.isDamageActive()) {
            cameraStrength = Math.min(0.34F, cameraStrength + 0.07F);
        }

        float targetYaw = (float) (Mth.atan2(delta.z, delta.x)
                * Mth.RAD_TO_DEG) - 90.0F;
        float targetPitch = (float) -(Mth.atan2(delta.y, horizontal)
                * Mth.RAD_TO_DEG);

        float yaw = Mth.rotLerp(cameraStrength, player.getYRot(), targetYaw);
        float pitch = Mth.lerp(cameraStrength, player.getXRot(),
                Mth.clamp(targetPitch, -90.0F, 90.0F));
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yHeadRot = yaw;
        player.yHeadRotO = yaw;
    }
}
