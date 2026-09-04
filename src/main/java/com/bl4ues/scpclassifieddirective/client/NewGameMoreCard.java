package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/** Advanced world-creation actions and compatibility slots for injected options. */
final class NewGameMoreCard {
    private static final int MUTED = 0xFF9FA6AD;
    private static final int ACCENT = 0xFFC99B18;
    private static final int CARD = 0xB20B0E12;
    private static final int BORDER = 0x70444C57;

    private final Consumer<GuiEventListener> registrar;
    private final NewGameWidgets.ActionButton gameRules;
    private final NewGameWidgets.ActionButton experiments;
    private final NewGameWidgets.ActionButton dataPacks;
    private final Map<String, ForeignBinding> foreign = new LinkedHashMap<>();

    NewGameMoreCard(Consumer<GuiEventListener> registrar,
            Runnable gameRulesAction, Runnable experimentsAction,
            Runnable dataPacksAction) {
        this.registrar = registrar;
        this.gameRules = register(new NewGameWidgets.ActionButton(
                0, 0, 220, 34, ScpFonts.roboto("Game Rules"), gameRulesAction));
        this.experiments = register(new NewGameWidgets.ActionButton(
                0, 0, 220, 34, ScpFonts.roboto("Experiments"), experimentsAction));
        this.dataPacks = register(new NewGameWidgets.ActionButton(
                0, 0, 220, 34, ScpFonts.roboto("Data Packs"), dataPacksAction));
    }

    void syncForeign(List<AbstractButton> sources,
            Consumer<GuiEventListener> currentRegistrar) {
        for (AbstractButton source : sources) {
            String key = sourceKey(source);
            if (key.isBlank()) continue;
            ForeignBinding binding = foreign.get(key);
            if (binding == null) {
                binding = new ForeignBinding(source);
                ForeignBinding captured = binding;
                NewGameWidgets.ActionButton wrapper = new NewGameWidgets.ActionButton(
                        0, 0, 220, 34, ScpFonts.roboto(source.getMessage()),
                        captured::press);
                binding.wrapper = wrapper;
                foreign.put(key, binding);
                currentRegistrar.accept(wrapper);
            } else {
                binding.source = source;
                binding.wrapper.setMessage(ScpFonts.roboto(source.getMessage()));
            }
            binding.wrapper.active = source.active;
            source.visible = false;
        }
    }

    int height(int width) {
        int extras = foreign.size();
        if (width < 620) {
            return 72 + (3 + extras) * 40 + (extras > 0 ? 24 : 0);
        }
        int extraRows = (extras + 2) / 3;
        return 116 + extraRows * 40 + (extras > 0 ? 22 : 0);
    }

    void position(int x, int y, int width) {
        int pad = Mth.clamp(Math.round(width * 0.035F), 18, 30);
        int inner = width - pad * 2;
        boolean narrow = width < 620;
        if (narrow) {
            int rowY = y + 48;
            for (NewGameWidgets.ActionButton button : allButtons()) {
                button.setX(x + pad);
                button.setY(rowY);
                button.setWidth(inner);
                button.setHeight(34);
                rowY += 40;
            }
            return;
        }

        int gap = 10;
        int column = (inner - gap * 2) / 3;
        gameRules.setX(x + pad);
        gameRules.setY(y + 48);
        gameRules.setWidth(column);
        experiments.setX(x + pad + column + gap);
        experiments.setY(y + 48);
        experiments.setWidth(column);
        dataPacks.setX(x + pad + (column + gap) * 2);
        dataPacks.setY(y + 48);
        dataPacks.setWidth(column);

        int index = 0;
        int startY = y + 106;
        for (ForeignBinding binding : foreign.values()) {
            NewGameWidgets.ActionButton button = binding.wrapper;
            int col = index % 3;
            int row = index / 3;
            button.setX(x + pad + col * (column + gap));
            button.setY(startY + row * 40);
            button.setWidth(column);
            button.setHeight(34);
            button.setMessage(ScpFonts.roboto(binding.source.getMessage()));
            button.active = binding.source.active;
            index++;
        }
    }

    void setVisibleInViewport(int top, int bottom) {
        for (NewGameWidgets.ActionButton button : allButtons()) {
            button.visible = button.getY() + button.getHeight() > top
                    && button.getY() < bottom;
        }
    }

    void renderBackground(GuiGraphics graphics, int x, int y, int width,
            float alpha) {
        int h = height(width);
        graphics.fill(x, y, x + width, y + h, fade(CARD, alpha));
        graphics.fill(x, y, x + 4, y + h, fade(ACCENT, alpha * 0.9F));
        graphics.fill(x + 18, y + 18, x + width - 18, y + 19,
                fade(BORDER, alpha));
        if (!foreign.isEmpty()) {
            int markerY = width < 620 ? y + 48 + 3 * 40 + 3 : y + 92;
            graphics.drawString(Minecraft.getInstance().font,
                    ScpFonts.montserrat("MOD OPTIONS"), x + 24, markerY,
                    fade(MUTED, alpha), false);
        }
    }

    void renderControls(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        for (NewGameWidgets.ActionButton button : allButtons()) {
            if (button.visible) button.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    List<GuiEventListener> widgets() {
        return new ArrayList<>(allButtons());
    }

    private List<NewGameWidgets.ActionButton> allButtons() {
        List<NewGameWidgets.ActionButton> out = new ArrayList<>();
        out.add(gameRules);
        out.add(experiments);
        out.add(dataPacks);
        for (ForeignBinding binding : foreign.values()) out.add(binding.wrapper);
        return out;
    }

    private <T extends GuiEventListener> T register(T listener) {
        registrar.accept(listener);
        return listener;
    }

    static boolean looksVanillaCreationButton(AbstractButton button) {
        String key = translationKey(button.getMessage()).toLowerCase(Locale.ROOT);
        String text = button.getMessage().getString().trim().toLowerCase(Locale.ROOT);
        String combined = key + " " + text;
        return combined.contains("createworld")
                || combined.contains("selectworld")
                || combined.contains("gamemode")
                || combined.contains("game mode")
                || combined.contains("difficulty")
                || combined.contains("allow cheats")
                || combined.contains("commands")
                || combined.contains("world type")
                || combined.contains("worldtype")
                || combined.contains("customize")
                || combined.contains("seed")
                || combined.contains("structures")
                || combined.contains("bonus chest")
                || combined.contains("bonuschest")
                || combined.contains("game rules")
                || combined.contains("gamerules")
                || combined.contains("experiments")
                || combined.contains("data packs")
                || combined.contains("datapacks")
                || "gui.cancel".equals(key)
                || "gui.done".equals(key)
                || text.equals("game")
                || text.equals("world")
                || text.equals("more")
                || text.equals("create new world")
                || text.equals("create");
    }

    private static String sourceKey(AbstractButton source) {
        String translation = translationKey(source.getMessage());
        if (!translation.isBlank()) return translation;
        return source.getMessage().getString().trim().toLowerCase(Locale.ROOT);
    }

    private static String translationKey(Component component) {
        if (component != null && component.getContents() instanceof TranslatableContents t) {
            return t.getKey();
        }
        return "";
    }

    private static int fade(int color, float alpha) {
        int source = (color >>> 24) & 0xFF;
        int out = Mth.clamp(Math.round(source * alpha), 0, 255);
        return (out << 24) | (color & 0x00FFFFFF);
    }

    private static final class ForeignBinding {
        private AbstractButton source;
        private NewGameWidgets.ActionButton wrapper;

        private ForeignBinding(AbstractButton source) {
            this.source = source;
        }

        private void press() {
            if (source == null || !source.active) return;
            source.onPress();
        }
    }
}
