package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.client.StructurePlacementFeedbackClient;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.Collection;
import java.util.List;

/**
 * Shared failure feedback for large structures whose placement can be blocked
 * outside the single clicked cell.
 */
public final class StructurePlacementFeedback {
    private StructurePlacementFeedback() {
    }

    public static void reportBlocked(BlockPlaceContext context,
            Collection<BlockPos> blockers) {
        if (context == null || blockers == null || blockers.isEmpty()) return;

        List<BlockPos> snapshot = blockers.stream()
                .map(BlockPos::immutable)
                .distinct()
                .limit(128)
                .toList();
        if (snapshot.isEmpty()) return;

        Player player = context.getPlayer();
        Component message = Component.literal(snapshot.size() == 1
                ? "Structure placement blocked: remove the highlighted block."
                : "Structure placement blocked: remove the highlighted blocks.");

        if (context.getLevel().isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> StructurePlacementFeedbackClient.show(
                            snapshot, message));
        } else if (player != null) {
            // Server-side fallback covers prediction mismatches caused by a
            // block changing between the client click and authoritative place.
            player.displayClientMessage(message, true);
        }
    }
}
