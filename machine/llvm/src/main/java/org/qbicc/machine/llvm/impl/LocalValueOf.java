package org.qbicc.machine.llvm.impl;

import java.io.IOException;

final class LocalValueOf extends AbstractValue {
    final AbstractInstruction instruction;
    final int index;

    LocalValueOf(final AbstractInstruction instruction, final int index) {
        this.instruction = instruction;
        this.index = index;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        target.append('%').append('L');
        return appendHex(target, index);
    }
}
