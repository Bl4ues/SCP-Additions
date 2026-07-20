from __future__ import annotations
from pathlib import Path
import re

ROOT = Path.cwd()
JAVA = ROOT / 'src/main/java'
if not JAVA.exists():
    ROOT = Path(__file__).resolve().parents[2]
    JAVA = ROOT / 'src/main/java'

def p(rel): return JAVA / rel
def read(rel): return p(rel).read_text(encoding='utf-8')
def write(rel, text): p(rel).write_text(text, encoding='utf-8')
def add_import(text, imp):
    line=f'import {imp};'
    if line in text: return text
    end=text.find(';', text.find('package '))
    return text[:end+1]+'\n\n'+line+text[end+1:]

# Recipe holder value migration.
rel='com/bl4ues/scpinventory/crafting/ScpCraftingRecipeHelper.java'
t=read(rel)
t=t.replace('return level.getRecipeManager().getRecipeFor(\n                RecipeType.CRAFTING, createInput(grid), level);',
            'return level.getRecipeManager().getRecipeFor(\n                RecipeType.CRAFTING, createInput(grid), level)\n                .map(RecipeHolder::value);')
write(rel,t)

# NBT stream API.
rel='net/mcreator/scpadditions/item/Scp914AssemblyKitItem.java'
t=read(rel).replace('NbtIo.readCompressed(stream)', 'NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap())')
write(rel,t)

# ResourceLocation parser throughout recipe config.
rel='net/mcreator/scpadditions/data/Scp914RecipeManager.java'
t=read(rel).replace('new ResourceLocation(', 'ResourceLocation.parse(')
write(rel,t)

# Mob effect registries are holder based in 1.21.1.
def wrap_effect_calls(rel):
    t=read(rel)
    t=add_import(t,'net.minecraft.core.registries.BuiltInRegistries')
    # Constructor, query, removal calls using mod suppliers/values.
    t=re.sub(r'new MobEffectInstance\((ScpAdditionsModMobEffects\.[A-Z0-9_]+)(?:\.get\(\))?,',
             r'new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(\1.get()),',t)
    t=re.sub(r'(hasEffect|getEffect|removeEffect)\((ScpAdditionsModMobEffects\.[A-Z0-9_]+)(?:\.get\(\))?\)',
             r'\1(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(\2.get()))',t)
    write(rel,t)

for rel in [
 'net/mcreator/scpadditions/procedures/Scp1176honeyPlayerFinishesUsingItemProcedure.java',
 'net/mcreator/scpadditions/procedures/DecontaminationCheckpointController.java',
 'net/mcreator/scpadditions/scp012/Scp012InfluenceEvents.java',
 'net/mcreator/scpadditions/scp012/Scp012BleedingEvents.java']:
    wrap_effect_calls(rel)

rel='net/mcreator/scpadditions/handler/Scp294DrinkHandler.java'
t=read(rel).replace('\n\t\t\t\t\teffect,', '\n\t\t\t\t\tBuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect),')
write(rel,t)

# 1.21.1 block tooltip signature. Generated blocks used BlockGetter as context.
for f in JAVA.rglob('*.java'):
    t=f.read_text(encoding='utf-8'); orig=t
    t=re.sub(r'public void appendHoverText\(ItemStack (\w+),\s*BlockGetter\s+\w+,\s*List<Component>\s+(\w+),\s*TooltipFlag\s+(\w+)\)',
             r'public void appendHoverText(ItemStack \1, Item.TooltipContext context, List<Component> \2, TooltipFlag \3)',t)
    if t!=orig:
        t=add_import(t,'net.minecraft.world.item.Item')
        f.write_text(t,encoding='utf-8')

# 1.21.1 pick-block signature (NeoForge extension includes player).
clone_pattern=re.compile(
 r'public ItemStack getCloneItemStack\(BlockState\s+(\w+),\s*(?:net\.minecraft\.world\.phys\.)?HitResult\s+\w+,\s*BlockGetter\s+(\w+),\s*BlockPos\s+(\w+),\s*Player\s+(\w+)\)')
for f in JAVA.rglob('*.java'):
    t=f.read_text(encoding='utf-8'); orig=t
    t=clone_pattern.sub(r'public ItemStack getCloneItemStack(LevelReader \2, BlockPos \3, BlockState \1, boolean includeData, Player \4)',t)
    if t!=orig:
        t=add_import(t,'net.minecraft.world.level.LevelReader')
        f.write_text(t,encoding='utf-8')

# Old Block#use became useWithoutItem. The generated implementations do not use the hand
# except in their obsolete super call.
use_pattern=re.compile(
 r'public InteractionResult use\(BlockState\s+(\w+),\s*Level\s+(\w+),\s*BlockPos\s+(\w+),\s*Player\s+(\w+),\s*InteractionHand\s+(\w+),\s*BlockHitResult\s+(\w+)\)')
for f in JAVA.rglob('*.java'):
    t=f.read_text(encoding='utf-8'); orig=t
    t=use_pattern.sub(r'protected InteractionResult useWithoutItem(BlockState \1, Level \2, BlockPos \3, Player \4, BlockHitResult \6)',t)
    t=re.sub(r'^\s*super\.use\([^;]+\);\s*\n', '', t, flags=re.M)
    if t!=orig: f.write_text(t,encoding='utf-8')

# 1.21.1 no longer exposes Tier#getLevel. Use vanilla tool correctness.
# All generated blocks using this pattern are pickaxe-mineable facility machinery.
can_harvest_pattern = re.compile(
    r'public boolean canHarvestBlock\(BlockState state, BlockGetter \w+, BlockPos \w+, Player player\) \{'
    r'.*?getTier\(\)\.getLevel\(\) >= \d+;.*?return false;\s*\}',
    flags=re.S,
)
for f in JAVA.rglob('*.java'):
    t=f.read_text(encoding='utf-8'); orig=t
    t=can_harvest_pattern.sub(
        'public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {\n'
        '        return player.getMainHandItem().isCorrectToolForDrops(state);\n'
        '    }',
        t,
    )
    if t!=orig: f.write_text(t,encoding='utf-8')

# SCP-294 container entities: registry-aware save/load and sync tags.
for name in ['Scp294BlockEntity.java','Scp294StockingBlockEntity.java','Scp294OutOfRangeBlockEntity.java']:
    rel='net/mcreator/scpadditions/block/entity/'+name
    t=read(rel); t=add_import(t,'net.minecraft.core.HolderLookup')
    t=t.replace('public void load(CompoundTag compound) {\n\t\tsuper.load(compound);',
                'protected void loadAdditional(CompoundTag compound, HolderLookup.Provider registries) {\n\t\tsuper.loadAdditional(compound, registries);')
    t=t.replace('ContainerHelper.loadAllItems(compound, this.stacks);',
                'ContainerHelper.loadAllItems(compound, this.stacks, registries);')
    t=t.replace('public void saveAdditional(CompoundTag compound) {\n\t\tsuper.saveAdditional(compound);',
                'protected void saveAdditional(CompoundTag compound, HolderLookup.Provider registries) {\n\t\tsuper.saveAdditional(compound, registries);')
    t=t.replace('ContainerHelper.saveAllItems(compound, this.stacks);',
                'ContainerHelper.saveAllItems(compound, this.stacks, registries);')
    t=t.replace('public CompoundTag getUpdateTag() {\n\t\treturn this.saveWithFullMetadata(world.registryAccess());',
                'public CompoundTag getUpdateTag(HolderLookup.Provider registries) {\n\t\treturn this.saveWithFullMetadata(registries);')
    write(rel,t)

# Directional block subclasses need codecs in 1.21.1. Unit codecs preserve the
# exact registered variant instance (door frame, state, stage, etc.).
def add_unit_codec(rel, class_types):
    t=read(rel); t=add_import(t,'com.mojang.serialization.MapCodec')
    for cls, base_type in class_types.items():
        # Add directly after class opening, unless already present nearby.
        pat=re.compile(r'(class\s+'+re.escape(cls)+r'\s+extends\s+[^\{]+\{)')
        m=pat.search(t)
        if not m: continue
        tail=t[m.end():m.end()+300]
        if 'MapCodec<? extends' in tail and 'codec()' in tail: continue
        indent = '    ' if rel.endswith('Scp012Block.java') else '        '
        block=(f'\n{indent}@Override\n'
               f'{indent}protected MapCodec<? extends {base_type}> codec() {{\n'
               f'{indent}    return MapCodec.unit(this);\n'
               f'{indent}}}\n')
        t=t[:m.end()]+block+t[m.end():]
    write(rel,t)

add_unit_codec('net/mcreator/scpadditions/scp012/Scp012Block.java',{
 'Scp012Block':'HorizontalDirectionalBlock'})
add_unit_codec('net/mcreator/scpadditions/facility/FacilityModule.java',{
 'WallLightBlock':'HorizontalDirectionalBlock',
 'HeaterBlock':'HorizontalDirectionalBlock',
 'SignSupportBlock':'HorizontalDirectionalBlock',
 'TvBlock':'DirectionalBlock',
 'TrashbinBlock':'HorizontalDirectionalBlock',
 'AnimatedDoorBlock':'HorizontalDirectionalBlock',
 'DoorButtonBlock':'HorizontalDirectionalBlock'})

# Facility-specific 1.21 method signatures.
rel='net/mcreator/scpadditions/facility/FacilityModule.java'
t=read(rel)
t=t.replace('public boolean isPathfindable(BlockState state, BlockGetter level,\n                BlockPos pos, PathComputationType type)',
            'protected boolean isPathfindable(BlockState state, PathComputationType type)')
write(rel,t)

# The same old pick-block signature may exist with differently-qualified HitResult after
# facility edits; normalize once more.
for f in JAVA.rglob('*.java'):
    t=f.read_text(encoding='utf-8'); orig=t
    t=clone_pattern.sub(r'public ItemStack getCloneItemStack(LevelReader \2, BlockPos \3, BlockState \1, boolean includeData, Player \4)',t)
    if t!=orig:
        t=add_import(t,'net.minecraft.world.level.LevelReader')
        f.write_text(t,encoding='utf-8')

print('Applied NeoForge 1.21.1 gameplay/API migration round 3')
