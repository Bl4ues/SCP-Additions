package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.scp079.ScpRoleSelectorScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/** Keeps the SCP-079 role card presentation aligned with its current toolkit. */
@Mixin(value = ScpRoleSelectorScreen.class, remap = false)
public abstract class Scp079RoleSelectorPreviewScaleMixin {
    @ModifyConstant(method = "<clinit>",
            constant = @Constant(floatValue = 2.28F), require = 1)
    private static float scpClassifiedDirective$rebalancePreview(float original) {
        return 1.30F;
    }

    /**
     * The selector's Ability record is intentionally private. Rebuild the list
     * at its single four-entry List.of call so the card can stay decoupled from
     * the implementation classes that actually own these systems.
     */
    @Redirect(method = "<clinit>",
            at = @At(value = "INVOKE",
                    target = "Ljava/util/List;of(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/List;"),
            require = 1)
    private static List<?> scpClassifiedDirective$currentAbilities(Object first,
            Object second, Object third, Object fourth) {
        try {
            Constructor<?> constructor = first.getClass().getDeclaredConstructor(
                    String.class, String.class);
            constructor.setAccessible(true);
            List<Object> abilities = new ArrayList<>(5);
            abilities.add(constructor.newInstance("FACILITY SURVEILLANCE",
                    "Navigate mapped cameras and use the facility map to follow activity pings."));
            abilities.add(constructor.newInstance("FACILITY CONTROL",
                    "Operate doors, deny access, and suppress Tesla Gates from camera feeds."));
            abilities.add(constructor.newInstance("ROOM OVERRIDES",
                    "Blackout lights or trigger Lockdown inside the active mapped room."));
            abilities.add(constructor.newInstance("SPEAKER BROADCAST",
                    "Broadcast voice through Speakers registered to the current mapped room."));
            abilities.add(constructor.newInstance("PROCESSING POWER",
                    "Remote actions consume AP; powered Auxiliary units restore it over time."));
            return abilities;
        } catch (ReflectiveOperationException exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not update the SCP-079 role selector abilities", exception);
            return List.of(first, second, third, fourth);
        }
    }

    @ModifyConstant(method = "renderDetails",
            constant = @Constant(intValue = 43), require = 1)
    private int scpClassifiedDirective$compactAbilityRows(int original) {
        return 36;
    }
}
