package com.bl4ues.scpclassifieddirective.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.network.ScpClassifiedDirectiveModVariables;

@Mod.EventBusSubscriber
public final class BloodType1Procedure {
    private BloodType1Procedure() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        ensureBloodType(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerRespawned(PlayerEvent.PlayerRespawnEvent event) {
        ensureBloodType(event.getEntity());
    }

    public static void execute(Entity entity) {
        ensureBloodType(entity);
    }

    private static void ensureBloodType(Entity entity) {
        if (entity == null || entity.level().isClientSide()) return;

        entity.getCapability(ScpClassifiedDirectiveModVariables.PLAYER_VARIABLES_CAPABILITY)
                .ifPresent(variables -> {
                    if (!hasBloodType(variables)) {
                        clearBloodType(variables);
                        switch (Math.floorMod(entity.getUUID().hashCode(), 8)) {
                            case 0 -> variables.Oneg = true;
                            case 1 -> variables.Opos = true;
                            case 2 -> variables.Aneg = true;
                            case 3 -> variables.Apos = true;
                            case 4 -> variables.Bneg = true;
                            case 5 -> variables.Bpos = true;
                            case 6 -> variables.ABneg = true;
                            default -> variables.ABpos = true;
                        }
                    }
                    variables.syncPlayerVariables(entity);
                });
    }

    private static boolean hasBloodType(
            ScpClassifiedDirectiveModVariables.PlayerVariables variables) {
        return variables.Oneg || variables.Opos
                || variables.Aneg || variables.Apos
                || variables.Bneg || variables.Bpos
                || variables.ABneg || variables.ABpos;
    }

    private static void clearBloodType(
            ScpClassifiedDirectiveModVariables.PlayerVariables variables) {
        variables.Oneg = false;
        variables.Opos = false;
        variables.Aneg = false;
        variables.Apos = false;
        variables.Bneg = false;
        variables.Bpos = false;
        variables.ABneg = false;
        variables.ABpos = false;
    }
}
