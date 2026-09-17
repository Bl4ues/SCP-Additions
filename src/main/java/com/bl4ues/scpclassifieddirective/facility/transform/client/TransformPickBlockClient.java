package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionModule;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

/** Restores vanilla middle-click copy semantics for transformed proxy blocks. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TransformPickBlockClient {
    private TransformPickBlockClient() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isPickBlock()) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.gameMode == null
                || minecraft.screen != null || !player.isCreative()
                || !(minecraft.hitResult instanceof BlockHitResult hit)
                || !minecraft.level.getBlockState(hit.getBlockPos()).is(
                        TransformConstructionModule.getProxy())) return;
        BlockState state = nearestState(minecraft, hit.getLocation());
        if (state == null || state.isAir()) return;
        ItemStack picked = state.getBlock().asItem().getDefaultInstance();
        if (picked.isEmpty()) return;

        player.getInventory().setPickedItem(picked);
        minecraft.gameMode.handleCreativeModeItemAdd(
                player.getInventory().getSelected(),
                36 + player.getInventory().selected);
        event.setCanceled(true);
        event.setSwingHand(false);
    }

    private static BlockState nearestState(Minecraft minecraft, Vec3 world) {
        BlockState best = null;
        double bestDistance = 4.0D;
        var dimension = minecraft.level.dimension().location();
        for (TransformGroup group
                : TransformConstructionClientState.groups(dimension)) {
            Vec3 local = TransformMath.worldToLocal(group.origin(), world,
                    group.rotationX(), group.rotationY(), group.rotationZ());
            for (Map.Entry<TransformGroup.GridPos, BlockState> entry
                    : group.cells().entrySet()) {
                if (entry.getValue() == null || entry.getValue().isAir()) continue;
                TransformGroup.GridPos cell = entry.getKey();
                double distance = local.distanceToSqr(
                        new Vec3(cell.x(), cell.y(), cell.z()));
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = entry.getValue();
                }
            }
        }
        for (ConstructionSurface surface
                : TransformConstructionClientState.surfaces(dimension)) {
            for (Map.Entry<ConstructionSurface.SurfaceSlot,
                    ConstructionSurface.SurfaceAttachment> entry
                    : surface.attachments().entrySet()) {
                ConstructionSurface.SurfaceSlot slot = entry.getKey();
                double u = (slot.column() + 0.5D) / surface.columns();
                double v = (slot.row() + 0.5D) / surface.rows();
                Vec3 center = surface.gridPoint(u, v)
                        .add(surface.gridNormal(u, v).scale(0.5D));
                double distance = center.distanceToSqr(world);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = entry.getValue().state();
                }
            }
        }
        return best;
    }
}
