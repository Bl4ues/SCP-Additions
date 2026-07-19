from pathlib import Path
import json,re
root=Path(__file__).resolve().parents[1]
manifest=json.load(open(root/'cleanup_manifest.json'))
modified=set(manifest['modified']); deleted=set(manifest['deleted'])

def delete(rel):
 p=root/rel
 if p.exists(): p.unlink(); deleted.add(rel)

def write(rel,text):
 p=root/rel; old=p.read_text('utf8') if p.exists() else None
 if old!=text:
  p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text,'utf8'); modified.add(rel)

def jsonwrite(rel,obj,compact=False):
 write(rel,(json.dumps(obj,separators=(',',':'),ensure_ascii=False) if compact else json.dumps(obj,indent=4,ensure_ascii=False))+'\n')

# Removed SCP-059 never has a Java block/entity/effect registration in 3.0.6.
for p in list((root/'src/main/resources').rglob('*')):
 if not p.is_file(): continue
 s=p.relative_to(root).as_posix().lower()
 if ('scp059' in s or 'scp_059' in s or 'delta_radiation' in s):
  delete(p.relative_to(root).as_posix())
# Its three unregistered Geiger items and sounds are also isolated remnants.
for p in list((root/'src/main/resources/assets/scp_additions').rglob('*')):
 if p.is_file() and 'geiger' in p.relative_to(root).as_posix().lower():
  delete(p.relative_to(root).as_posix())

# Remove SCP-059/Geiger sound registrations and definitions.
rel='src/main/java/net/mcreator/scpadditions/init/ScpAdditionsModSounds.java'
t=(root/rel).read_text('utf8')
t='\n'.join(line for line in t.splitlines() if not any(token in line for token in ['SCP059BOX =','SCP059_1 =','GEIGER1 =','GEIGER2 =','GEIGER3 =']))+'\n'
write(rel,t)
rel='src/main/resources/assets/scp_additions/sounds.json'
o=json.load(open(root/rel,encoding='utf8'))
for k in ['scp059box','scp059_1','geiger1','geiger2','geiger3']: o.pop(k,None)
jsonwrite(rel,o,compact=True)

# Remove stale language entries for content that has no registry/runtime owner.
rel='src/main/resources/assets/scp_additions/lang/en_us.json'
o=json.load(open(root/rel,encoding='utf8'))
for k in list(o):
 low=k.lower(); val=str(o[k]).lower()
 if any(x in low for x in ['scp_059','scp059','geiger_','delta_radiation','scp059delta']) or 'delta radiation' in val:
  o.pop(k)
jsonwrite(rel,o)

# Remove deleted SCP-059 block from the pickaxe tag.
rel='src/main/resources/data/minecraft/tags/blocks/mineable/pickaxe.json'
p=root/rel
if p.exists():
 o=json.load(open(p,encoding='utf8'))
 if isinstance(o.get('values'),list): o['values']=[x for x in o['values'] if x!='scp_additions:scp_059']
 jsonwrite(rel,o)

# Remove legacy overlays whose flags have no writer anywhere in the current code.
for rel in [
 'src/main/java/net/mcreator/scpadditions/client/screens/BlackHoleOverlay.java',
 'src/main/java/net/mcreator/scpadditions/client/screens/FearOverlayOverlay.java',
 'src/main/java/net/mcreator/scpadditions/client/screens/NuclearOverlay.java',
 'src/main/java/net/mcreator/scpadditions/procedures/BlackHoleDisplayOverlayIngameProcedure.java',
 'src/main/java/net/mcreator/scpadditions/procedures/FearOverlayDisplayOverlayIngameProcedure.java',
 'src/main/java/net/mcreator/scpadditions/procedures/NuclearDisplayOverlayIngameProcedure.java',
 'src/main/resources/assets/scp_additions/textures/screens/bdauxv.png',
 'src/main/resources/assets/scp_additions/textures/screens/pure-black-amh1tqh06166z417.png',
 'src/main/resources/assets/scp_additions/textures/screens/solid_white.svg.png',
]: delete(rel)

# Remove the three dead overlay fields from player capability serialization/sync.
rel='src/main/java/net/mcreator/scpadditions/network/ScpAdditionsModVariables.java'
t=(root/rel).read_text('utf8')
patterns=[
 r'^\s*public boolean (fear|nuclear|blackh) = false;\r?\n',
 r'^\s*clone\.(fear|nuclear|blackh) = original\.\1;\r?\n',
 r'^\s*nbt\.putBoolean\("(fear|nuclear|blackh)", \1\);\r?\n',
 r'^\s*(fear|nuclear|blackh) = nbt\.getBoolean\("\1"\);\r?\n',
 r'^\s*variables\.(fear|nuclear|blackh) = message\.data\.\1;\r?\n',
]
for pat in patterns: t=re.sub(pat,'',t,flags=re.M)
write(rel,t)

# Delete plain, non-event classes proven to have zero external textual references.
for rel in [
 'src/main/java/com/bl4ues/scpinventory/item/ScpKeyringMirror.java',
 'src/main/java/net/mcreator/scpadditions/procedures/CoffeePlayerFinishesUsingItemProcedure.java',
 'src/main/java/net/mcreator/scpadditions/procedures/Scp0591UpdateTickProcedure.java',
 'src/main/java/net/mcreator/scpadditions/procedures/Scp079controlsystemonProcedure.java',
 'src/main/java/net/mcreator/scpadditions/procedures/TerminalOffProcedure.java',
 'src/main/java/net/mcreator/scpadditions/procedures/TerminalOnProcedure.java',
 'src/main/java/net/mcreator/scpadditions/procedures/TerminalTeslaProcedure.java',
 'src/main/java/net/mcreator/scpadditions/procedures/TeslaRechargeUpdateTickProcedure.java',
 'src/main/java/net/mcreator/scpadditions/procedures/TeslaTerminalOffPProcedure.java',
 'src/main/java/net/mcreator/scpadditions/procedures/VeryFineItems2Procedure.java',
]: delete(rel)

# Changelog note.
rel='CHANGELOG.md'; t=(root/rel).read_text('utf8')
needle='- Reuses the shared SCP-294 geometry for stocking and out-of-range states.\n'
extra='- Removes the unregistered SCP-059/Geiger resource set and three unreachable legacy SCP-294 overlay systems.\n- Removes plain Java helpers and procedures with no runtime, event-bus or textual caller.\n'
if extra.splitlines()[0] not in t: t=t.replace(needle,needle+extra)
write(rel,t)

manifest={'modified':sorted(modified-deleted),'deleted':sorted(deleted)}
(root/'cleanup_manifest.json').write_text(json.dumps(manifest,indent=2)+'\n','utf8')
print('modified',len(manifest['modified']),'deleted',len(manifest['deleted']))
