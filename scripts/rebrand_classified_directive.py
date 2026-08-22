from pathlib import Path
import json, re, shutil

R=Path(__file__).resolve().parents[1]
OLD='scp_additions'; NEW='scp_classified_directive'
BRAND_OLD='SCP Additions'; BRAND_NEW='SCP: Classified Directive'
P_OLD='net.mcreator.scpadditions'; P_NEW='com.bl4ues.scpclassifieddirective'
I_OLD='com.bl4ues.scpinventory'; I_NEW=P_NEW+'.inventory'
LEGACY=(OLD,'scp_unity_extra_blocks','scp_ublocks','scpinventory')
EXT={'.java','.json','.toml','.gradle','.properties','.cfg','.md','.txt','.mcmeta','.yml','.yaml','.xml','.csv'}
SKIP={'.git','.gradle','build','run','out'}

def files():
    result=[]
    for p in R.rglob('*'):
        if not p.is_file(): continue
        rel=p.relative_to(R)
        # GITHUB_TOKEN may update repository contents but cannot publish workflow-file
        # changes. Actions workflows are migrated separately through the GitHub connector
        # after the validated rebrand commit lands on master.
        if rel.parts[:2]==('.github','workflows'): continue
        if any(x in SKIP for x in rel.parts): continue
        if p.suffix.lower() in EXT: result.append(p)
    return result

def rw(p):
    try: s=p.read_text(encoding='utf-8')
    except UnicodeDecodeError: return
    o=s
    for a,b in ((I_OLD,I_NEW),(P_OLD,P_NEW),('com.bl4ues.scpadditions',P_NEW),('ScpAdditions','ScpClassifiedDirective'),('scpAdditions','scpClassifiedDirective'),('SCP ADDITIONS','SCP: CLASSIFIED DIRECTIVE'),(BRAND_OLD,BRAND_NEW)):
        s=s.replace(a,b)
    for ns in LEGACY:
        s=re.sub(rf'(?<![A-Za-z0-9_]){re.escape(ns)}(?![A-Za-z0-9_])',NEW,s)
    for a,b in (
        ('scp_additions_voicechat','scp_classified_directive_voicechat'),
        ('scp-additions-${{ github.sha }}','scp-classified-directive-${{ github.sha }}'),
        ('config/scpadditions','config/'+NEW),('config/scpinventory','config/'+NEW),
        ('"scpadditions"','"'+NEW+'"'),("'scpadditions'","'"+NEW+"'"),
    ): s=s.replace(a,b)
    if s!=o: p.write_text(s,encoding='utf-8')

def merge_json(dst,src):
    a=json.loads(dst.read_text()); b=json.loads(src.read_text()); a.update(b)
    dst.write_text(json.dumps(a,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')

def merge(src,dst,ns):
    if not src.exists(): return
    dst.mkdir(parents=True,exist_ok=True)
    for p in sorted(src.rglob('*')):
        if not p.is_file(): continue
        q=dst/p.relative_to(src); q.parent.mkdir(parents=True,exist_ok=True)
        if not q.exists(): shutil.move(str(p),str(q)); continue
        if q.read_bytes()==p.read_bytes(): p.unlink(); continue
        rel=p.relative_to(src).as_posix()
        if p.suffix=='.json' and (rel.startswith('lang/') or rel=='sounds.json'):
            merge_json(q,p); p.unlink(); continue
        if ns in {'scp_unity_extra_blocks','scp_ublocks'}:
            q.unlink(); shutil.move(str(p),str(q)); continue
        raise RuntimeError(f'collision {ns}: {rel}')
    shutil.rmtree(src,ignore_errors=True)

def consolidate(kind):
    for d in sorted([p for p in R.rglob(kind) if p.is_dir()],key=lambda p:len(p.parts),reverse=True):
        if any(x in SKIP for x in d.relative_to(R).parts): continue
        dst=d/NEW; main=d/OLD
        if main.exists() and not dst.exists(): main.rename(dst)
        elif main.exists(): merge(main,dst,OLD)
        for ns in LEGACY[1:]: merge(d/ns,dst,ns)

def paths():
    j=R/'src/main/java'; tmp=j/'com/bl4ues/__inv_tmp'; oldi=j/'com/bl4ues/scpinventory'; oldm=j/'net/mcreator/scpadditions'; new=j/'com/bl4ues/scpclassifieddirective'
    if oldi.exists(): tmp.parent.mkdir(parents=True,exist_ok=True); oldi.rename(tmp)
    if oldm.exists(): new.parent.mkdir(parents=True,exist_ok=True); oldm.rename(new)
    if tmp.exists():
        target = new/'inventory'; target.mkdir(parents=True, exist_ok=True)
        for p in sorted(tmp.rglob('*')):
            if not p.is_file(): continue
            q = target/p.relative_to(tmp); q.parent.mkdir(parents=True, exist_ok=True)
            if q.exists():
                if q.read_bytes() != p.read_bytes(): raise RuntimeError(f'java package collision: {q.relative_to(j)}')
                p.unlink()
            else: shutil.move(str(p), str(q))
        shutil.rmtree(tmp, ignore_errors=True)
    for p in list(j.rglob('*ScpAdditions*.java')): p.rename(p.with_name(p.name.replace('ScpAdditions','ScpClassifiedDirective')))
    res=R/'src/main/resources'; a=res/'scp_additions.mixins.json'; b=res/'scp_classified_directive.mixins.json'
    if a.exists(): a.rename(b)
    a=res/'resourcepacks/scp_additions_voicechat'; b=res/'resourcepacks/scp_classified_directive_voicechat'
    if a.exists(): a.rename(b)

def configs():
    root=R/'config'; dst=root/NEW; dst.mkdir(parents=True,exist_ok=True)
    for n in ('scpadditions','scpinventory'): merge(root/n,dst,n)

def gradle_fix():
    p=R/'build.gradle'; s=p.read_text()
    s=re.sub(r'(        if \(legacyAssets\.exists\(\)\) \{\n).*?(            // The original Unity button has geometry)',r'\1\2',s,flags=re.S)
    s=re.sub(r'        // SCP UBlocks follows.*?        // Copy loot tables, recipes and other namespace-owned data into the new\n', '        // Data already lives in the unified namespace.\n',s,flags=re.S)
    s=re.sub(r'        def legacyData = file\(.*?\n        \}\n        def allData =', '        def allData =',s,flags=re.S)
    p.write_text(s)

def compat():
    d=R/'src/main/java/com/bl4ues/scpclassifieddirective/compat'; d.mkdir(parents=True,exist_ok=True)
    (d/'LegacyNamespaceRemapper.java').write_text('''package com.bl4ues.scpclassifieddirective.compat;\n\nimport java.util.List;\nimport java.util.Set;\nimport net.minecraft.core.Registry;\nimport net.minecraft.resources.ResourceKey;\nimport net.minecraft.resources.ResourceLocation;\nimport net.minecraftforge.eventbus.api.SubscribeEvent;\nimport net.minecraftforge.fml.common.Mod;\nimport net.minecraftforge.registries.IForgeRegistry;\nimport net.minecraftforge.registries.MissingMappingsEvent;\nimport com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;\n\n@Mod.EventBusSubscriber(modid=ScpClassifiedDirectiveMod.MODID,bus=Mod.EventBusSubscriber.Bus.FORGE)\npublic final class LegacyNamespaceRemapper {\n private static final Set<String> OLD=Set.of("scp_additions","scp_unity_extra_blocks","scp_ublocks","scpinventory");\n private LegacyNamespaceRemapper(){}\n @SubscribeEvent @SuppressWarnings({"rawtypes","unchecked"})\n public static void remap(MissingMappingsEvent e){\n  IForgeRegistry reg=e.getRegistry(); if(reg==null)return;\n  ResourceKey<? extends Registry<?>> key=e.getKey();\n  List<MissingMappingsEvent.Mapping<Object>> maps=(List)e.getAllMappings((ResourceKey)key);\n  for(var m:maps){ var old=m.getKey(); if(!OLD.contains(old.getNamespace()))continue;\n   var next=new ResourceLocation(ScpClassifiedDirectiveMod.MODID,old.getPath()); Object target=reg.getValue(next);\n   if(target!=null)m.remap(target); else ScpClassifiedDirectiveMod.LOGGER.warn("No migration target for {} in {}",old,reg.getRegistryName());\n  }\n }\n}\n''')
    (d/'LegacyConfigMigration.java').write_text('''package com.bl4ues.scpclassifieddirective.compat;\n\nimport java.io.IOException;\nimport java.nio.file.*;\nimport net.minecraftforge.fml.loading.FMLPaths;\nimport com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;\n\npublic final class LegacyConfigMigration {\n private LegacyConfigMigration(){}\n public static void migrate(){ Path root=FMLPaths.CONFIGDIR.get(),dst=root.resolve(ScpClassifiedDirectiveMod.MODID); try{ Files.createDirectories(dst); copy(root.resolve("scpadditions"),dst); copy(root.resolve("scpinventory"),dst); }catch(IOException e){ ScpClassifiedDirectiveMod.LOGGER.warn("Could not migrate legacy SCP configuration",e); }}\n private static void copy(Path src,Path dst)throws IOException{ if(!Files.isDirectory(src))return; try(var s=Files.walk(src)){ for(Path p:s.toList()){ Path q=dst.resolve(src.relativize(p)); if(Files.isDirectory(p))Files.createDirectories(q); else if(!Files.exists(q)){Files.createDirectories(q.getParent());Files.copy(p,q,StandardCopyOption.COPY_ATTRIBUTES);}}}}\n}\n''')
    m=R/'src/main/java/com/bl4ues/scpclassifieddirective/ScpClassifiedDirectiveMod.java'; s=m.read_text()
    anchor='import com.bl4ues.scpclassifieddirective.config.Scp714ConfigBootstrap;\n'
    s=s.replace(anchor,anchor+'import com.bl4ues.scpclassifieddirective.compat.LegacyConfigMigration;\n')
    s=s.replace('    public ScpClassifiedDirectiveMod() {\n','    public ScpClassifiedDirectiveMod() {\n        LegacyConfigMigration.migrate();\n')
    s=s.replace('@Mod("scp_classified_directive")','@Mod(ScpClassifiedDirectiveMod.MODID)')
    m.write_text(s)

def changelog():
    p=R/'CHANGELOG.md'; s=p.read_text(); h='- SCP: Classified Directive rebrand\n'
    if h not in s: s=s.replace('## Highlights\n','## Highlights\n'+h,1)
    section='''\n## SCP: Classified Directive rebrand\n\n- Rebranded the project as **SCP: Classified Directive** to reflect its expanded scope;\n- Migrated the public mod ID and registry namespace from the legacy ID to `scp_classified_directive`;\n- Consolidated the former Unity blocks, UBlocks, and inventory resource namespaces into `scp_classified_directive`;\n- Added Forge missing-mapping migration so legacy registered world content resolves to the new namespace;\n- Unified legacy configuration directories under `config/scp_classified_directive` without overwriting migrated files;\n- Refactored Java packages, mod metadata, mixin identifiers, build output naming, embedded resource-pack naming, and project-facing text around the new identity.\n'''
    if '## SCP: Classified Directive rebrand' not in s: s=s.replace('\n## SCP-106',section+'\n## SCP-106',1)
    p.write_text(s)
    p=R/'src/main/java/com/bl4ues/scpclassifieddirective/client/MainMenuWhatsNewPanelClient.java'; s=p.read_text(); item='            "SCP: Classified Directive rebrand",\n'; mark='    private static final List<String> HIGHLIGHTS = List.of(\n'
    if item not in s: s=s.replace(mark,mark+item,1)
    p.write_text(s)

def cleancheck():
    bad=[]
    for p in files():
        rel=p.relative_to(R).as_posix()
        if rel.endswith(('LegacyNamespaceRemapper.java','LegacyConfigMigration.java')): continue
        s=p.read_text(encoding='utf-8',errors='ignore')
        if P_OLD in s or I_OLD in s or any(ns+':' in s for ns in LEGACY): bad.append(rel)
    if bad: raise RuntimeError('legacy runtime refs: '+', '.join(sorted(set(bad))))
    for p in [R/'src/main/resources/assets'/OLD,R/'src/main/resources/assets/scpinventory',R/'src/main/resources/assets/scp_ublocks',R/'src/main/resources/assets/scp_unity_extra_blocks',R/'src/main/resources/data'/OLD,R/'src/main/resources/data/scpinventory']:
        if p.exists(): raise RuntimeError('legacy directory remains: '+str(p))

def main():
    for p in files(): rw(p)
    paths()
    for p in files(): rw(p)
    consolidate('assets'); consolidate('data'); configs()
    for p in files(): rw(p)
    gradle_fix(); compat(); changelog()
    for p in files():
        if p.name not in {'LegacyNamespaceRemapper.java','LegacyConfigMigration.java'}: rw(p)
    cleancheck(); print('rebrand prepared')
if __name__=='__main__': main()