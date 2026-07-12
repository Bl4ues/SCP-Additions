package com.bl4ues.scpinventory.event;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.entity.Scp173Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID)
public final class Scp173DamageEvents {
    private static final double CONTACT_EXPAND = 0.08D;

    private Scp173DamageEvents() {
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || !(event.getSource().getEntity() instanceof Scp173Entity scp173)) {
            return;
        }

        if (!isCloseContact(scp173, player)) {
            event.setCanceled(true);
        }
    }

    private static boolean isCloseContact(Scp173Entity scp173, Player player) {
        if (scp173 == null || player == null || !hasVerticalOverlap(scp173.getBoundingBox(), player.getBoundingBox())) {
            return false;
        }

        AABB contactBox = scp173.getBoundingBox().inflate(CONTACT_EXPAND, 0.12D, CONTACT_EXPAND);
        return contactBox.intersects(player.getBoundingBox());
    }

    private static boolean hasVerticalOverlap(AABB first, AABB second) {
        return first.maxY >= second.minY && first.minY <= second.maxY;
    }
}
