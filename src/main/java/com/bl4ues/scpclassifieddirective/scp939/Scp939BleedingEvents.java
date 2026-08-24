package com.bl4ues.scpclassifieddirective.scp939;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.entity.Scp939Entity;
import com.bl4ues.scpclassifieddirective.vitals.BleedingManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Gives every successful SCP-939 damage instance a one-in-three bleed chance. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp939BleedingEvents {
    private Scp939BleedingEvents() {
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getAmount() <= 0.0F
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(event.getSource().getEntity() instanceof Scp939Entity)) {
            return;
        }
        if (player.getRandom().nextInt(3) == 0) {
            BleedingManager.apply(player);
        }
    }
}
