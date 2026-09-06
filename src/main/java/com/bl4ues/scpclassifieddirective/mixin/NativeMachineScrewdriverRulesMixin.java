package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.inventory.context.ContextInteractionRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

/** Extends the built-in screwdriver target set to configurable SCP machines. */
@Mixin(value = ContextInteractionRegistry.class, remap = false)
public abstract class NativeMachineScrewdriverRulesMixin {
    @Redirect(method = "registerNativeScrewdriverRules",
            at = @At(value = "INVOKE",
                    target = "Ljava/util/List;of(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/List;"),
            require = 1)
    private static List<?> scpClassifiedDirective$addMachineTargets(Object first,
            Object second, Object third, Object fourth, Object fifth,
            Object sixth, Object seventh) {
        List<Object> targets = new ArrayList<>(12);
        targets.add(first);
        targets.add(second);
        targets.add(third);
        targets.add(fourth);
        targets.add(fifth);
        targets.add(sixth);
        targets.add(seventh);
        targets.add("scp_294");
        targets.add("scp_914");
        targets.add("scp_914_reservation");
        targets.add("scp_914_collision");
        targets.add("scp_914_door_collision");
        return targets;
    }
}
