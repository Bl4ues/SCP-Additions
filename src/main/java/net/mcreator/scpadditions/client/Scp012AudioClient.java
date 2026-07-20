package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModSounds;

/** Controls local trance and fifteen-second damage audio independently. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class Scp012AudioClient {
    private static Scp012FadingSound trance;
    private static Scp012FadingSound damage;
    private static boolean tranceWasActive;
    private static boolean damageWasActive;

    private Scp012AudioClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        boolean playerReady = minecraft.player != null
                && minecraft.level != null && minecraft.player.isAlive();
        boolean tranceActive = playerReady && Scp012ClientState.isActive();
        boolean damageActive = playerReady
                && Scp012ClientState.isDamageActive();

        if (tranceActive && (!tranceWasActive || trance == null)) {
            trance = restart(minecraft, trance,
                    new Scp012FadingSound(
                            ScpAdditionsModSounds.SCP012_TRANCE.get(),
                            true, 0.85F, 30, 35));
        } else if (!tranceActive && tranceWasActive && trance != null) {
            trance.beginFadeOut();
        }

        if (damageActive && !damageWasActive) {
            damage = restart(minecraft, damage,
                    new Scp012FadingSound(
                            ScpAdditionsModSounds.SCP012_DAMAGE.get(),
                            false, 1.0F, 25, 35));
        } else if (!damageActive && damageWasActive && damage != null) {
            damage.beginFadeOut();
        }

        tranceWasActive = tranceActive;
        damageWasActive = damageActive;

        if (trance != null && !minecraft.getSoundManager().isActive(trance)) {
            trance = null;
        }
        if (damage != null && !minecraft.getSoundManager().isActive(damage)) {
            damage = null;
        }
    }

    private static Scp012FadingSound restart(Minecraft minecraft,
                                             Scp012FadingSound previous,
                                             Scp012FadingSound next) {
        if (previous != null) {
            minecraft.getSoundManager().stop(previous);
        }
        minecraft.getSoundManager().play(next);
        return next;
    }
}
