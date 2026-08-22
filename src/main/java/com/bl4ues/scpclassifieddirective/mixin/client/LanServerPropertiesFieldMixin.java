package com.bl4ues.scpclassifieddirective.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import com.bl4ues.scpclassifieddirective.client.CustomPauseMenuScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

/** Centers Lan Server Properties' borderless max-player input inside our field. */
@Mixin(EditBox.class)
public abstract class LanServerPropertiesFieldMixin {
    @Unique private boolean scpClassifiedDirective$lanFieldShifted;
    @Unique private int scpClassifiedDirective$lanFieldOriginalX;
    @Unique private int scpClassifiedDirective$lanFieldOriginalY;

    @Inject(method = "renderWidget", at = @At("HEAD"))
    private void scpClassifiedDirective$alignLanMaxPlayers(GuiGraphics graphics,
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

        scpClassifiedDirective$lanFieldOriginalX = field.getX();
        scpClassifiedDirective$lanFieldOriginalY = field.getY();
        field.setX(scpClassifiedDirective$lanFieldOriginalX + 8);
        field.setY(scpClassifiedDirective$lanFieldOriginalY + 6);
        scpClassifiedDirective$lanFieldShifted = true;
    }

    @Inject(method = "renderWidget", at = @At("RETURN"))
    private void scpClassifiedDirective$restoreLanMaxPlayersPosition(GuiGraphics graphics,
            int mouseX, int mouseY, float partialTick, CallbackInfo callback) {
        if (!scpClassifiedDirective$lanFieldShifted) return;
        EditBox field = (EditBox) (Object) this;
        field.setX(scpClassifiedDirective$lanFieldOriginalX);
        field.setY(scpClassifiedDirective$lanFieldOriginalY);
        scpClassifiedDirective$lanFieldShifted = false;
    }
}
