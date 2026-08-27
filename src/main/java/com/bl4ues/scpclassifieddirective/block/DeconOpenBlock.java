package com.bl4ues.scpclassifieddirective.block;

/** Public idle endpoint for the rebuilt GeckoLib Decontamination Checkpoint. */
public class DeconOpenBlock extends AbstractDecontaminationBlock {
    @Override
    protected boolean isClosedState() {
        return false;
    }
}
