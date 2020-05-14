package cc.quarkus.qcc.graph;

import java.util.HashMap;
import java.util.Map;

import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.interpret.InterpreterThread;
import cc.quarkus.qcc.type.QType;

public class MockContext implements Context  {

    MockContext(InterpreterThread thread) {
        this.thread = thread;
    }

    @Override
    public <V extends QType> void set(Node<V> node, V value) {
        this.values.put(node, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V extends QType> V get(Node<V> node) {
        return (V) this.values.get(node);
    }

    @Override
    public InterpreterThread thread() {
        return this.thread;
    }

    private final InterpreterThread thread;
    private Map<Node<?>, Object> values = new HashMap<>();
}
