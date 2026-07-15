
package net.mcreator.scpadditions.client.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.network.ScpAdditionsModVariables;

/** Renders every blood type with one capability lookup and one screen listener. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class BloodTypeOverlayOverlay {
    private static final int COLOR = -65536;

    private BloodTypeOverlayOverlay() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void eventHandler(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen)) return;

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        Component label = resolveLabel(player);
        if (label == null) return;

        int width = event.getScreen().width;
        int height = event.getScreen().height;
        event.getGuiGraphics().drawString(minecraft.font, label,
                width / 2 - 208, height / 2 + 105, COLOR, false);
    }

    private static Component resolveLabel(Player player) {
        if (player == null) return null;
        return player.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY)
                .resolve()
                .map(variables -> {
                    if (variables.Oneg) return Component.translatable(
                            "gui.scp_additions.blood_type_overlay.label_blood_type_o");
                    if (variables.Opos) return Component.translatable(
                            "gui.scp_additions.blood_overlay_opos.label_blood_type_o");
                    if (variables.Aneg) return Component.translatable(
                            "gui.scp_additions.blood_overlay_aneg.label_blood_type_a");
                    if (variables.Apos) return Component.translatable(
                            "gui.scp_additions.blood_overlay_apos.label_blood_type_a");
                    if (variables.Bneg) return Component.translatable(
                            "gui.scp_additions.blood_overlay_bneg.label_blood_type_b");
                    if (variables.Bpos) return Component.translatable(
                            "gui.scp_additions.blood_overlay_bpos.label_blood_type_b");
                    if (variables.ABneg) return Component.translatable(
                            "gui.scp_additions.blood_overlay_a_bneg.label_blood_type_ab");
                    if (variables.ABpos) return Component.translatable(
                            "gui.scp_additions.blood_overlay_a_bpos.label_blood_type_ab");
                    return null;
                })
                .orElse(null);
    }
}
