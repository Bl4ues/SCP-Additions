from pathlib import Path
import json,re

root=Path(__file__).resolve().parents[1]
legacy=json.load(open(root/'build/reports/asset-audit/report.json',encoding='utf8'))['legacy_items']
legacy_ids={x['item_id'] for x in legacy}
legacy_rl={f'scp_additions:{x}' for x in legacy_ids}
deleted=[]
modified=[]

def mark_delete(p):
    p=Path(p)
    if p.exists():
        deleted.append(p.relative_to(root).as_posix())
        p.unlink()

def write_text(p,text):
    p=Path(p)
    old=p.read_text('utf8') if p.exists() else None
    if old!=text:
        p.parent.mkdir(parents=True,exist_ok=True)
        p.write_text(text,'utf8')
        modified.append(p.relative_to(root).as_posix())

def pretty_json(p,data):
    write_text(p,json.dumps(data,indent=4,ensure_ascii=False)+"\n")

# 1. Migrate every bundled legacy drink result to the generic NBT-backed cup.
p=root/'config/scpadditions/294drinks.json'
data=json.load(open(p,encoding='utf8'))
changed_drinks=[]
for drink in data.get('drinks',[]):
    result=drink.get('result')
    if isinstance(result,dict) and result.get('item') in legacy_rl:
        changed_drinks.append((drink.get('id'),result['item']))
        result['item']='scp_additions:cup_of_coffee'
pretty_json(p,data)

# 2. Remove bundled SCP-914 recipes whose input/output depends on removed legacy drinks.
p=root/'config/scpadditions/914recipes.json'
data914=json.load(open(p,encoding='utf8'))
removed_914=[]
def contains_legacy(obj):
    if isinstance(obj,dict):
        return any((k=='item' and v in legacy_rl) or contains_legacy(v) for k,v in obj.items())
    if isinstance(obj,list): return any(contains_legacy(v) for v in obj)
    return False
kept=[]
for recipe in data914.get('recipes',[]):
    if contains_legacy(recipe): removed_914.append(recipe.get('id'))
    else: kept.append(recipe)
data914['recipes']=kept
pretty_json(p,data914)

# 3. Remove legacy direct item registrations.
p=root/'src/main/java/net/mcreator/scpadditions/init/ScpAdditionsModItems.java'
text=p.read_text('utf8')
lines=text.splitlines(keepends=True)
new=[]
removed_regs=[]
reg_re=re.compile(r'RegistryObject<Item>\s+([A-Z0-9_]+).*REGISTRY\.register\("([a-z0-9_]+)"')
for line in lines:
    m=reg_re.search(line)
    if m and m.group(2) in legacy_ids:
        removed_regs.append(m.group(2)); continue
    new.append(line)
write_text(p,''.join(new))

# 4. Delete legacy item implementation classes and models.
deleted_item_files=set()
for item in legacy:
    class_path=root/f"src/main/java/net/mcreator/scpadditions/item/{item['class_name']}.java"
    model_path=root/item['model_path'] if item.get('model_path') else None
    if class_path.exists(): deleted_item_files.add(class_path.relative_to(root).as_posix())
    mark_delete(class_path)
    if model_path: mark_delete(model_path)

# 5. Delete procedures verified by the audit to be referenced only by the removed item classes.
procedure_names={proc for item in legacy for proc in item.get('imported_procedures',[])}
safe_procs=sorted(procedure_names)
blocked_procs={}
for name in safe_procs:
    mark_delete(root/f'src/main/java/net/mcreator/scpadditions/procedures/{name}.java')

# 6. Delete data recipes that reference removed item IDs.
removed_data_recipes=[]
recipes_root=root/'src/main/resources/data/scp_additions/recipes'
if recipes_root.exists():
    for fp in recipes_root.glob('*.json'):
        content=fp.read_text('utf8')
        if any(rl in content for rl in legacy_rl):
            removed_data_recipes.append(fp.relative_to(root).as_posix())
            mark_delete(fp)

# 7. Remove stale language keys owned by deleted item IDs.
for langrel in ['src/main/resources/assets/scp_additions/lang/en_us.json','src/main/resources/assets/scp_additions/lang/en_us_3_0.json']:
    lp=root/langrel
    if not lp.exists(): continue
    obj=json.load(open(lp,encoding='utf8'))
    removed_keys=[]
    for key in list(obj):
        if any(key==f'item.scp_additions.{item_id}' or key.startswith(f'item.scp_additions.{item_id}.') for item_id in legacy_ids):
            removed_keys.append(key); obj.pop(key)
    if removed_keys: pretty_json(lp,obj)

# 8. Runtime migration for existing 294 configs and old-world stacks.
p=root/'src/main/java/net/mcreator/scpadditions/data/Scp294DrinkManager.java'
text=p.read_text('utf8')
text=text.replace('import java.util.Map;\n', 'import java.util.Map;\nimport java.util.Set;\n')
legacy_java=',\n\t\t\t'.join(f'"{x}"' for x in sorted(legacy_ids))
needle='\tprivate static final String BUNDLED_CONFIG = "config/scpadditions/294drinks.json";\n'
insert=needle+f'''\tprivate static final ResourceLocation GENERIC_CUP = new ResourceLocation("scp_additions", "cup_of_coffee");
\tprivate static final Set<String> LEGACY_DRINK_ITEM_PATHS = Set.of(
\t\t\t{legacy_java});
'''
if needle not in text: raise SystemExit('BUNDLED_CONFIG needle not found')
text=text.replace(needle,insert,1)
old='\t\tResourceLocation resultItem = new ResourceLocation(GsonHelper.getAsString(result, "item", "scp_additions:cup_of_coffee"));\n'
new='\t\tResourceLocation resultItem = normalizeLegacyDrinkItem(new ResourceLocation(\n\t\t\t\tGsonHelper.getAsString(result, "item", GENERIC_CUP.toString())));\n'
if old not in text: raise SystemExit('result item needle not found')
text=text.replace(old,new,1)
needle2='\tprivate static List<String> readAliases(ResourceLocation id, JsonObject json) {\n'
method='''\tprivate static ResourceLocation normalizeLegacyDrinkItem(ResourceLocation requested) {
\t\tif (ScpAdditionsMod.MODID.equals(requested.getNamespace())
\t\t\t\t&& LEGACY_DRINK_ITEM_PATHS.contains(requested.getPath())) {
\t\t\treturn GENERIC_CUP;
\t\t}
\t\treturn requested;
\t}

'''
if needle2 not in text: raise SystemExit('readAliases needle not found')
text=text.replace(needle2,method+needle2,1)
write_text(p,text)

mapping_path=root/'src/main/java/net/mcreator/scpadditions/item/LegacyDrinkItemMappings.java'
mapping_content=f'''package net.mcreator.scpadditions.item;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModItems;

import java.util.Set;

/**
 * Preserves old-world inventory loading after the pre-3.0 SCP-294 drink items
 * were consolidated into the configurable, NBT-backed generic cup.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LegacyDrinkItemMappings {{
    private static final Set<String> LEGACY_DRINK_ITEMS = Set.of(
            {legacy_java.replace(chr(9), '    ')});

    private LegacyDrinkItemMappings() {{
    }}

    @SubscribeEvent
    public static void remapLegacyDrinkItems(MissingMappingsEvent event) {{
        Item replacement = ScpAdditionsModItems.CUP_OF_COFFEE.get();
        for (MissingMappingsEvent.Mapping<Item> mapping :
                event.getMappings(ForgeRegistries.Keys.ITEMS, ScpAdditionsMod.MODID)) {{
            if (LEGACY_DRINK_ITEMS.contains(mapping.getKey().getPath())) {{
                mapping.remap(replacement);
            }}
        }}
    }}
}}
'''
write_text(mapping_path,mapping_content)

# 9. Remove the disabled, legally unapproved login-music asset while reserving its packet slot.
mark_delete(root/'src/main/resources/assets/scp_additions/sounds/enter.ogg')
sp=root/'src/main/resources/assets/scp_additions/sounds.json'
sounds=json.load(open(sp,encoding='utf8'))
sounds.pop('enter',None)
write_text(sp,json.dumps(sounds,separators=(',',':'),ensure_ascii=False)+"\n")

p=root/'src/main/java/net/mcreator/scpadditions/init/ScpAdditionsModSounds.java'
text=p.read_text('utf8')
text='\n'.join(line for line in text.splitlines() if 'RegistryObject<SoundEvent> ENTER =' not in line)+'\n'
write_text(p,text)

p=root/'src/main/java/net/mcreator/scpadditions/client/ClientPacketActions.java'
text=p.read_text('utf8')
text=text.replace('import net.minecraft.client.Minecraft;\n','').replace('import net.minecraft.client.resources.sounds.SimpleSoundInstance;\n','').replace('import net.mcreator.scpadditions.init.ScpAdditionsModSounds;\n','')
text=re.sub(r'\n\s*public static void playEnterSound\(\) \{.*?\n\s*\}\n', '\n', text, flags=re.S)
write_text(p,text)

p=root/'src/main/java/net/mcreator/scpadditions/network/EnterSoundPacket.java'
text=p.read_text('utf8')
text=text.replace('        context.enqueueWork(() -> ClientPacketExecutor.run("playEnterSound"));\n','')
write_text(p,text)

p=root/'src/main/java/net/mcreator/scpadditions/network/ScpEntityNetwork.java'
text=p.read_text('utf8')
text=re.sub(r'\n\s*public static void playEnterSound\(ServerPlayer player\) \{.*?\n\s*\}\n', '\n', text, flags=re.S)
write_text(p,text)

p=root/'CHANGELOG.md'
text=p.read_text('utf8')
text=text.replace(
    '- Temporarily disables `enter.ogg` while permission to reuse SCP: Unity music is being sought from the original composer.',
    '- Removes the disabled `enter.ogg` asset from the distributed JAR while permission to reuse SCP: Unity music is being sought from the original composer.'
)
write_text(p,text)

# 10. Avoid packaging registry-facing legacy duplicates after their migrated copies are generated.
p=root/'build.gradle'
text=p.read_text('utf8')
needle='''            project.copy {
                from new File(legacyAssets, 'models/item')
                into new File(additionsAssets, 'models/item')
            }

            // The original Unity button has geometry only for one lateral
'''
repl='''            project.copy {
                from new File(legacyAssets, 'models/item')
                into new File(additionsAssets, 'models/item')
            }

            // Registry-facing copies now live under scp_additions. Remove the
            // duplicate legacy copies from processed resources while retaining
            // the legacy custom/block models, textures and sounds they reference.
            project.delete(new File(legacyAssets, 'blockstates'))
            project.delete(new File(legacyAssets, 'models/item'))

            // The original Unity button has geometry only for one lateral
'''
if needle not in text: raise SystemExit('legacy assets copy needle not found')
text=text.replace(needle,repl,1)
needle='''            project.copy {
                from new File(ublocksAssets, 'models/item')
                into new File(additionsAssets, 'models/item')
            }
        }

        // Copy loot tables, recipes and other namespace-owned data into the new
'''
repl='''            project.copy {
                from new File(ublocksAssets, 'models/item')
                into new File(additionsAssets, 'models/item')
            }
            project.delete(new File(ublocksAssets, 'blockstates'))
            project.delete(new File(ublocksAssets, 'models/item'))
        }

        // Copy loot tables, recipes and other namespace-owned data into the new
'''
if needle not in text: raise SystemExit('ublocks copy needle not found')
text=text.replace(needle,repl,1)
needle='''        if (legacyData.exists()) {
            project.copy {
                from legacyData
                into additionsData
            }
        }
        def allData = file("$buildDir/resources/main/data")
'''
repl='''        if (legacyData.exists()) {
            project.copy {
                from legacyData
                into additionsData
            }
            project.delete(legacyData)
        }
        def allData = file("$buildDir/resources/main/data")
'''
if needle not in text: raise SystemExit('legacy data copy needle not found')
text=text.replace(needle,repl,1)
write_text(p,text)

# 11. Consolidate byte-identical SCP-294 custom geometry.
for wrapper in ['scp_294_stocking.json','scp_294_out_of_range.json']:
    wp=root/'src/main/resources/assets/scp_additions/models/block'/wrapper
    if wp.exists():
        obj=json.load(open(wp,encoding='utf8'))
        obj['parent']='scp_additions:custom/scp294'
        pretty_json(wp,obj)
for custom in ['scp294stocking.json','scp294outofrange.json']:
    mark_delete(root/'src/main/resources/assets/scp_additions/models/custom'/custom)

# 12. Add cleanup note to changelog if section exists.
p=root/'CHANGELOG.md'
text=p.read_text('utf8')
anchor='## Audio licensing\n'
note='''## Internal cleanup

- Consolidates the pre-3.0 SCP-294 drink items into the configurable generic cup, preserving drink IDs, colors, effects and actions while removing obsolete `/give` entries.
- Adds old-world item remapping and legacy SCP-294 config normalization for the removed drink registry IDs.
- Removes legacy drink classes, procedures, item models and obsolete SCP-914/crafting recipes that depended on them.
- Stops packaging duplicate registry-facing resources from the integrated legacy namespaces after their `scp_additions` copies are generated.
- Reuses the shared SCP-294 geometry for stocking and out-of-range states.

'''
if '## Internal cleanup\n' not in text:
    if anchor in text: text=text.replace(anchor,note+anchor,1)
    else: text += '\n'+note
write_text(p,text)

print(json.dumps({
 'changed_drinks':len(changed_drinks),
 'removed_914':removed_914,
 'removed_regs':len(removed_regs),
 'safe_procs':len(safe_procs),
 'blocked_procs':blocked_procs,
 'removed_data_recipes':removed_data_recipes,
 'modified_count':len(set(modified)),
 'deleted_count':len(set(deleted)),
},indent=2))
(root/'cleanup_manifest.json').write_text(json.dumps({'modified':sorted(set(modified)),'deleted':sorted(set(deleted))},indent=2)+'\n','utf8')
