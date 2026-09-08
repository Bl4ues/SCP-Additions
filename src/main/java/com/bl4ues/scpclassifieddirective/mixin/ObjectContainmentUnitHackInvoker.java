package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.facility.ObjectContainmentUnitModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Narrow bridge used by the Hacking Device to trigger the OCU's real access path. */
@Mixin(ObjectContainmentUnitModule.UnitBlockEntity.class)
public interface ObjectContainmentUnitHackInvoker {
    @Invoker("playReaderSound")
    void scpclassifieddirective$playReaderSound(boolean accepted);

    @Invoker("startOpening")
    void scpclassifieddirective$startOpening();
}
