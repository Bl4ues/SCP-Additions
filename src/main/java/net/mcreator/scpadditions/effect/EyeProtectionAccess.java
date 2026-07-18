package net.mcreator.scpadditions.effect;

import com.bl4ues.scpinventory.item.ScpItemEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.mcreator.scpadditions.equipment.HazmatSuitAccess;
import net.mcreator.scpadditions.init.ScpAdditionsModMobEffects;

/** Shared eye-protection rules for gas, decontamination, and future hazards. */
public final class EyeProtectionAccess {
    private EyeProtectionAccess() {
    }

    public static boolean blocksExternalEyeSore(Player player) {
        return player != null && (player.hasEffect(ScpAdditionsModMobEffects.LUBRICATED_EYE.get())
                || HazmatSuitAccess.protectsEyes(player)
                || ScpItemEffects.hasProtectedEyesModifierEquipped(player));
    }

    public static boolean applyExternalEyeSore(Player player, int durationTicks) {
        if (player == null || durationTicks <= 0 || blocksExternalEyeSore(player)) return false;
        return player.addEffect(new MobEffectInstance(
                ScpAdditionsModMobEffects.EYE_SORE.get(), durationTicks, 0, false, false, false));
    }
}
