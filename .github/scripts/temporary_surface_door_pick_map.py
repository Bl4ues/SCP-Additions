from pathlib import Path
import sys

MODE = sys.argv[1]
ROOT = Path('src/main/java/com/bl4ues/scpclassifieddirective')

def replace_once(text, old, new):
    count = text.count(old)
    if count != 1:
        raise AssertionError(f'Expected one anchor, found {count}: {old[:110]!r}')
    return text.replace(old, new, 1)

if MODE == 'map':
    file = ROOT / 'client/scp079/Scp079FacilityMapScreen.java'
    text = file.read_text()
    start = text.index('    private void renderClosedKeycardBadge(')
    end = text.index('    private void drawFixedCenteredMapLabel(', start)
    text = text[:start] + '''    private void renderClosedKeycardBadge(GuiGraphics graphics,
            MapDoorMarker marker, MapTransform transform,
            boolean locked, int color) {
        int segmentColor = locked ? 0xFF89989D : color;
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(transform.fx(marker.x()), transform.fy(marker.z()), 0.0D);
        // Both the badge and its digit live in one map-space transform. Never
        // round the badge dimensions independently of the glyph scale.
        float mapScale = clearanceMapScale(transform);
        pose.scale(mapScale, mapScale, 1.0F);
        graphics.fill(-9, -9, 9, 9, segmentColor);
        drawClearanceDigit(graphics, marker.requiredLevel(), 0xFF07151C);
        pose.popPose();
    }

    private void renderOpenKeycardLevel(GuiGraphics graphics,
            MapDoorMarker marker, MapTransform transform, int color) {
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(transform.fx(marker.x()), transform.fy(marker.z()), 0.0D);
        pose.scale(clearanceMapScale(transform),
                clearanceMapScale(transform), 1.0F);
        drawClearanceDigit(graphics, marker.requiredLevel(), 0xFFFFFFFF);
        pose.popPose();
    }

    private static float clearanceMapScale(MapTransform transform) {
        // At close range the 18px badge is fully legible; at longer range the
        // ENTIRE symbol shrinks with the authored map instead of acting as HUD.
        return (float) Mth.clamp(transform.scale() * (0.38D / 9.0D),
                0.16D, 1.0D);
    }

    private void drawClearanceDigit(GuiGraphics graphics,
            int requiredLevel, int color) {
        String value = Integer.toString(requiredLevel);
        var pose = graphics.pose();
        pose.pushPose();
        pose.scale(1.5F, 1.5F, 1.0F);
        // Font glyphs are 8px high in a 9px line box. Center the ink, rather
        // than adding an unrelated screen-space baseline offset at each zoom.
        graphics.drawString(font, value, -font.width(value) / 2, -4,
                color, false);
        pose.popPose();
    }

''' + text[end:]
    file.write_text(text)
    print('Map clearance badge and number now share one map-space transform.')
elif MODE == 'pick':
    file = ROOT / 'facility/transform/client/TransformPickBlockClient.java'
    text = file.read_text()
    text = replace_once(text,
        'import net.minecraftforge.fml.common.Mod;\n',
        'import net.minecraftforge.fml.common.Mod;\nimport org.lwjgl.glfw.GLFW;\n')
    start = text.index('    @SubscribeEvent(priority = EventPriority.HIGHEST)')
    text = text[:start] + '''    /**
     * Forge's interaction-key event can be skipped when vanilla reports MISS
     * for a visual Off-Grid/Surface block (there is no vanilla target block).
     * Handle the physical middle click before vanilla's target-dependent path.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMiddleClick(InputEvent.MouseButton.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getAction() != GLFW.GLFW_PRESS
                || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_MIDDLE
                || !minecraft.options.keyPickItem.matchesMouse(event.getButton())) {
            return;
        }
        if (pickTransformedBlock(minecraft)) event.setCanceled(true);
    }

    /** Also support a remapped pick-block key when Forge dispatches it. */
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isPickBlock()) return;
        if (!pickTransformedBlock(Minecraft.getInstance())) return;
        event.setCanceled(true);
        event.setSwingHand(false);
    }

    private static boolean pickTransformedBlock(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.gameMode == null
                || minecraft.screen != null || !player.isCreative()) return false;

        TransformGroupPlacementClient.PayloadTarget group =
                TransformGroupPlacementClient.findBreakTarget(player);
        TransformSurfaceRaycast.Target surface = TransformSurfaceRaycast.occupiedTarget(
                player, TransformConstructionClientState.surfaces(
                        minecraft.level.dimension().location()));

        BlockState groupState = group == null ? null : group.state();
        BlockState surfaceState = null;
        if (surface != null) {
            ConstructionSurface.SurfaceAttachment attachment =
                    surface.layer().overlay()
                            ? surface.surface().overlay(surface.slot(),
                                    surface.normalSign())
                            : surface.surface().attachments().get(surface.slot());
            if (attachment != null && !attachment.state().isAir()) {
                surfaceState = attachment.state();
            } else {
                surface = null;
            }
        }

        double groupDistance = group == null
                ? Double.POSITIVE_INFINITY : group.distance();
        double surfaceDistance = surface == null
                ? Double.POSITIVE_INFINITY : surface.distance();
        BlockState state = groupDistance <= surfaceDistance
                ? groupState : surfaceState;
        if (state == null || state.isAir()) return false;
        ItemStack picked = FacilityPipeModule.pick(state);
        if (picked.isEmpty()) picked = state.getBlock().asItem().getDefaultInstance();
        if (picked.isEmpty()) return false;

        player.getInventory().setPickedItem(picked);
        minecraft.gameMode.handleCreativeModeItemAdd(
                player.getInventory().getSelected(),
                36 + player.getInventory().selected);
        return true;
    }
}
'''
    file.write_text(text)
    print('Middle-click now handles transformed blocks even when vanilla misses.')
else:
    raise ValueError(f'Unknown patch mode: {MODE}')
