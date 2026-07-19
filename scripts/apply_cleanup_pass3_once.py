from pathlib import Path
from collections import defaultdict
import hashlib,json
root=Path(__file__).resolve().parents[1]
manifest=json.load(open(root/'cleanup_manifest.json'))
modified=set(manifest['modified']); deleted=set(manifest['deleted'])
TEXT_SUFFIXES={'.java','.json','.gradle','.toml','.properties','.md','.txt','.yml','.yaml','.cfg','.mcmeta'}

def text_files():
 return [p for p in root.rglob('*') if p.is_file() and p.suffix.lower() in TEXT_SUFFIXES and 'build/' not in p.relative_to(root).as_posix()]

def replace_all(old,new,exclude=None):
 count=0
 for p in text_files():
  rel=p.relative_to(root).as_posix()
  if exclude and rel in exclude: continue
  try:t=p.read_text('utf8')
  except UnicodeDecodeError:continue
  if old in t:
   p.write_text(t.replace(old,new),'utf8'); modified.add(rel); count+=t.count(old)
 return count

def delete(p):
 rel=p.relative_to(root).as_posix()
 if p.exists(): p.unlink(); deleted.add(rel); modified.discard(rel)

# Deduplicate byte-identical custom model geometry. Custom models are loaded only
# through explicit parent references, which are rewritten before removal.
assets=root/'src/main/resources/assets'
g=defaultdict(list)
for p in assets.glob('*/models/custom/*.json'):
 b=p.read_bytes(); g[(hashlib.sha256(b).hexdigest(),len(b))].append(p)
model_map=[]
for (_,size),paths in sorted(g.items(),key=lambda kv:kv[0][1],reverse=True):
 if len(paths)<2 or size<10000: continue
 paths=sorted(paths)
 canonical=paths[0]
 ns=canonical.relative_to(assets).parts[0]
 canon_key=f"{ns}:custom/{canonical.stem}"
 for duplicate in paths[1:]:
  dns=duplicate.relative_to(assets).parts[0]
  old_key=f"{dns}:custom/{duplicate.stem}"
  refs=replace_all(old_key,canon_key,{duplicate.relative_to(root).as_posix()})
  model_map.append((old_key,canon_key,refs,size))
  delete(duplicate)

# Consolidate exact block/item texture copies with the same filename inside one
# namespace. Model/code references are changed to the block texture path.
texture_map=[]
for nsdir in assets.iterdir():
 if not nsdir.is_dir(): continue
 block=nsdir/'textures/block'; item=nsdir/'textures/item'
 if not block.exists() or not item.exists(): continue
 for ip in item.glob('*.png'):
  bp=block/ip.name
  if not bp.exists() or ip.read_bytes()!=bp.read_bytes(): continue
  old=f'{nsdir.name}:item/{ip.stem}'
  new=f'{nsdir.name}:block/{bp.stem}'
  refs=replace_all(old,new,{ip.relative_to(root).as_posix()})
  # Resource paths used only by convention would have no explicit references;
  # keep them rather than guessing. Referenced copies are safe to consolidate.
  if refs>0:
   texture_map.append((old,new,refs,ip.stat().st_size))
   delete(ip)

# Changelog summary.
rel='CHANGELOG.md'; p=root/rel; t=p.read_text('utf8')
needle='- Removes plain Java helpers and procedures with no runtime, event-bus or textual caller.\n'
extra='- Consolidates byte-identical custom model geometry and referenced block/item texture copies without removing registry-facing model paths.\n'
if extra.strip() not in t: p.write_text(t.replace(needle,needle+extra),'utf8'); modified.add(rel)

manifest={'modified':sorted(modified-deleted),'deleted':sorted(deleted)}
(root/'cleanup_manifest.json').write_text(json.dumps(manifest,indent=2)+'\n','utf8')
print('model groups entries',len(model_map),'saved source bytes',sum(x[3] for x in model_map))
for x in model_map: print('MODEL',x)
print('texture copies',len(texture_map),'saved source bytes',sum(x[3] for x in texture_map))
for x in texture_map: print('TEXTURE',x)
print('modified',len(manifest['modified']),'deleted',len(manifest['deleted']))
