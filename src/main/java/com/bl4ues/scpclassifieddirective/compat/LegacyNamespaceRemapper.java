package com.bl4ues.scpclassifieddirective.compat;

import java.util.List;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.MissingMappingsEvent;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

@Mod.EventBusSubscriber(modid=ScpClassifiedDirectiveMod.MODID,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class LegacyNamespaceRemapper {
 private static final Set<String> OLD=Set.of("scp_additions","scp_unity_extra_blocks","scp_ublocks","scpinventory");
 private LegacyNamespaceRemapper(){}
 @SubscribeEvent @SuppressWarnings({"rawtypes","unchecked"})
 public static void remap(MissingMappingsEvent e){
  IForgeRegistry reg=e.getRegistry(); if(reg==null)return;
  ResourceKey<? extends Registry<?>> key=e.getKey();
  List<MissingMappingsEvent.Mapping<Object>> maps=(List)e.getAllMappings((ResourceKey)key);
  for(var m:maps){ var old=m.getKey(); if(!OLD.contains(old.getNamespace()))continue;
   var next=new ResourceLocation(ScpClassifiedDirectiveMod.MODID,old.getPath()); Object target=reg.getValue(next);
   if(target!=null)m.remap(target); else ScpClassifiedDirectiveMod.LOGGER.warn("No migration target for {} in {}",old,reg.getRegistryName());
  }
 }
}
