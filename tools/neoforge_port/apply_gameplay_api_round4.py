from __future__ import annotations
from pathlib import Path
import re

ROOT = Path.cwd()
JAVA = ROOT / 'src/main/java'
if not JAVA.exists():
    ROOT = Path(__file__).resolve().parents[2]
    JAVA = ROOT / 'src/main/java'


def p(rel: str) -> Path:
    return JAVA / rel


def read(rel: str) -> str:
    return p(rel).read_text(encoding='utf-8')


def write(rel: str, text: str) -> None:
    path = p(rel)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding='utf-8')


def add_import(text: str, imp: str) -> str:
    line = f'import {imp};'
    if line in text:
        return text
    package_end = text.find(';', text.find('package '))
    return text[:package_end + 1] + '\n\n' + line + text[package_end + 1:]


# Mob effects are Holder-backed in 1.21.1. Normalize all calls that still
# pass raw registered MobEffect instances.
for java_file in JAVA.rglob('*.java'):
    text = java_file.read_text(encoding='utf-8')
    original = text
    if 'ScpAdditionsModMobEffects.' in text:
        text = add_import(text, 'net.minecraft.core.registries.BuiltInRegistries')
        text = re.sub(
            r'new MobEffectInstance\(\s*(ScpAdditionsModMobEffects\.[A-Z0-9_]+)\.get\(\),',
            r'new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(\1.get()),',
            text,
        )
        text = re.sub(
            r'(?<!wrapAsHolder\()(?P<method>hasEffect|getEffect|removeEffect)\(\s*'
            r'(?P<effect>ScpAdditionsModMobEffects\.[A-Z0-9_]+)\.get\(\)\s*\)',
            r'\g<method>(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(\g<effect>.get()))',
            text,
        )
    if text != original:
        java_file.write_text(text, encoding='utf-8')

# NeoForge 1.21.1 pick-block signature. Round 3 briefly used a later-version
# signature; restore the 1.21.1 HitResult form everywhere.
clone_pattern = re.compile(
    r'public ItemStack getCloneItemStack\(LevelReader\s+(\w+),\s*BlockPos\s+(\w+),\s*'
    r'BlockState\s+(\w+),\s*boolean\s+\w+,\s*Player\s+(\w+)\)'
)
for java_file in JAVA.rglob('*.java'):
    text = java_file.read_text(encoding='utf-8')
    original = text
    text = clone_pattern.sub(
        r'public ItemStack getCloneItemStack(BlockState \3, HitResult target, LevelReader \1, BlockPos \2, Player \4)',
        text,
    )
    if text != original:
        text = add_import(text, 'net.minecraft.world.phys.HitResult')
        java_file.write_text(text, encoding='utf-8')

# Player variable synchronization now uses the branch's data attachment helper.
rel = 'net/mcreator/scpadditions/network/ScpAdditionsModVariables.java'
text = read(rel)
text = re.sub(
    r'PlayerVariables variables = \(\(PlayerVariables\) player\s*'
    r'\.getCapability\(PLAYER_VARIABLES_CAPABILITY, null\)\s*'
    r'\.orElse\(new PlayerVariables\(\)\)\);',
    'PlayerVariables variables = ScpAdditionsModVariables.getPlayerVariables(player)\n'
    '\t\t\t\t\t\t\t\t.orElse(new PlayerVariables());',
    text,
    flags=re.S,
)
write(rel, text)

# Menu screen registration is a dedicated mod-bus event in NeoForge 1.21.1.
rel = 'net/mcreator/scpadditions/init/ScpAdditionsModScreens.java'
text = read(rel)
text = text.replace('import net.minecraft.client.gui.screens.MenuScreens;\n', '')
text = add_import(text, 'net.neoforged.neoforge.client.event.RegisterMenuScreensEvent')
old = '''\t@SubscribeEvent
\tpublic static void clientLoad(FMLClientSetupEvent event) {
\t\tevent.enqueueWork(() -> {
\t\t\tMenuScreens.register(ScpAdditionsModMenus.TESLA_TERMINAL.get(), TeslaTerminalScreen::new);
\t\t\tMenuScreens.register(ScpAdditionsModMenus.SCP_914_GUI.get(), Scp914GuiScreen::new);
\t\t\tMenuScreens.register(ScpAdditionsModMenus.SCP_294_GUI.get(), Scp294GuiScreen::new);

\t\t\tItemBlockRenderTypes.setRenderLayer(ScpAdditionsModBlocks.TESLA_GATE.get(), RenderType.translucent());
'''
new = '''\t@SubscribeEvent
\tpublic static void registerScreens(RegisterMenuScreensEvent event) {
\t\tevent.register(ScpAdditionsModMenus.TESLA_TERMINAL.get(), TeslaTerminalScreen::new);
\t\tevent.register(ScpAdditionsModMenus.SCP_914_GUI.get(), Scp914GuiScreen::new);
\t\tevent.register(ScpAdditionsModMenus.SCP_294_GUI.get(), Scp294GuiScreen::new);
\t}

\t@SubscribeEvent
\tpublic static void clientLoad(FMLClientSetupEvent event) {
\t\tevent.enqueueWork(() -> {
\t\t\tItemBlockRenderTypes.setRenderLayer(ScpAdditionsModBlocks.TESLA_GATE.get(), RenderType.translucent());
'''
if old in text:
    text = text.replace(old, new)
write(rel, text)

# Creative tab entries expose explicit remove APIs instead of a mutable entry set.
rel = 'net/mcreator/scpadditions/init/ScpAdditionsModTabs.java'
text = read(rel)
text = add_import(text, 'net.minecraft.world.item.CreativeModeTab')
text = re.sub(
    r'\s*var iterator = tabData\.getEntries\(\)\.iterator\(\);\s*'
    r'while \(iterator\.hasNext\(\)\) \{\s*'
    r'ItemStack stack = iterator\.next\(\)\.getKey\(\);\s*'
    r'if \(stack\.is\(FacilityModule\.itemByPath\("button_closed"\)\.get\(\)\)\s*'
    r'\|\| stack\.is\(FacilityModule\.itemByPath\("button_locked"\)\.get\(\)\)\) \{\s*'
    r'iterator\.remove\(\);\s*\}\s*\}',
    '''
            tabData.remove(new ItemStack(FacilityModule.itemByPath("button_closed").get()),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            tabData.remove(new ItemStack(FacilityModule.itemByPath("button_locked").get()),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);''',
    text,
    flags=re.S,
)
write(rel, text)

# Right-click result controls use TriState in NeoForge 1.21.1.
rel = 'net/mcreator/scpadditions/keycard/KeycardReaderInteractionEvents.java'
text = read(rel)
text = text.replace('import net.neoforged.bus.api.Event;\n', '')
text = add_import(text, 'net.neoforged.neoforge.common.util.TriState')
text = text.replace('event.setUseBlock(Event.Result.DENY);', 'event.setUseBlock(TriState.FALSE);')
text = text.replace('event.setUseItem(Event.Result.DENY);', 'event.setUseItem(TriState.FALSE);')
write(rel, text)

# Configuration center screen/input and registry API changes.
rel = 'net/mcreator/scpadditions/config/ui/ConfigCenterClient.java'
text = read(rel)
text = text.replace(
    '''container.registerExtensionPoint(
                            IConfigScreenFactory.class,
                            () -> (ignored, parent) -> openFromMods(
                                    Minecraft.getInstance(), parent))''',
    '''container.registerExtensionPoint(
                            IConfigScreenFactory.class,
                            (IConfigScreenFactory) (ignored, parent) -> openFromMods(
                                    Minecraft.getInstance(), parent))''',
)
text = text.replace('renderBackground(graphics);',
                    'renderBackground(graphics, mouseX, mouseY, partialTick);')
text = text.replace(
    'public boolean mouseScrolled(double mouseX, double mouseY, double delta)',
    'public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)',
)
# The methods are local GUI handlers; all old delta references are vertical scroll.
text = re.sub(r'\bdelta\b', 'scrollY', text)
text = text.replace(
    'super.mouseScrolled(mouseX, mouseY, scrollY)',
    'super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)',
)
text = text.replace('BuiltInRegistries.ITEM.getKeys()', 'BuiltInRegistries.ITEM.keySet()')
text = re.sub(r'new ResourceLocation\((string\([^\n;]+\))\)',
              r'ResourceLocation.parse(\1)', text)
write(rel, text)

# Single-argument ResourceLocation constructors in SCP-294 drink parsing.
rel = 'net/mcreator/scpadditions/data/Scp294DrinkManager.java'
text = read(rel)
text = re.sub(
    r'new ResourceLocation\(([^,\n]+)\)',
    r'ResourceLocation.parse(\1)',
    text,
)
write(rel, text)

# Remaining directional subclasses require codecs in 1.21.1.
def add_unit_codec(rel: str, classes: dict[str, str]) -> None:
    text = read(rel)
    text = add_import(text, 'com.mojang.serialization.MapCodec')
    for cls, return_type in classes.items():
        match = re.search(r'(class\s+' + re.escape(cls) + r'\s+extends\s+[^\{]+\{)', text)
        if not match:
            continue
        nearby = text[match.end():match.end() + 260]
        if 'protected MapCodec<? extends' in nearby and 'codec()' in nearby:
            continue
        indent = '        '
        codec = (
            f'\n{indent}@Override\n'
            f'{indent}protected MapCodec<? extends {return_type}> codec() {{\n'
            f'{indent}    return MapCodec.unit(this);\n'
            f'{indent}}}\n'
        )
        text = text[:match.end()] + codec + text[match.end():]
    write(rel, text)

add_unit_codec('net/mcreator/scpadditions/facility/MirroredDoorButtons.java', {
    'MirroredDoorButtonBlock': 'HorizontalDirectionalBlock',
})
add_unit_codec('net/mcreator/scpadditions/facility/UBlocksModule.java', {
    'AdaptiveWallDetailBlock': 'HorizontalDirectionalBlock',
    'UBlockDirectionalBlock': 'HorizontalDirectionalBlock',
})
add_unit_codec('net/mcreator/scpadditions/facility/LeftDoorButtons.java', {
    'LeftDoorButtonBlock': 'HorizontalDirectionalBlock',
})

print('Applied NeoForge 1.21.1 gameplay/API migration round 4')
