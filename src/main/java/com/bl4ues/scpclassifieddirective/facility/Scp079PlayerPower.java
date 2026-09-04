package com.bl4ues.scpclassifieddirective.facility;

import net.minecraft.server.level.ServerLevel;

/** Manual AP spending path used by a player controlling SCP-079. */
public final class Scp079PlayerPower {
    private Scp079PlayerPower() {
    }

    /**
     * Spends the normal difficulty-adjusted cost without the automatic AI's
     * strategic reserve/utility veto. The player is the strategist here.
     */
    public static boolean trySpend(ServerLevel level, double baseCost) {
        if (level == null || baseCost < 0.0D
                || !Scp079PlayableManager.hasController(level.getServer())
                || !Scp079ProcessingManager.isActive(level)) {
            return false;
        }
        // Advance lazy regeneration before touching persistent power directly.
        double current = Scp079ProcessingManager.getPower(level);
        double cost = Scp079ProcessingManager.adjustedActionCost(level, baseCost);
        if (current + 0.0001D < cost) return false;
        Scp079ProcessingSavedData data = Scp079ProcessingSavedData.get(
                level.getServer());
        data.setPower(current - cost);
        Scp079ScreenState.pulse(level.getServer());
        return true;
    }
}
