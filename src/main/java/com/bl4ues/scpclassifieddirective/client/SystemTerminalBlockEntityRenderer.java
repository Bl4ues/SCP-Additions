package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.block.SCP079SystemControlBlock;
import com.bl4ues.scpclassifieddirective.block.entity.SystemTerminalBlockEntity;
import com.bl4ues.scpclassifieddirective.client.render.PhysicalBlockScreenGeometry.Frame;
import com.bl4ues.scpclassifieddirective.network.FacilityDiagnosticsPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Shader-compatible placed renderer for the Facility Diagnostic Terminal. The
 * authored GeckoLib computer stays intact while a full-bright SCiPNET surface
 * is composited directly onto its angled CRT and remains visible when nobody is
 * interacting with it.
 */
public final class SystemTerminalBlockEntityRenderer
        extends GeoBlockRenderer<SystemTerminalBlockEntity> {
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final ResourceLocation GLOWMASK = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "textures/block/system_terminal_glowmask.png");
    private static final ResourceLocation SCREEN_BASE = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "textures/screens/diagnostic_terminal_screen.png");
    private static final ResourceLocation WHITE_PIXEL = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "textures/screens/white_pixel.png");
    private static final ResourceLocation TERMINAL_LOGO = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "textures/screens/terminallogo.png");
    private static final ResourceLocation TERMINAL_TEXT = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "scipnet_terminal");
    private static final ResourceLocation TERMINAL_CAPTION = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "scipnet_caption");

    private static final int OFF_WHITE = 0xFFE9EDEB;
    private static final int METAL_GRAY = 0xFFA6AFB2;
    private static final int SIGNAL_GOLD = 0xFFC9A21A;
    private static final int MUTED_BLUE = 0xFF70838D;
    private static final int FOUNDATION_RED = 0xFFB94145;
    private static final int BUTTON_HOVER = 0xFF315365;

    private static final double BASE_EPSILON = 0.0022D;
    private static final double DECOR_EPSILON = 0.0026D;
    private static final double TEXT_EPSILON = 0.0030D;

    private final Font font;

    public SystemTerminalBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {
        super(new SystemTerminalGeoModel());
        this.font = context.getFont();

        addRenderLayer(new GeoRenderLayer<>(this) {
            @Override
            public void render(PoseStack poseStack,
                    SystemTerminalBlockEntity animatable,
                    BakedGeoModel bakedModel, RenderType renderType,
                    MultiBufferSource bufferSource, VertexConsumer buffer,
                    float partialTick, int packedLight, int packedOverlay) {
                RenderType emissive = RenderType.eyes(GLOWMASK);
                getRenderer().reRender(bakedModel, poseStack, bufferSource,
                        animatable, emissive,
                        bufferSource.getBuffer(emissive), partialTick,
                        FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                        1.0F, 1.0F, 1.0F, 1.0F);
            }
        });
    }

    @Override
    public void render(SystemTerminalBlockEntity terminal, float partialTick,
            PoseStack poseStack, MultiBufferSource buffers, int packedLight,
            int packedOverlay) {
        super.render(terminal, partialTick, poseStack, buffers, packedLight,
                packedOverlay);

        // GeckoLib's translucent body/glow passes must land before the CRT. If
        // left queued, a later glowmask flush can repaint over the interface.
        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch();
        }

        BlockState state = terminal.getBlockState();
        Direction facing = state.hasProperty(SCP079SystemControlBlock.FACING)
                ? state.getValue(SCP079SystemControlBlock.FACING)
                : Direction.NORTH;
        BlockPos pos = terminal.getBlockPos();
        Frame frame = TeslaTerminalFocusClient.diagnosticFrame(pos, facing);
        DisplayView view = displayView(terminal, pos);

        renderQuad(poseStack, buffers, frame, pos, SCREEN_BASE,
                -0.5D, 0.5D, 0.5D, -0.5D, BASE_EPSILON,
                255, 255, 255, 255, true);
        renderSubQuad(poseStack, buffers, frame, pos, TERMINAL_LOGO,
                11, 5, 42, 42, DECOR_EPSILON,
                255, 255, 255, 255, true);

        FacilityDiagnosticsScreen active = activeScreen(pos);
        if (active != null && active.hoveringPrimaryAction()) {
            renderSubQuad(poseStack, buffers, frame, pos, WHITE_PIXEL,
                    FacilityDiagnosticsScreen.ACTION_X,
                    FacilityDiagnosticsScreen.ACTION_Y,
                    FacilityDiagnosticsScreen.ACTION_WIDTH,
                    FacilityDiagnosticsScreen.ACTION_HEIGHT,
                    DECOR_EPSILON, red(BUTTON_HOVER), green(BUTTON_HOVER),
                    blue(BUTTON_HOVER), 255, true);
            renderBorder(poseStack, buffers, frame, pos,
                    FacilityDiagnosticsScreen.ACTION_X,
                    FacilityDiagnosticsScreen.ACTION_Y,
                    FacilityDiagnosticsScreen.ACTION_WIDTH,
                    FacilityDiagnosticsScreen.ACTION_HEIGHT,
                    FOUNDATION_RED);
        }

        renderTextLayer(poseStack, buffers, frame, pos, facing, view,
                active != null);
    }

    private DisplayView displayView(SystemTerminalBlockEntity terminal,
            BlockPos pos) {
        FacilityDiagnosticsScreen screen = activeScreen(pos);
        if (screen != null) {
            FacilityDiagnosticsPacket packet = screen.data();
            return new DisplayView(packet.uncontainedScps(),
                    packet.activeTeslaGates(), packet.registeredTeslaGates(),
                    packet.teslaOverride(), packet.connectedDoors(),
                    packet.auxiliaryPowerOnline(),
                    screen.cooldownRemainingTicks(),
                    packet.unusualNetworkActivity(), packet.analysisComplete());
        }
        return new DisplayView(terminal.uncontainedScps(),
                terminal.activeTeslaGates(), terminal.registeredTeslaGates(),
                terminal.teslaOverride(), terminal.connectedDoors(),
                terminal.auxiliaryPowerOnline(),
                terminal.cachePurgeCooldownTicks(),
                terminal.unusualNetworkActivity(), terminal.analyzed());
    }

    private static FacilityDiagnosticsScreen activeScreen(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof FacilityDiagnosticsScreen screen
                && screen.isFor(pos)) {
            return screen;
        }
        return null;
    }

    private void renderTextLayer(PoseStack poseStack, MultiBufferSource buffers,
            Frame frame, BlockPos pos, Direction facing, DisplayView view,
            boolean focused) {
        Vec3 topLeft = local(frame.point(-0.5D, 0.5D, TEXT_EPSILON), pos);
        float pixelScaleX = (float) (frame.width()
                / FacilityDiagnosticsScreen.TEX_W);
        float pixelScaleY = (float) (frame.height()
                / FacilityDiagnosticsScreen.TEX_H);
        float depthScale = Math.min(pixelScaleX, pixelScaleY);

        poseStack.pushPose();
        poseStack.translate(topLeft.x, topLeft.y, topLeft.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(textYaw(facing)
                - (float) TeslaTerminalFocusClient
                .DIAGNOSTIC_SCREEN_YAW_DEGREES));
        poseStack.scale(pixelScaleX, -pixelScaleY, depthScale);

        drawHeading(poseStack, buffers, "SCiPNET FACILITY STATUS",
                62, 8, OFF_WHITE);
        drawHeading(poseStack, buffers,
                "ARC-SITE-48 // INTERNAL SYSTEMS NODE", 62, 23, METAL_GRAY);
        drawBody(poseStack, buffers, "NODE ARC48-SYS-01", 62, 38,
                MUTED_BLUE);
        drawBodyRight(poseStack, buffers, "BUILD 3.2.6", 518, 10,
                METAL_GRAY);

        drawHeading(poseStack, buffers, "CONTAINMENT INDEX", 23, 66,
                OFF_WHITE);
        drawBodyRight(poseStack, buffers, "CI-01", 211, 67, MUTED_BLUE);
        drawHeading(poseStack, buffers, "FACILITY TELEMETRY", 235, 66,
                OFF_WHITE);
        drawBodyRight(poseStack, buffers, "FT-02", 518, 67, MUTED_BLUE);

        boolean available = view.auxiliaryPowerOnline()
                && view.cachePurgeCooldownTicks() <= 0;
        boolean analyzed = view.analysisComplete() && available;

        String signatures = !view.auxiliaryPowerOnline() ? "UNAVAILABLE"
                : !analyzed ? "PENDING" : twoDigits(view.uncontainedScps());
        String integrity = !view.auxiliaryPowerOnline() ? "UNAVAILABLE"
                : !analyzed ? "ANALYSIS REQUIRED"
                : view.uncontainedScps() == 0 ? "NOMINAL"
                : view.uncontainedScps() <= 2 ? "DEGRADED" : "CRITICAL";
        int threatColor = !analyzed ? METAL_GRAY
                : view.uncontainedScps() == 0 ? SIGNAL_GOLD : FOUNDATION_RED;

        row(poseStack, buffers, "UNCONTAINED SIGNATURES", signatures,
                21, 93, 211, threatColor);
        row(poseStack, buffers, "INTEGRITY STATE", integrity,
                21, 120, 211, threatColor);
        drawBody(poseStack, buffers,
                analyzed ? "SCOPE // SITE-WIDE REGISTRY"
                        : view.cachePurgeCooldownTicks() > 0
                                ? "REGISTRY // REINDEXING"
                                : "SCOPE // ANALYSIS PENDING",
                23, 158, MUTED_BLUE);

        String tesla = !view.auxiliaryPowerOnline() ? "UNAVAILABLE"
                : !analyzed ? "PENDING"
                : twoDigits(view.activeTeslaGates()) + " / "
                        + twoDigits(view.registeredTeslaGates()) + " ONLINE";
        String override = !view.auxiliaryPowerOnline() ? "UNAVAILABLE"
                : !analyzed ? "PENDING"
                : view.teslaOverride() ? "ACTIVE" : "INACTIVE";
        String doors = !view.auxiliaryPowerOnline() ? "UNAVAILABLE"
                : !analyzed ? "PENDING" : twoDigits(view.connectedDoors());
        row(poseStack, buffers, "TESLA GATE SYSTEMS", tesla,
                233, 93, 519, analyzed ? OFF_WHITE : METAL_GRAY);
        row(poseStack, buffers, "TESLA GATE MANUAL OVERRIDE", override,
                233, 116, 519,
                analyzed && view.teslaOverride() ? FOUNDATION_RED : METAL_GRAY);
        row(poseStack, buffers, "DOOR SYSTEM ENDPOINTS", doors,
                233, 139, 519, analyzed ? OFF_WHITE : METAL_GRAY);
        drawBody(poseStack, buffers,
                analyzed ? "LINK // ENDPOINT REGISTRY CURRENT"
                        : view.cachePurgeCooldownTicks() > 0
                                ? "LINK // REGISTRY REBUILD ACTIVE"
                                : "LINK // ANALYSIS PENDING",
                235, 158, MUTED_BLUE);

        drawHeading(poseStack, buffers, "SCiPNET OPERATIONS", 24, 189,
                OFF_WHITE);
        drawBody(poseStack, buffers, "AUXILIARY BUS", 24, 206, METAL_GRAY);
        drawBody(poseStack, buffers,
                view.auxiliaryPowerOnline() ? "ONLINE" : "OFFLINE",
                106, 206, view.auxiliaryPowerOnline()
                        ? SIGNAL_GOLD : FOUNDATION_RED);

        String operationLabel;
        String operationState;
        int operationColor;
        if (!view.auxiliaryPowerOnline()) {
            operationLabel = "FACILITY ANALYSIS";
            operationState = "UNAVAILABLE";
            operationColor = METAL_GRAY;
        } else if (view.cachePurgeCooldownTicks() > 0) {
            operationLabel = "SESSION CACHE";
            operationState = "REINDEX "
                    + formatCooldown(view.cachePurgeCooldownTicks());
            operationColor = SIGNAL_GOLD;
        } else if (!view.analysisComplete()) {
            operationLabel = "FACILITY ANALYSIS";
            operationState = "REQUIRED";
            operationColor = SIGNAL_GOLD;
        } else {
            operationLabel = "SESSION CACHE";
            operationState = view.unusualNetworkActivity()
                    ? "REVIEW ADVISED" : "READY";
            operationColor = view.unusualNetworkActivity()
                    ? SIGNAL_GOLD : OFF_WHITE;
        }
        drawBody(poseStack, buffers, operationLabel, 24, 220, METAL_GRAY);
        drawBody(poseStack, buffers, operationState, 106, 220,
                operationColor);

        String button = !view.auxiliaryPowerOnline()
                ? view.analysisComplete() ? "PURGE UNAVAILABLE"
                        : "ANALYSIS UNAVAILABLE"
                : view.cachePurgeCooldownTicks() > 0 ? "INDEX REBUILD ACTIVE"
                : view.analysisComplete() ? "PURGE SESSION CACHE"
                : "RUN FACILITY ANALYSIS";
        drawCenteredBody(poseStack, buffers, button,
                FacilityDiagnosticsScreen.ACTION_X,
                FacilityDiagnosticsScreen.ACTION_Y,
                FacilityDiagnosticsScreen.ACTION_WIDTH,
                FacilityDiagnosticsScreen.ACTION_HEIGHT,
                available ? OFF_WHITE : METAL_GRAY);

        String warningOne;
        String warningTwo;
        int warningColor;
        if (view.cachePurgeCooldownTicks() > 0) {
            warningOne = "REINDEX ACTIVE // TELEMETRY LOCKED";
            warningTwo = "EST. "
                    + formatCooldown(view.cachePurgeCooldownTicks());
            warningColor = SIGNAL_GOLD;
        } else if (view.analysisComplete()) {
            warningOne = "WARNING // REMOTE TECHNICIAN SESSIONS END";
            warningTwo = "INDEX REBUILD REQUIRES APPROX. 05:00";
            warningColor = FOUNDATION_RED;
        } else {
            warningOne = "ANALYSIS // ACQUIRE FACILITY TELEMETRY";
            warningTwo = "NETWORK DISCOVERY PATH WILL BE EXPOSED";
            warningColor = SIGNAL_GOLD;
        }
        drawCenteredCaption(poseStack, buffers, warningOne, 328, 229,
                190, warningColor);
        drawCenteredCaption(poseStack, buffers, warningTwo, 328, 240,
                190, MUTED_BLUE);

        drawHeading(poseStack, buffers, "SYSTEM LOG", 23, 291, METAL_GRAY);
        drawBodyRight(poseStack, buffers, "BUFFER 03", 518, 292,
                MUTED_BLUE);
        renderLog(poseStack, buffers, view, analyzed);

        drawBody(poseStack, buffers, "ARC48:SCIPNET>", 23, 358, MUTED_BLUE);
        if ((System.currentTimeMillis() / 500L) % 2L == 0L) {
            drawBody(poseStack, buffers, "_", 91, 358, METAL_GRAY);
        }

        drawBody(poseStack, buffers,
                focused ? "FOUNDATION INTERNAL // SESSION ACTIVE"
                        : "FOUNDATION INTERNAL // TERMINAL STANDBY",
                14, 383, MUTED_BLUE);
        drawBodyRight(poseStack, buffers, "ESC // CLOSE", 526, 383,
                MUTED_BLUE);
        poseStack.popPose();
    }

    private void renderLog(PoseStack poseStack, MultiBufferSource buffers,
            DisplayView view, boolean analyzed) {
        if (!view.auxiliaryPowerOnline()) {
            logLine(poseStack, buffers, "PWR/01",
                    "AUXILIARY POWER BUS OFFLINE", 312, FOUNDATION_RED);
            logLine(poseStack, buffers, "NET/12",
                    "LIVE FACILITY TELEMETRY SUSPENDED", 327, METAL_GRAY);
            logLine(poseStack, buffers, "SEC/07",
                    "FACILITY ANALYSIS UNAVAILABLE", 342, METAL_GRAY);
        } else if (view.cachePurgeCooldownTicks() > 0) {
            logLine(poseStack, buffers, "IDX/03",
                    "FACILITY INDEX REBUILD IN PROGRESS", 312, SIGNAL_GOLD);
            logLine(poseStack, buffers, "TEL/06",
                    "TELEMETRY CHANNELS LOCKED", 327, METAL_GRAY);
            logLine(poseStack, buffers, "SEC/07",
                    "REMOTE TECHNICIAN TOKENS INVALIDATED", 342, METAL_GRAY);
        } else if (!analyzed) {
            logLine(poseStack, buffers, "SYS/00",
                    "FACILITY ANALYSIS REQUIRED", 312, SIGNAL_GOLD);
            logLine(poseStack, buffers, "NET/12",
                    "NETWORK DISCOVERY PATH NOT EXPOSED", 327, METAL_GRAY);
            logLine(poseStack, buffers, "SEC/07",
                    "REMOTE SESSION CACHE READY", 342, METAL_GRAY);
        } else if (view.unusualNetworkActivity()) {
            logLine(poseStack, buffers, "SYS/00",
                    "FACILITY SNAPSHOT ACQUIRED", 312, OFF_WHITE);
            logLine(poseStack, buffers, "NET/47",
                    "UNUSUAL NETWORK ACTIVITY DETECTED", 327, SIGNAL_GOLD);
            logLine(poseStack, buffers, "SEC/12",
                    "CONTACT SITE NETWORK INTEGRITY FOR REVIEW", 342,
                    METAL_GRAY);
        } else {
            logLine(poseStack, buffers, "SYS/00",
                    "FACILITY SNAPSHOT ACQUIRED", 312, OFF_WHITE);
            logLine(poseStack, buffers, "NET/12",
                    "ENDPOINT REGISTRY SYNCHRONIZED", 327, METAL_GRAY);
            logLine(poseStack, buffers, "SEC/07",
                    "REMOTE SESSION CACHE READY", 342, METAL_GRAY);
        }
    }

    private void row(PoseStack poseStack, MultiBufferSource buffers,
            String label, String value, int left, int y, int right,
            int valueColor) {
        drawBody(poseStack, buffers, label, left + 5, y, METAL_GRAY);
        drawBodyRight(poseStack, buffers, value, right - 5, y, valueColor);
    }

    private void logLine(PoseStack poseStack, MultiBufferSource buffers,
            String channel, String text, int y, int color) {
        drawBody(poseStack, buffers, channel, 23, y, MUTED_BLUE);
        drawBody(poseStack, buffers, text, 67, y, color);
    }

    private void drawHeading(PoseStack poseStack, MultiBufferSource buffers,
            String text, float x, float y, int color) {
        draw(poseStack, buffers, ScpFonts.montserrat(text), x, y, color);
    }

    private void drawBody(PoseStack poseStack, MultiBufferSource buffers,
            String text, float x, float y, int color) {
        draw(poseStack, buffers, body(text), x, y, color);
    }

    private void drawBodyRight(PoseStack poseStack, MultiBufferSource buffers,
            String text, float right, float y, int color) {
        Component component = body(text);
        draw(poseStack, buffers, component, right - font.width(component), y,
                color);
    }

    private void drawCenteredBody(PoseStack poseStack,
            MultiBufferSource buffers, String text, float x, float y,
            float width, float height, int color) {
        Component component = body(text);
        float drawX = x + Math.max(0.0F,
                (width - font.width(component)) * 0.5F);
        float drawY = y + Math.max(0.0F,
                (height - font.lineHeight) * 0.5F);
        draw(poseStack, buffers, component, drawX, drawY, color);
    }

    private void drawCenteredCaption(PoseStack poseStack,
            MultiBufferSource buffers, String text, float x, float y,
            float width, int color) {
        Component component = caption(text);
        float drawX = x + Math.max(0.0F,
                (width - font.width(component)) * 0.5F);
        draw(poseStack, buffers, component, drawX, y, color);
    }

    private void draw(PoseStack poseStack, MultiBufferSource buffers,
            Component text, float x, float y, int color) {
        font.drawInBatch(text, x, y, color, false, poseStack.last().pose(),
                buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
    }

    private static Component body(String text) {
        return Component.literal(text == null ? "" : text)
                .withStyle(style -> style.withFont(TERMINAL_TEXT));
    }

    private static Component caption(String text) {
        return Component.literal(text == null ? "" : text)
                .withStyle(style -> style.withFont(TERMINAL_CAPTION));
    }

    private static float textYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 180.0F;
            case EAST -> 90.0F;
            case SOUTH -> 0.0F;
            case WEST -> -90.0F;
            default -> 180.0F;
        };
    }

    private static void renderBorder(PoseStack poseStack,
            MultiBufferSource buffers, Frame frame, BlockPos pos,
            int x, int y, int width, int height, int color) {
        int r = red(color);
        int g = green(color);
        int b = blue(color);
        renderSubQuad(poseStack, buffers, frame, pos, WHITE_PIXEL,
                x, y, width, 1, DECOR_EPSILON + 0.0001D,
                r, g, b, 255, true);
        renderSubQuad(poseStack, buffers, frame, pos, WHITE_PIXEL,
                x, y + height - 1, width, 1, DECOR_EPSILON + 0.0001D,
                r, g, b, 255, true);
        renderSubQuad(poseStack, buffers, frame, pos, WHITE_PIXEL,
                x, y, 1, height, DECOR_EPSILON + 0.0001D,
                r, g, b, 255, true);
        renderSubQuad(poseStack, buffers, frame, pos, WHITE_PIXEL,
                x + width - 1, y, 1, height, DECOR_EPSILON + 0.0001D,
                r, g, b, 255, true);
    }

    private static void renderSubQuad(PoseStack poseStack,
            MultiBufferSource buffers, Frame frame, BlockPos pos,
            ResourceLocation texture, int x, int y, int width, int height,
            double normalOffset, int r, int g, int b, int a, boolean flush) {
        double left = x / (double) FacilityDiagnosticsScreen.TEX_W - 0.5D;
        double right = (x + width)
                / (double) FacilityDiagnosticsScreen.TEX_W - 0.5D;
        double top = 0.5D - y / (double) FacilityDiagnosticsScreen.TEX_H;
        double bottom = 0.5D - (y + height)
                / (double) FacilityDiagnosticsScreen.TEX_H;
        renderQuad(poseStack, buffers, frame, pos, texture,
                left, top, right, bottom, normalOffset, r, g, b, a, flush);
    }

    private static void renderQuad(PoseStack poseStack,
            MultiBufferSource buffers, Frame frame, BlockPos pos,
            ResourceLocation texture, double left, double top, double right,
            double bottom, double normalOffset, int r, int g, int b, int a,
            boolean flush) {
        RenderType renderType = RenderType.entityTranslucentEmissive(texture);
        VertexConsumer consumer = buffers.getBuffer(renderType);
        Vec3 topLeft = local(frame.point(left, top, normalOffset), pos);
        Vec3 topRight = local(frame.point(right, top, normalOffset), pos);
        Vec3 bottomRight = local(frame.point(right, bottom, normalOffset), pos);
        Vec3 bottomLeft = local(frame.point(left, bottom, normalOffset), pos);
        Vec3 normal = frame.outward();

        vertex(consumer, poseStack, topLeft, 0.0F, 0.0F, normal, r, g, b, a);
        vertex(consumer, poseStack, topRight, 1.0F, 0.0F, normal, r, g, b, a);
        vertex(consumer, poseStack, bottomRight, 1.0F, 1.0F, normal,
                r, g, b, a);
        vertex(consumer, poseStack, bottomLeft, 0.0F, 1.0F, normal,
                r, g, b, a);

        if (flush && buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(renderType);
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack poseStack,
            Vec3 point, float u, float v, Vec3 normal,
            int r, int g, int b, int a) {
        consumer.vertex(poseStack.last().pose(), (float) point.x,
                        (float) point.y, (float) point.z)
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(poseStack.last().normal(), (float) normal.x,
                        (float) normal.y, (float) normal.z)
                .endVertex();
    }

    private static Vec3 local(Vec3 world, BlockPos pos) {
        return world.subtract(pos.getX(), pos.getY(), pos.getZ());
    }

    private static String twoDigits(int value) {
        return String.format("%02d", Math.max(0, value));
    }

    private static String formatCooldown(int ticks) {
        int totalSeconds = Math.max(0, (ticks + 19) / 20);
        return String.format("%02d:%02d", totalSeconds / 60,
                totalSeconds % 60);
    }

    private static int red(int color) {
        return color >> 16 & 0xFF;
    }

    private static int green(int color) {
        return color >> 8 & 0xFF;
    }

    private static int blue(int color) {
        return color & 0xFF;
    }

    @Override
    public boolean shouldRenderOffScreen(
            SystemTerminalBlockEntity blockEntity) {
        return true;
    }

    @Override
    public RenderType getRenderType(SystemTerminalBlockEntity animatable,
            ResourceLocation texture, MultiBufferSource bufferSource,
            float partialTick) {
        return RenderType.entityTranslucent(texture, true);
    }

    private record DisplayView(int uncontainedScps, int activeTeslaGates,
            int registeredTeslaGates, boolean teslaOverride,
            int connectedDoors, boolean auxiliaryPowerOnline,
            int cachePurgeCooldownTicks, boolean unusualNetworkActivity,
            boolean analysisComplete) { }
}
