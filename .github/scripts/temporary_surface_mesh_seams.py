from pathlib import Path
p = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java')
s = p.read_text()

def once(old, new):
    global s
    count = s.count(old)
    if count != 1:
        raise AssertionError(f'Expected exactly one occurrence, got {count}: {old[:115]!r}')
    s = s.replace(old, new, 1)

once('import net.minecraft.world.level.block.RenderShape;\n', '''import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
''')
once('''        for (Direction side : SIDES) {
            random.setSeed(42L);
            for (BakedQuad quad : model.getQuads(state, side, random,
                    ModelData.EMPTY, null)) {
                prepareQuad(minecraft, output, surface, slot, attachment,
                        normalSign, overlay, quad, lightPos, packedLight);
            }
        }
    }

    private static void prepareQuad''','''        // The authored local grid has its own neighbours; the parent-world
        // BlockPos is not a valid source of vanilla face-culling information.
        // Shared full-cube faces otherwise overlap after deformation, causing
        // the bright/dark razor-thin seams visible even within one Surface.
        boolean solidCell = fullSurfaceCell(state);
        for (Direction side : SIDES) {
            if (solidCell && internalSurfaceFace(surface, slot,
                    normalSign, overlay, side)) continue;
            random.setSeed(42L);
            for (BakedQuad quad : model.getQuads(state, side, random,
                    ModelData.EMPTY, null)) {
                if (side == null && solidCell && internalSurfaceFace(
                        surface, slot, normalSign, overlay,
                        quad.getDirection())) continue;
                prepareQuad(minecraft, output, surface, slot, attachment,
                        normalSign, overlay, quad, lightPos, packedLight);
            }
        }
    }

    private static boolean internalSurfaceFace(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, int normalSign,
            boolean overlay, Direction face) {
        if (face == null || face.getAxis() == Direction.Axis.Z) return false;
        int side = normalSign < 0 ? -1 : 1;
        int frameSign = (surface.flipped() ? -1 : 1) * side;
        int dc = face == Direction.EAST ? frameSign
                : face == Direction.WEST ? -frameSign : 0;
        int dr = face == Direction.UP ? 1
                : face == Direction.DOWN ? -1 : 0;
        ConstructionSurface.SurfaceSlot neighbour =
                new ConstructionSurface.SurfaceSlot(slot.column() + dc,
                        slot.row() + dr);
        if (neighbour.column() < 0 || neighbour.column() >= surface.columns()
                || neighbour.row() < 0 || neighbour.row() >= surface.rows()) {
            return false;
        }
        ConstructionSurface.SurfaceAttachment other = overlay
                ? surface.overlay(neighbour, normalSign)
                : surface.attachments().get(neighbour);
        return other != null && fullSurfaceCell(other.state());
    }

    private static boolean fullSurfaceCell(BlockState state) {
        if (state == null || state.isAir() || state.hasBlockEntity()
                || FacilityPipeModule.isPipe(state)) return false;
        List<AABB> boxes = state.getCollisionShape(
                EmptyBlockGetter.INSTANCE, BlockPos.ZERO,
                CollisionContext.empty()).toAabbs();
        if (boxes.size() != 1) return false;
        AABB box = boxes.get(0);
        return box.minX >= -1.0E-6D && box.minY >= -1.0E-6D
                && box.minZ >= -1.0E-6D && box.maxX <= 1.000001D
                && box.maxY <= 1.000001D && box.maxZ <= 1.000001D
                && box.minX < 1.0E-6D && box.minY < 1.0E-6D
                && box.minZ < 1.0E-6D && box.maxX > 0.999999D
                && box.maxY > 0.999999D && box.maxZ > 0.999999D;
    }

    private static void prepareQuad''')
# FacilityPipeModule has a PipeBlock type, not a global isPipe helper.
once('FacilityPipeModule.isPipe(state)',
     'state.getBlock() instanceof FacilityPipeModule.PipeBlock')
p.write_text(s)
print('Internal shared faces are culled only for complete adjacent structural cells.')
