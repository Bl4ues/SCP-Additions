package com.bl4ues.scpclassifieddirective.effect;

import com.bl4ues.scpclassifieddirective.inventory.item.ScpItemEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import com.bl4ues.scpclassifieddirective.equipment.HazmatSuitAccess;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModMobEffects;

/** Shared eye-protection rules for gas, decontamination, and future hazards. */
public final class EyeProtectionAccess {
    private EyeProtectionAccess() {
    }

    public static boolean blocksExternalEyeSore(Player player) {
        return player != null && (player.hasEffect(ScpClassifiedDirectiveModMobEffects.LUBRICATED_EYE.get())
                || HazmatSuitAccess.protectsEyes(player)
                || ScpItemEffects.hasProtectedEyesModifierEquipped(player));
    }

    public static boolean applyExternalEyeSore(Player player, int durationTicks) {
        if (player == null || durationTicks <= 0 || blocksExternalEyeSore(player)) return false;
        // Keep particles disabled, but preserve the icon flag so the effect can
        // appear in inventory interfaces. EyeSoreEffect hides only the HUD icon.
        return player.addEffect(new MobEffectInstance(
                ScpClassifiedDirectiveModMobEffects.EYE_SORE.get(), durationTicks, 0,
                false, false, true));
    }
}
