package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Compact line-based multiline editor used by the live document composer. */
final class LineMarkdownEditor {
    private final List<String> lines = new ArrayList<>();
    private final Map<EditBox, Integer> visible = new LinkedHashMap<>();
    private int scroll;
    private int focus = -1;
    private int visibleCount = 4;

    LineMarkdownEditor(String body) { setText(body); }

    void setText(String text) {
        lines.clear();
        String normalized = text == null ? "" : text.replace("\r\n", "\n").replace('\r', '\n');
        for (String line : normalized.split("\n", -1)) lines.add(line);
        if (lines.isEmpty()) lines.add("");
        scroll = 0;
        focus = -1;
    }

    List<EditBox> build(Font font, int x, int y, int width, int height) {
        visible.clear();
        visibleCount = Math.max(3, height / 22);
        clamp();
        List<EditBox> widgets = new ArrayList<>();
        for (int row = 0; row < visibleCount; row++) {
            int index = scroll + row;
            if (index >= lines.size()) break;
            EditBox box = new EditBox(font, x + 28, y + row * 22, width - 28, 20,
                    Component.literal("Body line " + (index + 1)));
            box.setMaxLength(4096);
            box.setValue(lines.get(index));
            box.setFormatter((value, cursor) -> ScpFonts.roboto(value).getVisualOrderText());
            box.setResponder(value -> lines.set(index, value));
            if (index == focus) box.setFocused(true);
            visible.put(box, index);
            widgets.add(box);
        }
        return widgets;
    }

    Map<EditBox, Integer> visible() { return visible; }

    String text() { sync(); return String.join("\n", lines); }

    void sync() {
        visible.forEach((box, index) -> {
            if (index >= 0 && index < lines.size()) lines.set(index, box.getValue());
        });
    }

    boolean keyPressed(int keyCode) {
        for (Map.Entry<EditBox, Integer> entry : visible.entrySet()) {
            if (!entry.getKey().isFocused()) continue;
            int index = entry.getValue();
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                sync(); lines.add(index + 1, ""); focus = index + 1; reveal(); return true;
            }
            if (keyCode == GLFW.GLFW_KEY_UP && index > 0) {
                sync(); focus = index - 1; reveal(); return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN && index + 1 < lines.size()) {
                sync(); focus = index + 1; reveal(); return true;
            }
        }
        return false;
    }

    boolean scroll(double delta) {
        sync();
        int next = Math.max(0, Math.min(Math.max(0, lines.size() - visibleCount),
                scroll + (delta < 0 ? 1 : -1)));
        if (next == scroll) return false;
        scroll = next; focus = -1; return true;
    }

    void wrapFocused(String before, String after) {
        EditBox box = focusedBox();
        if (box == null) return;
        box.setValue(before + box.getValue() + after);
        lines.set(visible.get(box), box.getValue());
    }

    void divider() {
        EditBox box = focusedBox();
        int index = box == null ? lines.size() - 1 : visible.get(box);
        sync(); lines.add(Math.max(0, index + 1), "---"); focus = Math.max(0, index + 1); reveal();
    }

    private EditBox focusedBox() {
        for (EditBox box : visible.keySet()) if (box.isFocused()) return box;
        return null;
    }

    private void reveal() {
        if (focus < scroll) scroll = focus;
        if (focus >= scroll + visibleCount) scroll = focus - visibleCount + 1;
        clamp();
    }

    private void clamp() {
        scroll = Math.max(0, Math.min(Math.max(0, lines.size() - visibleCount), scroll));
    }
}
