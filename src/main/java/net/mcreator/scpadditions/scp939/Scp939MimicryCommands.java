package net.mcreator.scpadditions.scp939;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Session-only explicit consent controls for SCP-939 voice mimicry. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp939MimicryCommands {
    private Scp939MimicryCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("scp939")
                .then(Commands.literal("mimicry")
                        .then(Commands.literal("allow")
                                .executes(context -> setConsent(
                                        context.getSource().getPlayerOrException(),
                                        true)))
                        .then(Commands.literal("deny")
                                .executes(context -> setConsent(
                                        context.getSource().getPlayerOrException(),
                                        false)))
                        .then(Commands.literal("status")
                                .executes(context -> status(
                                        context.getSource().getPlayerOrException())))));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Scp939MimicryHooks.forget(event.getEntity().getUUID());
    }

    private static int setConsent(ServerPlayer player, boolean allowed) {
        if (!Scp939MimicryHooks.available()) {
            player.sendSystemMessage(Component.literal(
                    "SCP-939 voice mimicry is unavailable because the Simple Voice Chat integration is not active."));
            return 0;
        }
        if (!Scp939MimicryHooks.setConsent(player, allowed)) {
            player.sendSystemMessage(Component.literal(
                    "SCP-939 voice mimicry could not change consent while the integration is disabled."));
            return 0;
        }

        if (allowed) {
            player.sendSystemMessage(Component.literal(
                    "SCP-939 voice mimicry enabled for this session. Voice fragments may be held temporarily in memory only, expire automatically, and are cleared when you opt out or disconnect."));
        } else {
            player.sendSystemMessage(Component.literal(
                    "SCP-939 voice mimicry disabled. Any buffered voice fragments for you were cleared."));
        }
        return 1;
    }

    private static int status(ServerPlayer player) {
        if (!Scp939MimicryHooks.available()) {
            player.sendSystemMessage(Component.literal(
                    "SCP-939 voice mimicry: unavailable."));
            return 0;
        }
        boolean allowed = Scp939MimicryHooks.hasConsent(player.getUUID());
        player.sendSystemMessage(Component.literal(
                "SCP-939 voice mimicry: " + (allowed
                        ? "allowed for this session."
                        : "not allowed.")));
        return allowed ? 1 : 0;
    }
}
