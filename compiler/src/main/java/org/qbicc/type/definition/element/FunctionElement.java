package org.qbicc.type.definition.element;

import java.util.List;

import org.qbicc.type.FunctionType;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.annotation.type.TypeAnnotationList;
import io.smallrye.common.constraint.Assert;

/**
 * An element that represents some function.
 */
public final class FunctionElement extends InvokableElement implements NamedElement {
    private final String name;

    FunctionElement(final Builder builder) {
        super(builder);
        Assert.checkNotNullParam("builder.type", builder.type);
        name = Assert.checkNotNullParam("builder.name", builder.name);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public <T, R> R accept(ElementVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    public String toString() {
        return "function " + getName();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends InvokableElement.Builder implements NamedElement.Builder {
        private String name;

        Builder() {}

        @Override
        public void setName(String name) {
            this.name = Assert.checkNotNullParam("name", name);
        }

        @Override
        public FunctionElement build() {
            return new FunctionElement(this);
        }

        @Override
        public void setType(FunctionType type) {
            super.setType(Assert.checkNotNullParam("type", type));
        }

        @Override
        public void setVisibleAnnotations(List<Annotation> annotations) {
            throw new UnsupportedOperationException("Functions do not support annotations");
        }

        @Override
        public void setInvisibleAnnotations(List<Annotation> annotations) {
            throw new UnsupportedOperationException("Functions do not support annotations");
        }

        @Override
        public void setReturnVisibleTypeAnnotations(TypeAnnotationList returnVisibleTypeAnnotations) {
            throw new UnsupportedOperationException("Functions do not support annotations");
        }

        @Override
        public void setReturnInvisibleTypeAnnotations(TypeAnnotationList returnInvisibleTypeAnnotations) {
            throw new UnsupportedOperationException("Functions do not support annotations");
        }

    }
}
