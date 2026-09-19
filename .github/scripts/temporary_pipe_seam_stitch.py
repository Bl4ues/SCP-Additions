from pathlib import Path
p=Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java')
s=p.read_text()
def change(old,new):
 global s
 assert s.count(old)==1,(old,s.count(old))
 s=s.replace(old,new)
change('import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;', 'import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;\nimport com.bl4ues.scpclassifieddirective.facility.FacilityPipeModule;')
change('import net.minecraft.world.level.block.state.BlockState;', 'import net.minecraft.world.level.block.state.BlockState;\nimport net.minecraft.world.level.block.HorizontalDirectionalBlock;')
change('''        DIRTY_SURFACE_SLOTS.computeIfAbsent(id,
                ignored -> new java.util.LinkedHashSet<>()).add(slot);''', '''        Set<ConstructionSurface.SurfaceSlot> dirty =
                DIRTY_SURFACE_SLOTS.computeIfAbsent(id,
                        ignored -> new java.util.LinkedHashSet<>());
        dirty.add(slot);
        // A pipe's exposed end face depends on the next segment even if the
        // bracket state on the existing segment stays unchanged. Only rebake
        // its two immediate neighbours, never the full curved wall.
        dirty.add(new ConstructionSurface.SurfaceSlot(
                slot.column() - 1, slot.row()));
        dirty.add(new ConstructionSurface.SurfaceSlot(
                slot.column() + 1, slot.row()));''')
change('''        int xSteps = TransformSurfaceGeometry.effectiveDeform(attachment)
                && maxX - minX > 0.20D ? 4 : 1;''', '''        // Adjoining pipe end caps lie on the exact same deformed cell seam.
        // Drawing both internal caps there causes flicker, even when the
        // tubular side faces have perfectly matching vertices. Keep the cap
        // only at the exposed end of a run, across all three pipe finishes.
        if (attachment.state().getBlock() instanceof FacilityPipeModule.PipeBlock
                && maxX - minX < 1.0E-4D
                && (Math.abs(minX) < 1.0E-4D
                    || Math.abs(minX - 1.0D) < 1.0E-4D)
                && pipeNeighbor(surface, slot, attachment.state(),
                        normalSign, overlay, minX > 0.5D)) {
            return;
        }

        int xSteps = TransformSurfaceGeometry.effectiveDeform(attachment)
                && maxX - minX > 0.20D ? 4 : 1;''')
needle='''    private static void emitSurfaceVertex(List<PreparedVertex> output,'''
method='''    private static boolean pipeNeighbor(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, BlockState state,
            int normalSign, boolean overlay, boolean localRight) {
        int side = normalSign < 0 ? -1 : 1;
        int physicalDirection = (surface.flipped() ? -1 : 1) * side;
        ConstructionSurface.SurfaceSlot adjacent =
                new ConstructionSurface.SurfaceSlot(
                        slot.column() + (localRight ? 1 : -1)
                                * physicalDirection,
                        slot.row());
        ConstructionSurface.SurfaceAttachment other = overlay
                ? surface.overlay(adjacent, normalSign)
                : surface.attachments().get(adjacent);
        return other != null
                && other.state().getBlock()
                        instanceof FacilityPipeModule.PipeBlock
                && other.state().getValue(HorizontalDirectionalBlock.FACING)
                        == state.getValue(HorizontalDirectionalBlock.FACING);
    }

'''
change(needle,method+needle)
p.write_text(s)
