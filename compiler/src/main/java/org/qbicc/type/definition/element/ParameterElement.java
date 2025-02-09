package org.qbicc.type.definition.element;

import org.qbicc.type.ValueType;
import org.qbicc.context.ClassContext;
import org.qbicc.type.generic.TypeParameterContext;

/**
 * A method parameter variable.
 */
public final class ParameterElement extends VariableElement {
    public static final ParameterElement[] NO_PARAMETERS = new ParameterElement[0];

    ParameterElement(final Builder builder) {
        super(builder);
    }

    public <T, R> R accept(final ElementVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public static Builder builder() {
        return new Builder();
    }

    ValueType resolveTypeDescriptor(final ClassContext classContext, TypeParameterContext paramCtxt) {
        return classContext.resolveTypeFromMethodDescriptor(
                        getTypeDescriptor(),
                        paramCtxt,
                        getTypeSignature(),
                        getVisibleTypeAnnotations(),
                        getInvisibleTypeAnnotations());
    }

    public static final class Builder extends VariableElement.Builder {
        public ParameterElement build() {
            return new ParameterElement(this);
        }
    }
}
