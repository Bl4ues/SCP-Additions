package com.bl4ues.scpclassifieddirective.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import com.bl4ues.scpclassifieddirective.facility.ScpSignSupportBlockEntity;
import com.bl4ues.scpclassifieddirective.keycard.KeycardReaderInteractionEvents;

final class ScpSignTemplateEditAccess {
    private ScpSignTemplateEditAccess() {
    }

    static boolean allowed(ServerPlayer player, BlockPos pos) {
        if (player == null || pos == null
                || player.distanceToSqr(Vec3.atCenterOf(pos)) > 100.0D
                || !player.level().hasChunkAt(pos)
                || KeycardReaderInteractionEvents.screwdriver(player)
                        .isEmpty()) {
            return false;
        }
        return player.level().getBlockEntity(pos)
                instanceof ScpSignSupportBlockEntity;
    }
}
