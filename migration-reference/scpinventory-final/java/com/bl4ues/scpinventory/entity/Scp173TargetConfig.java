package com.bl4ues.scpinventory.entity;

import com.bl4ues.scpinventory.config.ScpInventoryConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.Locale;

public final class Scp173TargetConfig {
    private Scp173TargetConfig() {
    }

    public static boolean isConfiguredTarget(LivingEntity entity) {
        if (entity == null || entity instanceof Scp173Entity || !entity.isAlive()) {
            return false;
        }
        if (entity instanceof AbstractScp131Entity) {
            return true;
        }

        EntityType<?> type = entity.getType();
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id == null) {
            return false;
        }

        for (String rawRule : ScpInventoryConfig.scp173Targets()) {
            if (matchesRule(type, id, rawRule)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesRule(EntityType<?> type, ResourceLocation id, String rawRule) {
        if (rawRule == null || rawRule.isBlank()) {
            return false;
        }

        String rule = rawRule.trim();
        if (rule.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(rule.substring(1));
            if (tagId == null) {
                return false;
            }
            return type.builtInRegistryHolder().is(TagKey.create(Registries.ENTITY_TYPE, tagId));
        }

        ResourceLocation exactId = ResourceLocation.tryParse(rule);
        if (exactId != null) {
            return exactId.equals(id);
        }

        String lowerRule = rule.toLowerCase(Locale.ROOT);
        return id.getPath().equals(lowerRule) || id.toString().equals(lowerRule);
    }
}
