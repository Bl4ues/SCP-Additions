package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

import java.util.List;

/** Adds concise gameplay explanations without changing registry compatibility. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GameplayItemTooltipEvents {
    private GameplayItemTooltipEvents() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (id == null || !ScpClassifiedDirectiveMod.MODID.equals(id.getNamespace())) {
            return;
        }

        List<Component> tooltip = event.getToolTip();
        String path = id.getPath();

        // SCP artifacts, containment objects and spawn eggs use the high-rarity
        // purple name color. Ordinary facility props remain common even when
        // their registry path names the SCP they reference.
        if (path.startsWith("scp_")
                && !"scp_914_usage_notice".equals(path)
                && !tooltip.isEmpty()) {
            tooltip.set(0, tooltip.get(0).copy().withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        switch (path) {
            case "tesla_gate" -> addPair(tooltip,
                    "tooltip.scp_classified_directive.tesla_gate_primary",
                    "tooltip.scp_classified_directive.tesla_gate_secondary");
            case "button_closed" -> addPair(tooltip,
                    "tooltip.scp_classified_directive.button_functional_primary",
                    "tooltip.scp_classified_directive.button_functional_secondary");
            case "button_locked" -> addPair(tooltip,
                    "tooltip.scp_classified_directive.button_locked_primary",
                    "tooltip.scp_classified_directive.button_locked_secondary");
            case "default_door", "yellow_closed", "black_closed" -> addPair(tooltip,
                    "tooltip.scp_classified_directive.heavy_door_primary",
                    "tooltip.scp_classified_directive.heavy_door_secondary");
            case "tesla_terminal_block" -> {
                tooltip.add(Component.literal(
                                "Controls the connected Tesla Gate network")
                        .withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.literal(
                                "Use a Screwdriver to cycle the terminal on or off")
                        .withStyle(ChatFormatting.AQUA));
            }
            case "tesla_terminal_off" -> tooltip.add(
                    Component.translatable("tooltip.scp_classified_directive.decorative_prop")
                            .withStyle(ChatFormatting.GRAY));
            default -> {
            }
        }
    }

    private static void addPair(List<Component> tooltip,
            String primaryKey, String secondaryKey) {
        tooltip.add(Component.translatable(primaryKey)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(secondaryKey)
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
