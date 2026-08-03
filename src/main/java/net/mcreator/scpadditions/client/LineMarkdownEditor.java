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
    private static final int ROW_HEIGHT = 24;
    private static final int TEXT_PRIMARY = 0xFFE4E8EA;
    private static final int TEXT_MUTED = 0xFF879097;

    private final List<String> lines = new ArrayList<>();
    private final Map<EditBox, Integer> visible = new LinkedHashMap<>();

    private int scroll;
    private int focus = -1;
    private int desiredCursor = -1;
    private int visibleCount = 4;
    private EditBox focusedWidget;

    LineMarkdownEditor(String body) {
        setText(body);
    }

    void setText(String text) {
        lines.clear();
        String normalized = text == null ? ""
                : text.replace("\r\n", "\n").replace('\r', '\n');
        for (String line : normalized.split("\n", -1)) {
            lines.add(line);
        }
        if (lines.isEmpty()) lines.add("");
        scroll = 0;
        focus = -1;
        desiredCursor = -1;
        focusedWidget = null;
        visible.clear();
    }

    List<EditBox> build(Font font, int x, int y,
                        int width, int height) {
        visible.clear();
        focusedWidget = null;
        visibleCount = Math.max(3, height / ROW_HEIGHT);
        clamp();

        List<EditBox> widgets = new ArrayList<>();
        for (int row = 0; row < visibleCount; row++) {
            int index = scroll + row;
            if (index >= lines.size()) break;

            EditBox box = new EditBox(font,
                    x + 34, y + row * ROW_HEIGHT + 4,
                    Math.max(40, width - 40), 18,
                    Component.literal("Body paragraph " + (index + 1)));
            box.setBordered(false);
            box.setTextColor(TEXT_PRIMARY);
            box.setTextColorUneditable(TEXT_MUTED);
            box.setMaxLength(4096);
            box.setValue(lines.get(index));
            box.setFormatter((value, cursor) ->
                    ScpFonts.roboto(value).getVisualOrderText());
            box.setResponder(value -> lines.set(index, value));

            if (index == focus) {
                box.setFocused(true);
                int cursor = desiredCursor < 0
                        ? box.getValue().length()
                        : Math.min(desiredCursor, box.getValue().length());
                box.setCursorPosition(cursor);
                box.setHighlightPos(cursor);
                desiredCursor = -1;
                focusedWidget = box;
            }

            visible.put(box, index);
            widgets.add(box);
        }
        return widgets;
    }

    Map<EditBox, Integer> visible() {
        return visible;
    }

    EditBox focusedWidget() {
        return focusedWidget;
    }

    String text() {
        sync();
        return String.join("\n", lines);
    }

    void sync() {
        rememberFocus();
        visible.forEach((box, index) -> {
            if (index >= 0 && index < lines.size()) {
                lines.set(index, box.getValue());
            }
        });
    }

    void rememberFocus() {
        for (Map.Entry<EditBox, Integer> entry : visible.entrySet()) {
            if (entry.getKey().isFocused()) {
                focus = entry.getValue();
                focusedWidget = entry.getKey();
                return;
            }
        }
    }

    void detach() {
        sync();
        visible.clear();
        focusedWidget = null;
    }

    boolean keyPressed(int keyCode) {
        EditBox box = activeBox();
        if (box == null) return false;
        Integer indexValue = visible.get(box);
        if (indexValue == null) return false;
        int index = indexValue;

        if (keyCode == GLFW.GLFW_KEY_ENTER
                || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            sync();
            int cursor = Math.max(0,
                    Math.min(box.getCursorPosition(),
                            lines.get(index).length()));
            String current = lines.get(index);
            String before = current.substring(0, cursor);
            String after = current.substring(cursor);
            lines.set(index, before);
            lines.add(index + 1, after);
            focus = index + 1;
            desiredCursor = 0;
            reveal();
            visible.clear();
            focusedWidget = null;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE
                && box.getCursorPosition() == 0
                && box.getHighlightPos() == 0
                && index > 0) {
            sync();
            String previous = lines.get(index - 1);
            String current = lines.get(index);
            int joinCursor = previous.length();
            lines.set(index - 1, previous + current);
            lines.remove(index);
            focus = index - 1;
            desiredCursor = joinCursor;
            reveal();
            visible.clear();
            focusedWidget = null;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_DELETE
                && box.getCursorPosition() == box.getValue().length()
                && box.getHighlightPos() == box.getCursorPosition()
                && index + 1 < lines.size()) {
            sync();
            int joinCursor = lines.get(index).length();
            lines.set(index, lines.get(index) + lines.get(index + 1));
            lines.remove(index + 1);
            focus = index;
            desiredCursor = joinCursor;
            reveal();
            visible.clear();
            focusedWidget = null;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_UP && index > 0) {
            sync();
            focus = index - 1;
            desiredCursor = Math.min(box.getCursorPosition(),
                    lines.get(focus).length());
            reveal();
            visible.clear();
            focusedWidget = null;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_DOWN
                && index + 1 < lines.size()) {
            sync();
            focus = index + 1;
            desiredCursor = Math.min(box.getCursorPosition(),
                    lines.get(focus).length());
            reveal();
            visible.clear();
            focusedWidget = null;
            return true;
        }

        return false;
    }

    boolean scroll(double delta) {
        sync();
        int next = Math.max(0, Math.min(
                Math.max(0, lines.size() - visibleCount),
                scroll + (delta < 0 ? 1 : -1)));
        if (next == scroll) return false;
        scroll = next;
        focus = -1;
        desiredCursor = -1;
        visible.clear();
        focusedWidget = null;
        return true;
    }

    boolean wrapFocused(String before, String after) {
        EditBox box = activeBox();
        if (box == null) return false;
        Integer index = visible.get(box);
        if (index == null) return false;

        String value = box.getValue();
        box.setValue(before + value + after);
        lines.set(index, box.getValue());
        focus = index;
        return true;
    }

    boolean divider() {
        EditBox box = activeBox();
        if (box == null && lines.isEmpty()) return false;
        int index = box == null
                ? Math.max(0, lines.size() - 1)
                : visible.getOrDefault(box, Math.max(0, lines.size() - 1));

        sync();
        lines.add(Math.max(0, index + 1), "---");
        focus = Math.max(0, index + 1);
        desiredCursor = 3;
        reveal();
        visible.clear();
        focusedWidget = null;
        return true;
    }

    private EditBox activeBox() {
        for (EditBox box : visible.keySet()) {
            if (box.isFocused()) {
                focusedWidget = box;
                focus = visible.get(box);
                return box;
            }
        }
        if (focus >= 0) {
            for (Map.Entry<EditBox, Integer> entry : visible.entrySet()) {
                if (entry.getValue() == focus) {
                    focusedWidget = entry.getKey();
                    return entry.getKey();
                }
            }
        }
        return focusedWidget;
    }

    private void reveal() {
        if (focus < scroll) scroll = focus;
        if (focus >= scroll + visibleCount) {
            scroll = focus - visibleCount + 1;
        }
        clamp();
    }

    private void clamp() {
        scroll = Math.max(0, Math.min(
                Math.max(0, lines.size() - visibleCount), scroll));
    }
}
