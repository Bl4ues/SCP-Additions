package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;
import com.bl4ues.scpinventory.events.WeaponAttackRestrictionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Removes the punch action unless a weapon is actually held. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class WeaponAttackRestrictionClient {
    private WeaponAttackRestrictionClient() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKeyMapping(
            InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()
                || !InventoryModuleRuntimeState.isEnabledForClient()
                || !InventoryModuleRuntimeState
                .requireEquippedWeaponToAttackForClient()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || player.isCreative() || player.isSpectator()
                || WeaponAttackRestrictionEvents
                .hasWeaponInMainHand(player)) {
            return;
        }

        // Mining remains available. Entity attacks and empty-air punches do not.
        if (minecraft.hitResult instanceof BlockHitResult) return;

        event.setSwingHand(false);
        event.setCanceled(true);
    }
}
