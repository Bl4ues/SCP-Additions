package com.bl4ues.scpclassifieddirective.block;

/** Legacy reload ID retained only so old worlds can normalize back to idle. */
public class DeconOpenReloadBlock extends AbstractDecontaminationBlock {
    @Override
    protected boolean isClosedState() {
        return false;
    }
}
