from pathlib import Path


def read(path):
    return Path(path).read_text()


def write(path, text):
    Path(path).write_text(text)


def replace_exact(path, old, new, count=1):
    text = read(path)
    actual = text.count(old)
    if actual != count:
        raise SystemExit(f'{path}: expected {count} occurrence(s), found {actual}: {old[:160]!r}')
    write(path, text.replace(old, new, count))


mods = 'src/main/java/net/mcreator/scpadditions/client/PauseMenuModsPanelClient.java'
text = read(mods)
text = text.replace('import net.minecraft.client.gui.GuiGraphics;\n',
                    'import net.minecraft.client.gui.GuiGraphics;\nimport net.minecraft.client.gui.screens.Screen;\n', 1)
text = text.replace('import java.util.Map;\nimport java.util.WeakHashMap;\n',
                    'import java.util.Map;\nimport java.util.Set;\nimport java.util.WeakHashMap;\n', 1)
text = text.replace('/** Native Forge mod browser embedded in the custom pause presentation. */',
                    '/** Native Forge mod browser shared by the custom title and pause menus. */')
text = text.replace('private static final Map<CustomPauseMenuScreen, State> STATES =',
                    'private static final Map<Screen, State> STATES =')
text = text.replace('CustomPauseMenuScreen screen', 'Screen screen')
needle = '    private static final int DETAIL_SCROLL_STEP = 26;\n\n'
insert = '''    private static final int DETAIL_SCROLL_STEP = 26;\n    private static final int CONTROL_COUNT = 4;\n    private static final Set<String> INTERNAL_MOD_IDS = Set.of(\n            "minecraft", "forge", "fml", "fmlcore", "fmlloader",\n            "javafmllanguage", "lowcodelanguage", "mclanguage",\n            "mixinextras");\n\n'''
if needle not in text:
    raise SystemExit('mods constants anchor missing')
text = text.replace(needle, insert, 1)
old = '''        for (int index = 0; index < 3; index++) {\n            if (layout.sortContains(index, mouseX, mouseY)) {\n                SortMode mode = SortMode.values()[index];\n                if (state.sortMode != mode) {\n                    state.sortMode = mode;\n                    state.listScroll = 0;\n                    sortMods(state);\n                    playSelect();\n                }\n                return true;\n            }\n        }\n'''
new = '''        for (int index = 0; index < CONTROL_COUNT; index++) {\n            if (!layout.sortContains(index, mouseX, mouseY)) continue;\n            if (index == 3) {\n                state.hideInternal = !state.hideInternal;\n                state.listScroll = 0;\n                sortMods(state);\n                playSelect();\n                return true;\n            }\n            SortMode mode = SortMode.values()[index];\n            if (state.sortMode != mode) {\n                state.sortMode = mode;\n                state.listScroll = 0;\n                sortMods(state);\n                playSelect();\n            }\n            return true;\n        }\n'''
if text.count(old) != 1:
    raise SystemExit('mods click control block mismatch')
text = text.replace(old, new, 1)
old = '''                state.originalMods.add(new ModEntry(info,\n                        StringUtil.stripColor(info.getDisplayName()), version,\n                        loadLogo(info)));\n'''
new = '''                state.originalMods.add(new ModEntry(info,\n                        StringUtil.stripColor(info.getDisplayName()), version,\n                        loadLogo(info), isInternal(info), hasConfig(info)));\n'''
if text.count(old) != 1:
    raise SystemExit('mods entry constructor mismatch')
text = text.replace(old, new, 1)
old = '''    private static void sortMods(State state) {\n        state.mods.clear();\n        state.mods.addAll(state.originalMods);\n        Comparator<ModEntry> comparator = Comparator.comparing(entry ->\n                entry.name.toLowerCase(Locale.ROOT));\n        if (state.sortMode == SortMode.A_TO_Z) {\n            state.mods.sort(comparator);\n        } else if (state.sortMode == SortMode.Z_TO_A) {\n            state.mods.sort(comparator.reversed());\n        }\n    }\n'''
new = '''    private static void sortMods(State state) {\n        state.mods.clear();\n        for (ModEntry entry : state.originalMods) {\n            if (!state.hideInternal || !entry.internal) state.mods.add(entry);\n        }\n        Comparator<ModEntry> comparator = Comparator.comparing(entry ->\n                entry.name.toLowerCase(Locale.ROOT));\n        if (state.sortMode == SortMode.A_TO_Z) {\n            state.mods.sort(comparator);\n        } else if (state.sortMode == SortMode.Z_TO_A) {\n            state.mods.sort(comparator.reversed());\n        }\n        if (state.selectedId != null && state.mods.stream().noneMatch(entry ->\n                state.selectedId.equals(entry.info.getModId()))) {\n            state.selectedId = state.mods.isEmpty()\n                    ? null : state.mods.get(0).info.getModId();\n            state.detailScroll = 0;\n        }\n    }\n\n    private static boolean isInternal(IModInfo info) {\n        if (info == null) return false;\n        String id = info.getModId().toLowerCase(Locale.ROOT);\n        return INTERNAL_MOD_IDS.contains(id);\n    }\n\n    private static boolean hasConfig(IModInfo info) {\n        try {\n            return ConfigScreenHandler.getScreenFactoryFor(info).isPresent();\n        } catch (Throwable ignored) {\n            return false;\n        }\n    }\n'''
if text.count(old) != 1:
    raise SystemExit('mods sort block mismatch')
text = text.replace(old, new, 1)
old = '''        String[] labels = {"Off", "A-Z", "Z-A"};\n        for (int index = 0; index < labels.length; index++) {\n            int x = layout.sortX(index);\n            boolean hovered = layout.sortContains(index, mouseX, mouseY);\n            boolean selected = state.sortMode.ordinal() == index;\n'''
new = '''        String[] labels = {"Off", "A-Z", "Z-A", "Hide Internal"};\n        for (int index = 0; index < labels.length; index++) {\n            int x = layout.sortX(index);\n            boolean hovered = layout.sortContains(index, mouseX, mouseY);\n            boolean selected = index == 3\n                    ? state.hideInternal : state.sortMode.ordinal() == index;\n'''
if text.count(old) != 1:
    raise SystemExit('mods sort render block mismatch')
text = text.replace(old, new, 1)
old = '''            int textX = layout.listX + 45;\n            int textWidth = layout.listRight - textX - 8;\n            String name = compactToWidth(font, entry.name, textWidth);\n            graphics.drawString(font, ScpFonts.roboto(name),\n                    textX, y + 9,\n                    applyAlpha(selected ? ACCENT_BRIGHT : TEXT, alpha), false);\n            String version = compactToWidth(font, entry.version, textWidth);\n            graphics.drawString(font, ScpFonts.titillium(version),\n                    textX, y + 25, applyAlpha(MUTED, alpha), false);\n'''
new = '''            int textX = layout.listX + 45;\n            Component configMarker = entry.hasConfig\n                    ? ScpFonts.titillium("CFG") : null;\n            int markerWidth = configMarker == null\n                    ? 0 : font.width(configMarker) + 14;\n            int textWidth = layout.listRight - textX - 8 - markerWidth;\n            String name = compactToWidth(font, entry.name, textWidth);\n            graphics.drawString(font, ScpFonts.roboto(name),\n                    textX, y + 9,\n                    applyAlpha(selected ? ACCENT_BRIGHT : TEXT, alpha), false);\n            String version = compactToWidth(font, entry.version, textWidth);\n            graphics.drawString(font, ScpFonts.titillium(version),\n                    textX, y + 25, applyAlpha(MUTED, alpha), false);\n            if (configMarker != null) {\n                graphics.drawString(font, configMarker,\n                        layout.listRight - 9 - font.width(configMarker),\n                        y + 17, applyAlpha(ACCENT_BRIGHT, alpha), false);\n            }\n'''
if text.count(old) != 1:
    raise SystemExit('mods list text block mismatch')
text = text.replace(old, new, 1)
old = '''        graphics.drawString(font,\n                ScpFonts.titillium("Version " + selected.version),\n                headerTextX, layout.detailY + 30,\n                applyAlpha(ACCENT_BRIGHT, alpha), false);\n        graphics.drawString(font,\n                ScpFonts.titillium("Mod ID: " + selected.info.getModId()),\n                headerTextX, layout.detailY + 44,\n                applyAlpha(MUTED, alpha), false);\n'''
new = '''        drawScaledText(graphics, font,\n                ScpFonts.roboto("Version " + selected.version),\n                headerTextX, layout.detailY + 30, 1.18F,\n                applyAlpha(ACCENT_BRIGHT, alpha));\n        drawScaledText(graphics, font,\n                ScpFonts.roboto("Mod ID: " + selected.info.getModId()),\n                headerTextX, layout.detailY + 47, 1.14F,\n                applyAlpha(MUTED, alpha));\n'''
if text.count(old) != 1:
    raise SystemExit('mods detail header block mismatch')
text = text.replace(old, new, 1)
old = '''        state.hasConfig = ConfigScreenHandler\n                .getScreenFactoryFor(selected.info).isPresent();\n'''
new = '''        state.hasConfig = selected.hasConfig;\n'''
if text.count(old) != 1:
    raise SystemExit('mods selected config block mismatch')
text = text.replace(old, new, 1)
old = '''        int sortGap = 4;\n        int sortWidth = Math.max(38,\n                (listWidth - sortGap * 2) / 3);\n'''
new = '''        int sortGap = 4;\n        int sortWidth = Math.max(38,\n                (listWidth - sortGap * (CONTROL_COUNT - 1)) / CONTROL_COUNT);\n'''
if text.count(old) != 1:
    raise SystemExit('mods layout sort block mismatch')
text = text.replace(old, new, 1)
text = text.replace('        for (int index = 0; index < 3; index++) {\n            if (layout.sortContains(index, mouseX, mouseY))\n                return "sort:" + index;\n        }\n',
                    '        for (int index = 0; index < CONTROL_COUNT; index++) {\n            if (layout.sortContains(index, mouseX, mouseY))\n                return "sort:" + index;\n        }\n', 1)
helper_anchor = '''    private static String compactToWidth(Font font, String text,\n            int maxWidth) {\n'''
helper = '''    private static void drawScaledText(GuiGraphics graphics, Font font,\n            Component text, float x, float y, float scale, int color) {\n        graphics.pose().pushPose();\n        graphics.pose().translate(x, y, 0.0F);\n        graphics.pose().scale(scale, scale, 1.0F);\n        graphics.drawString(font, text, 0, 0, color, false);\n        graphics.pose().popPose();\n    }\n\n'''
if helper_anchor not in text:
    raise SystemExit('mods text helper anchor missing')
text = text.replace(helper_anchor, helper + helper_anchor, 1)
text = text.replace('''    private record ModEntry(IModInfo info, String name,\n            String version, LogoData logo) {\n    }\n''',
                    '''    private record ModEntry(IModInfo info, String name,\n            String version, LogoData logo, boolean internal,\n            boolean hasConfig) {\n    }\n''', 1)
text = text.replace('''        private SortMode sortMode = SortMode.OFF;\n        private int listScroll;\n''',
                    '''        private SortMode sortMode = SortMode.OFF;\n        private boolean hideInternal = true;\n        private int listScroll;\n''', 1)
write(mods, text)


main = 'src/main/java/net/mcreator/scpadditions/client/CustomMainMenuScreen.java'
text = read(main)
text = text.replace('''        transitionStartedAt = -1L;\n        pendingTransition = null;\n\n        refreshAvailableBackgrounds();\n''',
                    '''        transitionStartedAt = -1L;\n        pendingTransition = null;\n        PauseMenuModsPanelClient.close(this);\n\n        refreshAvailableBackgrounds();\n''', 1)
old = '''        for (MenuTextButton button : primaryButtons) {\n            button.render(graphics, mouseX, mouseY, partialTick);\n        }\n        renderExtraButtons(graphics, mouseX, mouseY, partialTick);\n\n        drawTransition(graphics, now);\n'''
new = '''        for (MenuTextButton button : primaryButtons) {\n            button.render(graphics, mouseX, mouseY, partialTick);\n        }\n        renderExtraButtons(graphics, mouseX, mouseY, partialTick);\n\n        int modsPanelX = Math.max(24, Math.round(this.width * 0.026F));\n        PauseMenuModsPanelClient.render(this, graphics,\n                mouseX, mouseY, partialTick, now, modsPanelX,\n                0, 0, 0, 0);\n\n        drawTransition(graphics, now);\n'''
if text.count(old) != 1:
    raise SystemExit('main render anchor mismatch')
text = text.replace(old, new, 1)
input_anchor = '''    private void captureVanillaSources() {\n'''
input_methods = '''    @Override\n    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {\n        if (PauseMenuModsPanelClient.keyPressed(this,\n                keyCode, scanCode, modifiers)) {\n            setMenuActive(true);\n            return true;\n        }\n        if (PauseMenuModsPanelClient.isOpen(this)) return true;\n        return super.keyPressed(keyCode, scanCode, modifiers);\n    }\n\n    @Override\n    public boolean mouseClicked(double mouseX, double mouseY, int button) {\n        if (PauseMenuModsPanelClient.isOpen(this)) {\n            PauseMenuModsPanelClient.mouseClicked(this,\n                    mouseX, mouseY, button);\n            return true;\n        }\n        return super.mouseClicked(mouseX, mouseY, button);\n    }\n\n    @Override\n    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {\n        if (PauseMenuModsPanelClient.isOpen(this)) {\n            PauseMenuModsPanelClient.mouseScrolled(this,\n                    mouseX, mouseY, delta);\n            return true;\n        }\n        return super.mouseScrolled(mouseX, mouseY, delta);\n    }\n\n'''
if input_anchor not in text:
    raise SystemExit('main input anchor missing')
text = text.replace(input_anchor, input_methods + input_anchor, 1)
old = '''    private Runnable sourceAction(String key) {\n        return () -> {\n            AbstractButton source = sourceButtons.get(key);\n            if (source != null && source.active) {\n                beginScreenTransition(source::onPress);\n            }\n        };\n    }\n'''
new = '''    private Runnable sourceAction(String key) {\n        if (MODS_KEY.equals(key)) {\n            return () -> {\n                AbstractButton source = sourceButtons.get(key);\n                if (source == null || !source.active\n                        || transitionStartedAt >= 0L) return;\n                extrasOpen = false;\n                PauseMenuModsPanelClient.toggle(this);\n                setMenuActive(false);\n            };\n        }\n        return () -> {\n            AbstractButton source = sourceButtons.get(key);\n            if (source != null && source.active) {\n                beginScreenTransition(source::onPress);\n            }\n        };\n    }\n'''
if text.count(old) != 1:
    raise SystemExit('main sourceAction mismatch')
text = text.replace(old, new, 1)
old = '''        boolean hovered = false;\n        for (MenuTextButton button : primaryButtons) {\n            if (button.source != null && transitionStartedAt < 0L) {\n                button.active = button.source.active;\n            }\n            hovered |= button.active && button.isMouseOver(mouseX, mouseY);\n        }\n        for (MenuTextButton button : extraButtons) {\n            if (extrasProgress > 0.05F) {\n                hovered |= button.isMouseOver(mouseX, mouseY);\n            }\n        }\n'''
new = '''        boolean modsPanelOpen = PauseMenuModsPanelClient.isOpen(this);\n        boolean hovered = false;\n        for (MenuTextButton button : primaryButtons) {\n            if (button.source != null && transitionStartedAt < 0L\n                    && !modsPanelOpen) {\n                button.active = button.source.active;\n            }\n            if (!modsPanelOpen) {\n                hovered |= button.active && button.isMouseOver(mouseX, mouseY);\n            }\n        }\n        for (MenuTextButton button : extraButtons) {\n            if (!modsPanelOpen && extrasProgress > 0.05F) {\n                hovered |= button.isMouseOver(mouseX, mouseY);\n            }\n        }\n'''
if text.count(old) != 1:
    raise SystemExit('main animation hover block mismatch')
text = text.replace(old, new, 1)
write(main, text)


chair = 'src/main/java/net/mcreator/scpadditions/facility/ArchivistsChairBlock.java'
text = read(chair)
start = text.index('    // GeckoLib mirrors Bedrock\'s model X axis')
end = text.index('    private static final VoxelShape EAST', start)
simple_shape = '''    // Deliberately simple collision. The Blockbench chair is authored at an\n    // angle, but turning that diagonal into dozens of axis-aligned slices only\n    // makes the collision noisy and less useful. Four broad volumes follow the\n    // base, pedestal, seat and backrest closely enough for normal play.\n    private static final VoxelShape NORTH = Shapes.or(\n            modelBox(0.20D, 0.00D, -6.50D, 12.65D, 3.65D, 6.50D),\n            modelBox(4.80D, 3.45D, -1.20D, 7.20D, 10.25D, 1.20D),\n            modelBox(-0.60D, 9.95D, -6.65D, 12.65D, 12.30D, 6.65D),\n            modelBox(-2.10D, 10.20D, -8.10D, 7.10D, 22.80D, 1.10D))\n            .optimize();\n'''
text = text[:start] + simple_shape + text[end:]
start = text.index('    private static VoxelShape shapeFor(Direction facing)')
end = text.index('    private static VoxelShape rotateY', start)
simple_helpers = '''    private static VoxelShape shapeFor(Direction facing) {\n        return switch (facing) {\n            case EAST -> EAST;\n            case SOUTH -> SOUTH;\n            case WEST -> WEST;\n            default -> NORTH;\n        };\n    }\n\n    private static VoxelShape modelBox(double minX, double minY, double minZ,\n            double maxX, double maxY, double maxZ) {\n        // GeckoLib mirrors Bedrock X around the block centre while preserving Z.\n        return box(8.0D - maxX, minY, 8.0D + minZ,\n                8.0D - minX, maxY, 8.0D + maxZ);\n    }\n\n'''
text = text[:start] + simple_helpers + text[end:]
write(chair, text)


item_type = 'src/main/java/com/bl4ues/scpinventory/item/ScpItemType.java'
text = read(item_type)
text = text.replace('    ACCESSORY_HAND("Accessory (Offhand)"),\n',
                    '    ACCESSORY_HAND("Accessory"),\n', 1)
old = '''    public String getDisplayName() {\n        return displayName;\n    }\n'''
new = '''    public String getDisplayName() {\n        return displayName;\n    }\n\n    public String getEditorDisplayName() {\n        return this == ACCESSORY_HAND\n                ? "Accessory (Offhand)" : displayName;\n    }\n'''
if text.count(old) != 1:
    raise SystemExit('item type display method mismatch')
text = text.replace(old, new, 1)
write(item_type, text)


editor = 'src/main/java/com/bl4ues/scpinventory/client/gui/ItemRuleEditorScreen.java'
replace_exact(editor,
              '                value -> ScpFonts.roboto(value.getDisplayName()),\n',
              '                value -> ScpFonts.roboto(value.getEditorDisplayName()),\n')


changelog = 'CHANGELOG.md'
replace_exact(changelog,
    '- Replaced Forge\'s separate Mods screen while using the custom pause menu with an animated native mod browser featuring fixed Off/A-Z/Z-A sorting controls, scrollable icon-backed mod entries, styled metadata and descriptions, direct in-game config access when supported, and an anchored Open mods folder action;\n',
    '- Replaced Forge\'s separate Mods screen in both custom title and pause menus with an animated native mod browser featuring fixed Off/A-Z/Z-A sorting controls, optional internal-component filtering, config-capability indicators, scrollable icon-backed mod entries, styled metadata and descriptions, direct in-game config access when supported, and an anchored Open mods folder action;\n')
