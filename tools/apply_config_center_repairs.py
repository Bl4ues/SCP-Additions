from pathlib import Path
import re


def load(path):
    return Path(path).read_text(encoding="utf-8")


def save(path, text):
    Path(path).write_text(text, encoding="utf-8")


def replace_exact(text, old, new, label, expected=None):
    count = text.count(old)
    if count == 0:
        raise RuntimeError(f"{label}: pattern not found")
    if expected is not None and count != expected:
        raise RuntimeError(f"{label}: expected {expected}, found {count}")
    return text.replace(old, new)


# ---------------------------------------------------------------------------
# Shared Configuration Center renderer
# ---------------------------------------------------------------------------
path = "src/main/java/net/mcreator/scpadditions/client/UnityConfigurationUiEvents.java"
s = load(path)
s = replace_exact(
    s,
    "return new PanelSpec(Math.max(8, (screen.width - w) / 2), y, w, h);",
    "return new PanelSpec(ConfigCenterVisuals.contentLeft(screen.width, w), y, w, h);",
    "panelSpec modern X",
    1,
)
old_x = "int x = (screen.width - w) / 2 + 12;"
count = s.count(old_x)
if count < 5:
    raise RuntimeError(f"legacy row X: expected at least 5, found {count}")
s = s.replace(old_x, "int x = ConfigCenterVisuals.contentLeft(screen.width, w) + 12;")
s = replace_exact(
    s,
    "    private static void styleEditBox(EditBox editBox) {\n        editBox.setFormatter",
    "    private static void styleEditBox(EditBox editBox) {\n        editBox.setBordered(true);\n        editBox.setFormatter",
    "EditBox vertical alignment",
    1,
)
old_footer = '''    private static void drawFooter(GuiGraphics graphics, Font font, PanelSpec spec,
                                   String text, int color) {
        coverTextLine(graphics, spec.x() + 8, spec.y() + spec.height() - 23,
                spec.width() - 16);
        graphics.drawString(font, ScpFonts.roboto(text), spec.x() + 12,
                spec.y() + spec.height() - 17, color, false);
    }
'''
new_footer = '''    private static void drawFooter(GuiGraphics graphics, Font font, PanelSpec spec,
                                   String text, int color) {
        graphics.drawString(font, ScpFonts.roboto(text), spec.x() + 12,
                spec.y() + spec.height() - 17, color, false);
    }
'''
s = replace_exact(s, old_footer, new_footer, "footer strip removal", 1)
save(path, s)


# ---------------------------------------------------------------------------
# Widget layer: suppress legacy custom renderers and align summary rows
# ---------------------------------------------------------------------------
path = "src/main/java/net/mcreator/scpadditions/config/ui/ConfigCenterModernWidgetEvents.java"
s = load(path)
s = replace_exact(
    s,
    "import java.lang.reflect.Field;\n",
    "import java.lang.reflect.Field;\nimport java.util.Map;\nimport java.util.WeakHashMap;\n",
    "modern widget imports",
    1,
)
s = replace_exact(
    s,
    "    private static final int ACCENT_BRIGHT = 0xFFE3C865;\n",
    "    private static final int ACCENT_BRIGHT = 0xFFE3C865;\n    private static final Map<AbstractButton, Integer> SUPPRESSED_X = new WeakHashMap<>();\n",
    "suppressed widget map",
    1,
)
old_pre = '''        for (GuiEventListener listener : screen.children()) {
            prepare(listener);
        }
'''
new_pre = '''        for (GuiEventListener listener : screen.children()) {
            if (listener instanceof AbstractButton button
                    && shouldSuppressNative(button)) {
                suppressNative(button);
            }
            prepare(listener);
        }
'''
s = replace_exact(s, old_pre, new_pre, "render-pre suppression", 1)
s = replace_exact(
    s,
    "        GuiGraphics graphics = event.getGuiGraphics();\n        Font font = Minecraft.getInstance().font;\n",
    "        restoreSuppressed(screen);\n\n        GuiGraphics graphics = event.getGuiGraphics();\n        Font font = Minecraft.getInstance().font;\n",
    "restore suppressed widgets",
    1,
)
s = replace_exact(
    s,
    '''        if (listener instanceof EditBox editBox) {
            editBox.setBordered(false);
            return;
        }
''',
    '''        if (listener instanceof EditBox editBox) {
            editBox.setBordered(true);
            editBox.setFormatter((value, cursor) ->
                    ScpFonts.roboto(value).getVisualOrderText());
            return;
        }
''',
    "EditBox bordered mode",
    1,
)
old_left = "left = (screen.width - w) / 2 + 12;"
count = s.count(old_left)
if count < 5:
    raise RuntimeError(f"modern summary row X: expected at least 5, found {count}")
s = s.replace(old_left, "left = ConfigCenterVisuals.contentLeft(screen.width, w) + 12;")
helper_anchor = "    private static Object invokeNoArgs(Object target, String name) {"
helper = '''    private static boolean shouldSuppressNative(AbstractButton button) {
        return button instanceof AbstractSliderButton
                || isSelfRenderedButton(button)
                || !(button instanceof Button);
    }

    private static void suppressNative(AbstractButton button) {
        if (button.getX() <= -9000) return;
        SUPPRESSED_X.put(button, button.getX());
        button.setX(-10000);
    }

    private static void restoreSuppressed(Screen screen) {
        for (GuiEventListener listener : screen.children()) {
            if (!(listener instanceof AbstractButton button)) continue;
            Integer x = SUPPRESSED_X.get(button);
            if (x != null && button.getX() <= -9000) button.setX(x);
        }
    }

'''
if helper_anchor not in s:
    raise RuntimeError("suppression helper anchor not found")
s = s.replace(helper_anchor, helper + helper_anchor, 1)
s = replace_exact(
    s,
    '''        if (name.startsWith("net.mcreator.scpadditions.config.ui.ConfigCenterClient$")
                || name.startsWith(
                "net.mcreator.scpadditions.config.ui.Scp079ModulesScreenExtension$")) {
''',
    '''        if (name.startsWith("net.mcreator.scpadditions.config.ui.ConfigCenterClient$")
                || name.startsWith(
                "net.mcreator.scpadditions.config.ui.Scp079ModulesScreenExtension$")
                || name.startsWith(
                "net.mcreator.scpadditions.client.RoombaConfigCenterEnhancements$")) {
''',
    "Roomba modern widget inclusion",
    1,
)
save(path, s)


# ---------------------------------------------------------------------------
# Shared button state formatting
# ---------------------------------------------------------------------------
path = "src/main/java/net/mcreator/scpadditions/config/ui/ConfigCenterVisuals.java"
s = load(path)
old_prefix = '''            String prefix = plain.substring(0, plain.length() - stateLength);
            String state = plain.substring(plain.length() - stateLength);
'''
new_prefix = '''            String prefix = plain.substring(0, plain.length() - stateLength).trim();
            if (prefix.endsWith(":")) prefix = prefix.substring(0, prefix.length() - 1).trim();
            String state = plain.substring(plain.length() - stateLength);
'''
s = replace_exact(s, old_prefix, new_prefix, "state colon cleanup", 1)
save(path, s)


# ---------------------------------------------------------------------------
# Crosshair belongs to the Configuration Center home, not General & Modules
# ---------------------------------------------------------------------------
path = "src/main/java/net/mcreator/scpadditions/config/ui/CrosshairModulesPlacement.java"
s = load(path)
s = replace_exact(
    s,
    '''        if (isGeneralModulesScreen(screen)) {
            injectCrosshairRow(screen);
            injectActionBarRow(screen);
        } else if (isCrosshairScreen(screen)) {
''',
    '''        if (isGeneralModulesScreen(screen)) {
            injectActionBarRow(screen);
        } else if (isCrosshairScreen(screen)) {
''',
    "remove Crosshair module row injection",
    1,
)
old_post = '''        if (isHomeScreen(screen)) {
            compactHomeLayout(screen);
        } else if (isGeneralModulesScreen(screen)) {
            wireCrosshairNavigation(screen);
        } else if (isCrosshairScreen(screen)) {
            wireEnabledDefaultsButton(screen);
        }
'''
new_post = '''        if (isCrosshairScreen(screen)) {
            wireEnabledDefaultsButton(screen);
        }
'''
s = replace_exact(s, old_post, new_post, "Crosshair init-post ownership", 1)
s = replace_exact(s, old_post, new_post, "Crosshair render-post ownership", 1)
save(path, s)


path = "src/main/java/net/mcreator/scpadditions/config/ui/ConfigurationHomePolish.java"
s = load(path)
hide_block = '''            Button crosshair = find(screen, "Crosshair");
            if (crosshair != null) {
                crosshair.visible = false;
                crosshair.active = false;
                crosshair.setX(-10000);
            }

'''
s = replace_exact(s, hide_block, "", "unhide Crosshair home button", 1)
old_tools = '''            int toolY = l.startY + 178;
            Button accessibility = find(screen, "Accessibility");
            Button debug = find(screen, "Debug Tools");
            Button reload = find(screen, "Reload Snapshot");
            Button done = find(screen, "Done");
            if (accessibility != null) {
                place(accessibility, l.infoX, toolY, l.infoWidth, l.rowHeight);
                toolY += l.rowHeight + l.rowGap;
            }
            if (debug != null) {
                place(debug, l.infoX, toolY, l.infoWidth, l.rowHeight);
                toolY += l.rowHeight + l.rowGap + 7;
            }
'''
new_tools = '''            int toolY = l.startY + 178;
            Button crosshair = find(screen, "Crosshair");
            Button accessibility = find(screen, "Accessibility");
            Button debug = find(screen, "Debug Tools");
            Button reload = find(screen, "Reload Snapshot");
            Button done = find(screen, "Done");
            if (crosshair != null) {
                place(crosshair, l.infoX, toolY, l.infoWidth, l.rowHeight);
                toolY += l.rowHeight + l.rowGap;
            }
            if (accessibility != null) {
                place(accessibility, l.infoX, toolY, l.infoWidth, l.rowHeight);
                toolY += l.rowHeight + l.rowGap;
            }
            if (debug != null) {
                place(debug, l.infoX, toolY, l.infoWidth, l.rowHeight);
                toolY += l.rowHeight + l.rowGap + 7;
            }
'''
s = replace_exact(s, old_tools, new_tools, "Crosshair home placement", 1)
info_anchor = '''                case "Accessibility" -> new Info("Accessibility",
                        "Client-side presentation options intended to improve comfort and readability.");
'''
info_new = '''                case "Crosshair" -> new Info("Crosshair",
                        "Configure the custom crosshair, visibility, colour channels and opacity.");
                case "Accessibility" -> new Info("Accessibility",
                        "Client-side presentation options intended to improve comfort and readability.");
'''
s = replace_exact(s, info_anchor, info_new, "Crosshair home info", 1)
save(path, s)


# ---------------------------------------------------------------------------
# Preserve ON/OFF at the right edge by moving CLIENT/SERVER scope badge left
# ---------------------------------------------------------------------------
path = "src/main/java/net/mcreator/scpadditions/config/ui/ClientPreferenceModulesUi.java"
s = load(path)
s = replace_exact(
    s,
    '        setActive(screen, "Accessibility", true);\n',
    '        setActive(screen, "Accessibility", true);\n        setActive(screen, "Crosshair", true);\n',
    "Crosshair client permission",
    1,
)
s = replace_exact(
    s,
    '                String text = personal ? "Client-side" : "Server-side";\n',
    '                String text = personal ? "CLIENT" : "SERVER";\n',
    "compact scope badge",
    1,
)
s = replace_exact(
    s,
    '                int right = button.getX() + button.getWidth() - 7;\n',
    '                int right = button.getX() + button.getWidth() - 58;\n',
    "reserve ON/OFF state space",
    1,
)
save(path, s)


# ---------------------------------------------------------------------------
# Remove obsolete Crosshair paint-over hacks
# ---------------------------------------------------------------------------
path = "src/main/java/net/mcreator/scpadditions/config/ui/AdditionalGameplayModulesUi.java"
s = load(path)
pattern = re.compile(
    r"\n    @SubscribeEvent\(priority = EventPriority\.LOWEST\)\n"
    r"    public static void onScreenRenderPost\(ScreenEvent\.Render\.Post event\) \{.*?\n"
    r"    \}\n\n    private static boolean isGeneralModulesScreen",
    re.S,
)
s, n = pattern.subn("\n\n    private static boolean isGeneralModulesScreen", s, count=1)
if n != 1:
    raise RuntimeError(f"AdditionalGameplay Crosshair overlay removal: {n}")
save(path, s)
cleanup = Path("src/main/java/net/mcreator/scpadditions/config/ui/CrosshairPreviewTextCleanup.java")
if cleanup.exists():
    cleanup.unlink()


# ---------------------------------------------------------------------------
# Roomba editor uses the same backdrop/content column and flat rows
# ---------------------------------------------------------------------------
path = "src/main/java/net/mcreator/scpadditions/client/RoombaConfigCenterEnhancements.java"
s = load(path)
s = replace_exact(
    s,
    "import net.mcreator.scpadditions.config.RoombaSpawnConfig;\n",
    "import net.mcreator.scpadditions.config.RoombaSpawnConfig;\nimport net.mcreator.scpadditions.config.ui.ConfigCenterVisuals;\n",
    "Roomba modern visuals import",
    1,
)
s = replace_exact(
    s,
    '''        protected int panelX() {
            return Math.max(14, (width - panelWidth()) / 2);
        }
''',
    '''        protected int panelX() {
            return ConfigCenterVisuals.contentLeft(width, panelWidth());
        }
''',
    "Roomba content column",
    1,
)
old_panel = '''        protected void drawPanel(GuiGraphics graphics, String title, String subtitle) {
            int x = panelX();
            int y = panelY();
            graphics.fill(x, y, x + panelWidth(), y + panelHeight(), PANEL);
            graphics.fill(x, y, x + panelWidth(), y + 34, HEADER);
            graphics.drawString(font, ScpFonts.montserrat(title),
                    x + 14, y + 10, TEXT, false);
            graphics.drawString(font, ScpFonts.roboto(subtitle),
                    x + 16, y + 39, MUTED, false);
        }
'''
new_panel = '''        protected void drawPanel(GuiGraphics graphics, String title, String subtitle) {
            int x = panelX();
            int y = panelY();
            ConfigCenterVisuals.drawPanel(graphics, font, x, y,
                    panelWidth(), panelHeight(), title);
            int slide = ConfigCenterVisuals.contentOffsetX();
            graphics.drawString(font, ScpFonts.roboto(subtitle),
                    x + slide + 16, y + 39,
                    ConfigCenterVisuals.fadeColor(MUTED), false);
        }
'''
s = replace_exact(s, old_panel, new_panel, "Roomba modern panel", 1)
count = s.count("            renderBackground(graphics);")
if count != 2:
    raise RuntimeError(f"Roomba backdrop calls: expected 2, found {count}")
s = s.replace(
    "            renderBackground(graphics);",
    "            ConfigCenterVisuals.renderBackdrop(this, graphics, mouseX, mouseY);",
)
s = replace_exact(
    s,
    '''                graphics.fill(x, rowY, x + listWidth, rowY + 36, rowColor);
                graphics.fill(x, rowY, x + listWidth, rowY + 1, rowBorder);
                graphics.fill(x, rowY + 35, x + listWidth, rowY + 36, rowBorder);
                graphics.fill(x, rowY, x + 4, rowY + 36,
''',
    '''                graphics.fill(x, rowY, x + listWidth, rowY + 36,
                        entry.enabled() ? 0xD20B0E12 : 0xB811151D);
                graphics.fill(x, rowY, x + 4, rowY + 36,
''',
    "Roomba spawning flat rows",
    1,
)
s = replace_exact(
    s,
    '''                graphics.fill(x, rowY, x + listWidth, rowY + 36,
                        row % 2 == 0 ? ROW : ROW_ALT);
                graphics.fill(x, rowY, x + listWidth, rowY + 1, BORDER);
                graphics.fill(x, rowY + 35, x + listWidth, rowY + 36, BORDER);
                graphics.fill(x, rowY, x + 4, rowY + 36, ACCENT);
''',
    '''                graphics.fill(x, rowY, x + listWidth, rowY + 36,
                        0xD20B0E12);
                graphics.fill(x, rowY, x + 4, rowY + 36, ACCENT);
''',
    "Roomba picker flat rows",
    1,
)
save(path, s)


# ---------------------------------------------------------------------------
# Chair collision: mirror X only
# ---------------------------------------------------------------------------
path = "src/main/java/net/mcreator/scpadditions/facility/ArchivistsChairBlock.java"
s = load(path)
s = replace_exact(
    s,
    '''        return box(14.0D - maxX, minY, 8.0D + minZ,
                14.0D - minX, maxY, 8.0D + maxZ);
''',
    '''        return box(2.0D + minX, minY, 8.0D + minZ,
                2.0D + maxX, maxY, 8.0D + maxZ);
''',
    "Archivist chair X mirror",
    1,
)
save(path, s)


# ---------------------------------------------------------------------------
# Difficulty: render full 128x128 asset when present; never leave a blank slot
# ---------------------------------------------------------------------------
path = "src/main/java/net/mcreator/scpadditions/client/PauseMenuSettingsPanelClient.java"
s = load(path)
s = replace_exact(
    s,
    '''            if (Minecraft.getInstance().getResourceManager()
                    .getResource(current.icon).isPresent()) {
                drawDifficultyIcon(graphics, current.icon, iconX, iconY, iconSize, alpha);
            }
''',
    '''            drawDifficultyIconOrFallback(graphics, current.icon,
                    current.title, iconX, iconY, iconSize, alpha);
''',
    "difficulty current icon fallback",
    1,
)
s = replace_exact(
    s,
    '''            if (Minecraft.getInstance().getResourceManager()
                    .getResource(choice.icon).isPresent()) {
                drawDifficultyIcon(graphics, choice.icon, iconX, iconY, iconSize, alpha);
            }
''',
    '''            drawDifficultyIconOrFallback(graphics, choice.icon,
                    choice.title, iconX, iconY, iconSize, alpha);
''',
    "difficulty flyout icon fallback",
    1,
)
icon_anchor = '''    private static void drawDifficultyIcon(GuiGraphics graphics,
            ResourceLocation texture, int x, int y, int size, float alpha) {
'''
icon_helper = '''    private static void drawDifficultyIconOrFallback(GuiGraphics graphics,
            ResourceLocation texture, String title, int x, int y, int size,
            float alpha) {
        if (Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()) {
            drawDifficultyIcon(graphics, texture, x, y, size, alpha);
            return;
        }
        int border = applyAlpha(ACCENT, alpha);
        int surface = applyAlpha(0x780B0E12, alpha);
        graphics.fill(x, y, x + size, y + size, surface);
        graphics.fill(x, y, x + size, y + 1, border);
        graphics.fill(x, y + size - 1, x + size, y + size, border);
        graphics.fill(x, y, x + 1, y + size, border);
        graphics.fill(x + size - 1, y, x + size, y + size, border);
        String initial = title == null || title.isBlank() ? "?"
                : title.substring(0, 1).toUpperCase(Locale.ROOT);
        Component glyph = ScpFonts.montserrat(initial);
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, glyph,
                x + (size - font.width(glyph)) / 2,
                y + (size - font.lineHeight) / 2,
                applyAlpha(ACCENT_BRIGHT, alpha), false);
    }

'''
if icon_anchor not in s:
    raise RuntimeError("difficulty icon anchor missing")
s = s.replace(icon_anchor, icon_helper + icon_anchor, 1)
save(path, s)

print("Configuration Center repair script completed")
