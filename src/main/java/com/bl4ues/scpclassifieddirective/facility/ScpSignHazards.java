package com.bl4ues.scpclassifieddirective.facility;

import net.minecraft.resources.ResourceLocation;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Stable hazard IDs shared by the editor, network data and world renderer. */
public final class ScpSignHazards {
    public static final Option NONE = new Option("", "None");

    public static final List<Option> OPTIONS = List.of(
            NONE,
            new Option("adaptive_object", "Adaptive Object"),
            new Option("antimemetic_hazard", "Antimemetic Hazard"),
            new Option("auditory_hazard", "Auditory Hazard"),
            new Option("autonomous_object", "Autonomous Object"),
            new Option("biohazard", "Biohazard"),
            new Option("bodily_harm_hazard", "Bodily Harm Hazard"),
            new Option("cognitohazard", "Cognitohazard"),
            new Option("corrosive_hazard", "Corrosive Hazard"),
            new Option("ectoentropic", "Ectoentropic"),
            new Option("electrical_hazard", "Electrical Hazard"),
            new Option("existential_threat", "Existential Threat"),
            new Option("extradimensional", "Extradimensional"),
            new Option("fire_hazard", "Fire Hazard"),
            new Option("hive-mind_organisms", "Hive-Mind Organisms"),
            new Option("indirect_injury_hazard", "Indirect Injury Hazard"),
            new Option("infohazard", "Infohazard"),
            new Option("invisible_object", "Invisible Object"),
            new Option("liquid", "Liquid"),
            new Option("medical_application", "Medical Application"),
            new Option("memetic_hazard", "Memetic Hazard"),
            new Option("mutagen_hazard", "Mutagen Hazard"),
            new Option("nonstandard_spacetime", "Nonstandard Spacetime"),
            new Option("radioactive_hazard", "Radioactive Hazard"),
            new Option("reality_manipulation", "Reality Manipulation"),
            new Option("self-evolving_system", "Self-Evolving System"),
            new Option("self-replicating_object", "Self-Replicating Object"),
            new Option("sentient_and_violent", "Sentient and Violent"),
            new Option("sentient_object", "Sentient Object"),
            new Option("teleporting_object", "Teleporting Object"),
            new Option("thermal", "Thermal"),
            new Option("toxic_hazard", "Toxic Hazard")
    );

    private static final Map<String, Option> BY_ID = buildIndex();

    private ScpSignHazards() {
    }

    private static Map<String, Option> buildIndex() {
        Map<String, Option> result = new LinkedHashMap<>();
        for (Option option : OPTIONS) {
            result.put(option.id(), option);
            if (option.id().contains("-")) {
                result.put(option.id().replace('-', '_'), option);
            }
        }

        // Compatibility with provisional IDs used while the asset set was pending.
        result.put("biological_hazard", optionFromList("biohazard"));
        result.put("hive_mind_organisms", optionFromList("hive-mind_organisms"));
        result.put("self_evolving_system", optionFromList("self-evolving_system"));
        result.put("self_replicating_object", optionFromList("self-replicating_object"));
        result.put("invisible_objective", optionFromList("invisible_object"));
        return Map.copyOf(result);
    }

    private static Option optionFromList(String id) {
        for (Option option : OPTIONS) {
            if (option.id().equals(id)) return option;
        }
        return NONE;
    }

    public static Option option(String id) {
        return BY_ID.getOrDefault(normalizeLookup(id), NONE);
    }

    public static String normalizeId(String id) {
        return option(id).id();
    }

    private static String normalizeLookup(String id) {
        if (id == null || id.isBlank()) return "";
        return id.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    /** Returns exactly three unique slots. Later duplicates are cleared. */
    public static List<String> normalizeSlots(List<String> input) {
        List<String> result = new ArrayList<>(ScpSignData.HAZARD_SLOTS);
        Set<String> used = new HashSet<>();
        for (int slot = 0; slot < ScpSignData.HAZARD_SLOTS; slot++) {
            String id = input != null && slot < input.size()
                    ? normalizeId(input.get(slot)) : "";
            if (!id.isEmpty() && !used.add(id)) id = "";
            result.add(id);
        }
        return List.copyOf(result);
    }

    public record Option(String id, String displayName) {
        public boolean isNone() {
            return id.isEmpty();
        }

        public ResourceLocation texture() {
            String file = isNone() ? "none" : id;
            return new ResourceLocation(ScpClassifiedDirectiveMod.MODID,
                    "textures/screens/scpsign/" + file + ".png");
        }
    }
}
