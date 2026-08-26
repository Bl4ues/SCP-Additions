package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.block.entity.TeslaGateBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/** Starts at most one ambient loop per loaded Tesla Gate. */
public final class TeslaGateAudioClient {
    private static final double START_DISTANCE_SQ = 20.0D * 20.0D;
    private static final Map<BlockPos, TeslaGateLoopSound> ACTIVE = new HashMap<>();

    private TeslaGateAudioClient() {
    }

    public static void ensureLoop(Level level, BlockPos pos,
            TeslaGateBlockEntity gate) {
        if (!(level instanceof ClientLevel clientLevel) || !gate.isPowered()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level != clientLevel) return;
        if (minecraft.player.distanceToSqr(pos.getX() + 0.5D,
                pos.getY() + 1.7D, pos.getZ() + 0.5D) > START_DISTANCE_SQ) return;

        BlockPos key = pos.immutable();
        TeslaGateLoopSound current = ACTIVE.get(key);
        if (current != null && !current.isFinished()
                && current.level() == clientLevel) return;
        if (current != null) current.finish();

        TeslaGateLoopSound loop = new TeslaGateLoopSound(clientLevel, key);
        ACTIVE.put(key, loop);
        minecraft.getSoundManager().play(loop);
    }

    static void onLoopFinished(TeslaGateLoopSound loop) {
        ACTIVE.remove(loop.pos(), loop);
    }
}
