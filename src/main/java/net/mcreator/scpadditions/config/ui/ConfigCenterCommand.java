package net.mcreator.scpadditions.config.ui;

import com.bl4ues.scpinventory.network.ModNetwork;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Optional command entry point for operators who prefer opening the editor from chat. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
