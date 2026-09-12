package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.FacilityDecorativePropsModule;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Keeps one quiet positional loop for the nearest Heater and Ceiling
 * Ventilation. Both source files are authored as seamless ten-second loops.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FacilityPropLoopAudioClient {
    private static final int DISCOVERY_INTERVAL_TICKS = 10;

    private static NearbyLoop heaterLoop;
    private static NearbyLoop ventilationLoop;
    private static int discoveryTicks;

    private FacilityPropLoopAudioClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            stopAll();
            discoveryTicks = 0;
            return;
        }

        if (++discoveryTicks < DISCOVERY_INTERVAL_TICKS) return;
        discoveryTicks = 0;

        heaterLoop = updateLoop(minecraft.level, minecraft.player.blockPosition(),
                heaterLoop, FacilityModule.HEATER.get(),
                ScpClassifiedDirectiveModSounds.HEATER_LOOP.get(),
                5, 4, 0.5F);

        ventilationLoop = updateLoop(minecraft.level,
                minecraft.player.blockPosition(), ventilationLoop,
                FacilityDecorativePropsModule.CEILING_VENTILATION.get(),
                ScpClassifiedDirectiveModSounds.CEILING_VENTILATION_LOOP.get(),
                8, 5, 1.0F);
    }

    private static NearbyLoop updateLoop(ClientLevel level, BlockPos center,
            NearbyLoop current, Block expectedBlock, SoundEvent sound,
            int horizontalRadius, int verticalRadius, float volume) {
        BlockPos nearest = findNearest(level, center, expectedBlock,
                horizontalRadius, verticalRadius);

        if (nearest == null) {
            if (current != null) current.finish();
            return null;
        }

        if (current == null || current.isFinished()
                || current.level() != level) {
            NearbyLoop created = new NearbyLoop(level, nearest,
                    expectedBlock, sound, volume);
            Minecraft.getInstance().getSoundManager().play(created);
            return created;
        }

        current.retarget(nearest);
        return current;
    }

    private static BlockPos findNearest(ClientLevel level, BlockPos center,
            Block expectedBlock, int horizontalRadius, int verticalRadius) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (int y = -verticalRadius; y <= verticalRadius; y++) {
            for (int x = -horizontalRadius; x <= horizontalRadius; x++) {
                for (int z = -horizontalRadius; z <= horizontalRadius; z++) {
                    cursor.set(center.getX() + x, center.getY() + y,
                            center.getZ() + z);
                    if (!level.hasChunkAt(cursor)
                            || !level.getBlockState(cursor).is(expectedBlock)) {
                        continue;
                    }
                    double distance = cursor.distSqr(center);
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearest = cursor.immutable();
                    }
                }
            }
        }
        return nearest;
    }

    private static void stopAll() {
        if (heaterLoop != null) heaterLoop.finish();
        if (ventilationLoop != null) ventilationLoop.finish();
        heaterLoop = null;
        ventilationLoop = null;
    }

    private static final class NearbyLoop
            extends AbstractTickableSoundInstance {
        private final ClientLevel level;
        private final Block expectedBlock;
        private BlockPos target;
        private boolean finished;

        private NearbyLoop(ClientLevel level, BlockPos pos, Block expectedBlock,
                SoundEvent event, float volume) {
            super(event, SoundSource.BLOCKS, RandomSource.create());
            this.level = level;
            this.expectedBlock = expectedBlock;
            this.target = pos.immutable();
            this.looping = true;
            this.delay = 0;
            this.volume = volume;
            this.pitch = 1.0F;
            this.relative = false;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            setPosition(pos);
        }

        private ClientLevel level() {
            return level;
        }

        private void retarget(BlockPos pos) {
            target = pos.immutable();
            setPosition(target);
        }

        private void setPosition(BlockPos pos) {
            x = pos.getX() + 0.5D;
            y = pos.getY() + 0.5D;
            z = pos.getZ() + 0.5D;
        }

        @Override
        public void tick() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != level
                    || minecraft.player == null
                    || !level.hasChunkAt(target)
                    || !level.getBlockState(target).is(expectedBlock)) {
                finish();
            }
        }

        private boolean isFinished() {
            return finished;
        }

        private void finish() {
            finished = true;
            stop();
        }
    }
}
