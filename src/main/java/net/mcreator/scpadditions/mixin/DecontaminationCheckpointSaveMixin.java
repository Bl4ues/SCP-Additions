package net.mcreator.scpadditions.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;
import net.mcreator.scpadditions.procedures.DecontaminationCheckpointController;
import net.mcreator.scpadditions.save.SaveDifficultyPolicy;
import net.mcreator.scpadditions.save.SaveGameContext;
import net.mcreator.scpadditions.save.SaveMethod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies Safe/Euclid/Keter/Thaumiel checkpoint saving rules. */
@Mixin(value = DecontaminationCheckpointController.class, remap = false)
public abstract class DecontaminationCheckpointSaveMixin {
    @Inject(method = "decontaminate", at = @At("HEAD"))
    private static void scpAdditions$applyDifficultySavePolicy(
            ServerLevel level, ServerPlayer player, CallbackInfo callback) {
        if (level == null || player == null) return;

        // The old gamerule remains registered only so existing worlds/configs do
        // not encounter a missing rule. It no longer owns checkpoint behavior.
        if (level.getGameRules().getBoolean(
                ScpAdditionsModGameRules.DECONCHECKPOINT)) {
            level.getGameRules().getRule(ScpAdditionsModGameRules.DECONCHECKPOINT)
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
