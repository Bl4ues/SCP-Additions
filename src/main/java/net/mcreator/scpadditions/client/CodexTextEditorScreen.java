package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class CodexTextEditorScreen extends Screen {
    private static final int MAX_TEXT_LENGTH = 65_536;
    private final Screen parent;
    private final String existingKey;
    private final Consumer<String> callback;
    private final List<String> lines = new ArrayList<>();
    private final Map<EditBox, Integer> lineBoxes = new LinkedHashMap<>();
    private int scroll;
    private int focusLine = -1;
    private boolean loadedExisting;
    private boolean uploading;
    private String notice;
    private int noticeColor = 0xFFA9AFBA;

    public CodexTextEditorScreen(Screen parent, String existingKey,
                                 Consumer<String> callback) {
        super(ScpFonts.roboto("Write Codex Text"));
        this.parent = parent;
        this.existingKey = existingKey == null ? "" : existingKey;
        this.callback = callback;
        setText("");
        this.loadedExisting = this.existingKey.isBlank();
        this.notice = this.loadedExisting
                ? "Enter creates a line. You may also drop one UTF-8 text file."
                : "Loading the text saved in this world...";
    }

    private void setText(String text) {
        lines.clear();
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        for (String line : normalized.split("\n", -1)) lines.add(line);
        if (lines.isEmpty()) lines.add("");
        scroll = 0;
        focusLine = -1;
    }

    @Override
    protected void init() { buildWidgets(); }

    @Override
    public void tick() {
        super.tick();
        if (!loadedExisting && !existingKey.isBlank()) {
            java.util.Optional<String> loaded = CodexAssetClient.getText(existingKey);
            if (loaded.isPresent()) {
                loadedExisting = true;
                setText(loaded.get());
                notice = "Loaded the text saved in this world.";
                noticeColor = 0xFF79D58B;
                rebuild();
            }
        }
    }

    private void buildWidgets() {
        lineBoxes.clear();
        int panelWidth = Math.min(720, width - 20);
        int panelHeight = Math.min(460, height - 16);
        int left = (width - panelWidth) / 2;
        int top = Math.max(8, (height - panelHeight) / 2);
        int editorTop = top + 58;
        int bottom = top + panelHeight - 54;
        int visible = Math.max(5, (bottom - editorTop) / 22);
        scroll = Math.max(0, Math.min(Math.max(0, lines.size() - visible), scroll));
        for (int row = 0; row < visible; row++) {
            int logical = scroll + row;
            if (logical >= lines.size()) break;
            EditBox box = new EditBox(font, left + 50, editorTop + row * 22,
                    panelWidth - 70, 20,
                    Component.literal("Line " + (logical + 1)));
            box.setMaxLength(4096);
            box.setValue(lines.get(logical));
            box.setFormatter((value, cursor) ->
                    ScpFonts.roboto(value).getVisualOrderText());
            box.setResponder(value -> lines.set(logical, value));
            addRenderableWidget(box);
            lineBoxes.put(box, logical);
            if (logical == focusLine) box.setFocused(true);
        }
        int third = (panelWidth - 52) / 3;
        addRenderableWidget(Button.builder(ScpFonts.roboto("Clear Text"), b -> {
            syncVisible();
            setText("");
            rebuild();
        }).bounds(left + 16, top + panelHeight - 30, third, 22).build());
        addRenderableWidget(Button.builder(ScpFonts.roboto("Save Text"),
                b -> save()).bounds(left + 26 + third,
                top + panelHeight - 30, third, 22).build());
        addRenderableWidget(Button.builder(ScpFonts.roboto("Cancel"),
                b -> Minecraft.getInstance().setScreen(parent))
                .bounds(left + 36 + third * 2,
                        top + panelHeight - 30, third, 22).build());
    }

    private void rebuild() { clearWidgets(); buildWidgets(); }

    private void syncVisible() {
        for (Map.Entry<EditBox, Integer> entry : lineBoxes.entrySet()) {
            lines.set(entry.getValue(), entry.getKey().getValue());
        }
    }

    private void save() {
        if (uploading) return;
        syncVisible();
        String text = String.join("\n", lines);
        if (text.length() > MAX_TEXT_LENGTH) {
            notice = "Text is too long. Maximum: 65,536 characters.";
            noticeColor = 0xFFD46060;
            return;
        }
        if (text.isBlank()) {
            if (callback != null) callback.accept("");
            return;
        }
        uploading = true;
        notice = "Saving text in this world...";
        noticeColor = 0xFFE5D49A;
        CodexAssetClient.upload("text", "codex-document.txt",
                text.getBytes(StandardCharsets.UTF_8), key -> {
                    uploading = false;
                    if (callback != null) callback.accept(key);
                }, message -> {
                    uploading = false;
                    notice = message;
                    noticeColor = 0xFFD46060;
                });
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (Map.Entry<EditBox, Integer> entry : lineBoxes.entrySet()) {
            if (!entry.getKey().isFocused()) continue;
            int index = entry.getValue();
            if (keyCode == GLFW.GLFW_KEY_ENTER
                    || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                syncVisible();
                lines.add(index + 1, "");
                focusLine = index + 1;
                ensureVisible(focusLine);
                rebuild();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_UP && index > 0) {
                syncVisible(); focusLine = index - 1;
                ensureVisible(focusLine); rebuild(); return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN && index + 1 < lines.size()) {
                syncVisible(); focusLine = index + 1;
                ensureVisible(focusLine); rebuild(); return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void ensureVisible(int line) {
        int panelHeight = Math.min(460, height - 16);
        int visible = Math.max(5, (panelHeight - 112) / 22);
        if (line < scroll) scroll = line;
        if (line >= scroll + visible) scroll = line - visible + 1;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        syncVisible();
        int visible = Math.max(5, (Math.min(460, height - 16) - 112) / 22);
        int next = Math.max(0, Math.min(Math.max(0, lines.size() - visible),
                scroll + (scrollY < 0 ? 1 : -1)));
        if (next != scroll) {
            scroll = next; focusLine = -1; rebuild(); return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        if (paths == null || paths.size() != 1) {
            notice = "Drop exactly one UTF-8 text file.";
            noticeColor = 0xFFD46060;
            return;
        }
        try {
            byte[] bytes = Files.readAllBytes(paths.get(0));
            String text = new String(bytes, StandardCharsets.UTF_8);
            if (text.length() > MAX_TEXT_LENGTH) {
                notice = "Text exceeds 65,536 characters.";
                noticeColor = 0xFFD46060;
                return;
            }
            loadedExisting = true;
            setText(text);
            notice = "Imported " + paths.get(0).getFileName();
            noticeColor = 0xFF79D58B;
            rebuild();
        } catch (Exception exception) {
            notice = "Could not read that text file.";
            noticeColor = 0xFFD46060;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
                       float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int panelWidth = Math.min(720, width - 20);
        int panelHeight = Math.min(460, height - 16);
        int left = (width - panelWidth) / 2;
        int top = Math.max(8, (height - panelHeight) / 2);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xFF111317);
        graphics.fill(left, top, left + panelWidth, top + 34, 0xFF24282E);
        graphics.fill(left, top + 33, left + panelWidth, top + 34, 0xFFC59A2A);
        graphics.drawString(font, ScpFonts.montserrat("WRITE CODEX TEXT"),
                left + 16, top + 12, 0xFFF7F8FC, false);
        graphics.drawString(font, ScpFonts.roboto(notice),
                left + 16, top + 42, noticeColor, false);
        for (Map.Entry<EditBox, Integer> entry : lineBoxes.entrySet()) {
            graphics.drawString(font,
                    ScpFonts.roboto(Integer.toString(entry.getValue() + 1)),
                    left + 18, entry.getKey().getY() + 6,
                    0xFF6F7888, false);
        }
        graphics.drawString(font, ScpFonts.roboto(lines.size() + " line(s) · "
                        + String.join("\n", lines).length() + "/"
                        + MAX_TEXT_LENGTH + " characters"),
                left + 16, top + panelHeight - 43, 0xFFA9AFBA, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() { Minecraft.getInstance().setScreen(parent); }
    @Override
    public boolean isPauseScreen() { return false; }
}
