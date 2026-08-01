package com.bl4ues.scpinventory.events;

import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

/** Prevents unarmed entity attacks when the optional gameplay module is enabled. */
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

    public static boolean hasEquippedWeapon(Player player) {
        if (player == null) return false;
        return player.getCapability(ScpInventoryCapability.INSTANCE)
                .map(inventory -> !inventory.getEquipment(
                        ScpEquipmentSlot.WEAPON).isEmpty())
                .orElse(false);
    }

    private static boolean shouldBlockServerAttack(Player player) {
        if (player == null || player.isCreative() || player.isSpectator()) {
            return false;
        }
        ScpAdditionsModulesConfig.Root modules =
                ScpAdditionsModulesConfig.get();
        return modules.inventory.enabled
                && modules.inventory.requireEquippedWeaponToAttack
                && !hasEquippedWeapon(player);
    }
}
