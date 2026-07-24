package net.mcreator.scpadditions.roamer;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.event.Scp106SpawnEvents;
import net.mcreator.scpadditions.event.Scp173SpawnEvents;

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
        LiteralArgumentBuilder<CommandSourceStack> forceSpawn =
                Commands.literal("roamerForceSpawn")
                        .requires(source -> source.hasPermission(2));
        for (RoamerType type : RoamerType.values()) {
            despawn.then(Commands.literal(type.commandId())
                    .executes(context -> despawn(context.getSource(), type)));
            forceSpawn.then(Commands.literal(type.commandId())
                    .executes(context -> forceSpawn(
                            context.getSource(), type)));
        }
        event.getDispatcher().register(despawn);
        event.getDispatcher().register(forceSpawn);
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

    private static int forceSpawn(CommandSourceStack source, RoamerType type)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        RoamerResult result = switch (type) {
            case SCP_173 -> Scp173SpawnEvents.forceSpawn(player);
            case SCP_106 -> Scp106SpawnEvents.forceSpawn(player);
        };

        if (result == RoamerResult.SPAWNED) {
            source.sendSuccess(() -> Component.literal("Force-spawned "
                    + type.displayName()
                    + " using its natural encounter placement and behavior."),
                    true);
            return 1;
        }

        source.sendFailure(Component.literal("Could not force-spawn "
                + type.displayName() + ": " + failureReason(result) + "."));
        return 0;
    }

    private static String failureReason(RoamerResult result) {
        return switch (result) {
            case BLOCKED_BY_EXISTING -> "an instance is already active";
            case NO_VALID_POSITION -> "no valid natural spawn position was found";
            case MODULE_DISABLED -> "its module is disabled";
            case NOT_IMPLEMENTED -> "its natural spawn is not implemented";
            default -> result.name().toLowerCase().replace('_', ' ');
        };
    }
}
