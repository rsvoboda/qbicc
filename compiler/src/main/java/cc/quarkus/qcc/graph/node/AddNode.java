package cc.quarkus.qcc.graph.node;

import java.util.function.BiFunction;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.QIntegral;
import cc.quarkus.qcc.type.QNumeric;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

public class AddNode<V extends QNumeric> extends BinaryNode<V,V> {

    public AddNode(Graph<?> graph, ControlNode<?> control, TypeDescriptor<V> outType, Node<V> lhs, Node<V> rhs, BiFunction<V,V,V> adder) {
        super(graph, control, outType);
        setLHS(lhs);
        setRHS(rhs);
        this.adder = adder;
    }

    @Override
    public V getValue(Context context) {
        V lhsValue = getLHSValue(context);
        V rhsValue = getRHSValue(context);
        //return (V) lhsValue.add(rhsValue);
        return adder.apply(lhsValue, rhsValue);
    }

    private final BiFunction<V, V, V> adder;
}
