package com.bl4ues.scpclassifieddirective.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModGameRules;
import com.bl4ues.scpclassifieddirective.procedures.DecontaminationCheckpointController;
import com.bl4ues.scpclassifieddirective.save.SaveDifficultyPolicy;
import com.bl4ues.scpclassifieddirective.save.SaveGameContext;
import com.bl4ues.scpclassifieddirective.save.SaveMethod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies Safe/Euclid/Keter/Thaumiel checkpoint saving rules. */
@Mixin(value = DecontaminationCheckpointController.class, remap = false)
public abstract class DecontaminationCheckpointSaveMixin {
    @Inject(method = "decontaminate", at = @At("HEAD"))
    private static void scpClassifiedDirective$applyDifficultySavePolicy(
            ServerLevel level, ServerPlayer player, CallbackInfo callback) {
        if (level == null || player == null) return;

        // The old gamerule remains registered only so existing worlds/configs do
        // not encounter a missing rule. It no longer owns checkpoint behavior.
        if (level.getGameRules().getBoolean(
                ScpClassifiedDirectiveModGameRules.DECONCHECKPOINT)) {
            level.getGameRules().getRule(ScpClassifiedDirectiveModGameRules.DECONCHECKPOINT)
                    .set(false, level.getServer());
        }

        if (!SaveDifficultyPolicy.allowsCheckpoint(level.getDifficulty())) {
            return;
        }

        SaveGameContext.run(SaveMethod.CHECKPOINT, () ->
                player.setRespawnPosition(player.level().dimension(),
                        player.blockPosition(), player.getYRot(), true, false));
    }
}
