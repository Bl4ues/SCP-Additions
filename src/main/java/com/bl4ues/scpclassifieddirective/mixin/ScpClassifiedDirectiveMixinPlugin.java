package com.bl4ues.scpclassifieddirective.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Keeps optional mixins from loading APIs that are not installed. */
public final class ScpClassifiedDirectiveMixinPlugin implements IMixinConfigPlugin {
    private static final String SCP1576_MIXIN =
            "com.bl4ues.scpclassifieddirective.mixin.Scp1576VoiceBridgeMixin";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName,
            String mixinClassName) {
        if (!SCP1576_MIXIN.equals(mixinClassName)) return true;
        try {
            Class.forName("de.maxhenkel.voicechat.api.events.MicrophonePacketEvent",
                    false, getClass().getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass,
            String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass,
            String mixinClassName, IMixinInfo mixinInfo) {
    }
}
