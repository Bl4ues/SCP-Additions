package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.block.LeftReaderAcceptBlock;
import com.bl4ues.scpclassifieddirective.block.Lv2LeftReaderAcceptBlock;
import com.bl4ues.scpclassifieddirective.block.Lv2RightReaderAcceptBlock;
import com.bl4ues.scpclassifieddirective.block.Lv3LeftReaderAcceptBlock;
import com.bl4ues.scpclassifieddirective.block.Lv3RightReaderAcceptBlock;
import com.bl4ues.scpclassifieddirective.block.Lv4LeftReaderAcceptBlock;
import com.bl4ues.scpclassifieddirective.block.Lv4RightReaderAcceptBlock;
import com.bl4ues.scpclassifieddirective.block.Lv5LeftReaderAcceptBlock;
import com.bl4ues.scpclassifieddirective.block.Lv5RightReaderAcceptBlock;
import com.bl4ues.scpclassifieddirective.block.Lv6LeftReaderAcceptBlock;
import com.bl4ues.scpclassifieddirective.block.Lv6RightReaderAcceptBlock;
import com.bl4ues.scpclassifieddirective.block.RightReaderAcceptBlock;
import com.bl4ues.scpclassifieddirective.keycard.KeycardReaderPulse;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gives every accepted reader a fresh, exact five-second redstone window.
 * Generated reader classes still contain their legacy 200-tick schedules; this
 * mixin makes those schedules harmless when they belong to an older activation.
 */
@Mixin({
        RightReaderAcceptBlock.class,
        LeftReaderAcceptBlock.class,
        Lv2RightReaderAcceptBlock.class,
        Lv2LeftReaderAcceptBlock.class,
        Lv3RightReaderAcceptBlock.class,
        Lv3LeftReaderAcceptBlock.class,
        Lv4RightReaderAcceptBlock.class,
        Lv4LeftReaderAcceptBlock.class,
        Lv5RightReaderAcceptBlock.class,
        Lv5LeftReaderAcceptBlock.class,
        Lv6RightReaderAcceptBlock.class,
        Lv6LeftReaderAcceptBlock.class
})
public abstract class KeycardReaderAcceptPulseMixin {
    /*
     * HEAD matters: schedule 100 ticks before the generated onPlace schedules its
     * old 200-tick reset. This also works with schedulers that coalesce duplicate
     * block ticks by position/type.
     */
    @Inject(method = "onPlace", at = @At("HEAD"))
    private void scpclassifieddirective$armFiveSecondPulse(BlockState state,
            Level level, BlockPos pos, BlockState oldState, boolean moving,
            CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel) {
            KeycardReaderPulse.arm(serverLevel, pos, state.getBlock());
        }
    }

    /*
     * A stale tick from a previous use may fire while a newer authorization is
     * active. Do not let it reset the reader; schedule the current deadline and
     * allow the original reset code only once the latest pulse has actually ended.
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void scpclassifieddirective$ignoreStaleReset(BlockState state,
            ServerLevel level, BlockPos pos, RandomSource random,
            CallbackInfo ci) {
        int remaining = KeycardReaderPulse.remainingTicks(level, pos);
        if (remaining > 0) {
            level.scheduleTick(pos, state.getBlock(), remaining);
            ci.cancel();
            return;
        }
        KeycardReaderPulse.clear(level, pos);
    }
}
