package com.bl4ues.scpinventory.context;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.config.ui.ConfigCenterService;

@Mod.EventBusSubscriber(modid = "scp_additions")
public final class ContextConfigCommands {
    private ContextConfigCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("scpinventory")
                .then(Commands.literal("context")
                        .requires(source -> source.getEntity() instanceof ServerPlayer player
                                ? ConfigCenterService.canEdit(player)
                                : source.hasPermission(ConfigCenterService.REQUIRED_PERMISSION_LEVEL))
                        .then(Commands.literal("select")
                                .executes(ctx -> {
                                    ContextConfigManager.selectLookedBlock(ctx.getSource().getPlayerOrException());
                                    return 1;
                                }))
                        .then(Commands.literal("gui")
                                .executes(ctx -> {
                                    ContextEntityConfigManager.openGuiForLookedTarget(ctx.getSource().getPlayerOrException());
                                    return 1;
                                }))
                        .then(Commands.literal("add")
                                .executes(ctx -> ContextConfigManager.addPending(ctx.getSource())))
                        .then(Commands.literal("cancel")
                                .executes(ctx -> ContextConfigManager.cancel(ctx.getSource())))
                        .then(Commands.literal("done")
                                .executes(ctx -> ContextConfigManager.done(ctx.getSource())))
                        .then(Commands.literal("reload")
                                .executes(ctx -> ContextConfigManager.reload(ctx.getSource())))
                        .then(Commands.literal("marker")
                                .executes(ctx -> ContextConfigManager.marker(ctx.getSource())))
                        .then(Commands.literal("set")
                                .then(Commands.literal("action")
                                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                                .executes(ctx -> ContextConfigManager.setText(ctx.getSource(), "action", StringArgumentType.getString(ctx, "text")))))
                                .then(Commands.literal("name")
                                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                                .executes(ctx -> ContextConfigManager.setText(ctx.getSource(), "name", StringArgumentType.getString(ctx, "text")))))
                                .then(Commands.literal("range")
                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.25D, 64.0D))
                                                .executes(ctx -> ContextConfigManager.setRange(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "value"))))))
                        .then(Commands.literal("input")
                                .then(Commands.argument("mode", StringArgumentType.word())
                                        .executes(ctx -> ContextConfigManager.setInput(ctx.getSource(), StringArgumentType.getString(ctx, "mode")))))
                        .then(Commands.literal("item")
                                .then(Commands.argument("mode", StringArgumentType.word())
                                        .executes(ctx -> ContextConfigManager.setUseItem(ctx.getSource(), StringArgumentType.getString(ctx, "mode")))))
                        .then(Commands.literal("clickface")
                                .then(Commands.argument("face", StringArgumentType.word())
                                        .executes(ctx -> ContextConfigManager.setClickFace(ctx.getSource(), StringArgumentType.getString(ctx, "face")))))
                        .then(Commands.literal("rotate")
                                .then(Commands.argument("mode", StringArgumentType.word())
                                        .executes(ctx -> ContextConfigManager.setRotateWith(ctx.getSource(), StringArgumentType.getString(ctx, "mode")))))
                        .then(Commands.literal("anchor")
                                .then(Commands.literal("hit")
                                        .executes(ctx -> ContextConfigManager.setAnchorHit(ctx.getSource())))
                                .then(Commands.literal("here")
                                        .then(Commands.argument("distance", DoubleArgumentType.doubleArg(0.25D, 16.0D))
                                                .executes(ctx -> ContextConfigManager.setAnchorHere(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "distance")))))
                                .then(Commands.literal("nudge")
                                        .then(Commands.argument("x", DoubleArgumentType.doubleArg(-16.0D, 16.0D))
                                                .then(Commands.argument("y", DoubleArgumentType.doubleArg(-16.0D, 16.0D))
                                                        .then(Commands.argument("z", DoubleArgumentType.doubleArg(-16.0D, 16.0D))
                                                                .executes(ctx -> ContextConfigManager.nudgeAnchor(ctx.getSource(),
                                                                        DoubleArgumentType.getDouble(ctx, "x"),
                                                                        DoubleArgumentType.getDouble(ctx, "y"),
                                                                        DoubleArgumentType.getDouble(ctx, "z"))))))))));
    }
}
