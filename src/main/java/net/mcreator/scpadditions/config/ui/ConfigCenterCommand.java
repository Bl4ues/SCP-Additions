package net.mcreator.scpadditions.config.ui;

import net.neoforged.fml.common.EventBusSubscriber;

import com.bl4ues.scpinventory.network.ModNetwork;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Optional command entry point for operators who prefer opening the editor from chat. */
@EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class ConfigCenterCommand {
    private ConfigCenterCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("scpadditions")
                .then(Commands.literal("config")
                        .requires(source -> source.getEntity() instanceof ServerPlayer player
                                ? ConfigCenterService.canEdit(player)
                                : source.hasPermission(ConfigCenterService.REQUIRED_PERMISSION_LEVEL))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            ConfigCenterNetwork.openFor(player, ModNetwork.CHANNEL);
                            return 1;
                        })));
    }
}
