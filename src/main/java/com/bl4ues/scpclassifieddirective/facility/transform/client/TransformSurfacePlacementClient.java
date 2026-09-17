package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.Selection;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.SelectionType;
import com.bl4ues.scpclassifieddirective.facility.transform.network.TransformConstructionNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Lets empty authored surfaces remain virtual while still accepting block
 * placement. The ray is tested against each local surface cell instead of
 * relying on a vanilla proxy BlockPos.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TransformSurfacePlacementClient {
    private static final double MAX_DISTANCE = 32.0D;
    private static final double HALF_CELL = 0.62D;

    private TransformSurfacePlacementClient() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        Selection selection = TransformConstructionClientState.selection();
        if (player == null || minecraft.level == null || minecraft.screen != null
                || !player.isCreative()
                || !(player.getMainHandItem().getItem() instanceof BlockItem)
                || selection == null
                || selection.type() != SelectionType.SURFACE) {
            return;
        }

        ConstructionSurface surface =
                TransformConstructionClientState.surface(selection.id());
        if (surface == null) return;
        SurfaceTarget target = target(player, surface);
        if (target == null) return;

        TransformConstructionNetwork.placeSurfaceBlock(surface.id(),
                target.slot(), target.hit());
        event.setCanceled(true);
    }

    private static SurfaceTarget target(LocalPlayer player,
            ConstructionSurface surface) {
        Vec3 eye = player.getEyePosition();
        Vec3 ray = player.getViewVector(1.0F).normalize();
        SurfaceTarget best = null;
        double bestDistance = MAX_DISTANCE + 1.0D;

        int columns = surface.columns();
        int rows = surface.rows();
        for (int column = 0; column < columns; column++) {
            double u = (column + 0.5D) / columns;
            for (int row = 0; row < rows; row++) {
                double v = (row + 0.5D) / rows;
                Vec3 center = surface.gridPoint(u, v);
                Vec3 tangent = surface.gridTangent(u, v).normalize();
                Vec3 normal = surface.gridNormal(u, v).normalize();
                Vec3 vertical = normal.cross(tangent).normalize();

                double denominator = ray.dot(normal);
                if (Math.abs(denominator) < 1.0E-6D) continue;
                double distance = center.subtract(eye).dot(normal) / denominator;
                if (distance < 0.0D || distance > MAX_DISTANCE
                        || distance >= bestDistance) continue;

                Vec3 hit = eye.add(ray.scale(distance));
                Vec3 local = hit.subtract(center);
                if (Math.abs(local.dot(tangent)) > HALF_CELL
                        || Math.abs(local.dot(vertical)) > HALF_CELL) {
                    continue;
                }
                bestDistance = distance;
                best = new SurfaceTarget(
                        new ConstructionSurface.SurfaceSlot(column, row), hit);
            }
        }
        return best;
    }

    private record SurfaceTarget(ConstructionSurface.SurfaceSlot slot,
            Vec3 hit) {
    }
}
