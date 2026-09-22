from pathlib import Path

ROOT = Path('src/main/java/com/bl4ues/scpclassifieddirective')

def patch(path, old, new, count=1):
    source = Path(path)
    text = source.read_text()
    actual = text.count(old)
    if actual != count:
        raise RuntimeError(f'{source}: expected {count} instances of {old[:70]!r}, got {actual}')
    source.write_text(text.replace(old, new))

hud = ROOT / 'client/BuilderToolGuideHud.java'
patch(hud,
'''                lines.add(new Line("LMB",
                        "select handle / drag colored arrow"));''',
'''                lines.add(new Line("B", "link two existing Surface edges"));
                lines.add(new Line("LMB",
                        "select handle / drag colored arrow"));''')

bridge = ROOT / 'facility/transform/client/TransformContextTargetClient.java'
patch(bridge,
'''import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceGeometry;
''',
'''import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceGeometry;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformWallFixturePlacement;
''')
patch(bridge,
'''import net.minecraft.world.phys.Vec3;
''',
'''import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
''')
patch(bridge,
'''    private TransformContextTargetClient() {
    }
''',
'''    private TransformContextTargetClient() {
    }

    // A proximity prompt must not raycast every full cube in the facility each
    // tick. Cache only actual door-button addresses, rebuilding the tiny index
    // when a group/surface snapshot is replaced by its authoritative state.
    private record GroupButtons(TransformGroup source,
            List<TransformGroup.GridPos> addresses) { }
    private record SurfaceButton(ConstructionSurface.SurfaceSlot slot,
            boolean overlay, int normalSign) { }
    private record SurfaceButtons(ConstructionSurface source,
            List<SurfaceButton> addresses) { }
    private static final Map<UUID, GroupButtons> GROUP_BUTTONS = new HashMap<>();
    private static final Map<UUID, SurfaceButtons> SURFACE_BUTTONS = new HashMap<>();

    public static void clearButtonCache() {
        GROUP_BUTTONS.clear();
        SURFACE_BUTTONS.clear();
    }

    /** Nearby authored buttons share vanilla's proximity/offscreen prompt
     * behavior. Other fixtures keep precise raycast targeting. */
    public static List<Target> nearbyDoorButtons(LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (player == null || minecraft.level == null) return List.of();
        Vec3 eye = player.getEyePosition(1.0F);
        var dimension = minecraft.level.dimension().location();
        List<Target> found = new ArrayList<>();
        for (TransformGroup group : TransformConstructionClientState.groups(dimension)) {
            GroupButtons cached = GROUP_BUTTONS.get(group.id());
            if (cached == null || cached.source() != group) {
                List<TransformGroup.GridPos> addresses = new ArrayList<>();
                group.cells().forEach((cell, state) -> {
                    if (TransformWallFixturePlacement.isDoorButton(state))
                        addresses.add(cell);
                });
                cached = new GroupButtons(group, List.copyOf(addresses));
                GROUP_BUTTONS.put(group.id(), cached);
            }
            if (cached.addresses().isEmpty()) continue;
            Vec3 eyeLocal = TransformMath.worldToLocal(group.origin(), eye,
                    group.rotationX(), group.rotationY(), group.rotationZ());
            for (TransformGroup.GridPos cell : cached.addresses()) {
                BlockState state = group.cells().get(cell);
                if (state == null || state.isAir()) continue;
                TransformGroup.GridPos visual =
                        TransformWallFixturePlacement.visualCell(cell, state);
                Vec3 local = new Vec3(visual.x(), visual.y(), visual.z());
                double distanceSqr = local.distanceToSqr(eyeLocal);
                if (distanceSqr > 64.0D) continue;
                found.add(new Target(Kind.GROUP, state,
                        Math.sqrt(distanceSqr), group.id(), cell,
                        null, null, 0));
            }
        }
        for (ConstructionSurface surface :
                TransformConstructionClientState.surfaces(dimension)) {
            SurfaceButtons cached = SURFACE_BUTTONS.get(surface.id());
            if (cached == null || cached.source() != surface) {
                List<SurfaceButton> addresses = new ArrayList<>();
                surface.attachments().forEach((slot, attachment) -> {
                    if (TransformWallFixturePlacement.isDoorButton(
                            attachment.state())) {
                        addresses.add(new SurfaceButton(slot, false,
                                TransformSurfaceGeometry.MAIN_SIDE));
                    }
                });
                surface.overlays().forEach((slot, attachment) -> {
                    if (TransformWallFixturePlacement.isDoorButton(
                            attachment.state())) {
                        addresses.add(new SurfaceButton(slot.slot(), true,
                                slot.normalSign()));
                    }
                });
                cached = new SurfaceButtons(surface, List.copyOf(addresses));
                SURFACE_BUTTONS.put(surface.id(), cached);
            }
            for (SurfaceButton address : cached.addresses()) {
                ConstructionSurface.SurfaceAttachment attachment =
                        address.overlay()
                        ? surface.overlay(address.slot(), address.normalSign())
                        : surface.attachments().get(address.slot());
                if (attachment == null || attachment.state().isAir()) continue;
                Vec3 center = TransformSurfaceGeometry.logicalPoint(surface,
                        address.slot(), false, address.normalSign(),
                        address.overlay(), 0.5D, 0.5D, 0.5D);
                double distanceSqr = center.distanceToSqr(eye);
                if (distanceSqr > 64.0D) continue;
                found.add(new Target(address.overlay()
                        ? Kind.SURFACE_OVERLAY : Kind.SURFACE_MAIN,
                        attachment.state(), Math.sqrt(distanceSqr),
                        null, null, surface.id(), address.slot(),
                        address.normalSign()));
            }
        }
        return found;
    }
''')

client = ROOT / 'inventory/client/ContextPromptClient.java'
patch(client,
'''import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformContextTargetClient;
''',
'''import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformContextTargetClient;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformWallFixturePlacement;
''')
patch(client,
'''import java.util.List;
''',
'''import java.util.ArrayList;
import java.util.List;
''')
start = '''    private static ContextTarget findTransformedTarget(Minecraft minecraft,
            LocalPlayer player) {'''
end = '''    private static ContextTarget findBlockTarget(Minecraft minecraft,
            LocalPlayer player) {'''
text = client.read_text()
a = text.index(start)
b = text.index(end, a)
new = '''    private static ContextTarget findTransformedTarget(Minecraft minecraft,
            LocalPlayer player) {
        List<TransformContextTargetClient.Target> candidates =
                new ArrayList<>(TransformContextTargetClient.nearbyDoorButtons(player));
        TransformContextTargetClient.Target aimed =
                TransformContextTargetClient.find(player);
        if (aimed != null) candidates.add(aimed);
        if (candidates.isEmpty()) return null;

        ContextTarget best = null;
        double bestScore = Double.MAX_VALUE;
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F).normalize();
        for (TransformContextTargetClient.Target transformed : candidates) {
            BlockState state = transformed.state();
            boolean doorButton = TransformWallFixturePlacement.isDoorButton(state);
            for (ContextInteractionRegistry.Rule rule
                    : ContextInteractionRegistry.getBlockRules(state.getBlock())) {
                if (!rule.isHeldItemSatisfied(player)) continue;
                Vec3 anchor = TransformContextTargetClient.anchor(transformed, rule);
                if (anchor == null || !Double.isFinite(anchor.x)
                        || !Double.isFinite(anchor.y)
                        || !Double.isFinite(anchor.z)) continue;
                double aimRadius = Math.min(0.34D,
                        Math.max(0.16D, rule.range() * 0.13D));
                boolean offscreen = doorButton || rule.allowOffscreen();
                double score = scorePoint(anchor, eye, look, rule.range(),
                        false, rule.priority(), !doorButton,
                        aimRadius * aimRadius, offscreen);
                if (rule.hasRequiredItem()) score -= 0.12D;
                if (score >= bestScore) continue;
                String name = rule.showName() ? rule.blockName(state) : "";
                boolean showName = rule.showName() && !name.isEmpty();
                boolean showAction = !doorButton && rule.showAction()
                        && rule.action() != null && !rule.action().isBlank();
                ResourceLocation icon = ContextPromptIcons.resolve(
                        rule.icon(), rule.id());
                boolean allowUse = rule.allowRightClick() || rule.allowE();
                bestScore = score;
                best = new ContextTarget(BlockPos.containing(anchor),
                        0, false, anchor, rule.interactionKey(),
                        rule.action(), name, showAction, showName,
                        allowUse, icon, (float) rule.promptScale(),
                        offscreen, score, transformed);
            }
        }
        return best;
    }

'''
client.write_text(text[:a] + new + text[b:])

state = ROOT / 'facility/transform/client/TransformConstructionClientState.java'
patch(state,
'''        TransformAlarmAudioClient.clear();
        TransformAlarmClientRenderer.resetIndices();
''',
'''        TransformAlarmAudioClient.clear();
        TransformAlarmClientRenderer.resetIndices();
        TransformContextTargetClient.clearButtonCache();
''')
