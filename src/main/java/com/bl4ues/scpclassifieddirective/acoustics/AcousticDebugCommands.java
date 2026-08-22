package com.bl4ues.scpclassifieddirective.acoustics;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

import java.util.Locale;

/** Operator-only probes for tuning sound-driven AI before SCP-939 is wired in. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AcousticDebugCommands {
    private AcousticDebugCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root =
                Commands.literal("acousticDebug")
                        .requires(source -> source.hasPermission(2));

        root.then(Commands.literal("listen")
                .executes(context -> listen(context.getSource())));

        LiteralArgumentBuilder<CommandSourceStack> emit =
                Commands.literal("emit");
        for (AcousticCategory category : AcousticCategory.values()) {
            emit.then(Commands.literal(category.name()
                            .toLowerCase(Locale.ROOT))
                    .executes(context -> emit(context.getSource(), category)));
        }
        root.then(emit);
        event.getDispatcher().register(root);
    }

    private static int emit(CommandSourceStack source, AcousticCategory category)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        AcousticStimulusSystem.emit(player.serverLevel(), player.position(),
                category, 1.0F, player);
        source.sendSuccess(() -> Component.literal("Emitted "
                + category.name().toLowerCase(Locale.ROOT)
                + " acoustic stimulus at intensity 1.0."), false);
        return 1;
    }

    private static int listen(CommandSourceStack source)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        long now = player.serverLevel().getGameTime();
        var perception = AcousticStimulusSystem.loudest(
                player.serverLevel(), player.position(),
                now - AcousticStimulusSystem.RETENTION_TICKS,
                1.0D, 0.001F);
        if (perception.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                    "No recent acoustic stimulus is audible here."), false);
            return 0;
        }

        AcousticPerception heard = perception.get();
        AcousticStimulus stimulus = heard.stimulus();
        String sourceId = stimulus.sourceEntityId() == null
                ? "none" : stimulus.sourceEntityId().toString();
        String message = String.format(Locale.ROOT,
                "%s perceived=%.3f raw=%.2f distance=%.2f blocks occlusion=%d age=%dt source=%s",
                stimulus.category().name(), heard.perceivedIntensity(),
                stimulus.intensity(), heard.distance(), heard.occlusionLayers(),
                heard.ageTicks(), sourceId);
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }
}
