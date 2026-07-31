package com.bl4ues.scpinventory.client.gui;

import com.bl4ues.scpinventory.client.ScpFonts;
import com.bl4ues.scpinventory.network.ContextConfigDeletePacket;
import com.bl4ues.scpinventory.network.ContextConfigOpenPacket;
import com.bl4ues.scpinventory.network.ContextConfigSavePacket;
import com.bl4ues.scpinventory.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public final class ContextAnchorEditorScreen extends Screen {
    private static final int PANEL_W = 330;
    private static final int PANEL_H = 400;
    private static final int MARGIN = 10;

    private static final int NAVY = 0xF000071F;
    private static final int NAVY_LIGHT = 0xE6141E42;
    private static final int FIELD = 0xFF080D1C;
    private static final int CONTROL = 0xFF111A31;
    private static final int CONTROL_HOVER = 0xFF192744;
    private static final int BORDER = 0xFF46536C;
    private static final int BORDER_HOVER = 0xFF73809A;
    private static final int ACCENT = 0xFFC59A2A;
    private static final int ACCENT_TEXT = 0xFFE5D49A;
    private static final int WHITE = 0xFFF7F8FC;
    private static final int MUTED = 0xFFA9AFBA;
    private static final int SECTION = 0xFFD3D9E4;
    private static final int WARNING = 0xFFFFB35C;
    private static final int DANGER = 0xFFD46060;

    private final BlockPos pos;
    private final String blockId;
    private final boolean existing;
    private final String startAction;
    private final String startName;
    private final String startRange;
    private final boolean likelyRightClick;

    private EditBox actionBox;
    private EditBox nameBox;
    private EditBox rangeBox;
    private ButtonControl forgetButton;

    private double anchorX;
    private double anchorY;
    private double anchorZ;
    private boolean showName;
    private boolean allowE;
    private boolean allowRightClick;
    private boolean allowOffscreen;
    private String useItem;
    private String clickFace;
    private String rotateWith;
    private boolean confirmForget;

    public ContextAnchorEditorScreen(ContextConfigOpenPacket packet) {
        super(ScpFonts.roboto("Context Interaction Editor"));
        this.pos = packet.pos();
        this.blockId = packet.blockId();
        this.existing = packet.existing();
        this.startAction = packet.action();
        this.startName = packet.name();
        this.startRange = Double.toString(packet.range());
        this.showName = packet.showName();
        this.allowE = packet.allowE();
        this.allowRightClick = packet.allowRightClick();
        this.allowOffscreen = packet.allowOffscreen();
        this.likelyRightClick = packet.likelyRightClick();
        this.useItem = "card".equalsIgnoreCase(packet.useItem()) ? "card" : "hand";
        this.clickFace = packet.clickFace();
        this.rotateWith = packet.rotateWith();
        this.anchorX = packet.anchorX();
        this.anchorY = packet.anchorY();
        this.anchorZ = packet.anchorZ();
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int top = panelTop();
        int x = left + 16;
        int width = PANEL_W - 32;

        actionBox = configureField(new CenteredEditBox(font, x, top + 92,
                width, 20, ScpFonts.roboto("Action")));
        actionBox.setValue(startAction == null || startAction.isBlank()
                ? "Use" : startAction);
        addRenderableWidget(actionBox);

        nameBox = configureField(new CenteredEditBox(font, x, top + 127,
                width - 92, 20, ScpFonts.roboto("Display name")));
        nameBox.setValue(startName == null || startName.isBlank()
                ? fallbackName() : startName);
        addRenderableWidget(nameBox);
        addRenderableWidget(new ButtonControl(x + width - 84, top + 127,
                84, 20, nameDisplayText(), ButtonStyle.NEUTRAL, () -> {
            showName = !showName;
            refreshButtonMessages();
        }));

        rangeBox = configureField(new CenteredEditBox(font, x, top + 162,
                64, 20, ScpFonts.roboto("Range")));
        rangeBox.setValue(startRange == null ? "2.25" : startRange);
        addRenderableWidget(rangeBox);
        addRenderableWidget(new ButtonControl(x + 72, top + 162,
                98, 20, inputText(), ButtonStyle.NEUTRAL, () -> {
            cycleInput();
            refreshButtonMessages();
        }));
        addRenderableWidget(new ButtonControl(x + 178, top + 162,
                width - 178, 20, itemText(), ButtonStyle.NEUTRAL, () -> {
            useItem = "card".equals(useItem) ? "hand" : "card";
            refreshButtonMessages();
        }));

        addRenderableWidget(new ButtonControl(x, top + 190, width, 20,
                offscreenText(), ButtonStyle.NEUTRAL, () -> {
            allowOffscreen = !allowOffscreen;
            refreshButtonMessages();
        }));

        addRenderableWidget(new ButtonControl(x, top + 235,
                (width - 8) / 2, 20, "Use crosshair", ButtonStyle.NEUTRAL,
                this::anchorFromCrosshair));
        addRenderableWidget(new ButtonControl(x + (width - 8) / 2 + 8,
                top + 235, (width - 8) / 2, 20, "Set 2m ahead",
                ButtonStyle.NEUTRAL, this::anchorAhead));

        int axisY = top + 263;
        int gap = 6;
        int axisWidth = (width - gap * 5) / 6;
        addRenderableWidget(axisButton(x, axisY, axisWidth, "X−",
                () -> nudge(-step(), 0, 0)));
        addRenderableWidget(axisButton(x + (axisWidth + gap), axisY,
                axisWidth, "X+", () -> nudge(step(), 0, 0)));
        addRenderableWidget(axisButton(x + 2 * (axisWidth + gap), axisY,
                axisWidth, "Y−", () -> nudge(0, -step(), 0)));
        addRenderableWidget(axisButton(x + 3 * (axisWidth + gap), axisY,
                axisWidth, "Y+", () -> nudge(0, step(), 0)));
        addRenderableWidget(axisButton(x + 4 * (axisWidth + gap), axisY,
                axisWidth, "Z−", () -> nudge(0, 0, -step())));
        addRenderableWidget(axisButton(x + 5 * (axisWidth + gap), axisY,
                axisWidth, "Z+", () -> nudge(0, 0, step())));

        addRenderableWidget(new ButtonControl(x, top + 291,
                (width - 8) / 2, 20, faceText(), ButtonStyle.NEUTRAL, () -> {
            clickFace = next(clickFace, new String[]{
                    "front", "back", "player", "north", "south",
                    "east", "west", "up", "down"
            });
            refreshButtonMessages();
        }));
        addRenderableWidget(new ButtonControl(x + (width - 8) / 2 + 8,
                top + 291, (width - 8) / 2, 20, rotateText(),
                ButtonStyle.NEUTRAL, () -> {
            rotateWith = next(rotateWith, new String[]{
                    "none", "auto", "facing", "horizontal_facing", "axis"
            });
            refreshButtonMessages();
        }));

        int bottomY = top + PANEL_H - 31;
        forgetButton = addRenderableWidget(new ButtonControl(x, bottomY,
                76, 22, "Forget", ButtonStyle.DANGER, this::forgetRule));
        addRenderableWidget(new ButtonControl(left + PANEL_W - 176, bottomY,
                78, 22, "Save", ButtonStyle.PRIMARY, this::save));
        addRenderableWidget(new ButtonControl(left + PANEL_W - 90, bottomY,
                74, 22, "Cancel", ButtonStyle.NEUTRAL, this::onClose));
    }

    private EditBox configureField(EditBox field) {
        field.setBordered(false);
        field.setTextColor(WHITE);
        field.setTextColorUneditable(MUTED);
        field.setFormatter((value, cursor) ->
                ScpFonts.roboto(value).getVisualOrderText());
        return field;
    }

    private ButtonControl axisButton(int x, int y, int width,
            String label, Runnable action) {
        return new ButtonControl(x, y, width, 20, label,
                ButtonStyle.NEUTRAL, action);
    }

    @Override
    public void tick() {
        super.tick();
        actionBox.tick();
        nameBox.tick();
        rangeBox.tick();
        spawnMarkerParticles();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        renderBackground(graphics);
        int left = panelLeft();
        int top = panelTop();

        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, NAVY);
        graphics.fill(left, top, left + PANEL_W, top + 31, NAVY_LIGHT);
        graphics.fill(left, top + 30, left + PANEL_W, top + 31, ACCENT);
        outline(graphics, left, top, PANEL_W, PANEL_H, BORDER);

        graphics.drawString(font, ScpFonts.roboto("CONTEXT INTERACTION"),
                left + 16, top + 11, WHITE, false);
        graphics.drawString(font, ScpFonts.roboto(
                        existing ? "Editing rule" : "Creating rule"),
                left + 16, top + 43, MUTED, false);
        graphics.drawString(font, ScpFonts.roboto(compact(blockId, 44)),
                left + 16, top + 56, ACCENT_TEXT, false);

        graphics.drawString(font, ScpFonts.roboto("INTERACTION"),
                left + 16, top + 76, SECTION, false);
        graphics.drawString(font, ScpFonts.roboto("Action"),
                left + 16, top + 84, MUTED, false);
        graphics.drawString(font, ScpFonts.roboto("Display name"),
                left + 16, top + 119, MUTED, false);
        graphics.drawString(font, ScpFonts.roboto("Range"),
                left + 16, top + 154, MUTED, false);

        drawField(graphics, actionBox);
        drawField(graphics, nameBox);
        drawField(graphics, rangeBox);

        graphics.drawString(font, ScpFonts.roboto("ANCHOR"),
                left + 16, top + 219, SECTION, false);

        graphics.fill(left + 16, top + 319, left + PANEL_W - 16,
                top + 342, FIELD);
        outline(graphics, left + 16, top + 319,
                PANEL_W - 32, 23, BORDER);
        graphics.drawString(font, ScpFonts.roboto("Local"),
                left + 23, top + 327, MUTED, false);
        graphics.drawString(font, ScpFonts.roboto(
                        "X " + fmt(anchorX) + "   Y " + fmt(anchorY)
                                + "   Z " + fmt(anchorZ)),
                left + 66, top + 327, ACCENT_TEXT, false);

        graphics.drawString(font, ScpFonts.roboto(
                        "Arrows X/Y · PgUp/PgDn Z · Wheel Y · Shift/Ctrl precision"),
                left + 16, top + 348, MUTED, false);

        if (!likelyRightClick) {
            graphics.drawString(font, ScpFonts.roboto(
                            "No right-click handler detected; saving is still allowed."),
                    left + 16, top + 359, WARNING, false);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static void drawField(GuiGraphics graphics, EditBox field) {
        graphics.fill(field.getX() - 3, field.getY() - 2,
                field.getX() + field.getWidth() + 3,
                field.getY() + field.getHeight() + 2, FIELD);
        outline(graphics, field.getX() - 3, field.getY() - 2,
                field.getWidth() + 6, field.getHeight() + 4, BORDER);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!actionBox.isFocused() && !nameBox.isFocused()
                && !rangeBox.isFocused()) {
            double amount = step();
            if (keyCode == GLFW.GLFW_KEY_LEFT) {
                nudge(-amount, 0, 0);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                nudge(amount, 0, 0);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_UP) {
                nudge(0, amount, 0);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                nudge(0, -amount, 0);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_PAGE_UP) {
                nudge(0, 0, amount);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
                nudge(0, 0, -amount);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        nudge(0, delta * step(), 0);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void save() {
        ModNetwork.CHANNEL.sendToServer(new ContextConfigSavePacket(
                pos, blockId, actionBox.getValue(), nameBox.getValue(),
                showName, parseRange(), allowE, allowRightClick,
                allowOffscreen, useItem, clickFace, rotateWith,
                anchorX, anchorY, anchorZ));
        Minecraft.getInstance().setScreen(null);
    }

    private void forgetRule() {
        if (!confirmForget) {
            confirmForget = true;
            if (forgetButton != null) {
                forgetButton.setMessage(ScpFonts.roboto("Confirm"));
            }
            return;
        }
        ModNetwork.CHANNEL.sendToServer(
                new ContextConfigDeletePacket(pos, blockId));
        Minecraft.getInstance().setScreen(null);
    }

    private void refreshButtonMessages() {
        for (var child : children()) {
            if (!(child instanceof ButtonControl button)) continue;
            String current = button.getMessage().getString();
            if (current.startsWith("Name:")) {
                button.setMessage(ScpFonts.roboto(nameDisplayText()));
            } else if (current.startsWith("Input:")) {
                button.setMessage(ScpFonts.roboto(inputText()));
            } else if (current.startsWith("Item:")) {
                button.setMessage(ScpFonts.roboto(itemText()));
            } else if (current.startsWith("Off-screen:")) {
                button.setMessage(ScpFonts.roboto(offscreenText()));
            } else if (current.startsWith("Face:")) {
                button.setMessage(ScpFonts.roboto(faceText()));
            } else if (current.startsWith("Rotate:")) {
                button.setMessage(ScpFonts.roboto(rotateText()));
            }
        }
    }

    private double parseRange() {
        try {
            return Math.max(0.25D,
                    Double.parseDouble(rangeBox.getValue()));
        } catch (Exception ignored) {
            return 2.25D;
        }
    }

    private void cycleInput() {
        if (allowE && allowRightClick) {
            allowRightClick = false;
        } else if (allowE) {
            allowE = false;
            allowRightClick = true;
        } else {
            allowE = true;
            allowRightClick = true;
        }
    }

    private String inputText() {
        if (allowE && allowRightClick) return "Input: Both";
        return allowE ? "Input: E" : "Input: Right-click";
    }

    private String offscreenText() {
        return allowOffscreen ? "Off-screen: On" : "Off-screen: Off";
    }

    private String itemText() {
        return "card".equals(useItem) ? "Item: Card" : "Item: Hand";
    }

    private String nameDisplayText() {
        return showName ? "Name: On" : "Name: Off";
    }

    private String faceText() {
        return "Face: " + clickFace;
    }

    private String rotateText() {
        return "Rotate: " + rotateWith;
    }

    private void anchorFromCrosshair() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.hitResult instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK) {
            setAnchorFromWorld(hit.getLocation());
        }
    }

    private void anchorAhead() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        setAnchorFromWorld(minecraft.player.getEyePosition()
                .add(minecraft.player.getViewVector(1.0F)
                        .normalize().scale(2.0D)));
    }

    private void nudge(double x, double y, double z) {
        setAnchorFromWorld(resolveAnchorWorld().add(x, y, z));
    }

    private void setAnchorFromWorld(Vec3 world) {
        Vec3 centeredWorld = world.subtract(
                Vec3.atLowerCornerOf(pos).add(0.5D, 0.5D, 0.5D));
        Vec3 canonical = inverseRotate(centeredWorld, blockState());
        setAnchor(canonical.x + 0.5D, canonical.y + 0.5D,
                canonical.z + 0.5D);
    }

    private Vec3 resolveAnchorWorld() {
        Vec3 centered = new Vec3(anchorX - 0.5D,
                anchorY - 0.5D, anchorZ - 0.5D);
        Vec3 rotated = rotate(centered, blockState());
        return Vec3.atLowerCornerOf(pos).add(0.5D, 0.5D, 0.5D)
                .add(rotated);
    }

    private void setAnchor(double x, double y, double z) {
        anchorX = round(x);
        anchorY = round(y);
        anchorZ = round(z);
    }

    private double step() {
        if (hasControlDown()) return 0.01D;
        if (hasShiftDown()) return 0.10D;
        return 0.05D;
    }

    private void spawnMarkerParticles() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        Vec3 world = resolveAnchorWorld();
        minecraft.level.addParticle(ParticleTypes.END_ROD,
                world.x, world.y, world.z, 0.0D, 0.01D, 0.0D);
        minecraft.level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                world.x, world.y, world.z, 0.0D, 0.0D, 0.0D);
    }

    private BlockState blockState() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null
                ? null : minecraft.level.getBlockState(pos);
    }

    private Vec3 rotate(Vec3 local, BlockState state) {
        Direction facing = resolveFacing(state);
        if (facing == null || facing == Direction.NORTH) return local;
        return switch (facing) {
            case SOUTH -> new Vec3(-local.x, local.y, -local.z);
            case EAST -> new Vec3(-local.z, local.y, local.x);
            case WEST -> new Vec3(local.z, local.y, -local.x);
            case UP, DOWN -> local;
            default -> local;
        };
    }

    private Vec3 inverseRotate(Vec3 local, BlockState state) {
        Direction facing = resolveFacing(state);
        if (facing == null || facing == Direction.NORTH) return local;
        return switch (facing) {
            case SOUTH -> new Vec3(-local.x, local.y, -local.z);
            case EAST -> new Vec3(local.z, local.y, -local.x);
            case WEST -> new Vec3(-local.z, local.y, local.x);
            case UP, DOWN -> local;
            default -> local;
        };
    }

    private Direction resolveFacing(BlockState state) {
        if (state == null || "none".equalsIgnoreCase(rotateWith)) {
            return null;
        }
        if ("horizontal_facing".equalsIgnoreCase(rotateWith)) {
            return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                    ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                    : null;
        }
        if ("facing".equalsIgnoreCase(rotateWith)) {
            return state.hasProperty(BlockStateProperties.FACING)
                    ? state.getValue(BlockStateProperties.FACING) : null;
        }
        if ("axis".equalsIgnoreCase(rotateWith)) return null;
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return null;
    }

    private String fallbackName() {
        String path = blockId.contains(":")
                ? blockId.substring(blockId.indexOf(':') + 1) : blockId;
        String[] parts = path.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1));
        }
        return builder.isEmpty() ? blockId : builder.toString();
    }

    private int panelLeft() {
        return Math.max(MARGIN, width - PANEL_W - MARGIN);
    }

    private int panelTop() {
        return Math.max(MARGIN, (height - PANEL_H) / 2);
    }

    private static String next(String current, String[] values) {
        for (int index = 0; index < values.length; index++) {
            if (values[index].equals(current)) {
                return values[(index + 1) % values.length];
            }
        }
        return values[0];
    }

    private static String compact(String text, int max) {
        if (text == null || text.length() <= max) {
            return text == null ? "" : text;
        }
        return text.substring(0, Math.max(0, max - 3)) + "...";
    }

    private static double round(double value) {
        return Math.round(value * 1000.0D) / 1000.0D;
    }

    private static String fmt(double value) {
        return String.format("%.3f",
                Mth.clamp(value, -999.0D, 999.0D));
    }

    private static void outline(GuiGraphics graphics, int x, int y,
            int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private static final class CenteredEditBox extends EditBox {
        private CenteredEditBox(Font font, int x, int y, int width,
                int height, Component message) {
            super(font, x, y, width, height, message);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX,
                int mouseY, float partialTick) {
            int offset = Math.max(0, (getHeight() - 9) / 2 + 2);
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, offset, 0.0F);
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            graphics.pose().popPose();
        }
    }

    private enum ButtonStyle {
        PRIMARY,
        NEUTRAL,
        DANGER
    }

    private final class ButtonControl extends AbstractButton {
        private final ButtonStyle style;
        private final Runnable action;

        private ButtonControl(int x, int y, int width, int height,
                String label, ButtonStyle style, Runnable action) {
            super(x, y, width, height, ScpFonts.roboto(label));
            this.style = style;
            this.action = action;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void updateWidgetNarration(
                NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX,
                int mouseY, float partialTick) {
            int background = !active ? 0xFF171B25
                    : isHoveredOrFocused() ? CONTROL_HOVER : CONTROL;
            int edge = style == ButtonStyle.DANGER ? DANGER
                    : style == ButtonStyle.PRIMARY ? ACCENT
                    : isHoveredOrFocused() ? BORDER_HOVER : BORDER;
            int text = !active ? MUTED
                    : style == ButtonStyle.PRIMARY ? ACCENT_TEXT : WHITE;
            graphics.fill(getX(), getY(), getX() + getWidth(),
                    getY() + getHeight(), background);
            outline(graphics, getX(), getY(), getWidth(),
                    getHeight(), edge);
            if (style != ButtonStyle.NEUTRAL) {
                graphics.fill(getX() + 1, getY() + 1, getX() + 4,
                        getY() + getHeight() - 1, edge);
            }
            graphics.drawCenteredString(font,
                    ScpFonts.roboto(getMessage()),
                    getX() + getWidth() / 2,
                    getY() + (getHeight() - 8) / 2, text);
        }
    }
}
