/*
 *    MCreator note:
 *
 *    This file is autogenerated to connect all MCreator mod elements together.
 *
 */
package net.mcreator.scpadditions;

import net.minecraftforge.forgespi.language.ModFileScanData;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.util.ResourceLocation;
import net.minecraft.tags.Tag;
import net.minecraft.network.PacketBuffer;
import net.minecraft.item.Item;
import net.minecraft.entity.EntityType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.block.Block;

import java.util.function.Supplier;
import java.util.function.Function;
import java.util.function.BiConsumer;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Collections;
import java.util.ArrayList;

import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;

public class ScpAdditionsModElements {
	public final List<ModElement> elements = new ArrayList<>();
	public final List<Supplier<Block>> blocks = new ArrayList<>();
	public final List<Supplier<Item>> items = new ArrayList<>();
	public final List<Supplier<EntityType<?>>> entities = new ArrayList<>();
	public final List<Supplier<Enchantment>> enchantments = new ArrayList<>();
	public static Map<ResourceLocation, net.minecraft.util.SoundEvent> sounds = new HashMap<>();

	public ScpAdditionsModElements() {
		sounds.put(new ResourceLocation("scp_additions", "teslaactivate"),
				new net.minecraft.util.SoundEvent(new ResourceLocation("scp_additions", "teslaactivate")));
		sounds.put(new ResourceLocation("scp_additions", "teslaready"),
				new net.minecraft.util.SoundEvent(new ResourceLocation("scp_additions", "teslaready")));
		sounds.put(new ResourceLocation("scp_additions", "teslarecharge"),
				new net.minecraft.util.SoundEvent(new ResourceLocation("scp_additions", "teslarecharge")));
		sounds.put(new ResourceLocation("scp_additions", "click"), new net.minecraft.util.SoundEvent(new ResourceLocation("scp_additions", "click")));
		sounds.put(new ResourceLocation("scp_additions", "terminalon"),
				new net.minecraft.util.SoundEvent(new ResourceLocation("scp_additions", "terminalon")));
		sounds.put(new ResourceLocation("scp_additions", "terminaloff"),
				new net.minecraft.util.SoundEvent(new ResourceLocation("scp_additions", "terminaloff")));
		sounds.put(new ResourceLocation("scp_additions", "scp079_1"),
				new net.minecraft.util.SoundEvent(new ResourceLocation("scp_additions", "scp079_1")));
		sounds.put(new ResourceLocation("scp_additions", "candyeat"),
				new net.minecraft.util.SoundEvent(new ResourceLocation("scp_additions", "candyeat")));
		sounds.put(new ResourceLocation("scp_additions", "candy"), new net.minecraft.util.SoundEvent(new ResourceLocation("scp_additions", "candy")));
		sounds.put(new ResourceLocation("scp_additions", "scp330death"),
				new net.minecraft.util.SoundEvent(new ResourceLocation("scp_additions", "scp330death")));
		sounds.put(new ResourceLocation("scp_additions", "scp1176"),
				new net.minecraft.util.SoundEvent(new ResourceLocation("scp_additions", "scp1176")));
		sounds.put(new ResourceLocation("scp_additions", "scp902"),
				new net.minecraft.util.SoundEvent(new ResourceLocation("scp_additions", "scp902")));
		sounds.put(new ResourceLocation("scp_additions", "scp902closing"),
				new net.minecraft.util.SoundEvent(new ResourceLocation("scp_additions", "scp902closing")));
		sounds.put(new ResourceLocation("scp_additions", "scp902opening"),
				new net.minecraft.util.SoundEvent(new ResourceLocation("scp_additions", "scp902opening")));
		sounds.put(new ResourceLocation("scp_additions", "scp079_2"),
				new net.minecraft.util.SoundEvent(new ResourceLocation("scp_additions", "scp079_2")));
		sounds.put(new ResourceLocation("scp_additions", "button"),
				new net.minecraft.util.SoundEvent(new ResourceLocation("scp_additions", "button")));
		sounds.put(new ResourceLocation("scp_additions", "scp914doorclose"),
				new net.minecraft.util.SoundEvent(new ResourceLocation("scp_additions", "scp914doorclose")));
		sounds.put(new ResourceLocation("scp_additions", "scp914dooropen"),
				new net.minecraft.util.SoundEvent(new ResourceLocation("scp_additions", "scp914dooropen")));
		sounds.put(new ResourceLocation("scp_additions", "scp914key"),
				new net.minecraft.util.SoundEvent(new ResourceLocation("scp_additions", "scp914key")));
		sounds.put(new ResourceLocation("scp_additions", "scp914refining"),
				new net.minecraft.util.SoundEvent(new ResourceLocation("scp_additions", "scp914refining")));
		sounds.put(new ResourceLocation("scp_additions", "scp914dial"),
				new net.minecraft.util.SoundEvent(new ResourceLocation("scp_additions", "scp914dial")));
		sounds.put(new ResourceLocation("scp_additions", "scp914inside"),
				new net.minecraft.util.SoundEvent(new ResourceLocation("scp_additions", "scp914inside")));
		sounds.put(new ResourceLocation("scp_additions", "scp914death"),
				new net.minecraft.util.SoundEvent(new ResourceLocation("scp_additions", "scp914death")));
		sounds.put(new ResourceLocation("scp_additions", "scp059box"),
				new net.minecraft.util.SoundEvent(new ResourceLocation("scp_additions", "scp059box")));
		sounds.put(new ResourceLocation("scp_additions", "geiger2"),
				new net.minecraft.util.SoundEvent(new ResourceLocation("scp_additions", "geiger2")));
		sounds.put(new ResourceLocation("scp_additions", "geiger1"),
				new net.minecraft.util.SoundEvent(new ResourceLocation("scp_additions", "geiger1")));
		sounds.put(new ResourceLocation("scp_additions", "scp059_1"),
				new net.minecraft.util.SoundEvent(new ResourceLocation("scp_additions", "scp059_1")));
		sounds.put(new ResourceLocation("scp_additions", "geiger3"),
				new net.minecraft.util.SoundEvent(new ResourceLocation("scp_additions", "geiger3")));
		sounds.put(new ResourceLocation("scp_additions", "spray"), new net.minecraft.util.SoundEvent(new ResourceLocation("scp_additions", "spray")));
		try {
			ModFileScanData modFileInfo = ModList.get().getModFileById("scp_additions").getFile().getScanResult();
			Set<ModFileScanData.AnnotationData> annotations = modFileInfo.getAnnotations();
			for (ModFileScanData.AnnotationData annotationData : annotations) {
				if (annotationData.getAnnotationType().getClassName().equals(ModElement.Tag.class.getName())) {
					Class<?> clazz = Class.forName(annotationData.getClassType().getClassName());
					if (clazz.getSuperclass() == ScpAdditionsModElements.ModElement.class)
						elements.add((ScpAdditionsModElements.ModElement) clazz.getConstructor(this.getClass()).newInstance(this));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		Collections.sort(elements);
		elements.forEach(ScpAdditionsModElements.ModElement::initElements);
		MinecraftForge.EVENT_BUS.register(new ScpAdditionsModVariables(this));
	}

	public void registerSounds(RegistryEvent.Register<net.minecraft.util.SoundEvent> event) {
		for (Map.Entry<ResourceLocation, net.minecraft.util.SoundEvent> sound : sounds.entrySet())
			event.getRegistry().register(sound.getValue().setRegistryName(sound.getKey()));
	}

	private int messageID = 0;

	public <T> void addNetworkMessage(Class<T> messageType, BiConsumer<T, PacketBuffer> encoder, Function<PacketBuffer, T> decoder,
			BiConsumer<T, Supplier<NetworkEvent.Context>> messageConsumer) {
		ScpAdditionsMod.PACKET_HANDLER.registerMessage(messageID, messageType, encoder, decoder, messageConsumer);
		messageID++;
	}

	public List<ModElement> getElements() {
		return elements;
	}

	public List<Supplier<Block>> getBlocks() {
		return blocks;
	}

	public List<Supplier<Item>> getItems() {
		return items;
	}

	public List<Supplier<EntityType<?>>> getEntities() {
		return entities;
	}

	public List<Supplier<Enchantment>> getEnchantments() {
		return enchantments;
	}

	public static class ModElement implements Comparable<ModElement> {
		@Retention(RetentionPolicy.RUNTIME)
		public @interface Tag {
		}

		protected final ScpAdditionsModElements elements;
		protected final int sortid;

		public ModElement(ScpAdditionsModElements elements, int sortid) {
			this.elements = elements;
			this.sortid = sortid;
		}

		public void initElements() {
		}

		public void init(FMLCommonSetupEvent event) {
		}

		public void serverLoad(FMLServerStartingEvent event) {
		}

		@OnlyIn(Dist.CLIENT)
		public void clientLoad(FMLClientSetupEvent event) {
		}

		@Override
		public int compareTo(ModElement other) {
			return this.sortid - other.sortid;
		}
	}
}
