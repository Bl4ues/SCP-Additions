package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-side debounce and visual cooldown state for SCP-079 device actions. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Scp079ActionCooldownClient {
    private static final int ACTION_COOLDOWN_TICKS = 16;
    private static boolean attackLatched;
    private static boolean useLatched;
    private static long attackUntil;
    private static long useUntil;

    private Scp079ActionCooldownClient() {
    }

    public static boolean blocked(boolean attack, boolean use) {
        Minecraft minecraft = Minecraft.getInstance();
        long now = now(minecraft);
        if (attack && (attackLatched || now < attackUntil)) return true;
        return use && (useLatched || now < useUntil);
    }

    public static void mark(boolean attack, boolean use) {
        Minecraft minecraft = Minecraft.getInstance();
        long until = now(minecraft) + ACTION_COOLDOWN_TICKS;
        if (attack) {
            attackLatched = true;
            attackUntil = until;
        }
        if (use) {
            useLatched = true;
            useUntil = until;
        }
    }

    public static float iconBrightness(ResourceLocation icon) {
        if (icon == null) return 1.0F;
        String path = icon.getPath();
        if (path.endsWith("camera.png")) return 1.0F;
        boolean door = path.endsWith("door.png");
        boolean tesla = path.endsWith("tesla_gate.png");
        if (tesla && blocked(true, false)) return 0.38F;
        if (door && (blocked(true, false) || blocked(false, true))) return 0.38F;
        return 1.0F;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (!Scp079PlayableClient.active()) {
            clear();
            return;
        }
        if (!minecraft.options.keyAttack.isDown()) attackLatched = false;
        if (!minecraft.options.keyUse.isDown()) useLatched = false;
    }

    private static long now(Minecraft minecraft) {
        return minecraft.level == null ? 0L : minecraft.level.getGameTime();
    }

    private static void clear() {
        attackLatched = false;
        useLatched = false;
        attackUntil = 0L;
        useUntil = 0L;
    }
}
