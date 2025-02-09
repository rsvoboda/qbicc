package org.qbicc.graph;

import java.util.List;
import java.util.Objects;

import org.qbicc.type.FunctionType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A plain method or function call with no side-effects.
 * The return value of the target is the type of this node (which may be {@link org.qbicc.type.VoidType VoidType}).
 * Exceptions are considered a side-effect, thus the target must not throw exceptions (this excludes most Java methods, which can throw {@code OutOfMemoryError} among other things).
 *
 * @see BasicBlockBuilder#callNoSideEffects(ValueHandle, List)
 */
public final class CallNoSideEffects extends AbstractValue {
    private final ValueHandle target;
    private final List<Value> arguments;
    private final FunctionType functionType;

    CallNoSideEffects(Node callSite, ExecutableElement element, int line, int bci, ValueHandle target, List<Value> arguments) {
        super(callSite, element, line, bci);
        this.target = target;
        this.arguments = arguments;
        functionType = (FunctionType) target.getValueType();
    }

    @Override
    int calcHashCode() {
        return Objects.hash(CallNoSideEffects.class, target, arguments);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CallNoSideEffects && equals((CallNoSideEffects) other);
    }

    public boolean equals(CallNoSideEffects other) {
        return this == other || other != null && target.equals(other.target) && arguments.equals(other.arguments);
    }

    public FunctionType getFunctionType() {
        return functionType;
    }

    @Override
    public ValueType getType() {
        return getFunctionType().getReturnType();
    }

    public List<Value> getArguments() {
        return arguments;
    }

    @Override
    public int getValueDependencyCount() {
        return arguments.size();
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return arguments.get(index);
    }

    @Override
    public boolean hasValueHandleDependency() {
        return true;
    }

    @Override
    public ValueHandle getValueHandle() {
        return target;
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}
