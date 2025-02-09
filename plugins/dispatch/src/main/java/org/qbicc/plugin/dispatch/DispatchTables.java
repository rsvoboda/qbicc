package org.qbicc.plugin.dispatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.object.Function;
import org.qbicc.object.Linkage;
import org.qbicc.object.Section;
import org.qbicc.plugin.reachability.RTAInfo;
import org.qbicc.type.ArrayType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.generic.BaseTypeSignature;
import io.smallrye.common.constraint.Assert;
import org.jboss.logging.Logger;

public class DispatchTables {
    private static final Logger slog = Logger.getLogger("org.qbicc.plugin.dispatch.stats");
    private static final Logger tlog = Logger.getLogger("org.qbicc.plugin.dispatch.tables");

    private static final AttachmentKey<DispatchTables> KEY = new AttachmentKey<>();

    private final CompilationContext ctxt;
    private final Map<LoadedTypeDefinition, VTableInfo> vtables = new ConcurrentHashMap<>();
    private final Map<LoadedTypeDefinition, ITableInfo> itables = new ConcurrentHashMap<>();
    private GlobalVariableElement vtablesGlobal;

    // Used to accumulate statistics
    private int emittedVTableCount;
    private int emittedVTableBytes;
    private int emittedClassITableCount;
    private int emittedClassITableBytes;
    private int emittedInterfaceITableCount;
    private int emittedInterfaceITableBytes;
    private int emittedInterfaceITableUsefulBytes;

    private DispatchTables(final CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    public static DispatchTables get(CompilationContext ctxt) {
        DispatchTables dt = ctxt.getAttachment(KEY);
        if (dt == null) {
            dt = new DispatchTables(ctxt);
            DispatchTables appearing = ctxt.putAttachmentIfAbsent(KEY, dt);
            if (appearing != null) {
                dt = appearing;
            }
        }
        return dt;
    }

    public VTableInfo getVTableInfo(LoadedTypeDefinition cls) {
        return vtables.get(cls);
    }

    public ITableInfo getITableInfo(LoadedTypeDefinition cls) { return itables.get(cls); }

    void buildFilteredVTable(LoadedTypeDefinition cls) {
        tlog.debugf("Building VTable for %s", cls.getDescriptor());

        ArrayList<MethodElement> vtableVector = new ArrayList<>();
        for (MethodElement m: cls.getInstanceMethods()) {
            if (ctxt.wasEnqueued(m)) {
                tlog.debugf("\tadding reachable method %s%s", m.getName(), m.getDescriptor().toString());
                ctxt.registerEntryPoint(m);
                vtableVector.add(m);
            }
        }
        MethodElement[] vtable = vtableVector.toArray(MethodElement.NO_METHODS);

        String vtableName = "vtable-" + cls.getInternalName().replace('/', '.');
        TypeSystem ts = ctxt.getTypeSystem();
        CompoundType.Member[] functions = new CompoundType.Member[vtable.length];
        for (int i=0; i<vtable.length; i++) {
            FunctionType funType = ctxt.getFunctionTypeForElement(vtable[i]);
            functions[i] = ts.getCompoundTypeMember("m"+i, funType.getPointer(), i*ts.getPointerSize(), ts.getPointerAlignment());
        }
        CompoundType vtableType = ts.getCompoundType(CompoundType.Tag.STRUCT, vtableName, vtable.length * ts.getPointerSize(),
            ts.getPointerAlignment(), () -> List.of(functions));
        SymbolLiteral vtableSymbol = ctxt.getLiteralFactory().literalOfSymbol(vtableName, vtableType.getPointer());

        vtables.put(cls,new VTableInfo(vtable, vtableType, vtableSymbol));
    }

    void buildFilteredITableForInterface(LoadedTypeDefinition cls) {
        tlog.debugf("Building ITable for %s", cls.getDescriptor());

        ArrayList<MethodElement> itableVector = new ArrayList<>();
        for (MethodElement m: cls.getInstanceMethods()) {
            if (ctxt.wasEnqueued(m)) {
                tlog.debugf("\tadding invokable signature %s%s", m.getName(), m.getDescriptor().toString());
                itableVector.add(m);
            }
        }

        // Build the CompoundType for the ITable using the (arbitrary) order of selectors in itableVector
        MethodElement[] itable = itableVector.toArray(MethodElement.NO_METHODS);
        String itableName = "itable-" + cls.getInternalName().replace('/', '.');
        TypeSystem ts = ctxt.getTypeSystem();
        CompoundType.Member[] functions = new CompoundType.Member[itable.length];
        for (int i=0; i<itable.length; i++) {
            FunctionType funType = ctxt.getFunctionTypeForElement(itable[i]);
            functions[i] = ts.getCompoundTypeMember("m"+i, funType.getPointer(), i*ts.getPointerSize(), ts.getPointerAlignment());
        }
        CompoundType itableType = ts.getCompoundType(CompoundType.Tag.STRUCT, itableName, itable.length * ts.getPointerSize(),
            ts.getPointerAlignment(), () -> List.of(functions));

        // Define the GlobalVariable that will hold the itables[] for this interface.
        GlobalVariableElement.Builder builder = GlobalVariableElement.builder();
        builder.setName("qbicc_itables_array_"+itableType.getName());
        // Yet another table indexed by typeId (like the VTableGlobal) that will only contain entries for instantiated classes.
        // Use the VTableGlobal to set the size to avoid replicating that logic...
        builder.setType(ctxt.getTypeSystem().getArrayType(itableType.getPointer(), ((ArrayType)vtablesGlobal.getType()).getElementCount()));
        builder.setEnclosingType(cls);
        // void for now, but this is cheating terribly
        builder.setDescriptor(BaseTypeDescriptor.V);
        builder.setSignature(BaseTypeSignature.V);
        GlobalVariableElement itablesGlobal = builder.build();

        itables.put(cls, new ITableInfo(itable, itableType, cls, itablesGlobal));
    }

    void buildVTablesGlobal(DefinedTypeDefinition containingType) {
        GlobalVariableElement.Builder builder = GlobalVariableElement.builder();
        builder.setName("qbicc_vtables_array");
        // Invariant: typeIds are assigned from 1...N, where N is the number of reachable classes as computed by RTA 
        // plus 18 for 8 primitive types, void, 8 primitive arrays and reference array.
        builder.setType(ctxt.getTypeSystem().getArrayType(ctxt.getTypeSystem().getVoidType().getPointer().getPointer(), vtables.size()+19));  //TODO: communicate this +19 better
        builder.setEnclosingType(containingType);
        // void for now, but this is cheating terribly
        builder.setDescriptor(BaseTypeDescriptor.V);
        builder.setSignature(BaseTypeSignature.V);
        vtablesGlobal = builder.build();
    }

    void emitVTable(LoadedTypeDefinition cls) {
        if (!cls.isAbstract()) {
            VTableInfo info = getVTableInfo(cls);
            MethodElement[] vtable = info.getVtable();
            Section section = ctxt.getImplicitSection(cls);
            HashMap<CompoundType.Member, Literal> valueMap = new HashMap<>();
            for (int i = 0; i < vtable.length; i++) {
                FunctionType funType = ctxt.getFunctionTypeForElement(vtable[i]);
                if (vtable[i].isAbstract() || vtable[i].hasAllModifiersOf(ClassFile.ACC_NATIVE)) {
                    MethodElement stub = ctxt.getVMHelperMethod(vtable[i].isAbstract() ? "raiseAbstractMethodError" : "raiseUnsatisfiedLinkError");
                    Function stubImpl = ctxt.getExactFunction(stub);
                    SymbolLiteral literal = ctxt.getLiteralFactory().literalOfSymbol(stubImpl.getLiteral().getName(), stubImpl.getType().getPointer());
                    section.declareFunction(stub, stubImpl.getName(), stubImpl.getType());
                    valueMap.put(info.getType().getMember(i), ctxt.getLiteralFactory().bitcastLiteral(literal, ctxt.getFunctionTypeForElement(vtable[i]).getPointer()));
                } else {
                    Function impl = ctxt.getExactFunctionIfExists(vtable[i]);
                    if (impl == null) {
                        ctxt.error(vtable[i], "Missing method implementation for vtable of %s", cls.getInternalName());
                        continue;
                    }
                    if (!vtable[i].getEnclosingType().load().equals(cls)) {
                        section.declareFunction(vtable[i], impl.getName(), funType);
                    }
                    valueMap.put(info.getType().getMember(i), impl.getLiteral());
                }
            }
            Literal vtableLiteral = ctxt.getLiteralFactory().literalOf(info.getType(), valueMap);
            section.addData(null, info.getSymbol().getName(), vtableLiteral).setLinkage(Linkage.EXTERNAL);
            emittedVTableCount += 1;
            emittedVTableBytes += info.getType().getMemberCount() * ctxt.getTypeSystem().getPointerSize();
        }
    }

    void emitVTableTable(LoadedTypeDefinition jlo) {
        ArrayType vtablesGlobalType = ((ArrayType)vtablesGlobal.getType());
        Section section = ctxt.getImplicitSection(jlo);
        Literal[] vtableLiterals = new Literal[(int)vtablesGlobalType.getElementCount()];
        Literal zeroLiteral = ctxt.getLiteralFactory().zeroInitializerLiteralOfType(vtablesGlobalType.getElementType());
        Arrays.fill(vtableLiterals, zeroLiteral);
        for (Map.Entry<LoadedTypeDefinition, VTableInfo> e: vtables.entrySet()) {
            LoadedTypeDefinition cls = e.getKey();
            if (!cls.isAbstract()) {
                if (!cls.equals(jlo)) {
                    section.declareData(null, e.getValue().getSymbol().getName(), e.getValue().getType());
                }
                int typeId = cls.getTypeId();
                Assert.assertTrue(vtableLiterals[typeId].equals(zeroLiteral));
                vtableLiterals[typeId] = ctxt.getLiteralFactory().bitcastLiteral(e.getValue().getSymbol(), (WordType) vtablesGlobalType.getElementType());
            }
        }
        Literal vtablesGlobalValue = ctxt.getLiteralFactory().literalOf(vtablesGlobalType, List.of(vtableLiterals));
        section.addData(null, vtablesGlobal.getName(), vtablesGlobalValue);
        slog.debugf("Root vtable[] has %d slots (%d bytes)", vtableLiterals.length, vtableLiterals.length * ctxt.getTypeSystem().getPointerSize());
        slog.debugf("Emitted %d vtables with combined size of %d bytes", emittedVTableCount, emittedVTableBytes);
    }

    public GlobalVariableElement getVTablesGlobal() { return this.vtablesGlobal; }

    public void emitInterfaceTables(RTAInfo rtaInfo) {
        LiteralFactory lf = ctxt.getLiteralFactory();
        MethodElement icceStub = ctxt.getVMHelperMethod("raiseIncompatibleClassChangeError");
        Function icceImpl = ctxt.getExactFunction(icceStub);
        SymbolLiteral iceeLiteral = lf.literalOfSymbol(icceImpl.getLiteral().getName(), icceImpl.getLiteral().getType().getPointer());
        MethodElement ameStub = ctxt.getVMHelperMethod("raiseAbstractMethodError");
        Function ameImpl = ctxt.getExactFunction(ameStub);
        SymbolLiteral ameLiteral = lf.literalOfSymbol(ameImpl.getLiteral().getName(), ameImpl.getLiteral().getType().getPointer());
        final int pointerSize = ctxt.getTypeSystem().getPointerSize();
        for (Map.Entry<LoadedTypeDefinition, ITableInfo> entry: itables.entrySet()) {
            LoadedTypeDefinition currentInterface = entry.getKey();
            Section iSection = ctxt.getImplicitSection(currentInterface);
            ITableInfo itableInfo= entry.getValue();
            MethodElement[] itable = itableInfo.getItable();
            if (itable.length == 0) {
                continue; // If there are no invokable methods then this whole family of itables will never be referenced.
            }
            tlog.debugf("Emitting itable[] for %s", currentInterface.getDescriptor().getClassName());

            // First, construct an iTable of the right length of icceStubs
            iSection.declareFunction(icceStub, icceImpl.getName(), icceImpl.getType());
            HashMap<CompoundType.Member, Literal> stubMap = new HashMap<>();
            for (int i = 0; i < itable.length; i++) {
                FunctionType sigType = ctxt.getFunctionTypeForElement(itable[i]);
                stubMap.put(itableInfo.getType().getMember(i), lf.bitcastLiteral(iceeLiteral, sigType.getPointer()));
            }
            String stubsName = "qbicc_itable_icce_stubs_for_"+currentInterface.getInterfaceType().toFriendlyString();
            Literal stubsLiteral = lf.literalOf(itableInfo.getType(), stubMap);
            iSection.addData(null, stubsName, stubsLiteral).setLinkage(Linkage.INTERNAL);
            SymbolLiteral stubPtrLiteral = lf.literalOfSymbol(stubsName, itableInfo.getType());

            // Initialize all the slots of the rootTable to point to the icce itable.
            // This enables us to elide an explicit check for IncompatibleClassChangeError at the dispatch site.
            ArrayType rootType = (ArrayType)itableInfo.getGlobal().getType();
            Literal[] rootTable = new Literal[(int)rootType.getElementCount()];
            Arrays.fill(rootTable, stubPtrLiteral);
            emittedInterfaceITableCount += 1;
            emittedInterfaceITableBytes += rootTable.length * pointerSize;

            // Now build all the implementing class itables and update the rootTable
            rtaInfo.visitLiveImplementors(currentInterface, cls -> {
                if (!cls.isAbstract() && !cls.isInterface()) {
                    // Build the itable for an instantiable class
                    tlog.debugf("\temitting itable for %s", cls.getDescriptor().getClassName());
                    HashMap<CompoundType.Member, Literal> valueMap = new HashMap<>();
                    Section cSection = ctxt.getImplicitSection(cls);
                    for (int i = 0; i < itable.length; i++) {
                        MethodElement methImpl = cls.resolveMethodElementVirtual(itable[i].getName(), itable[i].getDescriptor());
                        FunctionType funType = ctxt.getFunctionTypeForElement(itable[i]);
                        FunctionType implType = ctxt.getFunctionTypeForElement(methImpl);
                        if (methImpl == null) {
                            cSection.declareFunction(icceStub, icceImpl.getName(), icceImpl.getType());
                            valueMap.put(itableInfo.getType().getMember(i), lf.bitcastLiteral(iceeLiteral, implType.getPointer()));
                        } else if (methImpl.isAbstract()) {
                            cSection.declareFunction(ameStub, ameImpl.getName(), ameImpl.getType());
                            valueMap.put(itableInfo.getType().getMember(i), lf.bitcastLiteral(ameLiteral, implType.getPointer()));
                        } else {
                            Function impl = ctxt.getExactFunctionIfExists(methImpl);
                            if (impl == null) {
                                ctxt.error(methImpl, "Missing method implementation for vtable of %s", cls.getInternalName());
                                continue;
                            }
                            if (!methImpl.getEnclosingType().load().equals(cls)) {
                                cSection.declareFunction(methImpl, impl.getName(), implType);
                            }
                            valueMap.put(itableInfo.getType().getMember(i), impl.getLiteral());
                        }
                    }
                    // Emit itable and refer to it in rootTable
                    String tableName = "qbicc_itable_impl_"+cls.getInternalName().replace('/', '.')+"_for_"+itableInfo.getGlobal().getName();
                    Literal itableLiteral = lf.literalOf(itableInfo.getType(), valueMap);
                    cSection.addData(null, tableName, itableLiteral).setLinkage(Linkage.EXTERNAL);
                    iSection.declareData(null, tableName, itableInfo.getType());
                    rootTable[cls.getTypeId()] = lf.literalOfSymbol(tableName, itableInfo.getType());
                    emittedClassITableCount += 1;
                    emittedClassITableBytes += itable.length * pointerSize;
                    emittedInterfaceITableUsefulBytes += pointerSize;
                }
            });

            // Finally emit the root iTable[] for the interface
            iSection.addData(null, itableInfo.getGlobal().getName(), lf.literalOf(rootType, List.of(rootTable)));
        }
        slog.debugf("Emitted iTables for %d class+interface combinations consuming %d bytes", emittedClassITableCount, emittedClassITableBytes);
        slog.debugf("Emitted iTables for %d interfaces consuming %d bytes; %d non-icce bytes (%s)",
            emittedInterfaceITableCount, emittedInterfaceITableBytes, emittedInterfaceITableUsefulBytes,
            String.format("%.2f%%", (float)emittedInterfaceITableUsefulBytes/(float)emittedInterfaceITableBytes*100f));
    }


    public int getVTableIndex(MethodElement target) {
        LoadedTypeDefinition definingType = target.getEnclosingType().load();
        VTableInfo info = getVTableInfo(definingType);
        if (info != null) {
            MethodElement[] vtable = info.getVtable();
            for (int i = 0; i < vtable.length; i++) {
                if (target.getName().equals(vtable[i].getName()) && target.getDescriptor().equals(vtable[i].getDescriptor())) {
                    return i;
                }
            }
        }
        ctxt.error("No vtable entry found for "+target);
        return 0;
    }

    public int getITableIndex(MethodElement target) {
        LoadedTypeDefinition definingType = target.getEnclosingType().load();
        ITableInfo info = getITableInfo(definingType);
        if (info != null) {
            MethodElement[] itable = info.getItable();
            for (int i = 0; i < itable.length; i++) {
                if (target.getName().equals(itable[i].getName()) && target.getDescriptor().equals(itable[i].getDescriptor())) {
                    return i;
                }
            }
        }
        ctxt.error("No itable entry found for "+target);
        return 0;
    }

    public static final class VTableInfo {
        private final MethodElement[] vtable;
        private final CompoundType type;
        private final SymbolLiteral symbol;

        VTableInfo(MethodElement[] vtable, CompoundType type, SymbolLiteral symbol) {
            this.vtable = vtable;
            this.type = type;
            this.symbol = symbol;
        }

        public MethodElement[] getVtable() { return vtable; }
        public SymbolLiteral getSymbol() { return symbol; }
        public CompoundType getType() { return  type; }
    }

    public static final class ITableInfo {
        private final LoadedTypeDefinition myInterface;
        private final MethodElement[] itable;
        private final CompoundType type;
        private final GlobalVariableElement global;

        ITableInfo(MethodElement[] itable, CompoundType type, LoadedTypeDefinition myInterface, GlobalVariableElement global) {
            this.myInterface = myInterface;
            this.itable = itable;
            this.type = type;
            this.global = global;
        }

        public LoadedTypeDefinition getInterface() { return myInterface; }
        public MethodElement[] getItable() { return itable; }
        public CompoundType getType() { return type; }
        public GlobalVariableElement getGlobal() { return global; }
    }
}
