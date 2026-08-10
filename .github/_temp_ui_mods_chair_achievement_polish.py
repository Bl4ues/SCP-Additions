from pathlib import Path


def replace(path: str, old: str, new: str, count: int = 1) -> None:
    p = Path(path)
    text = p.read_text()
    actual = text.count(old)
    if actual != count:
        raise SystemExit(
            f"{path}: expected {count} occurrence(s), found {actual}: {old[:140]!r}"
        )
    p.write_text(text.replace(old, new, count))


mods = "src/main/java/net/mcreator/scpadditions/client/PauseMenuModsPanelClient.java"
replace(
    mods,
    '''            int textX = layout.listX + 45;\n            Component configMarker = entry.hasConfig\n                    ? ScpFonts.titillium("CFG") : null;\n            int markerWidth = configMarker == null\n                    ? 0 : font.width(configMarker) + 14;\n            int textWidth = layout.listRight - textX - 8 - markerWidth;\n            String name = compactToWidth(font, entry.name, textWidth);\n            graphics.drawString(font, ScpFonts.roboto(name),\n                    textX, y + 9,\n                    applyAlpha(selected ? ACCENT_BRIGHT : TEXT, alpha), false);\n            String version = compactToWidth(font, entry.version, textWidth);\n            graphics.drawString(font, ScpFonts.titillium(version),\n                    textX, y + 25, applyAlpha(MUTED, alpha), false);\n            if (configMarker != null) {\n                graphics.drawString(font, configMarker,\n                        layout.listRight - 9 - font.width(configMarker),\n                        y + 17, applyAlpha(ACCENT_BRIGHT, alpha), false);\n            }\n''',
    '''            int textX = layout.listX + 45;\n            int textWidth = layout.listRight - textX - 18;\n            String name = compactToWidth(font, entry.name, textWidth);\n            graphics.drawString(font, ScpFonts.roboto(name),\n                    textX, y + 9,\n                    applyAlpha(selected ? ACCENT_BRIGHT : TEXT, alpha), false);\n            String version = compactToWidth(font, entry.version, textWidth);\n            graphics.drawString(font, ScpFonts.titillium(version),\n                    textX, y + 25, applyAlpha(MUTED, alpha), false);\n            if (entry.hasConfig) {\n                int markerX = layout.listRight - 7;\n                int markerY = y + (MOD_ROW_HEIGHT - 12) / 2;\n                graphics.fill(markerX, markerY, markerX + 2, markerY + 12,\n                        applyAlpha(ACCENT_BRIGHT, alpha * 0.78F));\n            }\n''',
)
replace(
    mods,
    '''    private static Layout layout(Screen screen, int baseX) {\n        int availableWidth = Math.max(330, screen.width - baseX - 28);\n        int panelWidth = Math.min(820, availableWidth);\n        int panelHeight = Mth.clamp(screen.height - 64, 300, 620);\n        int panelY = Math.max(24, (screen.height - panelHeight) / 2);\n        int panelX = baseX;\n''',
    '''    private static Layout layout(Screen screen, int baseX) {\n        boolean titleMenu = screen instanceof CustomMainMenuScreen;\n        int availableWidth = Math.max(330, screen.width - baseX - 28);\n        int panelWidth = titleMenu\n                ? Math.min(availableWidth, Mth.clamp(\n                        Math.round(screen.width * 0.46F), 520, 720))\n                : Math.min(820, availableWidth);\n        int panelHeight = titleMenu\n                ? Mth.clamp(Math.round(screen.height * 0.62F), 340, 540)\n                : Mth.clamp(screen.height - 64, 300, 620);\n        int panelY = titleMenu\n                ? Mth.clamp(Math.round(screen.height * 0.18F), 48,\n                        Math.max(48, screen.height - panelHeight - 24))\n                : Math.max(24, (screen.height - panelHeight) / 2);\n        int panelX = baseX;\n''',
)

menu = "src/main/java/net/mcreator/scpadditions/client/CustomMainMenuScreen.java"
replace(
    menu,
    '''        int modsPanelX = Math.max(24, Math.round(this.width * 0.026F));\n        PauseMenuModsPanelClient.render(this, graphics,\n''',
    '''        int primaryLeft = Math.max(42, Math.round(this.width * 0.073F));\n        int primaryWidth = Mth.clamp(Math.round(this.width * 0.265F),\n                220, 330);\n        int modsPanelX = primaryLeft + primaryWidth + 16;\n        PauseMenuModsPanelClient.render(this, graphics,\n''',
)
replace(
    menu,
    '''    public boolean mouseClicked(double mouseX, double mouseY, int button) {\n        if (PauseMenuModsPanelClient.isOpen(this)) {\n            PauseMenuModsPanelClient.mouseClicked(this,\n                    mouseX, mouseY, button);\n            return true;\n        }\n        return super.mouseClicked(mouseX, mouseY, button);\n    }\n\n    @Override\n    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {\n        if (PauseMenuModsPanelClient.isOpen(this)) {\n            PauseMenuModsPanelClient.mouseScrolled(this,\n                    mouseX, mouseY, delta);\n            return true;\n        }\n        return super.mouseScrolled(mouseX, mouseY, delta);\n    }\n''',
    '''    public boolean mouseClicked(double mouseX, double mouseY, int button) {\n        if (PauseMenuModsPanelClient.isOpen(this)\n                && PauseMenuModsPanelClient.mouseClicked(this,\n                        mouseX, mouseY, button)) {\n            return true;\n        }\n        return super.mouseClicked(mouseX, mouseY, button);\n    }\n\n    @Override\n    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {\n        if (PauseMenuModsPanelClient.isOpen(this)\n                && PauseMenuModsPanelClient.mouseScrolled(this,\n                        mouseX, mouseY, delta)) {\n            return true;\n        }\n        return super.mouseScrolled(mouseX, mouseY, delta);\n    }\n''',
)
replace(
    menu,
    '''                extrasOpen = false;\n                PauseMenuModsPanelClient.toggle(this);\n                setMenuActive(false);\n''',
    '''                extrasOpen = false;\n                MainMenuPlayPanelsClient.close(this);\n                MainMenuSettingsPanelClient.close(this);\n                PauseMenuModsPanelClient.toggle(this);\n''',
)
replace(
    menu,
    '''            AbstractButton source = sourceButtons.get(key);\n            if (source != null && source.active) {\n                beginScreenTransition(source::onPress);\n            }\n''',
    '''            AbstractButton source = sourceButtons.get(key);\n            if (source != null && source.active) {\n                PauseMenuModsPanelClient.close(this);\n                beginScreenTransition(source::onPress);\n            }\n''',
)
replace(
    menu,
    '''    private void toggleExtras() {\n        if (transitionStartedAt >= 0L) return;\n        extrasOpen = !extrasOpen;\n    }\n''',
    '''    private void toggleExtras() {\n        if (transitionStartedAt >= 0L) return;\n        PauseMenuModsPanelClient.close(this);\n        extrasOpen = !extrasOpen;\n    }\n''',
)
replace(
    menu,
    '''        boolean modsPanelOpen = PauseMenuModsPanelClient.isOpen(this);\n        boolean hovered = false;\n        for (MenuTextButton button : primaryButtons) {\n            if (button.source != null && transitionStartedAt < 0L\n                    && !modsPanelOpen) {\n                button.active = button.source.active;\n            }\n            if (!modsPanelOpen) {\n                hovered |= button.active && button.isMouseOver(mouseX, mouseY);\n            }\n        }\n        for (MenuTextButton button : extraButtons) {\n            if (!modsPanelOpen && extrasProgress > 0.05F) {\n                hovered |= button.isMouseOver(mouseX, mouseY);\n            }\n        }\n''',
    '''        boolean hovered = false;\n        for (MenuTextButton button : primaryButtons) {\n            if (button.source != null && transitionStartedAt < 0L) {\n                button.active = button.source.active;\n            }\n            hovered |= button.active && button.isMouseOver(mouseX, mouseY);\n        }\n        for (MenuTextButton button : extraButtons) {\n            if (extrasProgress > 0.05F) {\n                hovered |= button.isMouseOver(mouseX, mouseY);\n            }\n        }\n''',
)

settings = "src/main/java/net/mcreator/scpadditions/client/MainMenuSettingsPanelClient.java"
p = Path(settings)
text = p.read_text()
marker = '''    public static void render(CustomMainMenuScreen screen,\n            GuiGraphics graphics, int mouseX, int mouseY) {\n'''
insertion = '''    public static void close(CustomMainMenuScreen screen) {\n        State state = STATES.get(screen);\n        if (state != null) {\n            state.open = false;\n            state.draggingFov = false;\n        }\n    }\n\n'''
if insertion not in text:
    if text.count(marker) != 1:
        raise SystemExit("settings render marker mismatch")
    p.write_text(text.replace(marker, insertion + marker, 1))
replace(
    settings,
    '''        if (settingsButton != null\n                && settingsButton.isMouseOver(event.getMouseX(), event.getMouseY())) {\n            state.open = !state.open;\n            if (state.open) {\n                ensureEntries(screen, state);\n                closeExtras(screen);\n            }\n''',
    '''        if (settingsButton != null\n                && settingsButton.isMouseOver(event.getMouseX(), event.getMouseY())) {\n            PauseMenuModsPanelClient.close(screen);\n            MainMenuPlayPanelsClient.close(screen);\n            state.open = !state.open;\n            if (state.open) {\n                ensureEntries(screen, state);\n                closeExtras(screen);\n            }\n''',
)

play = "src/main/java/net/mcreator/scpadditions/client/MainMenuPlayPanelsClient.java"
replace(
    play,
    '''    private static void closeOtherPanels(CustomMainMenuScreen screen) {\n        closeExtras(screen);\n        closeSettings(screen);\n    }\n''',
    '''    private static void closeOtherPanels(CustomMainMenuScreen screen) {\n        closeExtras(screen);\n        closeSettings(screen);\n        PauseMenuModsPanelClient.close(screen);\n    }\n''',
)

chair = "src/main/java/net/mcreator/scpadditions/facility/ArchivistsChairBlock.java"
replace(
    chair,
    '''    private static VoxelShape shapeFor(Direction facing) {\n        return switch (facing) {\n            case EAST -> EAST;\n            case SOUTH -> SOUTH;\n            case WEST -> WEST;\n            default -> NORTH;\n        };\n    }\n''',
    '''    private static VoxelShape shapeFor(Direction facing) {\n        // GeckoLib's authored chair forward axis is one quarter-turn clockwise\n        // from the vanilla horizontal FACING convention used by this block.\n        return switch (facing) {\n            case NORTH -> EAST;\n            case EAST -> SOUTH;\n            case SOUTH -> WEST;\n            case WEST -> NORTH;\n            default -> EAST;\n        };\n    }\n''',
)

toast = "src/main/java/net/mcreator/scpadditions/client/CustomAdvancementToastClient.java"
replace(
    toast,
    'ScpFonts.roboto("ADVANCEMENT  //  UNLOCKED")',
    'ScpFonts.roboto("ACHIEVEMENT  //  UNLOCKED")',
)

config_ui = "src/main/java/net/mcreator/scpadditions/config/ui/CustomLoadingScreenModulesUi.java"
replace(
    config_ui,
    'private static final String ADVANCEMENT_LABEL = "Custom Advancement Toasts";',
    'private static final String ACHIEVEMENT_LABEL = "Custom Achievement Toasts";',
)
replace(config_ui, "ADVANCEMENT_LABEL", "ACHIEVEMENT_LABEL", 2)
replace(
    config_ui,
    '"Replaces advancement popups and their vanilla sounds with the animated SCP Additions presentation."',
    '"Replaces achievement popups and their vanilla sounds with the animated SCP Additions presentation."',
)
