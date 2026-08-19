package net.mcreator.scpadditions.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.mcreator.scpadditions.client.CustomPauseMenuScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

/** Centers Lan Server Properties' borderless max-player input inside our field. */
@Mixin(EditBox.class)
public abstract class LanServerPropertiesFieldMixin {
    @Unique private boolean scpAdditions$lanFieldShifted;
    @Unique private int scpAdditions$lanFieldOriginalX;
    @Unique private int scpAdditions$lanFieldOriginalY;

    @Inject(method = "renderWidget", at = @At("HEAD"))
    private void scpAdditions$alignLanMaxPlayers(GuiGraphics graphics,
            int mouseX, int mouseY, float partialTick, CallbackInfo callback) {
        EditBox field = (EditBox) (Object) this;
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof CustomPauseMenuScreen)) return;

        String label = field.getMessage() == null ? ""
                : field.getMessage().getString().trim().toLowerCase(Locale.ROOT);
        String compact = label.replace(" ", "").replace(":", "");
        if (!(compact.startsWith("maxplayer")
                || compact.startsWith("maxplayers"))) {
            return;
        }

        scpAdditions$lanFieldOriginalX = field.getX();
        scpAdditions$lanFieldOriginalY = field.getY();
        field.setX(scpAdditions$lanFieldOriginalX + 8);
        field.setY(scpAdditions$lanFieldOriginalY + 6);
        scpAdditions$lanFieldShifted = true;
    }

    @Inject(method = "renderWidget", at = @At("RETURN"))
    private void scpAdditions$restoreLanMaxPlayersPosition(GuiGraphics graphics,
            int mouseX, int mouseY, float partialTick, CallbackInfo callback) {
        if (!scpAdditions$lanFieldShifted) return;
        EditBox field = (EditBox) (Object) this;
        field.setX(scpAdditions$lanFieldOriginalX);
        field.setY(scpAdditions$lanFieldOriginalY);
        scpAdditions$lanFieldShifted = false;
    }
}
