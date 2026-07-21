package net.mcreator.scpadditions.roamer;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Operator commands for world-persistent roamer spawn rules and cleanup. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RoamerCommands {
    private RoamerCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("disableAllRoamers")
                .requires(source -> source.hasPermission(2))
                .executes(context -> setAll(context.getSource(), false)));
        event.getDispatcher().register(Commands.literal("enableAllRoamers")
                .requires(source -> source.hasPermission(2))
                .executes(context -> setAll(context.getSource(), true)));
        event.getDispatcher().register(Commands.literal("despawnAllRoamers")
                .requires(source -> source.hasPermission(2))
                .executes(context -> despawnAll(context.getSource())));

        LiteralArgumentBuilder<CommandSourceStack> despawn =
                Commands.literal("despawnRoamer")
                        .requires(source -> source.hasPermission(2));
        for (RoamerType type : RoamerType.values()) {
            despawn.then(Commands.literal(type.commandId())
                    .executes(context -> despawn(context.getSource(), type)));
        }
        event.getDispatcher().register(despawn);
    }

    private static int setAll(CommandSourceStack source, boolean enabled) {
        MinecraftServer server = source.getServer();
        for (RoamerType type : RoamerType.values()) {
            RoamerManager.setSpawnRule(server, type, enabled);
        }
        source.sendSuccess(() -> Component.literal(enabled
                ? "Enabled every roamer spawn gamerule."
                : "Disabled every roamer spawn gamerule."), true);
        return RoamerType.values().length;
    }

    private static int despawnAll(CommandSourceStack source) {
        int removed = RoamerManager.despawnAll(source.getServer());
        source.sendSuccess(() -> Component.literal("Despawned " + removed
                + " loaded roamer" + (removed == 1 ? "" : "s")
                + ". Enabled roamers restarted their spawn cycle."), true);
        return removed;
    }

    private static int despawn(CommandSourceStack source, RoamerType type) {
        int removed = RoamerManager.despawn(source.getServer(), type);
        source.sendSuccess(() -> Component.literal("Despawned " + removed
                + " loaded " + type.displayName() + " instance"
                + (removed == 1 ? "" : "s")
                + ". Its spawn cycle will continue if enabled."), true);
        return removed;
    }
}
