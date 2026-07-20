package net.mcreator.scpadditions.scp012;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp012Commands {
    private static final int DEFAULT_SEARCH_RADIUS = 32;

    private Scp012Commands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("scp012")
                .requires(source -> source.hasPermission(2))
                .then(action("open", Action.OPEN))
                .then(action("close", Action.CLOSE))
                .then(action("toggle", Action.TOGGLE))
                .then(action("status", Action.STATUS)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> action(
            String name, Action action) {
        return Commands.literal(name)
                .executes(context -> execute(context.getSource(), null, action))
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(context -> execute(context.getSource(),
                                BlockPosArgument.getLoadedBlockPos(context, "pos"),
                                action)));
    }

    private static int execute(CommandSourceStack source, BlockPos requested,
                               Action action) {
        ServerLevel level = source.getLevel();
        BlockPos pos = requested;
        if (pos == null) {
            pos = Scp012Module.findNearest(level, source.getPosition(),
                    DEFAULT_SEARCH_RADIUS, false);
        }
        if (pos == null || !Scp012Module.isScp012(level.getBlockState(pos))) {
            source.sendFailure(Component.literal(
                    "No SCP-012 containment box was found within "
                            + DEFAULT_SEARCH_RADIUS + " blocks."));
            return 0;
        }

        Scp012Stage before = Scp012Module.stageOf(level.getBlockState(pos));
        boolean changed = switch (action) {
            case OPEN -> Scp012Module.open(level, pos);
            case CLOSE -> Scp012Module.close(level, pos);
            case TOGGLE -> Scp012Module.toggle(level, pos);
            case STATUS -> false;
        };
        Scp012Stage after = Scp012Module.stageOf(level.getBlockState(pos));
        BlockPos finalPos = pos;
        source.sendSuccess(() -> Component.literal("SCP-012 at "
                + finalPos.toShortString() + ": "
                + (changed ? before + " -> " + after : String.valueOf(after))),
                true);
        return 1;
    }

    private enum Action {
        OPEN,
        CLOSE,
        TOGGLE,
        STATUS
    }
}
