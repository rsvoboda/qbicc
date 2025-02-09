package org.qbicc.plugin.gc.nogc;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.type.ClassObjectType;
import org.qbicc.context.ClassContext;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.MethodElement;

/**
 *
 */
public final class NoGc {
    private final CompilationContext ctxt;
    private final MethodElement allocateMethod;
    private final MethodElement copyMethod;
    private final MethodElement zeroMethod;
    private final ClassObjectType stackObjectType;

    private NoGc(final CompilationContext ctxt) {
        this.ctxt = ctxt;
        ClassContext classContext = ctxt.getBootstrapClassContext();
        DefinedTypeDefinition defined = classContext.findDefinedType("org/qbicc/runtime/gc/nogc/NoGcHelpers");
        if (defined == null) {
            throw runtimeMissing();
        }
        LoadedTypeDefinition loaded = defined.load();
        int index = loaded.findMethodIndex(e -> e.getName().equals("allocate"));
        if (index == -1) {
            throw methodMissing();
        }
        allocateMethod = loaded.getMethod(index);
        index = loaded.findMethodIndex(e -> e.getName().equals("copy"));
        if (index == -1) {
            throw methodMissing();
        }
        copyMethod = loaded.getMethod(index);
        index = loaded.findMethodIndex(e -> e.getName().equals("clear"));
        if (index == -1) {
            throw methodMissing();
        }
        zeroMethod = loaded.getMethod(index);
        defined = classContext.findDefinedType("org/qbicc/runtime/StackObject");
        if (defined == null) {
            throw runtimeMissing();
        }
        loaded = defined.load();
        stackObjectType = loaded.getClassType();
    }

    private static IllegalStateException methodMissing() {
        return new IllegalStateException("Required method is missing from the NoGC helpers");
    }

    private static IllegalStateException runtimeMissing() {
        return new IllegalStateException("The NoGC helpers runtime classes are not present in the bootstrap class path");
    }

    private static final AttachmentKey<NoGc> KEY = new AttachmentKey<>();

    public static NoGc get(CompilationContext ctxt) {
        return ctxt.computeAttachmentIfAbsent(KEY, () -> new NoGc(ctxt));
    }

    public MethodElement getAllocateMethod() {
        return allocateMethod;
    }

    public MethodElement getCopyMethod() {
        return copyMethod;
    }

    public MethodElement getZeroMethod() {
        return zeroMethod;
    }

    public ClassObjectType getStackObjectType() {
        return stackObjectType;
    }
}

