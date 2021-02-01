package cc.quarkus.qcc.plugin.instanceofcheckcast;

import java.util.function.Consumer;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.plugin.reachability.RTAInfo;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;

/**
 * Build Cohen's Display for Super types for all classes present in
 * the RTAInfo.
 */
public class SupersDisplayBuilder implements Consumer<CompilationContext> {
    
    @Override
    public void accept(CompilationContext ctxt) {
        RTAInfo info = RTAInfo.get(ctxt);
        SupersDisplayTables tables = SupersDisplayTables.get(ctxt);
        // Starting from java.lang.Object walk down the live class hierarchy and
        // compute supers display that contain just the classes where RTAInfo
        // marks the class as live
        ClassContext classContext = ctxt.getBootstrapClassContext();
        DefinedTypeDefinition jloDef = classContext.findDefinedType("java/lang/Object");
        ValidatedTypeDefinition jlo = jloDef.validate();
        tables.buildSupersDisplay(jlo);
        info.visitLiveSubclasses(jlo, cls -> tables.buildSupersDisplay(cls));
        tables.statistics();
    }
}