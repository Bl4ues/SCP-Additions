package com.bl4ues.scpinventory.client.gui;

import com.bl4ues.scpinventory.network.ContextConfigDeletePacket;
import com.bl4ues.scpinventory.network.ContextConfigOpenPacket;
import com.bl4ues.scpinventory.network.ContextConfigSavePacket;
import com.bl4ues.scpinventory.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
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

public class ContextConfigScreen extends Screen {
    private static final int PANEL_W = 270;
    private static final int PANEL_H = 366;
    private static final int MARGIN = 10;

    private final BlockPos pos;
    private final String blockId;
    private final boolean existing;
    private final String startAction;
    private final String startName;
    private final String startRange;
    private EditBox actionBox;
    private EditBox nameBox;
    private EditBox rangeBox;
    private double anchorX;
    private double anchorY;
    private double anchorZ;
    private boolean showName;
    private boolean allowE;
    private boolean allowRightClick;
    private String useItem;
    private String clickFace;
    private String rotateWith;
    private Button forgetButton;
    private boolean confirmForget;

    public ContextConfigScreen(ContextConfigOpenPacket packet) {
        super(Component.literal("Context Interaction Editor"));
        this.pos = packet.pos();
        this.blockId = packet.blockId();
        this.existing = packet.existing();
        this.startAction = packet.action();
        this.startName = packet.name();
        this.startRange = Double.toString(packet.range());
        this.showName = packet.showName();
        this.allowE = packet.allowE();
        this.allowRightClick = packet.allowRightClick();
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
        int fieldX = left + 12;
        int fieldW = PANEL_W - 24;
        int y = top + 45;

        actionBox = new EditBox(font, fieldX, y, fieldW, 18, Component.literal("Action"));
        actionBox.setValue(startAction == null || startAction.isBlank() ? "Use" : startAction);
        addRenderableWidget(actionBox);

        y += 36;
        nameBox = new EditBox(font, fieldX, y, 154, 18, Component.literal("Name"));
        nameBox.setValue(startName == null || startName.isBlank() ? fallbackName() : startName);
        addRenderableWidget(nameBox);
        addRenderableWidget(Button.builder(Component.literal(nameDisplayText()), b -> {
            showName = !showName;
            b.setMessage(Component.literal(nameDisplayText()));
        }).bounds(fieldX + 164, y, 82, 18).build());

        y += 36;
        rangeBox = new EditBox(font, fieldX, y, 72, 18, Component.literal("Range"));
        rangeBox.setValue(startRange == null ? "2.25" : startRange);
        addRenderableWidget(rangeBox);
        addRenderableWidget(Button.builder(Component.literal(inputText()), b -> {
            cycleInput();
            b.setMessage(Component.literal(inputText()));
        }).bounds(fieldX + 82, y, 78, 18).build());
        addRenderableWidget(Button.builder(Component.literal(itemText()), b -> {
            useItem = "card".equals(useItem) ? "hand" : "card";
            b.setMessage(Component.literal(itemText()));
        }).bounds(fieldX + 168, y, 78, 18).build());

        y += 34;
        addRenderableWidget(Button.builder(Component.literal("Anchor: crosshair"), b -> anchorFromCrosshair()).bounds(fieldX, y, 118, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Anchor: 2m ahead"), b -> anchorAhead()).bounds(fieldX + 128, y, 118, 20).build());

        y += 28;
        int bw = 34;
        addRenderableWidget(Button.builder(Component.literal("X-"), b -> nudge(-step(), 0, 0)).bounds(fieldX, y, bw, 18).build());
        addRenderableWidget(Button.builder(Component.literal("X+"), b -> nudge(step(), 0, 0)).bounds(fieldX + 38, y, bw, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Y-"), b -> nudge(0, -step(), 0)).bounds(fieldX + 80, y, bw, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Y+"), b -> nudge(0, step(), 0)).bounds(fieldX + 118, y, bw, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Z-"), b -> nudge(0, 0, -step())).bounds(fieldX + 160, y, bw, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Z+"), b -> nudge(0, 0, step())).bounds(fieldX + 198, y, bw, 18).build());

        y += 30;
        addRenderableWidget(Button.builder(Component.literal("Face: " + clickFace), b -> {
            clickFace = next(clickFace, new String[]{"front", "back", "player", "north", "south", "east", "west", "up", "down"});
            b.setMessage(Component.literal("Face: " + clickFace));
        }).bounds(fieldX, y, 118, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Rotate: " + rotateWith), b -> {
            rotateWith = next(rotateWith, new String[]{"none", "auto", "facing", "horizontal_facing", "axis"});
            b.setMessage(Component.literal("Rotate: " + rotateWith));
        }).bounds(fieldX + 128, y, 118, 18).build());

        int bottomY = top + PANEL_H - 30;
        forgetButton = addRenderableWidget(Button.builder(Component.literal("Forget"), b -> forgetRule()).bounds(left + 12, bottomY, 68, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> save()).bounds(left + PANEL_W - 154, bottomY, 68, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose()).bounds(left + PANEL_W - 80, bottomY, 68, 20).build());
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
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int left = panelLeft();
        int top = panelTop();
        g.fill(left, top, left + PANEL_W, top + PANEL_H, 0xCC111317);
        g.fill(left, top, left + PANEL_W, top + 34, 0xE525282D);
        g.drawString(font, "Context Interaction Editor", left + 12, top + 7, 0xFFE8E8E8, false);
        g.drawString(font, (existing ? "Editing " : "New ") + compact(blockId, 30), left + 12, top + 21, 0xFFB5C7FF, false);

        g.drawString(font, "Action", left + 12, top + 36, 0xFFB7B7B7, false);
        g.drawString(font, "Name / Display", left + 12, top + 71, 0xFFB7B7B7, false);
        g.drawString(font, "Range / Input / Item", left + 12, top + 107, 0xFFB7B7B7, false);
        g.drawString(font, "Anchor tools", left + 12, top + 142, 0xFFB7B7B7, false);
        g.drawString(font, "Anchor local", left + 12, top + 242, 0xFFE6E6B0, false);
        g.drawString(font, "X " + fmt(anchorX) + "  Y " + fmt(anchorY) + "  Z " + fmt(anchorZ), left + 12, top + 254, 0xFFE6E6B0, false);
        g.drawString(font, "Arrows X/Y, PgUp/PgDn Z, wheel Y", left + 12, top + 270, 0xFF999999, false);
        g.drawString(font, "Shift=0.10  Ctrl=0.01  normal=0.05", left + 12, top + 282, 0xFF999999, false);
        g.drawString(font, "Rotate mode now previews final in-world anchor.", left + 12, top + 296, 0xFF88DDEE, false);
        g.drawString(font, "Forget asks twice, then removes this rule.", left + 12, top + 310, 0xFFFF9D9D, false);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!actionBox.isFocused() && !nameBox.isFocused() && !rangeBox.isFocused()) {
            double s = step();
            if (keyCode == GLFW.GLFW_KEY_LEFT) {
                nudge(-s, 0, 0);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                nudge(s, 0, 0);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_UP) {
                nudge(0, s, 0);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                nudge(0, -s, 0);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_PAGE_UP) {
                nudge(0, 0, s);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
                nudge(0, 0, -s);
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
        ModNetwork.CHANNEL.sendToServer(new ContextConfigSavePacket(pos, blockId, actionBox.getValue(), nameBox.getValue(), showName, parseRange(), allowE, allowRightClick, useItem, clickFace, rotateWith, anchorX, anchorY, anchorZ));
        Minecraft.getInstance().setScreen(null);
    }

    private void forgetRule() {
        if (!confirmForget) {
            confirmForget = true;
            if (forgetButton != null) {
                forgetButton.setMessage(Component.literal("Confirm"));
            }
            return;
        }
        ModNetwork.CHANNEL.sendToServer(new ContextConfigDeletePacket(pos, blockId));
        Minecraft.getInstance().setScreen(null);
    }

    private double parseRange() {
        try {
            return Math.max(0.25D, Double.parseDouble(rangeBox.getValue()));
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
        return allowE ? "Input: E" : "Input: Right";
    }

    private String itemText() {
        return "card".equals(useItem) ? "Item: Card" : "Item: Hand";
    }

    private String nameDisplayText() {
        return showName ? "Name: On" : "Name: Off";
    }

    private void anchorFromCrosshair() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            setAnchorFromWorld(hit.getLocation());
        }
    }

    private void anchorAhead() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        setAnchorFromWorld(mc.player.getEyePosition().add(mc.player.getViewVector(1.0F).normalize().scale(2.0D)));
    }

    private void nudge(double x, double y, double z) {
        setAnchorFromWorld(resolveAnchorWorld().add(x, y, z));
    }

    private void setAnchorFromWorld(Vec3 world) {
        Vec3 centeredWorld = world.subtract(Vec3.atLowerCornerOf(pos).add(0.5D, 0.5D, 0.5D));
        Vec3 canonical = inverseRotate(centeredWorld, blockState());
        setAnchor(canonical.x + 0.5D, canonical.y + 0.5D, canonical.z + 0.5D);
    }

    private Vec3 resolveAnchorWorld() {
        Vec3 centered = new Vec3(anchorX - 0.5D, anchorY - 0.5D, anchorZ - 0.5D);
        Vec3 rotated = rotate(centered, blockState());
        return Vec3.atLowerCornerOf(pos).add(0.5D, 0.5D, 0.5D).add(rotated);
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Vec3 world = resolveAnchorWorld();
        mc.level.addParticle(ParticleTypes.END_ROD, world.x, world.y, world.z, 0.0D, 0.01D, 0.0D);
        mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, world.x, world.y, world.z, 0.0D, 0.0D, 0.0D);
    }

    private BlockState blockState() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? null : mc.level.getBlockState(pos);
    }

    private Vec3 rotate(Vec3 local, BlockState state) {
        Direction facing = resolveFacing(state);
        if (facing == null || facing == Direction.NORTH) {
            return local;
        }
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
        if (facing == null || facing == Direction.NORTH) {
            return local;
        }
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
            return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? state.getValue(BlockStateProperties.HORIZONTAL_FACING) : null;
        }
        if ("facing".equalsIgnoreCase(rotateWith)) {
            return state.hasProperty(BlockStateProperties.FACING) ? state.getValue(BlockStateProperties.FACING) : null;
        }
        if ("axis".equalsIgnoreCase(rotateWith)) {
            return null;
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return null;
    }

    private String fallbackName() {
        String path = blockId.contains(":") ? blockId.substring(blockId.indexOf(':') + 1) : blockId;
        String[] parts = path.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                if (!builder.isEmpty()) builder.append(' ');
                builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
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
        for (int i = 0; i < values.length; i++) if (values[i].equals(current)) return values[(i + 1) % values.length];
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
        return String.format("%.3f", Mth.clamp(value, -999.0D, 999.0D));
    }
}
