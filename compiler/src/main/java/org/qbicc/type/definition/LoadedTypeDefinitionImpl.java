package org.qbicc.type.definition;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.qbicc.type.InterfaceObjectType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.definition.element.NestedClassElement;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
final class LoadedTypeDefinitionImpl extends DelegatingDefinedTypeDefinition implements LoadedTypeDefinition {
    private final ObjectType type;
    private final DefinedTypeDefinitionImpl delegate;
    private final LoadedTypeDefinition superType;
    private final LoadedTypeDefinition[] interfaces;
    private final ArrayList<FieldElement> fields;
    private final MethodElement[] methods;
    private final MethodElement[] instanceMethods;
    private final ConstructorElement[] ctors;
    private final InitializerElement init;
    private final FieldSet staticFieldSet;
    private final FieldSet instanceFieldSet;
    private final NestedClassElement enclosingClass;
    private final NestedClassElement[] enclosedClasses;
    private int typeId = -1;
    private int maximumSubtypeId = -1;
    private final boolean hasDefaultMethods;
    private final boolean declaresDefaultMethods;

    LoadedTypeDefinitionImpl(final DefinedTypeDefinitionImpl delegate, final LoadedTypeDefinition superType, final LoadedTypeDefinition[] interfaces, final ArrayList<FieldElement> fields, final MethodElement[] methods, final MethodElement[] instanceMethods, final ConstructorElement[] ctors, final InitializerElement init, final NestedClassElement enclosingClass, final NestedClassElement[] enclosedClasses) {
        this.delegate = delegate;
        this.superType = superType;
        this.interfaces = interfaces;
        this.fields = fields;
        this.methods = methods;
        this.instanceMethods = instanceMethods;
        this.ctors = ctors;
        this.init = init;
        this.enclosingClass = enclosingClass;
        this.enclosedClasses = enclosedClasses;
        int interfaceCnt = interfaces.length;
        InterfaceObjectType[] interfaceTypes = new InterfaceObjectType[interfaceCnt];
        for (int i = 0; i < interfaceCnt; i ++) {
            interfaceTypes[i] = interfaces[i].getInterfaceType();
        }
        if (isInterface()) {
            type = getContext().getTypeSystem().generateInterfaceObjectType(delegate, List.of(interfaceTypes));
        } else {
            type = getContext().getTypeSystem().generateClassObjectType(delegate, superType == null ? null : superType.getClassType(), List.of(interfaceTypes));
        }
        instanceFieldSet = new FieldSet(this, false);
        staticFieldSet = new FieldSet(this, true);

        /* Walk methods of interfaces looking for default methods */
        boolean buildDeclaresDefaultMethods = false;
        if (isInterface()) {
            for (MethodElement method : instanceMethods) {
                if (!method.isStatic() && !method.isAbstract()) {
                    buildDeclaresDefaultMethods = true;
                }
            }
        }
        declaresDefaultMethods = buildDeclaresDefaultMethods;

        /* For both classes & interfaces, they either inherit "hasDefaultMethods"
         * from their superclass or interfaces, or compute it (aka declaresDefaultMethods)
         */
        boolean buildHasDefaultMethods = declaresDefaultMethods;
        if (superType != null) { /* Object */
            buildHasDefaultMethods |= superType.hasDefaultMethods(); 
        }
        if (!buildHasDefaultMethods) {
            for (LoadedTypeDefinition ltd : interfaces) {
                buildHasDefaultMethods = ltd.hasDefaultMethods();
            }
        }
        hasDefaultMethods = buildHasDefaultMethods;
    }

    // delegates

    public DefinedTypeDefinition getDelegate() {
        return delegate;
    }

    public LoadedTypeDefinition load() {
        return this;
    }

    // local methods

    public ObjectType getType() {
        return type;
    }

    public LoadedTypeDefinition getSuperClass() {
        return superType;
    }

    public LoadedTypeDefinition getInterface(final int index) throws IndexOutOfBoundsException {
        return interfaces[index];
    }

    public LoadedTypeDefinition[] getInterfaces() {
        return interfaces.clone();
    }

    public void forEachInterfaceFullImplementedSet(Consumer<LoadedTypeDefinition> function) {
        for (LoadedTypeDefinition i : interfaces) {
            function.accept(i);
        }
        // Walk up the heirarchy and visit each inteface from the the superclass
        LoadedTypeDefinition superClass = getSuperClass();
        if (superClass != null) {
            superClass.forEachInterfaceFullImplementedSet(function);
        }
    }

    public MethodElement[] getInstanceMethods() { return instanceMethods; }

    public FieldSet getStaticFieldSet() {
        return staticFieldSet;
    }

    public NestedClassElement getEnclosingNestedClass() {
        return enclosingClass;
    }

    public int getEnclosedNestedClassCount() {
        return enclosedClasses.length;
    }

    public NestedClassElement getEnclosedNestedClass(final int index) throws IndexOutOfBoundsException {
        return enclosedClasses[index];
    }

    public FieldSet getInstanceFieldSet() {
        return instanceFieldSet;
    }

    public int getFieldCount() {
        return fields.size();
    }

    public FieldElement getField(final int index) {
        return fields.get(index);
    }

    public void injectField(final FieldElement field) {
        Assert.checkNotNullParam("field", field);
        if ((field.getModifiers() & ClassFile.I_ACC_HIDDEN) == 0) {
            throw new IllegalArgumentException("Injected fields must be hidden");
        }
        fields.add(field);
    }

    public MethodElement getMethod(final int index) {
        return methods[index];
    }

    public ConstructorElement getConstructor(final int index) {
        return ctors[index];
    }

    public InitializerElement getInitializer() {
        return init;
    }

    // next stage

    public int getTypeId() {
        return typeId;
    }

    public int getMaximumSubtypeId() {
        return maximumSubtypeId;
    }

    public boolean isTypeIdValid() {
        return typeId != -1;
    }

    public void assignTypeId(int myTypeId) {
        // typeId shouldn't hae already been assigned
        Assert.assertTrue(typeId == -1);
        typeId = myTypeId;
    }

    public void assignMaximumSubtypeId(int subTypeId) {
        // maximumSubtypeId shouldn't hae already been assigned
        Assert.assertTrue(maximumSubtypeId == -1);
        maximumSubtypeId = subTypeId;
    }

    public boolean declaresDefaultMethods() {
        // only interfaces can declare default methods
        Assert.assertTrue(isInterface() || (declaresDefaultMethods == false));
        return declaresDefaultMethods;
    }

    public boolean hasDefaultMethods() {
        return hasDefaultMethods;
    }
}

