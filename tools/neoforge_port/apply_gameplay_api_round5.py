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

# Remaining attachment access call sites.
rel='net/mcreator/scpadditions/client/Scp914SkinRenderEvents.java'
t=read(rel)
t=re.sub(
    r'ScpAdditionsModVariables\.PlayerVariables variables = player\s*'
    r'\.getCapability\(ScpAdditionsModVariables\.PLAYER_VARIABLES_CAPABILITY\)\s*'
    r'\.orElse\(null\);',
    'ScpAdditionsModVariables.PlayerVariables variables = '\
    'ScpAdditionsModVariables.getPlayerVariables(player).orElse(null);',
    t,
    flags=re.S,
)
write(rel,t)

rel='net/mcreator/scpadditions/config/ui/CodexAssetStorage.java'
t=read(rel)
t=t.replace('import com.bl4ues.scpinventory.capability.ScpInventoryProvider;\n',
            'import com.bl4ues.scpinventory.capability.ScpInventoryCapability;\n')
t=t.replace('player.getCapability(ScpInventoryProvider.INSTANCE).ifPresent(inventory -> {',
            'ScpInventoryCapability.get(player).ifPresent(inventory -> {')
write(rel,t)

# Client reader interactions also use TriState.
rel='net/mcreator/scpadditions/client/KeycardReaderClientInteractionEvents.java'
t=read(rel).replace('import net.neoforged.bus.api.Event;\n','')
t=add_import(t,'net.neoforged.neoforge.common.util.TriState')
t=t.replace('event.setUseBlock(Event.Result.DENY);','event.setUseBlock(TriState.FALSE);')
t=t.replace('event.setUseItem(Event.Result.DENY);','event.setUseItem(TriState.FALSE);')
write(rel,t)

# MobEffectEvent.Applicable owns its result enum in 1.21.1.
for rel in [
 'net/mcreator/scpadditions/effect/EyeSoreEffectEvents.java',
 'net/mcreator/scpadditions/effect/HazmatExternalEffectEvents.java']:
    t=read(rel).replace('import net.neoforged.bus.api.Event;\n','')
    t=t.replace('event.setResult(Event.Result.DENY);',
                'event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);')
    write(rel,t)

rel='net/mcreator/scpadditions/effect/EyeSoreEffectEvents.java'
t=read(rel)
t=t.replace('instance.getEffect() == ScpAdditionsModMobEffects.EYE_SORE.get()',
            'instance.getEffect().value() == ScpAdditionsModMobEffects.EYE_SORE.get()')
t=t.replace('entity.getEffect(ScpAdditionsModMobEffects.EYE_SORE)',
            'entity.getEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ScpAdditionsModMobEffects.EYE_SORE.get()))')
t=t.replace('new MobEffectInstance(ScpAdditionsModMobEffects.EYE_SORE,',
            'new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ScpAdditionsModMobEffects.EYE_SORE.get()),')
write(rel,t)

# Screen background and input signatures outside the configuration center.
backgrounds = {
 'net/mcreator/scpadditions/client/UnityColorPickerScreen.java': ('graphics','partialTick'),
 'net/mcreator/scpadditions/client/gui/TeslaTerminalScreen.java': ('guiGraphics','partialTicks'),
 'net/mcreator/scpadditions/client/CodexTextEditorScreen.java': ('graphics','partialTick'),
 'net/mcreator/scpadditions/client/gui/KeycardReaderConfigScreen.java': ('guiGraphics','partialTick'),
 'net/mcreator/scpadditions/client/CodexImageDropScreen.java': ('graphics','partialTick'),
 'net/mcreator/scpadditions/client/gui/Scp914GuiScreen.java': ('guiGraphics','partialTicks'),
 'net/mcreator/scpadditions/client/gui/Scp294GuiScreen.java': ('guiGraphics','partialTicks'),
}
for rel,(graphics,partial) in backgrounds.items():
    t=read(rel)
    t=t.replace(f'renderBackground({graphics});',
                f'renderBackground({graphics}, mouseX, mouseY, {partial});')
    t=t.replace(f'this.renderBackground({graphics});',
                f'this.renderBackground({graphics}, mouseX, mouseY, {partial});')
    write(rel,t)

rel='net/mcreator/scpadditions/client/CodexTextEditorScreen.java'
t=read(rel)
t=t.replace('public boolean mouseScrolled(double mouseX, double mouseY, double delta)',
            'public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)')
t=re.sub(r'\bdelta\b','scrollY',t)
t=t.replace('super.mouseScrolled(mouseX, mouseY, scrollY)',
            'super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)')
write(rel,t)

# Item display names moved to the CUSTOM_NAME data component.
rel='net/mcreator/scpadditions/config/ui/CodexAssetStorage.java'
t=read(rel)
t=add_import(t,'net.minecraft.core.component.DataComponents')
t=t.replace('stack.setHoverName(Component.literal(displayName.trim()));',
            'stack.set(DataComponents.CUSTOM_NAME, Component.literal(displayName.trim()));')
write(rel,t)

# ResourceLocation constructors are private in 1.21.1.
for rel in [
 'net/mcreator/scpadditions/client/CodexAssetClient.java',
 'net/mcreator/scpadditions/client/Scp914SkinRenderEvents.java',
 'net/mcreator/scpadditions/client/HazmatVisorOverlay.java',
 'net/mcreator/scpadditions/client/HazmatArmorModel.java',
 'net/mcreator/scpadditions/data/Scp914RecipeBridge.java']:
    t=read(rel)
    t=re.sub(r'new ResourceLocation\(([^,\n]+),\s*([^\)]+)\)',
             r'ResourceLocation.fromNamespaceAndPath(\1, \2)',t)
    write(rel,t)

rel='net/mcreator/scpadditions/data/Scp294DrinkManager.java'
t=read(rel).replace('new ResourceLocation(', 'ResourceLocation.parse(')
write(rel,t)

# Updated entity preview and two-argument resource id factory.
rel='net/mcreator/scpadditions/client/UnityConfigurationUiEvents.java'
t=read(rel)
t=t.replace(
    '''InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                    x + 8, y + 17, scale, 0.0F, 0.0F, living);''',
    '''InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                    x - 1, y - 1, x + 18, y + 19, scale,
                    0.0F, 0.0F, 0.0F, living);''')
t=t.replace('ResourceLocation.parse(id.getNamespace(),',
            'ResourceLocation.fromNamespaceAndPath(id.getNamespace(),')
write(rel,t)

# 1.21 vertex pipeline: begin from Tesselator, addVertex/setColor, buildOrThrow.
rel='net/mcreator/scpadditions/client/SmoothRadialVignetteRenderer.java'
t=read(rel)
t=t.replace(
    'BufferBuilder buffer = Tesselator.getInstance().getBuilder();',
    '''BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);''')
t=t.replace(
    '''        buffer.begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR);
''','')
t=t.replace('BufferUploader.drawWithShader(buffer.end());',
            'BufferUploader.drawWithShader(buffer.buildOrThrow());')
t=t.replace(
    '''buffer.vertex(matrix, x, y, 0.0F)
                .color(color.red(), color.green(), color.blue(), color.alpha())
                .endVertex();''',
    '''buffer.addVertex(matrix, x, y, 0.0F)
                .setColor(color.red(), color.green(), color.blue(), color.alpha());''')
write(rel,t)

# Vanilla feature registry and holder values.
rel='net/mcreator/scpadditions/world/features/StructureFeature.java'
t=read(rel)
t=t.replace('DeferredRegister.create(ForgeRegistries.FEATURES, ScpAdditionsMod.MODID)',
            'DeferredRegister.create(BuiltInRegistries.FEATURE, ScpAdditionsMod.MODID)')
t=t.replace('map(Holder::get)', 'map(Holder::value)')
write(rel,t)

# Attribute constants are holders in 1.21.1.
rel='net/mcreator/scpadditions/event/Scp173DurabilityEvents.java'
t=read(rel)
t=add_import(t,'net.minecraft.core.Holder')
t=t.replace('private static void setBaseAttribute(Scp173Entity scp173, Attribute attribute, double value)',
            'private static void setBaseAttribute(Scp173Entity scp173, Holder<Attribute> attribute, double value)')
write(rel,t)

# Resolve Efficiency through the dynamic enchantment registry.
rel='net/mcreator/scpadditions/facility/FacilityBlockMiningEvents.java'
t=read(rel)
for imp in [
 'net.minecraft.core.Holder',
 'net.minecraft.core.registries.Registries',
 'net.minecraft.world.item.enchantment.Enchantment',
 'net.minecraft.world.item.enchantment.Enchantments']:
    t=add_import(t,imp)
t=t.replace(
    'int efficiency = EnchantmentHelper.getBlockEfficiency(player);',
    '''Holder.Reference<Enchantment> efficiencyHolder = player.level()
                        .registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                        .getHolderOrThrow(Enchantments.EFFICIENCY);
                int efficiency = EnchantmentHelper.getItemEnchantmentLevel(
                        efficiencyHolder, tool);''')
write(rel,t)

# DeferredHolder wildcard entries are adapted to plain Suppliers.
rel='net/mcreator/scpadditions/facility/UBlocksModule.java'
t=read(rel)
t=t.replace(
    '''return BLOCKS.getEntries().stream()
                .filter(entry -> entry.getId().getPath().equals(path))
                .findFirst()
                .orElse(null);''',
    '''return BLOCKS.getEntries().stream()
                .filter(entry -> entry.getId().getPath().equals(path))
                .<Supplier<Block>>map(entry -> entry::get)
                .findFirst()
                .orElse(null);''')
t=t.replace(
    '''return ITEMS.getEntries().stream()
                .filter(entry -> entry.getId().getPath().equals(path))
                .findFirst()
                .orElse(null);''',
    '''return ITEMS.getEntries().stream()
                .filter(entry -> entry.getId().getPath().equals(path))
                .<Supplier<Item>>map(entry -> entry::get)
                .findFirst()
                .orElse(null);''')
write(rel,t)

# RecipeManager now returns RecipeHolder values and recipe ids live on holders.
rel='net/mcreator/scpadditions/data/Scp914GenericRecipeResolver.java'
t=read(rel)
t=add_import(t,'net.minecraft.world.item.crafting.RecipeHolder')
t=t.replace(
    'for (CraftingRecipe recipe : recipes(level)) {',
    '''for (RecipeHolder<CraftingRecipe> recipeHolder : recipes(level)) {
            CraftingRecipe recipe = recipeHolder.value();''')
t=t.replace('recipe.getId()', 'recipeHolder.id()')
t=t.replace('private static List<CraftingRecipe> recipes(ServerLevel level)',
            'private static List<RecipeHolder<CraftingRecipe>> recipes(ServerLevel level)')
t=t.replace('input.getMaxStackSize() == output.getMaxStackSize()',
            'input.getDefaultInstance().getMaxStackSize() == output.getDefaultInstance().getMaxStackSize()')
write(rel,t)

# Fire and mob-effect APIs.
rel='net/mcreator/scpadditions/data/Scp294ActionExecutor.java'
t=read(rel)
t=t.replace('entity.setSecondsOnFire(', 'entity.igniteForSeconds(')
t=t.replace('''new MobEffectInstance(
				effect,''',
            '''new MobEffectInstance(
				BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect),''')
t=t.replace('living.removeEffect(effect);',
            'living.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect));')
write(rel,t)

# Main bootstrap attachment registration import.
rel='net/mcreator/scpadditions/ScpAdditionsMod.java'
t=read(rel)
t=add_import(t,'net.mcreator.scpadditions.network.ScpAdditionsModVariables')
write(rel,t)

# Multi-line two-argument constructors were not covered by the compact regex.
for rel in [
 'net/mcreator/scpadditions/client/CodexAssetClient.java',
 'net/mcreator/scpadditions/client/Scp914SkinRenderEvents.java',
 'net/mcreator/scpadditions/client/HazmatVisorOverlay.java',
 'net/mcreator/scpadditions/client/HazmatArmorModel.java',
 'net/mcreator/scpadditions/data/Scp914RecipeBridge.java']:
    t=read(rel).replace('new ResourceLocation(',
                        'ResourceLocation.fromNamespaceAndPath(')
    write(rel,t)

print('Applied NeoForge 1.21.1 gameplay/API migration round 5')
