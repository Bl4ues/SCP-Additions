from pathlib import Path


def read(path):
    return Path(path).read_text()


def write(path, text):
    Path(path).write_text(text)


def replace_exact(path, old, new, count=1):
    text = read(path)
    actual = text.count(old)
    if actual != count:
        raise SystemExit(
            f'{path}: expected {count} occurrence(s), found {actual}: {old[:120]!r}')
    write(path, text.replace(old, new, count))


pause = 'src/main/java/net/mcreator/scpadditions/client/CustomPauseMenuScreen.java'
replace_exact(pause,
    '    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {\n'
    '        if (PauseMenuEmbeddedPanelsClient.keyPressed(this,\n',
    '    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {\n'
    '        if (PauseMenuModsPanelClient.keyPressed(this,\n'
    '                keyCode, scanCode, modifiers)) {\n'
    '            return true;\n'
    '        }\n'
    '        if (PauseMenuEmbeddedPanelsClient.keyPressed(this,\n')
replace_exact(pause,
    '    public boolean mouseClicked(double mouseX, double mouseY, int button) {\n'
    '        if (PauseMenuEmbeddedPanelsClient.mouseClicked(this,\n',
    '    public boolean mouseClicked(double mouseX, double mouseY, int button) {\n'
    '        if (PauseMenuModsPanelClient.mouseClicked(this,\n'
    '                mouseX, mouseY, button)) {\n'
    '            return true;\n'
    '        }\n'
    '        if (PauseMenuEmbeddedPanelsClient.mouseClicked(this,\n')
replace_exact(pause,
    '    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {\n'
    '        if (PauseMenuEmbeddedPanelsClient.mouseScrolled(this,\n',
    '    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {\n'
    '        if (PauseMenuModsPanelClient.mouseScrolled(this,\n'
    '                mouseX, mouseY, delta)) {\n'
    '            return true;\n'
    '        }\n'
    '        if (PauseMenuEmbeddedPanelsClient.mouseScrolled(this,\n')
replace_exact(pause,
    '            addButton(new PauseMenuButton(ScpFonts.roboto("Settings"), () -> {\n'
    '                PauseMenuEmbeddedPanelsClient.close(this);\n'
    '                PauseMenuSettingsPanelClient.toggle(this);\n',
    '            addButton(new PauseMenuButton(ScpFonts.roboto("Settings"), () -> {\n'
    '                PauseMenuEmbeddedPanelsClient.close(this);\n'
    '                PauseMenuModsPanelClient.close(this);\n'
    '                PauseMenuSettingsPanelClient.toggle(this);\n')
replace_exact(pause,
    '        addSourceButton(MODS_KEY, "Mods");\n',
    '        AbstractButton mods = sourceButtons.get(MODS_KEY);\n'
    '        if (mods != null) {\n'
    '            addButton(new PauseMenuButton(ScpFonts.roboto("Mods"), () -> {\n'
    '                PauseMenuSettingsPanelClient.close(this);\n'
    '                PauseMenuEmbeddedPanelsClient.close(this);\n'
    '                PauseMenuModsPanelClient.toggle(this);\n'
    '            }, mods));\n'
    '        }\n')
replace_exact(pause,
    '        addButton(new PauseMenuButton(ScpFonts.roboto(label), () -> {\n'
    '            PauseMenuSettingsPanelClient.close(this);\n'
    '            if (!PauseMenuEmbeddedPanelsClient.toggle(this, mode)) {\n',
    '        addButton(new PauseMenuButton(ScpFonts.roboto(label), () -> {\n'
    '            PauseMenuSettingsPanelClient.close(this);\n'
    '            PauseMenuModsPanelClient.close(this);\n'
    '            if (!PauseMenuEmbeddedPanelsClient.toggle(this, mode)) {\n')
replace_exact(pause,
    '        PauseMenuEmbeddedPanelsClient.render(this, graphics,\n'
    '                mouseX, mouseY, partialTick, now, panelX,\n'
    '                startY, width, height, gap);\n',
    '        PauseMenuEmbeddedPanelsClient.render(this, graphics,\n'
    '                mouseX, mouseY, partialTick, now, panelX,\n'
    '                startY, width, height, gap);\n'
    '        PauseMenuModsPanelClient.render(this, graphics,\n'
    '                mouseX, mouseY, partialTick, now, panelX,\n'
    '                startY, width, height, gap);\n')
replace_exact(pause,
    '        PauseMenuSettingsPanelClient.close(this);\n'
    '        PauseMenuEmbeddedPanelsClient.close(this);\n'
    '        leavingAt = Util.getMillis();\n',
    '        PauseMenuSettingsPanelClient.close(this);\n'
    '        PauseMenuEmbeddedPanelsClient.close(this);\n'
    '        PauseMenuModsPanelClient.close(this);\n'
    '        leavingAt = Util.getMillis();\n')

achievements = 'src/main/java/net/mcreator/scpadditions/client/PauseMenuNativePanelsClient.java'
replace_exact(achievements,
    '            int iconX = layout.contentX + 9;\n'
    '            int iconY = y + 13;\n',
    '            int iconX = layout.contentX + 9;\n'
    '            int iconY = y + (concealed ? 13 : 12);\n')
replace_exact(achievements,
    '            graphics.drawString(font, ScpFonts.roboto(title), textX, y + 9,\n',
    '            graphics.drawString(font, ScpFonts.roboto(title), textX,\n'
    '                    concealed ? y + 17 : y + 9,\n')
replace_exact(achievements,
    '        graphics.drawCenteredString(font, question, x + 8, y + 4,\n',
    '        graphics.drawCenteredString(font, question, x + 8, y + 5,\n')

chair = 'src/main/java/net/mcreator/scpadditions/facility/ArchivistsChairBlock.java'
old_shape = '''    private static final double MODEL_ORIGIN_X = 10.0D;
    private static final double MODEL_ORIGIN_Z = 8.0D;

    private static final VoxelShape NORTH = Shapes.or(
            // Five-star base footprint.
            modelBox(0.20D, 0.00D, -6.50D, 12.65D, 3.65D, 6.50D),
            // Central pedestal.
            modelBox(4.80D, 3.45D, -1.20D, 7.20D, 10.25D, 1.20D),
            // Seat and immediate frame.
            modelBox(-0.60D, 9.95D, -6.65D, 12.65D, 12.30D, 6.65D),
            // Broad backrest envelope.
            modelBox(-2.10D, 10.20D, -8.10D, 7.10D, 22.80D, 1.10D))
            .optimize();
'''
new_shape = '''    // GeckoLib mirrors Bedrock's model X axis when baking cubes. The chair's
    // authored base is also rotated -17.5 degrees, while its swivelling upper
    // assembly adds another -27.5 degrees around the same horizontal pivot.
    private static final double MODEL_PIVOT_X = 6.0D;
    private static final double MODEL_PIVOT_Z = 0.0D;
    private static final double BASE_YAW = -17.5D;
    private static final double UPPER_YAW = -45.0D;

    private static final VoxelShape NORTH = Shapes.or(
            // Five-star base footprint, following the root bone.
            rotatedModelBox(0.20D, 0.00D, -6.50D,
                    12.65D, 3.65D, 6.50D, BASE_YAW, 7),
            // Central pedestal.
            rotatedModelBox(4.80D, 3.45D, -1.20D,
                    7.20D, 10.25D, 1.20D, BASE_YAW, 2),
            // Seat and immediate frame, following the swivelling upper bone.
            rotatedModelBox(-0.60D, 9.95D, -6.65D,
                    12.65D, 12.30D, 6.65D, UPPER_YAW, 8),
            // Broad backrest envelope.
            rotatedModelBox(-2.10D, 10.20D, -8.10D,
                    7.10D, 22.80D, 1.10D, UPPER_YAW, 6))
            .optimize();
'''
replace_exact(chair, old_shape, new_shape)
old_helpers = '''    private static VoxelShape shapeFor(Direction facing) {
        // GeckoLib renders this authored model with the opposite
        // horizontal forward convention from the vanilla block-facing
        // collision transform. Mirror the facing by 180 degrees so the
        // collision sits on the visible chair instead of across the
        // placement origin.
        return switch (facing) {
            case EAST -> WEST;
            case SOUTH -> NORTH;
            case WEST -> EAST;
            default -> SOUTH;
        };
    }

    private static VoxelShape modelBox(double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ) {
        return box(minX + MODEL_ORIGIN_X, minY, minZ + MODEL_ORIGIN_Z,
                maxX + MODEL_ORIGIN_X, maxY, maxZ + MODEL_ORIGIN_Z);
    }
'''
new_helpers = '''    private static VoxelShape shapeFor(Direction facing) {
        // GeoBlockRenderer uses the same quarter-turn convention: EAST is
        // -90 degrees, SOUTH is 180, and WEST is +90 from NORTH.
        return switch (facing) {
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> NORTH;
        };
    }

    /**
     * Builds a stepped approximation of an arbitrarily rotated model-space
     * rectangle. VoxelShapes remain axis-aligned, so thin strips keep the
     * collision close to the visible diagonal chair instead of inflating one
     * enormous bounding box around it.
     */
    private static VoxelShape rotatedModelBox(double minX, double minY,
            double minZ, double maxX, double maxY, double maxZ,
            double degrees, int slices) {
        int count = Math.max(1, slices);
        double step = (maxX - minX) / count;
        VoxelShape result = Shapes.empty();
        for (int index = 0; index < count; index++) {
            double sliceMinX = minX + step * index;
            double sliceMaxX = index == count - 1
                    ? maxX : minX + step * (index + 1);
            double[] bounds = rotatedBounds(sliceMinX, minZ,
                    sliceMaxX, maxZ, degrees);
            result = Shapes.or(result, modelBox(bounds[0], minY, bounds[1],
                    bounds[2], maxY, bounds[3]));
        }
        return result.optimize();
    }

    private static double[] rotatedBounds(double minX, double minZ,
            double maxX, double maxZ, double degrees) {
        double radians = Math.toRadians(degrees);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        double[] xs = {minX, minX, maxX, maxX};
        double[] zs = {minZ, maxZ, minZ, maxZ};
        double outMinX = Double.POSITIVE_INFINITY;
        double outMinZ = Double.POSITIVE_INFINITY;
        double outMaxX = Double.NEGATIVE_INFINITY;
        double outMaxZ = Double.NEGATIVE_INFINITY;
        for (int index = 0; index < 4; index++) {
            double dx = xs[index] - MODEL_PIVOT_X;
            double dz = zs[index] - MODEL_PIVOT_Z;
            double x = MODEL_PIVOT_X + dx * cosine - dz * sine;
            double z = MODEL_PIVOT_Z + dx * sine + dz * cosine;
            outMinX = Math.min(outMinX, x);
            outMinZ = Math.min(outMinZ, z);
            outMaxX = Math.max(outMaxX, x);
            outMaxZ = Math.max(outMaxZ, z);
        }
        return new double[] {outMinX, outMinZ, outMaxX, outMaxZ};
    }

    private static VoxelShape modelBox(double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ) {
        // GeckoLib's BakedModelFactory maps Bedrock X as -(origin + size)
        // and GeoBlockRenderer then translates the model to the block centre.
        // In Block.box's 0..16 coordinate space this is exactly X = 8 - modelX,
        // while Z remains Z = 8 + modelZ.
        return box(8.0D - maxX, minY, 8.0D + minZ,
                8.0D - minX, maxY, 8.0D + maxZ);
    }
'''
replace_exact(chair, old_helpers, new_helpers)

changelog = 'CHANGELOG.md'
replace_exact(changelog,
    '- Added a default-enabled **Custom Advancement Toasts** client preference that replaces vanilla advancement popups with animated SCP Additions cards;\n',
    '- Added a default-enabled **Custom Advancement Toasts** client preference that replaces vanilla advancement popups with animated SCP Additions cards;\n'
    '- Replaced Forge\'s separate Mods screen while using the custom pause menu with an animated native mod browser featuring fixed Off/A-Z/Z-A sorting controls, scrollable icon-backed mod entries, styled metadata and descriptions, direct in-game config access when supported, and an anchored Open mods folder action;\n')
