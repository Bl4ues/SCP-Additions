package net.mcreator.scpadditions.client;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.List;

/** Adds concise gameplay explanations without changing registry compatibility. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GameplayItemTooltipEvents {
    private GameplayItemTooltipEvents() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (id == null || !ScpAdditionsMod.MODID.equals(id.getNamespace())) {
            return;
        }

        List<Component> tooltip = event.getToolTip();
        String path = id.getPath();

        // SCP artifacts, containment objects and spawn eggs use the high-rarity
        // purple name color. Ordinary facility architecture remains unchanged.
        if (path.startsWith("scp_") && !tooltip.isEmpty()) {
            tooltip.set(0, tooltip.get(0).copy().withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        switch (path) {
            case "tesla_gate" -> addPair(tooltip,
                    "tooltip.scp_additions.tesla_gate_primary",
                    "tooltip.scp_additions.tesla_gate_secondary");
            case "button_closed" -> addPair(tooltip,
                    "tooltip.scp_additions.button_functional_primary",
                    "tooltip.scp_additions.button_functional_secondary");
            case "button_locked" -> addPair(tooltip,
                    "tooltip.scp_additions.button_locked_primary",
                    "tooltip.scp_additions.button_locked_secondary");
            case "default_door", "yellow_closed", "black_closed" -> addPair(tooltip,
                    "tooltip.scp_additions.heavy_door_primary",
                    "tooltip.scp_additions.heavy_door_secondary");
            case "tesla_terminal_block", "tesla_terminal_off" -> addPair(tooltip,
                    "tooltip.scp_additions.tesla_terminal_primary",
                    "tooltip.scp_additions.tesla_terminal_secondary");
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
