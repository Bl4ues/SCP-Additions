from pathlib import Path
import json

JAVA = Path('src/main/java/com/bl4ues/scpclassifieddirective')
RES = Path('src/main/resources')

legacy_fields = {
    'SCP_914BLOCK', 'SCP_914CLOCKWORKS', 'SCP_914BODY',
    'SCP_914DIAL_1TO_1', 'SCP_914_KEY_WIND', 'SCP_914_INTAKE',
    'SCP_914_OUTPUT', 'SCP_914_INTAKE_DOOR', 'SCP_914_OUTPUT_DOOR',
    'SCP_914DIAL_ROUGH', 'SCP_914DIAL_COARSE', 'SCP_914DIAL_FINE',
    'SCP_914DIAL_VERY_FINE', 'SCP_914CLOCKWORKS_2',
    'SCP_914_OUTPUT_DOOR_CLOSED', 'SCP_914_INTAKE_DOOR_CLOSED',
    'SCP_914_ASSEMBLY_KIT',
}
legacy_classes = {
    'Scp914dialVeryFineBlock', 'Scp914dialRoughBlock', 'Scp914dialFineBlock',
    'Scp914dialCoarseBlock', 'Scp914dial1to1Block', 'Scp914clockworksBlock',
    'Scp914clockworks2Block', 'Scp914bodyBlock', 'Scp914blockBlock',
    'Scp914OutputDoorClosedBlock', 'Scp914OutputDoorBlock', 'Scp914OutputBlock',
    'Scp914KeyWindBlock', 'Scp914IntakeDoorClosedBlock', 'Scp914IntakeDoorBlock',
    'Scp914IntakeBlock', 'Scp914AssemblyKitItem', 'Scp914GuiScreen',
    'Scp914GuiMenu', 'Scp914GuiButtonMessage',
}
legacy_ids = {
    'scp_914block', 'scp_914clockworks', 'scp_914clockworks_2', 'scp_914body',
    'scp_914dial_1to_1', 'scp_914dial_rough', 'scp_914dial_coarse',
    'scp_914dial_fine', 'scp_914dial_very_fine', 'scp_914_key_wind',
    'scp_914_intake', 'scp_914_output', 'scp_914_intake_door',
    'scp_914_output_door', 'scp_914_intake_door_closed',
    'scp_914_output_door_closed', 'scp_914_assembly_kit', 'scp_914_gui',
}
legacy_screen_assets = {'scp_914_gui.png', 'scp_914_dial.png', 'scp_914_dial_hover.png'}

# Generated block/item registries: remove the old component declarations/imports.
for rel in [
    'src/main/java/com/bl4ues/scpclassifieddirective/init/ScpClassifiedDirectiveModBlocks.java',
    'src/main/java/com/bl4ues/scpclassifieddirective/init/ScpClassifiedDirectiveModItems.java',
]:
    path = Path(rel)
    lines = path.read_text().splitlines(keepends=True)
    path.write_text(''.join(
        line for line in lines
        if not any(field in line for field in legacy_fields)
        and not any(cls in line for cls in legacy_classes)
    ))

# The old GUI/menu was only a dial proxy. The physical dial replaces it.
menus = Path('src/main/java/com/bl4ues/scpclassifieddirective/init/ScpClassifiedDirectiveModMenus.java')
text = menus.read_text()
text = '\n'.join(line for line in text.splitlines()
                 if 'Scp914GuiMenu' not in line and 'SCP_914_GUI' not in line) + '\n'
menus.write_text(text)

screens = Path('src/main/java/com/bl4ues/scpclassifieddirective/init/ScpClassifiedDirectiveModScreens.java')
text = screens.read_text()
text = '\n'.join(line for line in text.splitlines()
                 if 'Scp914GuiScreen' not in line and 'SCP_914_GUI' not in line) + '\n'
screens.write_text(text)

# Scp914Processor is now only the reusable transformation engine consumed by
# Scp914CycleProcessor. All old block scans, door swaps, offsets, GUI state and
# global refining flags are deliberately gone.
processor = Path('src/main/java/com/bl4ues/scpclassifieddirective/data/Scp914Processor.java')
processor.write_text(r'''package com.bl4ues.scpclassifieddirective.data;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModSounds;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/** Reusable SCP-914 transformation operations for the rebuilt physical machine. */
public final class Scp914Processor {
    public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(ScpClassifiedDirectiveMod.MODID, "scp914"));

    private Scp914Processor() {
    }

    public static void processPlayer(ServerPlayer player, Vec3 outputCenter,
            Scp914RecipeManager.Setting setting) {
        if (!isAvailable(player)) return;

        player.connection.teleport(outputCenter.x, outputCenter.y, outputCenter.z,
                player.getYRot(), player.getXRot());

        switch (setting) {
            case ROUGH -> {
                hurtWithMessage(player, 18.0F, "scp914rough");
                ScpClassifiedDirectiveMod.queueServerWork(10, () -> {
                    if (isAvailable(player)) hurtWithMessage(player, 50.0F, "scp914rough");
                });
            }
            case COARSE -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        200, 3, false, false));
                hurtWithMessage(player, 18.0F, "scp914coarse");
                ScpClassifiedDirectiveMod.queueServerWork(200, () -> {
                    if (isAvailable(player)) hurtWithMessage(player, 50.0F, "scp914coarse");
                });
            }
            case ONE_TO_ONE -> {
                Scp914SkinManager.assignRandomSkin(player);
                awardMetamorphosisAdvancement(player);
            }
            case FINE -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                        200, 1, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP,
                        200, 1, false, false));
                ScpClassifiedDirectiveMod.queueServerWork(200, () -> {
                    if (isAvailable(player)) hurtWithMessage(player, 50.0F, "scp914fine");
                });
            }
            case VERY_FINE -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                        300, 5, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP,
                        300, 1, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST,
                        300, 7, false, false));
                ScpClassifiedDirectiveMod.queueServerWork(300, () -> {
                    if (isAvailable(player)) hurtWithMessage(player, 80.0F, "scp914veryfine");
                });
            }
        }
    }

    private static boolean isAvailable(ServerPlayer player) {
        return player != null && !player.isRemoved() && player.isAlive()
                && player.connection != null;
    }

    private static void hurtWithMessage(ServerPlayer player, float amount,
            String translationKey) {
        var damageRegistry = player.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE);
        var genericType = damageRegistry.getHolderOrThrow(DamageTypes.GENERIC);
        DamageSource source = new DamageSource(
                damageRegistry.getHolderOrThrow(DAMAGE_TYPE)) {
            @Override
            public boolean is(TagKey<DamageType> tag) {
                return super.is(tag) || genericType.is(tag);
            }

            @Override
            public Component getLocalizedDeathMessage(LivingEntity entity) {
                return Component.translatable("death.attack." + translationKey,
                        entity.getDisplayName());
            }
        };
        boolean wasAlive = player.isAlive();
        boolean damaged = player.hurt(source, amount);
        if (damaged && wasAlive && player.isDeadOrDying()
                && !"scp914coarse".equals(translationKey)) {
            player.level().playSound(null, player.blockPosition(),
                    ScpClassifiedDirectiveModSounds.SCP914DEATH.get(),
                    SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
    }

    private static void awardMetamorphosisAdvancement(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        Advancement advancement = server.getAdvancements().getAdvancement(
                new ResourceLocation("scp_classified_directive", "scp_914_metamorphosis"));
        if (advancement == null) return;
        AdvancementProgress progress = player.getAdvancements()
                .getOrStartProgress(advancement);
        if (!progress.isDone()) {
            for (String criterion : progress.getRemainingCriteria()) {
                player.getAdvancements().award(advancement, criterion);
            }
        }
    }

    public static void applyRecipe(ServerLevel level, Vec3 outputCenter,
            Scp914RecipeManager.RecipeMatch match) {
        if (level.random.nextFloat() > match.recipe().chance()) {
            consumeInputs(match);
            return;
        }

        ItemStack firstInputStack = match.firstInputStack();
        consumeInputs(match);

        for (Scp914RecipeManager.ItemOutput output :
                Scp914RecipeManager.rollItemOutputs(match.recipe(), level.random)) {
            ItemStack outputStack = Scp914RecipeManager.createItemOutput(
                    output, firstInputStack, match.recipe().copyInputNbt());
            if (!outputStack.isEmpty()) {
                ItemEntity outputEntity = new ItemEntity(level, outputCenter.x,
                        outputCenter.y, outputCenter.z, outputStack);
                outputEntity.setPickUpDelay(10);
                level.addFreshEntity(outputEntity);
            }
        }

        for (Scp914RecipeManager.EntityOutput output : match.recipe().entityOutputs()) {
            Optional<EntityType<?>> type = Scp914RecipeManager.getEntityType(output);
            if (type.isEmpty()) {
                ScpClassifiedDirectiveMod.LOGGER.warn(
                        "SCP-914 recipe {} points to missing entity output {}",
                        match.recipe().id(), output.entity());
                continue;
            }
            for (int i = 0; i < output.count(); i++) {
                Entity spawned = type.get().spawn(level,
                        BlockPos.containing(outputCenter), MobSpawnType.MOB_SUMMONED);
                if (spawned != null) spawned.setDeltaMovement(0, 0, 0);
            }
        }
    }

    private static void consumeInputs(Scp914RecipeManager.RecipeMatch match) {
        for (Scp914RecipeManager.ItemUse itemUse : match.itemUses()) {
            ItemStack stack = itemUse.entity().getItem();
            stack.shrink(itemUse.count());
            if (stack.isEmpty()) itemUse.entity().discard();
            else itemUse.entity().setItem(stack);
        }
        for (Scp914RecipeManager.EntityUse entityUse : match.entityUses()) {
            if (entityUse.consume()) entityUse.entity().discard();
        }
    }

    public static void consumeLooseItems(List<ItemEntity> items) {
        for (ItemEntity item : items) {
            if (item != null && !item.isRemoved()) item.discard();
        }
    }
}
''')

# Delete the obsolete component classes, GUI/menu/network packet, and all old
# generated SCP-914 procedures. The rebuilt implementation lives in scp914/.
for cls in legacy_classes:
    for path in list(JAVA.rglob(cls + '.java')):
        path.unlink()

procedures_dir = JAVA / 'procedures'
if procedures_dir.exists():
    for path in list(procedures_dir.glob('Scp914*.java')):
        path.unlink()

for rel in [
    'src/main/java/com/bl4ues/scpclassifieddirective/client/gui/Scp914GuiScreen.java',
    'src/main/java/com/bl4ues/scpclassifieddirective/world/inventory/Scp914GuiMenu.java',
    'src/main/java/com/bl4ues/scpclassifieddirective/network/Scp914GuiButtonMessage.java',
]:
    path = Path(rel)
    if path.exists(): path.unlink()

# Remove old registry-id-specific resources while preserving the new scp_914
# item/block and scp914 GeckoLib model, animation and PBR textures.
for path in list(RES.rglob('*')):
    if not path.is_file():
        continue
    lower = path.name.lower()
    if lower in legacy_screen_assets or any(token in lower for token in legacy_ids):
        path.unlink()

# Remove language entries for deleted component/menu ids.
lang_dir = RES / 'assets/scp_classified_directive/lang'
if lang_dir.exists():
    for path in lang_dir.glob('*.json'):
        try:
            data = json.loads(path.read_text())
        except Exception:
            continue
        changed = False
        for key in list(data):
            lower = key.lower()
            if any(token in lower for token in legacy_ids):
                del data[key]
                changed = True
        if changed:
            path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n')

# Normal 4.0 feature section, intentionally not a Highlight.
changelog = Path('CHANGELOG.md')
text = changelog.read_text()
section = '''## SCP-914

- Completely rebuilt SCP-914 as a single large GeckoLib machine with hidden multiblock reservation and collision cells instead of the former collection of visible component blocks;
- Added obstruction-aware placement for the full machine footprint, with blocked cells highlighted before placement;
- Reworked the configuration dial into a physical contextual control that can be held and dragged directly on the placed model, with smooth client motion, mechanical detents, server-authoritative settings, gear feedback, and snap-to-setting release behavior;
- Reworked the winding key into a physical contextual **Start** control anchored to the key itself;
- Rebuilt the 15-second refining cycle around the new model animation, physical intake/output chamber volumes, door timing, machine audio, and the existing configurable SCP-914 transformation recipes;
- Removed the obsolete SCP-914 assembly kit, component blocks, component items, GUI, generated procedures, models, textures, and contextual-interaction definitions.

'''
if '## SCP-914\n' not in text:
    marker = '## SCP-330\n'
    if marker not in text:
        raise SystemExit('CHANGELOG SCP-330 insertion marker missing')
    text = text.replace(marker, section + marker, 1)
    changelog.write_text(text)

# The final source tree must have no dependency on any deleted legacy class or
# registry field. This guard is intentionally strict.
remaining_java = []
for path in JAVA.rglob('*.java'):
    content = path.read_text(errors='ignore')
    hits = [name for name in legacy_fields | legacy_classes if name in content]
    if hits:
        remaining_java.append((str(path), sorted(hits)))
if remaining_java:
    for path, hits in remaining_java:
        print('LEGACY JAVA REF:', path, ', '.join(hits))
    raise SystemExit('Legacy SCP-914 Java references remain')

remaining_resources = []
text_suffixes = {'.json', '.mcmeta', '.toml', '.properties', '.txt', '.lang'}
for path in RES.rglob('*'):
    if not path.is_file() or path.suffix.lower() not in text_suffixes:
        continue
    content = path.read_text(errors='ignore').lower()
    hits = [token for token in legacy_ids if token in content]
    if hits:
        remaining_resources.append((str(path), sorted(hits)))
if remaining_resources:
    for path, hits in remaining_resources:
        print('LEGACY RESOURCE REF:', path, ', '.join(hits))
    raise SystemExit('Legacy SCP-914 resource references remain')

# Successful cleanup removes its own scaffolding so master is left clean.
for rel in [
    '.github/workflows/scp914-finish-context.yml',
    '.github/scripts/scp914_finish_context.py',
    '.github/scripts/scp914_cleanup_legacy.py',
]:
    path = Path(rel)
    if path.exists(): path.unlink()
