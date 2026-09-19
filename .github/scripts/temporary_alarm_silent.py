from pathlib import Path
r=Path('src/main/java/com/bl4ues/scpclassifieddirective')
def patch(path, old, new, count=1):
 p=r/path;s=p.read_text();assert s.count(old)==count,(path,old,s.count(old));p.write_text(s.replace(old,new))

path='facility/alarm/AlarmModule.java'
patch(path,'import com.bl4ues.scpclassifieddirective.facility.blastdoor.BlastDoorStructure;', 'import com.bl4ues.scpclassifieddirective.facility.blastdoor.BlastDoorStructure;\nimport com.bl4ues.scpclassifieddirective.item.ScrewdriverItem;')
patch(path,'import net.minecraft.world.item.ItemStack;', 'import net.minecraft.world.item.ItemStack;\nimport net.minecraft.world.InteractionHand;\nimport net.minecraft.world.phys.BlockHitResult;')
patch(path,'    public static final BooleanProperty ACTIVE =\n            BooleanProperty.create("active");', '    public static final BooleanProperty ACTIVE =\n            BooleanProperty.create("active");\n    /** Configuration survives save/load and is shared by normal and transformed Alarms. */\n    public static final BooleanProperty SILENT =\n            BooleanProperty.create("silent");')
patch(path, '                    .setValue(ACTIVE, false)\n                    .setValue(MOUNT_X,', '                    .setValue(ACTIVE, false)\n                    .setValue(SILENT, false)\n                    .setValue(MOUNT_X,')
patch(path,'            builder.add(FACING, ACTIVE, MOUNT_X, MOUNT_Y);', '            builder.add(FACING, ACTIVE, SILENT, MOUNT_X, MOUNT_Y);')
patch(path, '                    .setValue(ACTIVE, false)\n                    .setValue(MOUNT_X,', '                    .setValue(ACTIVE, false)\n                    .setValue(SILENT, false)\n                    .setValue(MOUNT_X,') if False else None
# The second placement state uses the same default state and already inherits SILENT=false.
patch(path,'        @Override\n        public void onPlace(BlockState state, Level level, BlockPos pos,\n                BlockState oldState, boolean moving) {','''        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos,
                Player player, InteractionHand hand, BlockHitResult hit) {
            if (!(player.getItemInHand(hand).getItem() instanceof ScrewdriverItem)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(SILENT,
                        !state.getValue(SILENT)), Block.UPDATE_CLIENTS);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        public void onPlace(BlockState state, Level level, BlockPos pos,
                BlockState oldState, boolean moving) {''')
patch(path,'            AlarmAudioClient.update(level, pos, active);','            AlarmAudioClient.update(level, pos, active && !state.getValue(SILENT));')
patch(path,'            tooltip.add(Component.translatable(\n                    "tooltip.scp_classified_directive.alarm")\n                    .withStyle(ChatFormatting.GRAY));', '''            tooltip.add(Component.translatable(
                    "tooltip.scp_classified_directive.alarm")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal(
                    "Use a Screwdriver to toggle alarm sound without disabling its light.")
                    .withStyle(ChatFormatting.GRAY));''')

path='facility/transform/client/TransformAlarmAudioClient.java'
patch(path,'                && state.getValue(AlarmModule.ACTIVE);','                && state.getValue(AlarmModule.ACTIVE)\n                && !state.getValue(AlarmModule.SILENT);')
patch(path,'                    || !state.getValue(AlarmModule.ACTIVE)) {','                    || !state.getValue(AlarmModule.ACTIVE)\n                    || state.getValue(AlarmModule.SILENT)) {')
patch(path,'            boolean active = state.hasProperty(AlarmModule.ACTIVE)\n                    && state.getValue(AlarmModule.ACTIVE);','            boolean active = state.hasProperty(AlarmModule.ACTIVE)\n                    && state.getValue(AlarmModule.ACTIVE)\n                    && !state.getValue(AlarmModule.SILENT);')

path='inventory/context/ContextInteractionRegistry.java'
patch(path, '        for (String path : List.of("scp_131_a", "scp_131_b", "roomba")) {', '''        count += addToolVariant(configuredIdentities, Kind.BLOCK,
                new ResourceLocation(ScpClassifiedDirectiveMod.MODID, "alarm"),
                "configure_alarm_sound", "Toggle Sound", screwdriver, 90);

        for (String path : List.of("scp_131_a", "scp_131_b", "roomba")) {''')

path='facility/transform/TransformControlRuntime.java'
patch(path,'import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;', '''import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.alarm.AlarmModule;
import com.bl4ues.scpclassifieddirective.item.ScrewdriverItem;''')
patch(path,'        BlockState state = group.cells().get(cell);\n        if (!control(state)) return false;', '''        BlockState state = group.cells().get(cell);
        if (toggleAlarmSound(player, level, state,
                ControlHit.group(group, cell, state, group.cellCenter(cell)))) {
            return true;
        }
        if (!control(state)) return false;''')
patch(path,'        if (attachment == null || !control(attachment.state())) return false;\n        Vec3 center = surfaceCenter(surface, slot);', '''        if (attachment == null) return false;
        Vec3 center = surfaceCenter(surface, slot);
        if (toggleAlarmSound(player, level, attachment.state(),
                ControlHit.surface(surface, slot, attachment.state(), center))) {
            return true;
        }
        if (!control(attachment.state())) return false;''')
patch(path,'        if (attachment == null || !control(attachment.state())) return false;\n        Vec3 center = surfaceCenter(surface, slot, side);', '''        if (attachment == null) return false;
        Vec3 center = surfaceCenter(surface, slot, side);
        if (toggleAlarmSound(player, level, attachment.state(),
                ControlHit.surface(surface, slot, side,
                        attachment.state(), center))) {
            return true;
        }
        if (!control(attachment.state())) return false;''')
patch(path,'    private static boolean activate(ServerLevel level, ControlHit hit) {', '''    private static boolean toggleAlarmSound(ServerPlayer player,
            ServerLevel level, BlockState state, ControlHit hit) {
        if (!AlarmModule.isController(state)
                || !(player.getMainHandItem().getItem() instanceof ScrewdriverItem
                    || player.getOffhandItem().getItem() instanceof ScrewdriverItem)
                || player.getEyePosition().distanceToSqr(hit.center()) > 36.0D) {
            return false;
        }
        set(level, hit, state.setValue(AlarmModule.SILENT,
                !state.getValue(AlarmModule.SILENT)));
        return true;
    }

    private static boolean activate(ServerLevel level, ControlHit hit) {''')
