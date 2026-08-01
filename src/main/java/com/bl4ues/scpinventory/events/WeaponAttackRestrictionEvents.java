package com.bl4ues.scpinventory.events;

import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.bl4ues.scpinventory.item.ScpItemType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

/** Prevents entity attacks unless a weapon is actually held in the main hand. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID)
public final class WeaponAttackRestrictionEvents {
    private WeaponAttackRestrictionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()
                || !shouldBlockServerAttack(player)) {
            return;
        }
        event.setCanceled(true);
    }

    public static boolean hasWeaponInMainHand(Player player) {
        if (player == null) return false;
        ItemStack held = player.getMainHandItem();
        return held != null && !held.isEmpty()
                && ScpItemClassifier.getType(held) == ScpItemType.WEAPON;
    }

    private static boolean shouldBlockServerAttack(Player player) {
        if (player == null || player.isCreative() || player.isSpectator()) {
            return false;
        }
        ScpAdditionsModulesConfig.Root modules =
                ScpAdditionsModulesConfig.get();
        return modules.inventory.enabled
                && modules.inventory.requireEquippedWeaponToAttack
                && !hasWeaponInMainHand(player);
    }
}
