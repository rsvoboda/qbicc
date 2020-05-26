package cc.quarkus.qcc.type.definition;

import java.util.List;

import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;
import cc.quarkus.qcc.type.universe.Universe;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

public class MethodDefinitionNode<V> extends MethodNode implements MethodDefinition<V> {

    //public MethodDefinitionNode(TypeDefinitionNode typeDefinition, int access, String name, String descriptor, String signature, String[] exceptions) {
    public MethodDefinitionNode(TypeDefinitionNode typeDefinition, int access, String name, MethodDescriptor<V> methodDescriptor, String signature, String[] exceptions) {
        super(Universe.ASM_VERSION, access, name, methodDescriptor.getDescriptor(), signature, exceptions);
        this.typeDefinition = typeDefinition;

        //MethodDescriptorParser parser = new MethodDescriptorParser(typeDefinition.getUniverse(), typeDefinition, name, descriptor, isStatic());
        this.methodDescriptor = methodDescriptor;
        //this.signature = signature;

    }

    public MethodNode getMethodNode() {
        return this;
    }

    @Override
    public String getDescriptor() {
        return this.methodDescriptor.getDescriptor();
    }

    @Override
    public List<TryCatchBlockNode> getTryCatchBlocks() {
        return this.tryCatchBlocks;
    }

    @Override
    public InsnList getInstructions() {
        return this.instructions;
    }

    @Override
    public int getMaxLocals() {
        return this.maxLocals;
    }

    @Override
    public int getMaxStack() {
        return this.maxStack;
    }

    @Override
    public List<TypeDescriptor<?>> getParamTypes() {
        return this.methodDescriptor.getParamTypes();
    }

    @Override
    public TypeDescriptor<V> getReturnType() {
        return this.methodDescriptor.getReturnType();
    }

    @Override
    public TypeDefinition getOwner() {
        return this.typeDefinition;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public boolean isStatic() {
        return ( getMethodNode().access & Opcodes.ACC_STATIC ) != 0;
    }

    @Override
    public boolean isSynchronized() {
        return ( getMethodNode().access & Opcodes.ACC_SYNCHRONIZED ) != 0;
    }

    @Override
    public TypeDefinition getTypeDefinition() {
        return this.typeDefinition;
    }

    @Override
    public String toString() {
        return this.typeDefinition + " " + this.name + this.desc;
    }

    private final MethodDescriptor<V> methodDescriptor;

    private final TypeDefinitionNode typeDefinition;

}
