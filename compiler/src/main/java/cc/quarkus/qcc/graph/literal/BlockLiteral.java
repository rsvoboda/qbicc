package cc.quarkus.qcc.graph.literal;

import java.util.Objects;

import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.ValueVisitor;
import cc.quarkus.qcc.type.BlockType;

public final class BlockLiteral extends Literal {
    private final BlockType type;
    private final BlockLabel blockLabel;
    private final int hashCode;

    BlockLiteral(final BlockType type, final BlockLabel blockLabel) {
        this.type = type;
        this.blockLabel = blockLabel;
        hashCode = Objects.hash(BlockLiteral.class, blockLabel);
    }

    public BlockType getType() {
        return type;
    }

    public BlockLabel getBlockLabel() {
        return blockLabel;
    }

    public boolean equals(final Literal other) {
        return other instanceof BlockLiteral && equals((BlockLiteral) other);
    }

    public boolean equals(final BlockLiteral other) {
        return this == other || other != null && blockLabel.equals(other.blockLabel) && type.equals(other.type);
    }

    public int hashCode() {
        return hashCode;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
