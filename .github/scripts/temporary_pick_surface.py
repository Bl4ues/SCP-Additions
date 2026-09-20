from pathlib import Path
root=Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client')
p=root/'TransformSurfaceRaycast.java';s=p.read_text()
def once(old,new):
 global s
 assert s.count(old)==1,(old[:100],s.count(old))
 s=s.replace(old,new,1)
once('''    public static Target target(LocalPlayer player,
            Collection<ConstructionSurface> surfaces) {
        Minecraft minecraft = Minecraft.getInstance();''','''    public static Target target(LocalPlayer player,
            Collection<ConstructionSurface> surfaces) {
        return target(player, surfaces, false);
    }

    /** Pick-block must not stop at an empty construction guide in front
     * of an occupied slot. Placement intentionally still sees those guides. */
    public static Target occupiedTarget(LocalPlayer player,
            Collection<ConstructionSurface> surfaces) {
        return target(player, surfaces, true);
    }

    private static Target target(LocalPlayer player,
            Collection<ConstructionSurface> surfaces, boolean occupiedOnly) {
        Minecraft minecraft = Minecraft.getInstance();''')
once('''            Target candidate = targetSurface(surface, eye, ray,
                    Math.min(limit, bestDistance));''','''            Target candidate = targetSurface(surface, eye, ray,
                    Math.min(limit, bestDistance), occupiedOnly);''')
once('''    private static Target targetSurface(ConstructionSurface surface,
            Vec3 eye, Vec3 ray, double limit) {''','''    private static Target targetSurface(ConstructionSurface surface,
            Vec3 eye, Vec3 ray, double limit, boolean occupiedOnly) {''')
once('''                        Target candidate = targetCell(surface, column, row,
                                eye, ray, Math.min(limit, bestDistance));''','''                        Target candidate = targetCell(surface, column, row,
                                eye, ray, Math.min(limit, bestDistance),
                                occupiedOnly);''')
once('''    private static Target targetCell(ConstructionSurface surface,
            int column, int row, Vec3 eye, Vec3 ray, double limit) {''','''    private static Target targetCell(ConstructionSurface surface,
            int column, int row, Vec3 eye, Vec3 ray, double limit,
            boolean occupiedOnly) {''')
once('''        if (best != null) return best;

        // Empty authored cell: the guide itself is the placement plane.''','''        if (best != null || occupiedOnly) return best;

        // Empty authored cell: the guide itself is the placement plane.''')
p.write_text(s)
p=root/'TransformPickBlockClient.java';s=p.read_text();old='''        TransformSurfaceRaycast.Target surface = TransformSurfaceRaycast.target(
                player, TransformConstructionClientState.surfaces(''';new='''        TransformSurfaceRaycast.Target surface = TransformSurfaceRaycast.occupiedTarget(
                player, TransformConstructionClientState.surfaces(''';assert s.count(old)==1;s=s.replace(old,new,1);p.write_text(s)
print('Pick-block now raycasts occupied Surface slots, ignoring empty guides.')
