package org.qbicc.graph.literal;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.qbicc.graph.ValueVisitor;
import org.qbicc.type.ArrayType;

/**
 * A literal array.  This is not a Java array object literal (use {@code ObjectLiteral}).
 */
public final class ArrayLiteral extends Literal {
    private final List<Literal> values;
    private final ArrayType type;
    private final int hashCode;

    ArrayLiteral(final ArrayType type, final List<Literal> values) {
        this.values = values;
        this.type = type;
        hashCode = Objects.hash(type, values);
    }

    public List<Literal> getValues() {
        return values;
    }

    public ArrayType getType() {
        return type;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public boolean isZero() {
        return false;
    }

    public boolean equals(final Literal other) {
        return other instanceof ArrayLiteral && equals((ArrayLiteral) other);
    }

    public boolean equals(final ArrayLiteral other) {
        return this == other || other != null && hashCode == other.hashCode && values.equals(other.values) && type.equals(other.type);
    }

    public int hashCode() {
        return hashCode;
    }

    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    public StringBuilder toString(StringBuilder target) {
        target.append('[');
        Iterator<Literal> iterator = values.iterator();
        if (iterator.hasNext()) {
            target.append(iterator.next());
            while (iterator.hasNext()) {
                target.append(',');
                target.append(iterator.next());
            }
        }
        target.append(']');
        return target;
    }
}
